package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.User;
import com.cashticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {	
	private final Long id = 1L;
	private final UserService userService;

	@GetMapping("/mypage")
	public String mypage(Model model) {
		User user = userService.getUserById(id);
		model.addAttribute("user", user);
		return "mypage";
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@CurrentUser User user) {
		return ResponseEntity.ok(user);
	}
}
