package com.magiconcall.domain.llm;

public record LlmRequest(
    String systemPrompt,
    String userPrompt,
    int maxTokens,
    String model
) {}
