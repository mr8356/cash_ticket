package com.cashticket.service;

import com.cashticket.entity.Payment;
import com.cashticket.entity.User;
import com.cashticket.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /* Toss 테스트 Secret Key (운영 시 교체) */
    private static final String SECRET_KEY = "test_sk_GePWvyJnrKmkKBoYlbdL3gLzN97E";
    private static final String BASE_URL   = "https://api.tosspayments.com/v1/payments";

    /** Toss 승인 API 호출 + DB 기록 */
    public void approveAndSave(String paymentKey,
                               String orderId,
                               Long amount,
                               Long auctionId,
                               User user) {

        WebClient client = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        client.post()
                .uri("/{paymentKey}", paymentKey)
                .bodyValue(Map.of("orderId", orderId, "amount", amount))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("Toss 승인 실패", e)))
                .block();   // 간단한 과제이므로 blokcing

        paymentRepository.save(
                Payment.builder()
                        .paymentKey(paymentKey)
                        .orderId(orderId)
                        .amount(amount)
                        .auctionId(auctionId)
                        .user(user)
                        .success(true)
                        .approvedAt(LocalDateTime.now())
                        .build()
        );
    }

    public java.util.List<Payment> history(Long userId) {
        return paymentRepository.findAllByUser_IdOrderByApprovedAtDesc(userId);
    }

    /* basic auth header */
    private String basicAuthHeader() {
        String raw = SECRET_KEY + ":";
        return "Basic " +
                Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}