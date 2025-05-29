package com.cashticket.repository;

import com.cashticket.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUser_IdAndAuctionId(Long userId, Long auctionId);

    List<Payment> findAllByUser_IdOrderByApprovedAtDesc(Long userId);
}