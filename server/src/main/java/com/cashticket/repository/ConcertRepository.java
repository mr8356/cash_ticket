package com.cashticket.repository;

import com.cashticket.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Set;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
    Set<Concert> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
