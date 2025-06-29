package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.AuctionResult;
import com.cashticket.entity.Concert;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@GetMapping("/mypage")
	public String mypage(@CurrentUser User user, Model model, RedirectAttributes redirectAttributes) {
		// 1. 로그인된 사용자인지 확인합니다.
		if (user == null) {
			// 2. 비로그인 상태이면, 리다이렉트될 페이지로 메시지를 전달합니다.
			redirectAttributes.addFlashAttribute("alertMessage", "로그인 후 이용 가능합니다.");
			// 3. 홈("/")으로 리다이렉트합니다.
			return "redirect:/";
		}

		// 기존 로직: 로그인 상태이면 마이페이지로 이동합니다.
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
					  @RequestParam(required = false) String password,
					  Model model) {
		
		
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
		List<Concert> concerts = userService.getFavorites(user);
		model.addAttribute("concerts", concerts);
		return "mypage/favorites";
	}

	@DeleteMapping("/mypage/favorites/{concertId}")
	public String deleteFavorite(@PathVariable Long concertId, @CurrentUser User user) {
		userService.deleteFavorite(concertId, user);
		return "redirect:/users/mypage/favorites"; // [!] 이렇게 수정해야 합니다.
	}

	// 사용자의 입찰 내역 조회
	@GetMapping("/mypage/bids")
	public String getBidHistory(@CurrentUser User user, Model model) {
		List<com.cashticket.entity.Bid> bids = userService.getBidHistory(user);
		model.addAttribute("bids", bids);
		return "mypage/bid-history";
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@CurrentUser User user) {
		return ResponseEntity.ok(user);
	}
}
