package com.cashticket.controller;

import com.cashticket.entity.User;
import com.cashticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping("/login")
	@ResponseBody
	public ResponseEntity<?> login(@RequestParam String email, 
								 @RequestParam String password,
								 HttpSession session) {
		try {
			User user = userService.login(email, password);
			session.setAttribute("userId", user.getId());
			session.setAttribute("userEmail", user.getEmail());
			return ResponseEntity.ok().body("로그인 성공");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping("/logout")
	@ResponseBody
	public ResponseEntity<?> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok().body("로그아웃 성공");
	}

	@GetMapping("/me")
	@ResponseBody
	public ResponseEntity<?> getCurrentUser(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return ResponseEntity.badRequest().body("로그인이 필요합니다.");
		}
		return ResponseEntity.ok().body(userService.getUserById(userId));
	}
}
