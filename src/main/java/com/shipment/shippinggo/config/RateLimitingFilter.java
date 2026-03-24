package com.shipment.shippinggo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Cache<String, Integer> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Apply rate limiting only to API endpoints starting with /api/
        if (request.getRequestURI().startsWith("/api/")) {
            String clientIp = getClientIP(request);
            
            if (isRateLimited(clientIp)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too Many Requests. Please try again later.");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        Integer count = requestCounts.getIfPresent(clientIp);
        if (count == null) {
            requestCounts.put(clientIp, 1);
            return false;
        } else if (count >= MAX_REQUESTS_PER_MINUTE) {
            return true;
        } else {
            requestCounts.put(clientIp, count + 1);
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }


}
