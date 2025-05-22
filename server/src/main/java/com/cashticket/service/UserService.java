package com.cashticket.service;

import com.cashticket.entity.User;
import com.cashticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;

<<<<<<< HEAD
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

=======
	@Transactional
	public User register(User user) {
		// 이메일 중복 체크
		if (userRepository.findByEmail(user.getEmail()).isPresent()) {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
		}
		
		// userId 중복 체크
		if (userRepository.findByUserId(user.getUserId()).isPresent()) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}
		
		return userRepository.save(user);
	}

	public User login(String userId, String password) {
		User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
		
		if (!user.getPassword().equals(password)) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
		
		return user;
	}

	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
	}

	public User getUserByUserId(String userId) {
		return userRepository.findByUserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
	}
>>>>>>> b80605ab2163a406be438e20585c45723658b4e7

}
