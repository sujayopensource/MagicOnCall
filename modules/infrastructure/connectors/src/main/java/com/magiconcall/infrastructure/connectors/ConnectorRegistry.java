package com.magiconcall.infrastructure.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for external service connectors (PagerDuty, OpsGenie, Slack, etc.).
 * Placeholder — connectors will be registered at startup via config.
 */
@Component
public class ConnectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConnectorRegistry.class);

    private final Map<String, Object> connectors = new ConcurrentHashMap<>();

    public void register(String name, Object connector) {
        connectors.put(name, connector);
        log.info("Connector registered: {}", name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object connector = connectors.get(name);
        if (connector == null) {
            throw new IllegalArgumentException("No connector registered with name: " + name);
        }
        return type.cast(connector);
    }

    public boolean hasConnector(String name) {
        return connectors.containsKey(name);
    }
}
