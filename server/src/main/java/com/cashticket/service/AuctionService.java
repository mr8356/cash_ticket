package com.cashticket.service;

import com.cashticket.entity.*;
import com.cashticket.exception.AuctionException;
import com.cashticket.exception.BidException;
import com.cashticket.repository.AuctionRepository;
import com.cashticket.repository.AuctionResultRepository;
import com.cashticket.repository.BidRepository;
import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

        redisTemplate.opsForZSet().add(auctionKey, "initial", startPrice);
    }

    // 입찰 처리
    @Transactional
    public boolean placeBid(Long concertId, Long userId, int bidAmount) {
        log.info("입찰 시도 - 콘서트ID: {}, 사용자ID: {}, 입찰금액: {}", concertId, userId, bidAmount);
        
        try {
            String auctionKey = AUCTION_KEY_PREFIX + concertId;
            String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;
            String bidCountKey = AUCTION_BID_COUNT_PREFIX + concertId + ":" + userId;

            // 경매 상태 확인 (Redis 대신 DB 사용)
            if (!isAuctionActive(concertId)) {
                log.warn("경매가 비활성 상태 - 콘서트ID: {}", concertId);
                throw new AuctionException("경매가 종료되었거나 존재하지 않습니다.");
            }

            // 입찰 횟수 확인
            String bidCountStr = redisTemplate.opsForValue().get(bidCountKey);
            int bidCount = bidCountStr != null ? Integer.parseInt(bidCountStr) : 0;
            log.debug("현재 입찰 횟수 - 사용자ID: {}, 횟수: {}/{}", userId, bidCount, MAX_BID_COUNT);
            
            if (bidCount >= MAX_BID_COUNT) {
                log.warn("입찰 횟수 초과 - 사용자ID: {}, 횟수: {}", userId, bidCount);
                throw new BidException("입찰 가능 횟수를 초과했습니다. (최대 " + MAX_BID_COUNT + "회)");
            }

            // 현재 최고가 확인
            Set<String> currentBids = redisTemplate.opsForZSet().range(auctionKey, -1, -1);
            if (currentBids.isEmpty()) {
                log.warn("경매 정보 없음 - 콘서트ID: {}", concertId);
                throw new AuctionException("경매 정보를 찾을 수 없습니다.");
            }

            Double currentHighestBid = redisTemplate.opsForZSet().score(auctionKey, currentBids.iterator().next());
            log.debug("현재 최고가: {}, 입찰 시도 금액: {}", currentHighestBid, bidAmount);
            
            if (currentHighestBid != null && bidAmount <= currentHighestBid) {
                log.warn("입찰 금액 부족 - 최고가: {}, 시도 금액: {}", currentHighestBid, bidAmount);
                throw new BidException("입찰 금액은 현재 최고가(" + currentHighestBid.intValue() + "원)보다 높아야 합니다.");
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
                    log.debug("이전 입찰 삭제 - 사용자ID: {}, 입찰ID: {}", userId, lastUserBidId);
                    redisTemplate.opsForZSet().remove(auctionKey, lastUserBidId);
                    redisTemplate.opsForHash().delete(biddersKey, lastUserBidId);
                }
            }

            // 새로운 입찰 추가
            String bidId = userId + ":" + System.currentTimeMillis();
            redisTemplate.opsForZSet().add(auctionKey, bidId, bidAmount);
            redisTemplate.opsForHash().put(biddersKey, bidId, userId.toString());
            log.debug("새 입찰 추가 - 입찰ID: {}, 금액: {}", bidId, bidAmount);
            
            // 입찰 횟수 증가
            redisTemplate.opsForValue().increment(bidCountKey);

            // Bid 엔티티 저장
            Auction auction = auctionRepository.findByConcertId(concertId)
                    .orElseThrow(() -> new AuctionException("경매를 찾을 수 없습니다: " + concertId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuctionException("사용자를 찾을 수 없습니다: " + userId));

            Bid bid = Bid.builder()
                    .auction(auction)
                    .user(user)
                    .amount((long) bidAmount)
                    .bidTime(LocalDateTime.now())
                    .build();
            bidRepository.save(bid);
            log.info("입찰 성공 - 콘서트ID: {}, 사용자ID: {}, 금액: {}", concertId, userId, bidAmount);

            return true;
        } catch (AuctionException | BidException e) {
            log.warn("입찰 실패 - 콘서트ID: {}, 사용자ID: {}, 사유: {}", concertId, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("입찰 처리 중 오류 - 콘서트ID: {}, 사용자ID: {}, 오류: {}", concertId, userId, e.getMessage(), e);
            throw new RuntimeException("입찰 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 현재 최고가 조회
    public int getCurrentHighestBid(Long concertId) {
        log.debug("최고가 조회 - 콘서트ID: {}", concertId);
        
        try {
            // 경매가 존재하는지 먼저 확인
            if (!isAuctionActive(concertId)) {
                log.warn("경매가 비활성 상태 - 최고가 조회 실패, 콘서트ID: {}", concertId);
                throw new AuctionException("경매가 존재하지 않거나 종료되었습니다.");
            }
            
            String auctionKey = AUCTION_KEY_PREFIX + concertId;
            Set<String> currentBids = redisTemplate.opsForZSet().range(auctionKey, -1, -1);
            
            if (currentBids.isEmpty()) {
                log.debug("입찰 없음 - 콘서트ID: {}", concertId);
                return 0;
            }

            Double highestBid = redisTemplate.opsForZSet().score(auctionKey, currentBids.iterator().next());
            int result = highestBid != null ? highestBid.intValue() : 0;
            log.debug("최고가 조회 결과 - 콘서트ID: {}, 최고가: {}", concertId, result);
            
            return result;
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            log.error("최고가 조회 중 오류 - 콘서트ID: {}, 오류: {}", concertId, e.getMessage(), e);
            throw new RuntimeException("최고가 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 최종 입찰자 정보 조회
    public List<Long> getWinners(Long concertId) {
        try {
            String auctionKey = AUCTION_KEY_PREFIX + concertId;
            String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;

            // Auction 엔티티에서 availableSeats 조회
            Auction auction = auctionRepository.findByConcertId(concertId)
                    .orElseThrow(() -> new AuctionException("경매를 찾을 수 없습니다: " + concertId));
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
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("입찰자 정보 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 경매 종료 처리
    @Transactional
    public boolean endAuction(Long concertId) {
        try {
            String auctionKey = AUCTION_KEY_PREFIX + concertId;
            String biddersKey = AUCTION_BIDDERS_KEY_PREFIX + concertId;

            // 경매가 존재하는지 확인
            Auction auction = auctionRepository.findByConcertId(concertId)
                    .orElseThrow(() -> new AuctionException("경매를 찾을 수 없습니다: " + concertId));

            // 이미 종료된 경매인지 확인
            if (auction.getStatus() == AuctionStatusEnum.CLOSED) {
                throw new AuctionException("이미 종료된 경매입니다.");
            }

            // 최종 입찰자들 정보 조회
            List<Long> winnerIds = getWinners(concertId);
            if (winnerIds.isEmpty()) {
                throw new AuctionException("입찰자가 없어 경매를 종료할 수 없습니다.");
            }

            // 최종 입찰가 조회
            int finalBid = getCurrentHighestBid(concertId);

            // Auction 엔티티 업데이트
            auction.setStatus(AuctionStatusEnum.CLOSED);
            auctionRepository.save(auction);

            // 각 승자에 대해 AuctionResult 엔티티 생성
            for (int i = 0; i < winnerIds.size(); i++) {
                final int seatNo = i + 1; // 좌석 번호는 1부터 순차적으로 할당
                final Long winnerId = winnerIds.get(i);
                
                User winner = userRepository.findById(winnerId)
                        .orElseThrow(() -> new AuctionException("승자를 찾을 수 없습니다: " + winnerId));
                
                AuctionResult result = AuctionResult.builder()
                        .auction(auction)
                        .user(winner)
                        .finalBidAmount((long) finalBid)
                        .status(AuctionResultStatusEnum.WINNER)
                        .seatNo(seatNo)
                        .build();
                
                auctionResultRepository.save(result);
            }

            // Redis 데이터 삭제
            redisTemplate.delete(auctionKey);
            redisTemplate.delete(biddersKey);
            
            // 입찰 횟수 데이터 삭제
            String bidCountPattern = AUCTION_BID_COUNT_PREFIX + concertId + ":*";
            Set<String> bidCountKeys = redisTemplate.keys(bidCountPattern);
            if (bidCountKeys != null && !bidCountKeys.isEmpty()) {
                redisTemplate.delete(bidCountKeys);
            }

            return true;
        } catch (AuctionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("경매 종료 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 경매 상태 조회
    public boolean isAuctionActive(Long concertId) {
        log.debug("경매 상태 조회 - 콘서트ID: {}", concertId);
        
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElse(null);
        
        if (auction == null) {
            log.debug("경매 없음 - 자동으로 경매 생성 - 콘서트ID: {}", concertId);
            // 경매가 없으면 자동으로 생성
            try {
                Concert concert = concertRepository.findById(concertId)
                        .orElseThrow(() -> new AuctionException("콘서트를 찾을 수 없습니다: " + concertId));
                
                // 기본 경매 설정 (시작가 10,000원, 공연 시작 1시간 전 종료)
                startAuction(concertId, 10000, concert.getDateTime().minusHours(1));
                
                log.info("경매 자동 생성 완료 - 콘서트ID: {}", concertId);
                return true;
            } catch (Exception e) {
                log.error("경매 자동 생성 실패 - 콘서트ID: {}, 오류: {}", concertId, e.getMessage());
                return false;
            }
        }

        boolean isActive = auction.getEndTime().isAfter(LocalDateTime.now());
        log.debug("경매 상태 - 콘서트ID: {}, 활성: {}, 종료시간: {}", concertId, isActive, auction.getEndTime());
        
        return isActive;
    }

    // 활성화된 경매 목록 조회
    public Set<Long> getActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();
        
        return auctionRepository.findByStatusAndEndTimeAfter(AuctionStatusEnum.OPEN, now)
                .stream()
                .map(auction -> auction.getConcert().getId())
                .collect(Collectors.toSet());
    }

    // 경매 종료 시간 조회
    public LocalDateTime getAuctionEndTime(Long concertId) {
        Auction auction = auctionRepository.findByConcertId(concertId)
                .orElse(null);
        
        if (auction == null) {
            return null;
        }
        
        return auction.getEndTime();
    }

    // 결제 시 검증 전용 메서드
    @Transactional(readOnly = true)
    public void validateBid(Long concertId, Long userId, int bidAmount) {

        /* 1) 경매 진행 중인지 확인 */
        if (!isAuctionActive(concertId)) {
            throw new AuctionException("이미 종료된 경매입니다.");
        }

        /* 2) 입찰 횟수 제한 확인 */
        String bidCountKey = AUCTION_BID_COUNT_PREFIX + concertId + ":" + userId;
        String bidCountStr = redisTemplate.opsForValue().get(bidCountKey);
        int bidCount = bidCountStr != null ? Integer.parseInt(bidCountStr) : 0;
        if (bidCount >= MAX_BID_COUNT) {
            throw new BidException("입찰 가능 횟수를 초과했습니다.");
        }

        /* 3) 현재 최고가보다 높은가? */
        int currentHighest = getCurrentHighestBid(concertId);
        if (bidAmount <= currentHighest) {
            throw new BidException("입찰 금액은 현재 최고가보다 높아야 합니다.");
        }

        /* 4) 최소 호가 단위(1,000원) 준수 여부 */
        final int MIN_STEP = 1_000;
        if ((bidAmount - currentHighest) % MIN_STEP != 0) {
            throw new BidException("입찰 단위는 1,000원이어야 합니다.");
        }
    }

    // 결제 성공 후 실제 입찰 처리 메서드
    @Transactional
    public boolean processBidAfterPayment(Long concertId, Long userId, int bidAmount) {
        try {
            // 검증
            validateBid(concertId, userId, bidAmount);
            
            // 실제 입찰 처리
            return placeBid(concertId, userId, bidAmount);
        } catch (AuctionException | BidException e) {
            log.warn("결제 후 입찰 처리 실패 - 콘서트ID: {}, 사용자ID: {}, 사유: {}", concertId, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 후 입찰 처리 중 오류 - 콘서트ID: {}, 사용자ID: {}, 오류: {}", concertId, userId, e.getMessage(), e);
            throw new RuntimeException("입찰 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 