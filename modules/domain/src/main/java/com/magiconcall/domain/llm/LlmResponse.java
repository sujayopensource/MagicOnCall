package com.magiconcall.domain.llm;

public record LlmResponse(
    String content,
    int promptTokens,
    int completionTokens,
    String model,
    String finishReason
) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
