package com.cashticket.service;

import com.cashticket.entity.Concert;
import com.cashticket.entity.LikeTable;
import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final ConcertRepository concertRepository;
    private final LikeRepository likeRepository;

    // TODO: 콘서트 목록 조회
    public List<Concert> getConcertList() {
        return concertRepository.findAll();
    }

    // TODO: 콘서트 상세 조회
    public Concert getConcertDetail(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new RuntimeException("콘서트를 찾을 수 없습니다."));
    }

    // TODO: 콘서트 검색
    public List<Concert> searchConcerts(String keyword) {
        return concertRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword);
    }

    // TODO: 찜 추가/삭제



    // TODO: 찜 여부 확인

    // TODO: 찜 목록 조회

}
