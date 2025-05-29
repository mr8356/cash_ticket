package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.User;
import com.cashticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String userId,
                                    @RequestParam String email,
                                    @RequestParam String password,
                                    @RequestParam String nickname,
                                    @RequestParam LocalDate birthDay,
                                    @RequestParam(required = false) String phoneNumber) {
        try {
            User user = User.builder()
                    .userId(userId)
                    .email(email)
                    .password(password)
                    .nickname(nickname)
                    .birthDay(birthDay)
                    .phoneNumber(phoneNumber)
                    .build();
            
            userService.register(user);
            return ResponseEntity.ok().body("회원가입이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String userId, 
                                 @RequestParam String password,
                                 HttpSession session) {
        try {
            User user = userService.login(userId, password);
            
            // Redis 세션에 사용자 ID 저장
            String sessionId = session.getId();
            redisTemplate.opsForValue().set("spring:session:" + sessionId + ":userId", user.getId().toString());
            
            return ResponseEntity.ok().body("로그인 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        // Redis 세션에서 사용자 정보 삭제
        String sessionId = session.getId();
        redisTemplate.delete("spring:session:" + sessionId + ":userId");
        
        session.invalidate();
        return ResponseEntity.ok().body("로그아웃 성공");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CurrentUser User user) {
        return ResponseEntity.ok(user);
    }
} 