package com.cashticket.service;

import com.cashticket.dto.BidOrderRequest;
import com.cashticket.dto.BidOrderResponse;
import com.cashticket.entity.Payment;
import com.cashticket.entity.PaymentStatus;
import com.cashticket.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

// service/BidOrderService.java
@Service @RequiredArgsConstructor
public class BidOrderService {

    private final AuctionService   auctionService;   // 이미 존재
    private final PaymentRepository paymentRepo;

    public BidOrderResponse createOrder(Long concertId, Long amount, Long userId) {

        // 1) 입찰할 자격·금액 검증 ― 기존 AuctionService 로직 재사용
        auctionService.validateBid(concertId, userId, amount.intValue());

        // 2) 주문번호 생성
        String orderId = UUID.randomUUID().toString();

        // 3) DB에 PENDING 저장
        paymentRepo.save(new Payment(
                orderId, userId, concertId,
                amount.intValue(), PaymentStatus.PENDING
        ));

        // 4) 프론트로 반환
        return new BidOrderResponse(orderId, amount.intValue());
    }
}