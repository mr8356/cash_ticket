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
import com.cashticket.exception.BidCountExceededException;


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
                              Model model,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long concertIdLong = null; // try-catch 외부에서 사용하기 위해
        try {
            log.info("결제 페이지 요청 - 사용자: {}, 콘서트ID: {}, 금액: {}",
                user != null ? user.getId() : "null", concertId, amount);
            
            if (user == null) {
                log.error("사용자가 로그인되지 않음");
                return "redirect:/login";
            }
            
            // 파라미터 유효성 검사 및 안전한 파싱
            Integer amountInt;
            try {
                concertIdLong = Long.valueOf(concertId.trim());
                amountInt = Integer.valueOf(amount.trim());
                if (amountInt <= 0) throw new NumberFormatException("금액이 0 이하입니다.");
            } catch (NumberFormatException e) {
                log.error("파라미터 파싱 실패: concertId={}, amount={}", concertId, amount, e);
                redirectAttributes.addFlashAttribute("error", "유효하지 않은 요청입니다.");
                return "redirect:/auction/" + (concertId != null ? concertId : "");
            }

            // 입찰 금액 사전 검증
            try {
                auctionService.validateBid(concertIdLong, user.getId(), amountInt);
                log.info("입찰 금액 검증 성공 - 콘서트ID: {}, 사용자ID: {}, 금액: {}", concertIdLong, user.getId(), amountInt);
            } catch (BidCountExceededException e) {
                log.warn("입찰 횟수 초과 - concertId={}, userId={}", concertIdLong, user.getId());
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/auction/" + concertIdLong;
            } catch (com.cashticket.exception.AuctionException | com.cashticket.exception.BidException e) {
                log.warn("입찰 금액 검증 실패 - 콘서트ID: {}, 사유: {}", concertIdLong, e.getMessage());
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/auction/" + concertIdLong;
            }
            
            model.addAttribute("concertId", concertIdLong);
            model.addAttribute("amount", amountInt);
            model.addAttribute("user", user);
            
            log.info("결제 페이지 모델 설정 완료 - 콘서트ID: {}, 금액: {}", concertIdLong, amountInt);
            return "payment/index";
            
        } catch (Exception e) {
            log.error("결제 페이지 로드 중 오류 - 콘서트ID: {}, 금액: {}, 오류: {}",
                concertId, amount, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            return "redirect:/auction/" + concertId;
        }
    }

    @GetMapping("/success")
    public String success(@CurrentUser User user, 
                         @RequestParam String concertId, 
                         @RequestParam(required = false) String amount,
                         @RequestParam(required = false) String orderId,
                         @RequestParam(required = false) String paymentKey) {
        try {
            if (user == null) {
                log.error("사용자가 로그인되지 않음");
                return "redirect:/login";
            }
            
            log.info("결제 성공 콜백 - 사용자: {}, 콘서트ID: {}, 금액: {}, 주문ID: {}, 결제키: {}", 
                user.getId(), concertId, amount, orderId, paymentKey);
            
            // 파라미터 유효성 검사
            if (concertId == null || concertId.trim().isEmpty()) {
                log.error("콘서트 ID가 비어있음");
                return "redirect:/error?message=invalid_concert_id";
            }
            
            // Toss Payments 파라미터 검증
            if (paymentKey == null || paymentKey.trim().isEmpty()) {
                log.error("결제키가 비어있음");
                return "redirect:/error?message=invalid_payment";
            }
            
            if (orderId == null || orderId.trim().isEmpty()) {
                log.error("주문ID가 비어있음");
                return "redirect:/error?message=invalid_order";
            }
            
            // 안전한 파싱
            Long concertIdLong;
            Integer amountInt = null;
            
            try {
                concertIdLong = Long.valueOf(concertId.trim());
            } catch (NumberFormatException e) {
                log.error("콘서트 ID 파싱 실패: {}", concertId, e);
                return "redirect:/error?message=invalid_concert_id";
            }
            
            // amount 파라미터가 있는 경우에만 파싱 (Toss Payments에서 전달된 값 사용)
            if (amount != null && !amount.trim().isEmpty()) {
                try {
                    amountInt = Integer.valueOf(amount.trim());
                    if (amountInt <= 0) {
                        log.error("금액이 0 이하: {}", amountInt);
                        return "redirect:/error?message=invalid_amount";
                    }
                } catch (NumberFormatException e) {
                    log.error("금액 파싱 실패: {}", amount, e);
                    return "redirect:/error?message=invalid_amount";
                }
            } else {
                log.warn("금액 파라미터가 없음 - Toss Payments 검증 필요");
                // TODO: Toss Payments API를 통한 결제 검증 로직 추가 필요
                return "redirect:/error?message=payment_verification_required";
            }
            
            // 결제 성공 후 실제 입찰 처리
            boolean success = auctionService.processBidAfterPayment(concertIdLong, user.getId(), amountInt);
            
            if (success) {
                log.info("입찰 처리 성공 - 콘서트ID: {}, 사용자ID: {}, 금액: {}, 주문ID: {}", 
                    concertIdLong, user.getId(), amountInt, orderId);
                return "payment/success";
            } else {
                log.warn("입찰 처리 실패 - 콘서트ID: {}, 사용자ID: {}, 금액: {}, 주문ID: {}", 
                    concertIdLong, user.getId(), amountInt, orderId);
                return "payment/fail";
            }
        } catch (Exception e) {
            log.error("입찰 처리 중 예외 발생 - 콘서트ID: {}, 사용자ID: {}, 금액: {}, 주문ID: {}, 오류: {}", 
                concertId, user != null ? user.getId() : "null", amount, orderId, e.getMessage(), e);
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
