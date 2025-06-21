package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.dto.BidOrderRequest;
import com.cashticket.dto.BidOrderResponse;
import com.cashticket.entity.User;
import com.cashticket.service.BidOrderService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


// controller/BidPaymentController.java
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/auction")
public class BidPaymentController {

    private final BidOrderService bidOrderService;

    @GetMapping("/orders")
    public String createOrder(@CurrentUser User user,
     @RequestParam String concertId,
      @RequestParam String amount,
       Model model) {
        model.addAttribute("concertId", concertId);
        model.addAttribute("amount", amount);
        model.addAttribute("user", user);
        return "/payment/index";
    }

    @GetMapping("/success")
    public String success(@CurrentUser User user, @RequestParam String concertId, @RequestParam String amount) {
        bidOrderService.createOrder(Long.parseLong(concertId), Long.parseLong(amount), user.getId());
        return "/payment/success";
    }

    @GetMapping("/fail")
    public String fail(@CurrentUser User user) {
        return "/payment/fail";
    }

    @GetMapping("/cancel")
    public String cancel(@CurrentUser User user) {
        return "/payment/cancel";
    }
}
