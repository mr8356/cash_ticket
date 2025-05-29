package com.cashticket.repository;

import com.cashticket.entity.LikeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<LikeTable, Long> {

    // TODO: 찜 여부 확인, 사용자별 찜 목록 조회 기능 추가 예정
    Optional<LikeTable> findByConcertIdAndUser_Id(Long concertId, Long userId);
    List<LikeTable> findAllByUserId(Long userId);
}