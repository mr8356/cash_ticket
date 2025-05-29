package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatusEnum status = AuctionStatusEnum.OPEN;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
} 