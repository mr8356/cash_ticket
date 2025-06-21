package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.dto.BidOrderRequest;
import com.cashticket.dto.BidOrderResponse;
import com.cashticket.entity.User;
import com.cashticket.service.AuctionService;
import com.cashticket.service.BidOrderService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// controller/BidPaymentController.java
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/auction")
public class BidPaymentController {

    private final BidOrderService bidOrderService;
    private final AuctionService auctionService;
    private static final Logger log = LoggerFactory.getLogger(BidPaymentController.class);

    @GetMapping("/orders")
    public String createOrder(@CurrentUser User user,
     @RequestParam String concertId,
      @RequestParam String amount,
       Model model) {
        try {
            log.info("결제 페이지 요청 - 사용자: {}, 콘서트ID: {}, 금액: {}", 
                user != null ? user.getId() : "null", concertId, amount);
            
            if (user == null) {
                log.error("사용자가 로그인되지 않음");
                return "redirect:/login";
            }
            
            model.addAttribute("concertId", concertId);
            model.addAttribute("amount", amount);
            model.addAttribute("user", user);
            
            log.info("결제 페이지 모델 설정 완료");
            return "payment/index";
            
        } catch (Exception e) {
            log.error("결제 페이지 로드 중 오류 - 콘서트ID: {}, 금액: {}, 오류: {}", 
                concertId, amount, e.getMessage(), e);
            return "error";
        }
    }

    @GetMapping("/success")
    public String success(@CurrentUser User user, @RequestParam String concertId, @RequestParam String amount) {
        try {
            if (user == null) {
                log.error("사용자가 로그인되지 않음");
                return "redirect:/login";
            }
            
            // 결제 성공 후 실제 입찰 처리
            boolean success = auctionService.processBidAfterPayment(
                Long.parseLong(concertId), 
                user.getId(), 
                Integer.parseInt(amount)
            );
            
            if (success) {
                return "payment/success";
            } else {
                return "payment/fail";
            }
        } catch (Exception e) {
            log.error("입찰 처리 실패 - 콘서트ID: {}, 사용자ID: {}, 금액: {}, 오류: {}", 
                concertId, user != null ? user.getId() : "null", amount, e.getMessage(), e);
            return "error";
        }
    }

    @GetMapping("/fail")
    public String fail(@CurrentUser User user) {
        return "payment/fail";
    }

    @GetMapping("/cancel")
    public String cancel(@CurrentUser User user) {
        return "payment/cancel";
    }
}
