package com.cashticket.repository;

import com.cashticket.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
} 