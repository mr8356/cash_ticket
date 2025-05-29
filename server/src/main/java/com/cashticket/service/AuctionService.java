package com.cashticket.service;

import com.cashticket.entity.*;
import com.cashticket.repository.AuctionRepository;
import com.cashticket.repository.AuctionResultRepository;
import com.cashticket.repository.BidRepository;
import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuctionRepository auctionRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final BidRepository bidRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;

    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String AUCTION_BIDDERS_KEY_PREFIX = "auction:bidders:";
    private static final String AUCTION_END_TIME_KEY_PREFIX = "auction:endtime:";
    private static final String AUCTION_BID_COUNT_PREFIX = "auction:bidcount:";
    private static final int MAX_BID_COUNT = 3;

    // 매일 자정에 실행되어 다음날 시작할 경매를 시작
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void scheduleAuctionStart() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        // 내일 시작할 경매 목록 조회
        Set<Concert> upcomingConcerts = concertRepository.findByDateTimeBetween(
            tomorrow, tomorrow.plusDays(1));
        
        for (Concert concert : upcomingConcerts) {
            // 경매 시작 (시작가와 종료 시간 설정)
            startAuction(concert.getId(), 10000, // 기본 시작가 10000원
                concert.getDateTime().minusHours(1)); // 공연 시작 1시간 전에 경매 종료
        }
    }

    // 1분마다 실행되어 종료 시간이 된 경매를 종료
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduleAuctionEnd() {
        Set<Long> activeAuctions = getActiveAuctions();
        LocalDateTime now = LocalDateTime.now();

        for (Long concertId : activeAuctions) {
            LocalDateTime endTime = getAuctionEndTime(concertId);
            if (endTime != null && endTime.isBefore(now)) {
                endAuction(concertId);
            }
        }
    }

    // 경매 시작
    @Transactional
    public void startAuction(Long concertId, int startPrice, LocalDateTime endTime) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));

        // Auction 엔티티 생성
        Auction auction = Auction.builder()
                .concert(concert)
                .availableSeats(1) // 경매 티켓 수량
                .status(AuctionStatusEnum.OPEN)
                .startTime(LocalDateTime.now())
                .endTime(endTime)
                .build();
        auctionRepository.save(auction);

        // Redis 초기화
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;

        redisTemplate.opsForZSet().add(auctionKey, "initial", startPrice);
        redisTemplate.opsForValue().set(endTimeKey, endTime.toString());
    }

    // 입찰 처리
    @Transactional
    public boolean placeBid(Long concertId, Long userId, int bidAmount) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;
        String bidCountKey = AUCTION_BID_COUNT_PREFIX + concertId + ":" + userId;

        // 경매 종료 시간 확인
        String endTimeStr = redisTemplate.opsForValue().get(endTimeKey);
        if (endTimeStr == null || LocalDateTime.parse(endTimeStr).isBefore(LocalDateTime.now())) {
            return false;
        }

        // 입찰 횟수 확인
        String bidCountStr = redisTemplate.opsForValue().get(bidCountKey);
        int bidCount = bidCountStr != null ? Integer.parseInt(bidCountStr) : 0;
        if (bidCount >= MAX_BID_COUNT) {
            return false;
        }

        // 현재 최고가 확인
        Set<String> currentBids = redisTemplate.opsForZSet().range(auctionKey, -1, -1);
        if (currentBids.isEmpty()) {
            return false;
        }

        Double currentHighestBid = redisTemplate.opsForZSet().score(auctionKey, currentBids.iterator().next());
        if (currentHighestBid != null && bidAmount <= currentHighestBid) {
            return false;
        }

        // 사용자의 가장 최근 이전 입찰 삭제
        Set<String> allBids = redisTemplate.opsForZSet().range(auctionKey, 0, -1);
        String lastUserBidId = null;
        if (allBids != null) {
            for (String bidId : allBids) {
                if (bidId.startsWith(userId + ":")) {
                    lastUserBidId = bidId;
                }
            }
            if (lastUserBidId != null) {
                redisTemplate.opsForZSet().remove(auctionKey, lastUserBidId);
                redisTemplate.opsForHash().delete(biddersKey, lastUserBidId);
            }
        }

        // 새로운 입찰 추가
        String bidId = userId + ":" + System.currentTimeMillis();
        redisTemplate.opsForZSet().add(auctionKey, bidId, bidAmount);
        redisTemplate.opsForHash().put(biddersKey, bidId, userId.toString());
        
        // 입찰 횟수 증가
        redisTemplate.opsForValue().increment(bidCountKey);

        // Bid 엔티티 저장
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bid bid = Bid.builder()
                .auction(auction)
                .user(user)
                .amount((long) bidAmount)
                .bidTime(LocalDateTime.now())
                .build();
        bidRepository.save(bid);

        return true;
    }

    // 현재 최고가 조회
    public int getCurrentHighestBid(Long concertId) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        Set<String> currentBids = redisTemplate.opsForZSet().range(auctionKey, -1, -1);
        
        if (currentBids.isEmpty()) {
            return 0;
        }

        Double highestBid = redisTemplate.opsForZSet().score(auctionKey, currentBids.iterator().next());
        return highestBid != null ? highestBid.intValue() : 0;
    }

    // 최종 입찰자 정보 조회
    public List<Long> getWinners(Long concertId) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;

        // Auction 엔티티에서 availableSeats 조회
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        int availableSeats = auction.getAvailableSeats();

        // 상위 입찰자들 조회
        Set<String> winningBids = redisTemplate.opsForZSet().range(auctionKey, -availableSeats, -1);
        if (winningBids == null || winningBids.isEmpty()) {
            return Collections.emptyList();
        }

        // 입찰자 ID 목록 반환
        return winningBids.stream()
                .map(bidId -> {
                    Object winnerId = redisTemplate.opsForHash().get(biddersKey, bidId);
                    return winnerId != null ? Long.parseLong(winnerId.toString()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 경매 종료 처리
    @Transactional
    public boolean endAuction(Long concertId) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;

        // 최종 입찰자들 정보 조회
        List<Long> winnerIds = getWinners(concertId);
        if (winnerIds.isEmpty()) {
            return false;
        }

        // 최종 입찰가 조회
        int finalBid = getCurrentHighestBid(concertId);

        // Auction 엔티티 업데이트
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        auction.setStatus(AuctionStatusEnum.CLOSED);
        auctionRepository.save(auction);

        // 각 승자에 대해 AuctionResult 엔티티 생성
        for (int i = 0; i < winnerIds.size(); i++) {
            User winner = userRepository.findById(winnerIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Winner not found"));
            
            AuctionResult result = AuctionResult.builder()
                    .auction(auction)
                    .user(winner)
                    .finalBidAmount((long) finalBid)
                    .status(AuctionResultStatusEnum.WINNER)
                    .seatNo(i + 1) // 좌석 번호는 1부터 순차적으로 할당
                    .build();
            
            auctionResultRepository.save(result);
        }

        // Redis 데이터 삭제
        redisTemplate.delete(auctionKey);
        redisTemplate.delete(biddersKey);
        redisTemplate.delete(endTimeKey);
        
        // 입찰 횟수 데이터 삭제
        String bidCountPattern = AUCTION_BID_COUNT_PREFIX + concertId + ":*";
        Set<String> bidCountKeys = redisTemplate.keys(bidCountPattern);
        if (bidCountKeys != null && !bidCountKeys.isEmpty()) {
            redisTemplate.delete(bidCountKeys);
        }

        return true;
    }

    // 경매 상태 조회
    public boolean isAuctionActive(Long concertId) {
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;
        String endTimeStr = redisTemplate.opsForValue().get(endTimeKey);
        
        if (endTimeStr == null) {
            return false;
        }

        return LocalDateTime.parse(endTimeStr).isAfter(LocalDateTime.now());
    }

    // 활성화된 경매 목록 조회
    public Set<Long> getActiveAuctions() {
        String pattern = AUCTION_END_TIME_KEY_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        return keys.stream()
                .map(key -> Long.parseLong(key.replace(AUCTION_END_TIME_KEY_PREFIX, "")))
                .filter(this::isAuctionActive)
                .collect(Collectors.toSet());
    }

    // 경매 종료 시간 조회
    public LocalDateTime getAuctionEndTime(Long concertId) {
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;
        String endTimeStr = redisTemplate.opsForValue().get(endTimeKey);
        
        if (endTimeStr == null) {
            return null;
        }
        
        return LocalDateTime.parse(endTimeStr);
    }
} 