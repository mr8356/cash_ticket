package com.cashticket.service;

import com.cashticket.entity.*;
import com.cashticket.repository.AuctionRepository;
import com.cashticket.repository.BidRepository;
import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;

    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String AUCTION_BIDDERS_KEY_PREFIX = "auction:bidders:";
    private static final String AUCTION_END_TIME_KEY_PREFIX = "auction:endtime:";

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

        // 경매 종료 시간 확인
        String endTimeStr = redisTemplate.opsForValue().get(endTimeKey);
        if (endTimeStr == null || LocalDateTime.parse(endTimeStr).isBefore(LocalDateTime.now())) {
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

        // 새로운 입찰 추가
        String bidId = userId + ":" + System.currentTimeMillis();
        redisTemplate.opsForZSet().add(auctionKey, bidId, bidAmount);
        redisTemplate.opsForHash().put(biddersKey, bidId, userId.toString());

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
    public Long getWinner(Long concertId) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;

        Set<String> winningBids = redisTemplate.opsForZSet().range(auctionKey, -1, -1);
        if (winningBids.isEmpty()) {
            return null;
        }

        String winningBidId = winningBids.iterator().next();
        Object winnerId = redisTemplate.opsForHash().get(biddersKey, winningBidId);
        
        return winnerId != null ? Long.parseLong(winnerId.toString()) : null;
    }

    // 경매 종료 처리
    @Transactional
    public boolean endAuction(Long concertId) {
        String auctionKey = AUCTION_KEY_PREFIX + concertId;
        String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;
        String endTimeKey = AUCTION_END_TIME_KEY_PREFIX + concertId;

        // 최종 입찰자 정보 조회
        Long winnerId = getWinner(concertId);
        if (winnerId == null) {
            return false;
        }

        // 최종 입찰가 조회
        int finalBid = getCurrentHighestBid(concertId);

        // Auction 엔티티 업데이트
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        auction.setStatus(AuctionStatusEnum.CLOSED);
        auctionRepository.save(auction);

        // AuctionResult 엔티티 생성
        User winner = userRepository.findById(winnerId)
                .orElseThrow(() -> new IllegalArgumentException("Winner not found"));
        
        AuctionResult result = AuctionResult.builder()
                .auction(auction)
                .user(winner)
                .finalBidAmount((long) finalBid)
                .status(AuctionResultStatusEnum.WINNER)
                .seatNo(1) // 좌석 번호는 실제 구현에 따라 다르게 처리
                .build();

        // Redis 데이터 삭제
        redisTemplate.delete(auctionKey);
        redisTemplate.delete(biddersKey);
        redisTemplate.delete(endTimeKey);

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