package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.Concert;
import com.cashticket.entity.User;
import com.cashticket.exception.AuctionException;
import com.cashticket.exception.BidException;
import com.cashticket.service.AuctionService;
import com.cashticket.service.TicketService;
import com.cashticket.strategy.ConcertFilterContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final TicketService ticketService;
    private final AuctionService auctionService;

    // 경매 상세 페이지
    @GetMapping("/{concertId}")
    public String getAuctionDetail(@PathVariable Long concertId, Model model) {
        try {
            Concert concert = ticketService.getConcertDetail(concertId);
            if (concert == null) {
                log.error("Concert not found: {}", concertId);
                model.addAttribute("error", "콘서트를 찾을 수 없습니다.");
                return "error";
            }
            
            // 경매가 존재하는지 확인
            boolean isActive = auctionService.isAuctionActive(concertId);
            if (!isActive) {
                // 경매가 없거나 종료된 경우
                model.addAttribute("concert", concert);
                model.addAttribute("isActive", false);
                model.addAttribute("currentBid", 0);
                return "concert_auction";
            }
            
            int currentBid = auctionService.getCurrentHighestBid(concertId);
            
            model.addAttribute("concert", concert);
            model.addAttribute("currentBid", currentBid);
            model.addAttribute("isActive", isActive);
            
            return "concert_auction";
        } catch (Exception e) {
            log.error("Error in getAuctionDetail: {}", e.getMessage(), e);
            model.addAttribute("error", "경매 정보를 불러오는 중 오류가 발생했습니다.");
            return "error";
        }
    }

    // 입찰 처리
    @PostMapping("/bid")
    public String placeBid(
            @RequestParam Long concertId,
            @RequestParam int bidAmount,
            @CurrentUser User user,
            Model model) {
        
        try {
            if (user == null) {
                model.addAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }
            
            if (bidAmount <= 0) {
                model.addAttribute("error", "유효하지 않은 입찰 금액입니다.");
                return "redirect:/auction/" + concertId;
            }
            
            boolean success = auctionService.placeBid(concertId, user.getId(), bidAmount);
            
            if (success) {
                model.addAttribute("success", "입찰이 성공적으로 처리되었습니다.");
            } else {
                model.addAttribute("error", "입찰 처리에 실패했습니다. 경매가 종료되었거나 유효하지 않은 금액입니다.");
            }
            
        } catch (BidException e) {
            log.warn("Bid validation failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (AuctionException e) {
            log.warn("Auction validation failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid bid attempt: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error in placeBid: {}", e.getMessage(), e);
            model.addAttribute("error", "입찰 처리 중 오류가 발생했습니다.");
        }
        
        return "redirect:/auction/" + concertId;
    }

    // 경매 종료 처리
    @PostMapping("/{concertId}/end")
    @ResponseBody
    public ResponseEntity<String> endAuction(@PathVariable Long concertId) {
        try {
            boolean success = auctionService.endAuction(concertId);
            
            if (success) {
                return ResponseEntity.ok("경매가 성공적으로 종료되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("경매 종료 처리에 실패했습니다. 입찰자가 없거나 이미 종료된 경매일 수 있습니다.");
            }
        } catch (AuctionException e) {
            log.warn("Auction end failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error in endAuction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("경매 종료 처리 중 오류가 발생했습니다.");
        }
    }

    // 현재 입찰가 조회 (AJAX용)
    @GetMapping("/{concertId}/current-bid")
    @ResponseBody
    public ResponseEntity<Integer> getCurrentBid(@PathVariable Long concertId) {
        try {
            int currentBid = auctionService.getCurrentHighestBid(concertId);
            return ResponseEntity.ok(currentBid);
        } catch (AuctionException e) {
            log.warn("Auction not found for current bid: {}", e.getMessage());
            return ResponseEntity.badRequest().body(0);
        } catch (Exception e) {
            log.error("Error in getCurrentBid: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(0);
        }
    }
}