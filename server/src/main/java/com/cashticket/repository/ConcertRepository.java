package com.cashticket.repository;

import com.cashticket.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {

    // TODO: 콘서트 검색 기능용 쿼리 메서드 추가 예정
}
