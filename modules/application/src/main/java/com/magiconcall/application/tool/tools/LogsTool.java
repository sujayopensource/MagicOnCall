package com.magiconcall.application.tool.tools;

import com.magiconcall.domain.tool.Tool;
import com.magiconcall.domain.tool.ToolRequest;
import com.magiconcall.domain.tool.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LogsTool implements Tool {

    @Override
    public String name() { return "logs"; }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String service = request.parameters().getOrDefault("service", "unknown");
        String timeRange = request.parameters().getOrDefault("timeRange", "1h");
        String query = request.parameters().getOrDefault("query", "*");

        String content = """
            [logs] Results for service=%s, timeRange=%s, query=%s
            2024-01-15T10:30:00Z ERROR Connection pool exhausted — HikariPool-1
            2024-01-15T10:30:05Z WARN  Retry attempt 3/3 failed for downstream call
            2024-01-15T10:30:10Z ERROR Request timeout after 30s on /api/v1/orders
            """.formatted(service, timeRange, query);

        return ToolResponse.success(content.strip(), Map.of(
            "service", service, "timeRange", timeRange, "resultCount", "3"
        ));
    }
}
