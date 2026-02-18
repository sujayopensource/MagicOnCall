package com.magiconcall.application.triage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.llm.LlmRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("returns valid JSON array of hypotheses")
    void validJson() throws Exception {
        var request = new LlmRequest("system prompt", "user prompt", 4096, null);

        var response = client.complete(request);

        assertThat(response.content()).isNotBlank();
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.model()).isEqualTo("mock");

        // Verify parseable as list of maps
        List<Map<String, Object>> hypotheses = objectMapper.readValue(
            response.content(), new TypeReference<>() {});

        assertThat(hypotheses).hasSize(3);
        for (var h : hypotheses) {
            assertThat(h).containsKeys("title", "description", "confidence",
                "evidenceFor", "evidenceAgainst", "nextBestTest", "stopCondition");
        }
    }

    @Test
    @DisplayName("token counts are positive")
    void tokenCounts() {
        var request = new LlmRequest("system", "user input", 1000, null);

        var response = client.complete(request);

        assertThat(response.promptTokens()).isGreaterThan(0);
        assertThat(response.completionTokens()).isGreaterThan(0);
        assertThat(response.totalTokens()).isEqualTo(response.promptTokens() + response.completionTokens());
    }
}
