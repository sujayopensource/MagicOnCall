package com.magiconcall.domain.tenant;

/**
 * ThreadLocal holder for the current tenant ID.
 * Set by TenantFilter from X-Customer-Id header, cleared after request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static String requireTenantId() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant ID not set — missing X-Customer-Id header?");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
