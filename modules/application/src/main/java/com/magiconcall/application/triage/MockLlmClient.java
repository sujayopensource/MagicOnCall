package com.magiconcall.application.triage;

import com.magiconcall.domain.llm.LlmClient;
import com.magiconcall.domain.llm.LlmRequest;
import com.magiconcall.domain.llm.LlmResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "magiconcall.llm.enabled", havingValue = "false", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private static final String MOCK_RESPONSE = """
        [
          {
            "title": "Database connection pool exhaustion",
            "description": "The application may be leaking database connections, causing pool exhaustion under load.",
            "confidence": 0.75,
            "evidenceFor": "Connection timeout errors in logs, rising active connection count in metrics",
            "evidenceAgainst": "No recent deployment changes to connection handling code",
            "nextBestTest": "Check HikariCP metrics for active/idle/pending connection counts",
            "stopCondition": "Connection pool metrics return to normal levels after fix"
          },
          {
            "title": "Upstream service degradation",
            "description": "A dependent service may be responding slowly, causing thread pool exhaustion.",
            "confidence": 0.60,
            "evidenceFor": "Increased latency in service dependency metrics",
            "evidenceAgainst": "No alerts from upstream service dashboards",
            "nextBestTest": "Check upstream service health endpoints and latency percentiles",
            "stopCondition": "Upstream service latency returns below p99 SLA threshold"
          },
          {
            "title": "Memory pressure from GC pauses",
            "description": "Long GC pauses may be causing request timeouts and cascading failures.",
            "confidence": 0.45,
            "evidenceFor": "GC pause duration spikes correlate with error rate increases",
            "evidenceAgainst": "Heap usage appears within normal bounds",
            "nextBestTest": "Review GC logs for stop-the-world pause durations > 500ms",
            "stopCondition": "GC pause times consistently below 200ms"
          }
        ]
        """;

    @Override
    public LlmResponse complete(LlmRequest request) {
        int promptTokens = estimateTokens(request.systemPrompt()) + estimateTokens(request.userPrompt());
        int completionTokens = estimateTokens(MOCK_RESPONSE);
        return new LlmResponse(MOCK_RESPONSE, promptTokens, completionTokens, "mock", "stop");
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return text.length() / 4;
    }
}
