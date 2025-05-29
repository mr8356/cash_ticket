package com.cashticket.controller;

import com.cashticket.config.CurrentUser;
import com.cashticket.entity.Concert;
import com.cashticket.entity.User;
import com.cashticket.service.TicketService;
import com.cashticket.strategy.ConcertFilterContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public String getConcertDetail(@PathVariable Long concertId, Model model) {
        try {
            // Redis에서 캐시된 콘서트 정보 조회
            String cacheKey = "concert:" + concertId;
            String cachedConcert = redisTemplate.opsForValue().get(cacheKey);
            
            Concert concert;
            if (cachedConcert != null) {
                // 캐시된 데이터가 있으면 Redis에서 가져온 데이터 사용
                concert = objectMapper.readValue(cachedConcert, Concert.class);
            } else {
                // 캐시된 데이터가 없으면 DB에서 조회하고 Redis에 캐싱
                concert = ticketService.getConcertDetail(concertId);
                String concertJson = objectMapper.writeValueAsString(concert);
                redisTemplate.opsForValue().set(cacheKey, concertJson, 1, TimeUnit.HOURS);
            }
            
            model.addAttribute("concert", concert);
            return "concert_information";
        } catch (Exception e) {
            // 캐싱 중 오류 발생 시 DB에서 직접 조회
            Concert concert = ticketService.getConcertDetail(concertId);
            model.addAttribute("concert", concert);
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
        
        // 기본 검색 결과 가져오기
        List<Concert> concerts = ticketService.searchConcerts(query);
        
        // 필터 적용
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
    public ResponseEntity<String> toggleConcertLike(@PathVariable Long concertId,
                                                    @RequestParam boolean like,
                                                    @CurrentUser User user) {
        boolean result = ticketService.toggleConcertLike(concertId, user);
        return ResponseEntity.ok(result ? (like ? "찜 완료" : "찜 취소") : "처리 실패");
    }
    // TODO: 찜 여부 확인
    @GetMapping("/likes")
    public String getUserLikedConcerts(@CurrentUser User user, Model model) {
        List<Concert> likedConcerts = ticketService.getUserLikedConcerts(user.getId());
        model.addAttribute("likedConcerts", likedConcerts);
        return "mypage/favorites"; // templates/mypage/favorites.html 로 렌더링
    }

    // TODO: 찜 목록 조회

}
