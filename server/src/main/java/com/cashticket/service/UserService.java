package com.cashticket.service;

import com.cashticket.entity.User;
import com.cashticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

    public User getUserById(Long id) {
        // 테스트용 더미 유저 데이터 반환
        if (id == 1L) {
            return User.builder()
                    .id(1L)
                    .userId("testuser")
                    .email("test@test.com")
                    .password("1234")
                    .nickname("동현 방금 수정함")
                    .birthDay(LocalDate.of(1990, 1, 1))
                    .phoneNumber("010-1234-5678")
                    .build();
        }
        return userRepository.findById(id).orElse(null);
    }


}
