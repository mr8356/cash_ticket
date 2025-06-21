package com.cashticket.dto;

public record BidOrderResponse(
        String orderId,
        int    amount
) {}