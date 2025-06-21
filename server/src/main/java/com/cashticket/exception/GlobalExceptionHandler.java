package com.cashticket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 경매 예외 처리
    @ExceptionHandler(AuctionException.class)
    public ResponseEntity<String> handleAuctionException(AuctionException e) {
        log.warn("AuctionException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // 커스텀 입찰 예외 처리
    @ExceptionHandler(BidException.class)
    public ResponseEntity<String> handleBidException(BidException e) {
        log.warn("BidException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // IllegalStateException 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // RuntimeException 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    // 일반 Exception 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("예상치 못한 오류가 발생했습니다. 관리자에게 문의해주세요.");
    }
} 