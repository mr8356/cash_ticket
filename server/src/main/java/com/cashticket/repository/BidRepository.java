package com.cashticket.repository;

import com.cashticket.entity.Bid;
import com.cashticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query("SELECT b FROM Bid b " +
           "JOIN FETCH b.auction a " +
           "JOIN FETCH a.concert " +
           "WHERE b.user = :user AND a.status = com.cashticket.entity.AuctionStatusEnum.OPEN " +
           "ORDER BY b.bidTime DESC")
    List<Bid> findByUserWithAuctionAndConcert(@Param("user") User user);
} 