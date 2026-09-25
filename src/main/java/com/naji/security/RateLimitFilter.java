package com.naji.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int WINDOW_MINUTES = 10;

    private record Rule(String method, String path, int limit) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/player/register", 10),
            new Rule("PUT", "/player/reset-password", 5),
            new Rule("POST", "/player/guest", 30),
            new Rule("POST", "/verification/verify-email", 20)
    );

    private final RedisTemplate<String, String> redis;

    public RateLimitFilter(@Qualifier("verificationCodeRedisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Rule> rule = RULES.stream()
                .filter(candidate -> candidate.method().equals(request.getMethod())
                        && candidate.path().equals(request.getServletPath()))
                .findFirst();

        if (rule.isPresent() && isOverLimit(rule.get(), request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType("text/plain");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("Too many requests. Please wait a few minutes and try again.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isOverLimit(Rule rule, String clientAddress) {
        try {
            String key = "rate:" + rule.path() + ":" + clientAddress;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            return count != null && count > rule.limit();
        } catch (RuntimeException ex) {
            logger.warn("Rate limit check skipped: {}", ex.getMessage());
            return false;
        }
    }
}
