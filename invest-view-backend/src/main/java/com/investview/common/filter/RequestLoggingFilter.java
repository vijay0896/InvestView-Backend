package com.investview.common.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;



/**
 * Enterprise HTTP Request Logging Filter (Express Morgan equivalent).
 * Logs HTTP Method, URI, Query Params, Status Code, and Execution Time (ms).
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Ye filter sabse pehle chalega
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        
        // Har request ko ek unique correlation ID assign karo (Debugging ke liye)
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        response.setHeader("X-Request-Id", requestId);
        // URI + Query params format karein (e.g. /api/v1/stocks/search?q=tata)
        String queryString = request.getQueryString();
        String fullPath = (queryString != null) 
                ? request.getRequestURI() + "?" + queryString 
                : request.getRequestURI();
        try {
            // Node.js ke next() ki tarah request ko Controller tak aage bhej do
            filterChain.doFilter(request, response);
        } finally {
            // Ye block request finish hone ke baad hamesha chalega (chahe success ho ya exception)
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            // Status ke hisaab se clean logging
            if (status >= 500) {
                log.error("[HTTP] [{}] {} {} | Status: {} | Time: {} ms",
                        requestId, request.getMethod(), fullPath, status, duration);
            } else if (status >= 400) {
                log.warn("[HTTP] [{}] {} {} | Status: {} | Time: {} ms",
                        requestId, request.getMethod(), fullPath, status, duration);
            } else {
                log.info("[HTTP] [{}] {} {} | Status: {} | Time: {} ms",
                        requestId, request.getMethod(), fullPath, status, duration);
            }
        }
    }
}