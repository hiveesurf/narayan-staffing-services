package com.narayan.staffing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.narayan.staffing.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = limitFor(path, request.getMethod());
        if (limit > 0) {
            String key = clientIp(request) + "|" + path;
            long now = Instant.now().toEpochMilli();
            Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
            synchronized (window) {
                while (!window.isEmpty() && now - window.peekFirst() > 60_000) {
                    window.removeFirst();
                }
                if (window.size() >= limit) {
                    response.setStatus(429);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            ApiError.of("Too many requests. Please wait a minute and try again.", path, List.of()));
                    return;
                }
                window.addLast(now);
            }
        }
        filterChain.doFilter(request, response);
    }

    private int limitFor(String path, String method) {
        if (!"POST".equalsIgnoreCase(method)) {
            return 0;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/forgot-password") || path.equals("/api/auth/reset-password")) {
            return 10;
        }
        if (path.equals("/api/public/applications") || path.equals("/api/public/contact")) {
            return 8;
        }
        return 0;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
