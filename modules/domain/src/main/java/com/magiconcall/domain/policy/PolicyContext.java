package com.magiconcall.domain.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PolicyContext {

    private final String action;
    private final String resourceType;
    private final Map<String, Object> attributes;

    private PolicyContext(String action, String resourceType, Map<String, Object> attributes) {
        this.action = action;
        this.resourceType = resourceType;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public static Builder builder(String action, String resourceType) {
        return new Builder(action, resourceType);
    }

    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Map<String, Object> getAttributes() { return attributes; }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public static class Builder {
        private final String action;
        private final String resourceType;
        private final Map<String, Object> attributes = new HashMap<>();

        private Builder(String action, String resourceType) {
            this.action = action;
            this.resourceType = resourceType;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public PolicyContext build() {
            return new PolicyContext(action, resourceType, attributes);
        }
    }
}
