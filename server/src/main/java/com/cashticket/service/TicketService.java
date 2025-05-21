package com.cashticket.service;

import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final ConcertRepository concertRepository;
    private final LikeRepository likeRepository;

    // TODO: 콘서트 목록 조회

    // TODO: 콘서트 상세 조회

    // TODO: 콘서트 검색

    // TODO: 찜 추가/삭제

    // TODO: 찜 여부 확인

    // TODO: 찜 목록 조회
}
