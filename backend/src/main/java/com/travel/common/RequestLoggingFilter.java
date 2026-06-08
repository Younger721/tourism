package com.travel.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /**
     * 记录每一次 HTTP 请求的方法、路径、状态码、耗时和客户端地址。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String query = request.getQueryString();
        String path = request.getRequestURI() + (query == null ? "" : "?" + query);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("HTTP请求 方法={} 路径={} 状态码={} 耗时={}ms 客户端={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    durationMs,
                    request.getRemoteAddr());
        }
    }
}
