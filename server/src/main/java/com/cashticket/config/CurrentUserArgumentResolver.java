package com.cashticket.config;

import com.cashticket.entity.User;
import com.cashticket.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
               parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        HttpSession session = webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class)
                .getSession(false);
        
        if (session != null) {
            // Redis 세션에서 사용자 ID 조회
            String sessionId = session.getId();
            String userId = redisTemplate.opsForValue().get("spring:session:" + sessionId + ":userId");
            
            if (userId != null) {
                return userService.getUserById(Long.parseLong(userId));
            }
        }
        return null;
    }
} 