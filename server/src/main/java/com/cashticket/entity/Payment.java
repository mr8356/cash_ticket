package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentKey;
    private String orderId;
    private Long  amount;
    private Long  auctionId;

    private boolean success;

    private LocalDateTime approvedAt;

    /* (선택) 결제자 연관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}