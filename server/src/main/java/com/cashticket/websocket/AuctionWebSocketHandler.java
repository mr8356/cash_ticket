package com.cashticket.websocket;

import com.cashticket.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuctionWebSocketHandler {
    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionService auctionService;

    @Scheduled(fixedRate = 1000)
    public void broadcastAuctionUpdates() {
        // 활성화된 모든 경매에 대해 업데이트 브로드캐스트
        auctionService.getActiveAuctions().forEach(auction -> {
            Map<String, Object> update = new HashMap<>();
            update.put("currentBid", auctionService.getCurrentHighestBid(auction));
            update.put("timeLeft", calculateTimeLeft(auction));
            
            messagingTemplate.convertAndSend(
                "/topic/auction/" + auction,
                update
            );
        });
    }

    private long calculateTimeLeft(Long concertId) {
        LocalDateTime endTime = auctionService.getAuctionEndTime(concertId);
        if (endTime == null) {
            return 0;
        }
        
        long seconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(0, seconds);
    }
} 