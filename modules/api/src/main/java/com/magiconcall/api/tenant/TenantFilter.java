package com.magiconcall.api.tenant;

import com.magiconcall.domain.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extracts X-Customer-Id header and sets TenantContext for the request.
 * Required for all /api/** requests.
 */
@Component
@Order(2)
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Customer-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = httpRequest.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"MISSING_TENANT\",\"message\":\"X-Customer-Id header is required\"}");
            return;
        }

        try {
            TenantContext.setTenantId(tenantId);
            log.debug("Tenant context set: tenantId={}", tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
