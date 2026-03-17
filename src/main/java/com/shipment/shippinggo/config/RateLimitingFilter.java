package com.shipment.shippinggo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int MAX_REQUESTS_PER_MINUTE = 100;
    private final ConcurrentHashMap<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

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
        long currentTime = System.currentTimeMillis();
        
        requestCounts.compute(clientIp, (key, requestInfo) -> {
            if (requestInfo == null || (currentTime - requestInfo.timestamp > TimeUnit.MINUTES.toMillis(1))) {
                // Reset or initialize count for the new minute
                return new RequestInfo(currentTime, 1);
            } else {
                // Increment count within the same minute
                requestInfo.count++;
                return requestInfo;
            }
        });

        RequestInfo info = requestCounts.get(clientIp);
        return info != null && info.count > MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class RequestInfo {
        long timestamp;
        int count;

        RequestInfo(long timestamp, int count) {
            this.timestamp = timestamp;
            this.count = count;
        }
    }
}
