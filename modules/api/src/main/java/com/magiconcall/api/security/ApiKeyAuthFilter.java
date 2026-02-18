package com.magiconcall.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Validates X-Api-Key header on all /api/** requests.
 * Skips health/actuator endpoints.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final Set<String> validApiKeys;

    public ApiKeyAuthFilter(@Value("${magiconcall.security.api-keys}") Set<String> validApiKeys) {
        this.validApiKeys = validApiKeys;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        // Skip non-API paths (actuator, health, etc.)
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            log.warn("Unauthorized API request: path={}, remoteAddr={}",
                path, httpRequest.getRemoteAddr());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid X-Api-Key header\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
