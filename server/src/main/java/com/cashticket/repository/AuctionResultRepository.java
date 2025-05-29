package com.cashticket.repository;

import com.cashticket.entity.AuctionResult;
import com.cashticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {
    List<AuctionResult> findByUser(User user);

    @Query("SELECT ar FROM AuctionResult ar " +
           "JOIN FETCH ar.auction a " +
           "JOIN FETCH a.concert " +
           "WHERE ar.user = :user")
    List<AuctionResult> findByUserWithAuctionAndConcert(@Param("user") User user);
} 