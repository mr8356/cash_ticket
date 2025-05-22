package com.cashticket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        log.info("Home page requested");
        return "home";
    }

    @GetMapping("/mypage")
    public String mypage() {
        log.info("Mypage requested");
        return "mypage";
    }

    @GetMapping("/login")
    public String login() {
        log.info("Login page requested");
        return "login";
    }
    
    @GetMapping("/join")
    public String join() {
        log.info("Join page requested");
        return "join";
    }
}
