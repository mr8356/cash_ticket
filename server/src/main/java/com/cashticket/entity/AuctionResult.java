package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long finalBidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionResultStatusEnum status = AuctionResultStatusEnum.WINNER;

    @Column(nullable = false)
    private Integer seatNo;
} 