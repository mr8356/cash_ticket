package com.cashticket.service;

import com.cashticket.entity.Concert;
import com.cashticket.entity.LikeTable;
import com.cashticket.entity.User;
import com.cashticket.repository.ConcertRepository;
import com.cashticket.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // 콘서트 검색
    public List<Concert> searchConcerts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return concertRepository.findAll();
        }
        return concertRepository.findAll().stream()
                .filter(concert -> 
                    concert.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                    concert.getArtist().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    // TODO: 찜 추가/삭제
    public boolean toggleConcertLike(Long concertId, User user) {
        Optional<LikeTable> existing = likeRepository.findByConcertIdAndUser_Id(concertId, user.getId());
        Concert concert = concertRepository.getReferenceById(concertId);

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false; // 찜 취소됨
        } else {
            LikeTable newLike = LikeTable.builder()
                    .concert(concert)
                    .user(user)
                    .build();

            likeRepository.save(newLike);
            return true; // 찜 추가됨
        }
    }

    // TODO: 찜 여부 확인
    public List<Concert> getUserLikedConcerts(Long userId) {
        List<LikeTable> likes = likeRepository.findAllByUserId(userId);

        return likes.stream()
                .map(LikeTable::getConcert)
                .collect(Collectors.toList());
    }
}
