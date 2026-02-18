package com.magiconcall.application.tool.tools;

import com.magiconcall.domain.tool.Tool;
import com.magiconcall.domain.tool.ToolRequest;
import com.magiconcall.domain.tool.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TopologyTool implements Tool {

    @Override
    public String name() { return "topology"; }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String service = request.parameters().getOrDefault("service", "unknown");

        String content = """
            [topology] Service dependency map for %s
            Upstream:
              - api-gateway → %s (HTTP, p99=45ms)
              - event-processor → %s (Kafka, topic=orders)
            Downstream:
              - %s → postgres-primary (JDBC, pool=10)
              - %s → redis-cache (TCP, ttl=300s)
              - %s → payment-service (gRPC, p99=120ms)
            """.formatted(service, service, service, service, service, service);

        return ToolResponse.success(content.strip(), Map.of(
            "service", service, "upstreamCount", "2", "downstreamCount", "3"
        ));
    }
}
