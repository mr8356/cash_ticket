package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.AuctionResult;
import com.cashticket.entity.User;
import com.cashticket.service.UserService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@GetMapping("/mypage")
	public String mypage(@CurrentUser User user, Model model) {
		model.addAttribute("user", user);
		return "mypage/index";
	}

	
	// 내 정보 수정관련 api 개발
	@GetMapping("/mypage/edit")
	public String editForm(@CurrentUser User user, Model model) {
		model.addAttribute("user", user);
		return "mypage/edit";
	}

	@PostMapping("/mypage/edit")
	public String edit(@CurrentUser User user,
					  @RequestParam String nickname,
					  @RequestParam String email,
					  @RequestParam LocalDate birthDay,
					  @RequestParam(required = false) String phoneNumber,
					  @RequestParam(required = false) String password) {
		User updatedUser = User.builder()
				.id(user.getId())
				.userId(user.getUserId())
				.email(email)
				.password(password != null && !password.isEmpty() ? password : user.getPassword())
				.nickname(nickname)
				.birthDay(birthDay)
				.phoneNumber(phoneNumber)
				.build();

		userService.updateUser(updatedUser);
		return "redirect:/users/mypage";
	}
	// 예매 정보 확인
	@GetMapping("/mypage/reservations") 
	public String getReservations(@CurrentUser User user, Model model) {
		List<AuctionResult> auctionResults = userService.getAuctionResults(user);
		model.addAttribute("auctionResults", auctionResults);
		return "mypage/reservations";
	}

	@GetMapping("/mypage/reservations/{reservationId}")
	public String getReservationDetail(@PathVariable Long reservationId,
									 @CurrentUser User user,
									 Model model) {
		AuctionResult result = userService.getAuctionResultDetail(reservationId, user);
		model.addAttribute("result", result);
		return "mypage/reservation-detail";
	}
	// 예매 취소
	@GetMapping("/mypage/reservations/{reservationId}/cancel")
	public String cancelReservation(@PathVariable Long reservationId,
									@CurrentUser User user) {
		userService.cancelAuctionResult(reservationId, user);
		return "redirect:/users/mypage/reservations";
	}

	// 사용자의 찜목록 조회
	@GetMapping("/mypage/favorites")
	public String getFavorites(@CurrentUser User user, Model model) {
		List<AuctionResult> auctionResults = userService.getAuctionResults(user);
		model.addAttribute("auctionResults", auctionResults);
		return "mypage/favorites";
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@CurrentUser User user) {
		return ResponseEntity.ok(user);
	}
}
