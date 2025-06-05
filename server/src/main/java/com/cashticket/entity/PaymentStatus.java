package com.cashticket.entity;

public enum PaymentStatus {
    PENDING,  // 결제창만 띄운 상태
    PAID,     // confirm 성공
    FAILED    // 사용자 취소·잔액부족 등
}