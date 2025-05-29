package com.cashticket.service;

import com.cashticket.entity.User;
import com.cashticket.entity.AuctionResult;
import com.cashticket.entity.AuctionResultStatusEnum;
import com.cashticket.repository.UserRepository;
import com.cashticket.repository.AuctionResultRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;
	private final AuctionResultRepository auctionResultRepository;

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

	public List<AuctionResult> getAuctionResults(User user) {
		return auctionResultRepository.findByUserWithAuctionAndConcert(user);
	}

	public AuctionResult getAuctionResultDetail(Long resultId, User user) {
		return auctionResultRepository.findById(resultId)
				.filter(result -> result.getUser().equals(user))
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 낙찰 정보입니다."));
	}

	public void cancelAuctionResult(Long resultId, User user) {
		AuctionResult auctionResult = auctionResultRepository.findById(resultId)
				.filter(ar -> ar.getUser().equals(user))
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 낙찰 정보입니다."));
		auctionResult.setStatus(AuctionResultStatusEnum.CANCELLED);
		auctionResultRepository.save(auctionResult);
	}

	@Transactional
	public void updateUser(User updatedUser) {
		User existingUser = userRepository.findById(updatedUser.getId())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// 기존 사용자의 정보를 유지하면서 업데이트할 필드만 변경
		existingUser.setNickname(updatedUser.getNickname());
		existingUser.setEmail(updatedUser.getEmail());
		existingUser.setBirthDay(updatedUser.getBirthDay());
		existingUser.setPhoneNumber(updatedUser.getPhoneNumber());

		// 비밀번호가 변경된 경우에만 업데이트
		if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
			existingUser.setPassword(updatedUser.getPassword());
		}

		userRepository.save(existingUser);
	}

}
