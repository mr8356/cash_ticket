package com.cashticket.repository;

import com.cashticket.entity.Auction;
import com.cashticket.entity.AuctionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findByConcertId(Long concertId);
    
    List<Auction> findByStatusAndEndTimeAfter(AuctionStatusEnum status, LocalDateTime endTime);
    
    List<Auction> findByStatus(AuctionStatusEnum status);
} 