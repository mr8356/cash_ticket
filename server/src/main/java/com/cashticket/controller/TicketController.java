package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.Concert;
import com.cashticket.entity.User;
import com.cashticket.service.TicketService;
import com.cashticket.strategy.ConcertFilterContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcertFilterContext filterContext;

    // TODO: 콘서트 상세 조회
    @GetMapping("/{concertId}")
    public String getConcertDetail(@PathVariable Long concertId, @CurrentUser User user, Model model) {
        try {
            String cacheKey = "concert:" + concertId;
            String cachedConcert = redisTemplate.opsForValue().get(cacheKey);

            Concert concert;
            if (cachedConcert != null) {
                concert = objectMapper.readValue(cachedConcert, Concert.class);
            } else {
                concert = ticketService.getConcertDetail(concertId);
                String concertJson = objectMapper.writeValueAsString(concert);
                redisTemplate.opsForValue().set(cacheKey, concertJson, 1, TimeUnit.HOURS);
            }

            boolean isLiked = (user != null) && ticketService.isConcertLikedByUser(concertId, user.getId());

            model.addAttribute("concert", concert);
            model.addAttribute("isLiked", isLiked);
            return "concert_information";
        } catch (Exception e) {
            Concert concert = ticketService.getConcertDetail(concertId);
            boolean isLiked = (user != null) && ticketService.isConcertLikedByUser(concertId, user.getId());
            model.addAttribute("concert", concert);
            model.addAttribute("isLiked", isLiked);
            return "concert_information";
        }
    }

    // TODO: 콘서트 검색
    @GetMapping("/search")
    public String searchConcerts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String category,
            Model model) {

        List<Concert> concerts = ticketService.searchConcerts(query);

        if (artist != null && !artist.isEmpty()) {
            concerts = filterContext.applyFilter("artist", concerts, artist);
        }
        if (date != null && !date.isEmpty()) {
            concerts = filterContext.applyFilter("date", concerts, date);
        }
        if (category != null && !category.isEmpty()) {
            concerts = filterContext.applyFilter("category", concerts, category);
        }

        model.addAttribute("concerts", concerts);
        return "search_results";
    }

    // TODO: 찜 추가/삭제
    @PostMapping("/{concertId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleConcertLike(@PathVariable Long concertId, @CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        boolean isNowLiked = ticketService.toggleConcertLike(concertId, user);
        return ResponseEntity.ok(Map.of("liked", isNowLiked));
    }


    // TODO: 찜 여부 확인
    @GetMapping("/likes")
    public String getUserLikedConcerts(@CurrentUser User user, Model model) {
        List<Concert> likedConcerts = ticketService.getUserLikedConcerts(user.getId());
        model.addAttribute("likedConcerts", likedConcerts);
        return "mypage/favorites"; // templates/mypage/favorites.html 로 렌더링
    }
}