package com.magiconcall.application.tool.tools;

import com.magiconcall.domain.tool.Tool;
import com.magiconcall.domain.tool.ToolRequest;
import com.magiconcall.domain.tool.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MetricsTool implements Tool {

    @Override
    public String name() { return "metrics"; }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String service = request.parameters().getOrDefault("service", "unknown");
        String metric = request.parameters().getOrDefault("metric", "latency_p99");
        String timeRange = request.parameters().getOrDefault("timeRange", "1h");

        String content = """
            [metrics] %s for service=%s over %s
            p50: 45ms
            p90: 120ms
            p99: 890ms
            error_rate: 4.2%%
            throughput: 1,250 req/s
            """.formatted(metric, service, timeRange);

        return ToolResponse.success(content.strip(), Map.of(
            "service", service, "metric", metric, "timeRange", timeRange
        ));
    }
}
