package com.cashticket.controller;

import com.cashticket.entity.Concert;
import com.cashticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // TODO: 콘서트 목록 조회
    @GetMapping
    public ResponseEntity<List<Concert>> getConcertList() {
        return ResponseEntity.ok(ticketService.getConcertList());
    }

    // TODO: 콘서트 상세 조회
    @GetMapping("/{concertId}")
    public ResponseEntity<Concert> getConcertDetail(@PathVariable Long concertId) {
        return ResponseEntity.ok(ticketService.getConcertDetail(concertId));
    }

    // TODO: 콘서트 검색
    @GetMapping("/search")
    public ResponseEntity<List<Concert>> searchConcerts(@RequestParam String keyword) {
        return ResponseEntity.ok(ticketService.searchConcerts(keyword));
    }

    // TODO: 찜 추가/삭제

    // TODO: 찜 여부 확인

    // TODO: 찜 목록 조회

}
