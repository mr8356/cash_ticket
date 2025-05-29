package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.User;
import com.cashticket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /* ■ 사전결제 위젯 페이지 (auctionId, amount 는 모델에 담긴 상태) */
    @GetMapping("/auction/{auctionId}")
    public String paymentPage(@PathVariable Long auctionId,
                              @RequestParam Long amount,
                              @CurrentUser User user,
                              Model model) {

        model.addAttribute("auctionId", auctionId);
        model.addAttribute("amount",    amount);
        model.addAttribute("user",      user);
        return "payment/index";
    }

    /* ■ Toss 위젯 successUrl */
    @GetMapping("/success")
    public String success(@RequestParam String paymentKey,
                          @RequestParam String orderId,
                          @RequestParam Long amount,
                          @RequestParam Long auctionId,
                          @CurrentUser User user,
                          RedirectAttributes ra) {

        paymentService.approveAndSave(paymentKey, orderId, amount, auctionId, user);
        ra.addFlashAttribute("message", "결제가 완료되었습니다. 이제 입찰 가능합니다!");
        return "redirect:/auction/" + auctionId;
    }

    /* ■ Toss 위젯 failUrl */
    @GetMapping("/fail")
    public String fail(@RequestParam(required = false) String code,
                       @RequestParam(required = false) String message,
                       Model model) {

        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "payment/fail";
    }

}