package com.cashticket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.cashticket.service.TicketService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    private final TicketService ticketService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("concertList", ticketService.getConcertList());
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
