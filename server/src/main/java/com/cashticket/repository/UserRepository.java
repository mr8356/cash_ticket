package com.cashticket.repository;

import com.cashticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByName(String name);
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
}
