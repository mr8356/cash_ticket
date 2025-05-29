package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.Concert;
import com.cashticket.entity.User;
import com.cashticket.service.AuctionService;
import com.cashticket.service.TicketService;
import com.cashticket.strategy.ConcertFilterContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final TicketService ticketService;
    private final AuctionService auctionService;

    // 경매 상세 페이지
    @GetMapping("/{concertId}")
    public String getAuctionDetail(@PathVariable Long concertId, Model model) {
        Concert concert = ticketService.getConcertDetail(concertId);
        int currentBid = auctionService.getCurrentHighestBid(concertId);
        boolean isActive = auctionService.isAuctionActive(concertId);
        
        model.addAttribute("concert", concert);
        model.addAttribute("currentBid", currentBid);
        model.addAttribute("isActive", isActive);
        
        return "concert_auction";
    }

    // 입찰 처리
    @PostMapping("/bid")
    public String placeBid(
            @RequestParam Long concertId,
            @RequestParam int bidAmount,
            @CurrentUser User user,
            Model model) {
        
        boolean success = auctionService.placeBid(concertId, user.getId(), bidAmount);
        
        if (success) {
            model.addAttribute("success", true);
        } else {
            model.addAttribute("error", true);
        }
        
        return "redirect:/auction/" + concertId;
    }

    // 경매 종료 처리
    @PostMapping("/{concertId}/end")
    @ResponseBody
    public ResponseEntity<String> endAuction(@PathVariable Long concertId) {
        boolean success = auctionService.endAuction(concertId);
        
        if (success) {
            return ResponseEntity.ok("경매가 성공적으로 종료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("경매 종료 처리에 실패했습니다.");
        }
    }

    // 현재 입찰가 조회 (AJAX용)
    @GetMapping("/{concertId}/current-bid")
    @ResponseBody
    public ResponseEntity<Integer> getCurrentBid(@PathVariable Long concertId) {
        int currentBid = auctionService.getCurrentHighestBid(concertId);
        return ResponseEntity.ok(currentBid);
    }
}
