package com.cashticket.dto;

public record BidOrderRequest(
        Long concertId,
        int  bidAmount          // 단위: 원
) {}