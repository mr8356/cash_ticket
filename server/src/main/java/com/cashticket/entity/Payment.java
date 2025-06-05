package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    /* 1) 기본 키 (Long) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* 2) 주문번호(비즈니스 키) */
    @Column(length = 64, unique = true, nullable = false)
    private String orderId;

    private Long userId;
    private Long concertId;
    private int  amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    public Payment(String orderId,
                   Long userId,
                   Long concertId,
                   int amount,
                   PaymentStatus status) {
        this.orderId   = orderId;
        this.userId    = userId;
        this.concertId = concertId;
        this.amount    = amount;
        this.status    = status;
        this.createdAt = LocalDateTime.now();
    }

    public void markPaid()   { this.status = PaymentStatus.PAID; }
    public void markFailed() { this.status = PaymentStatus.FAILED; }
}
