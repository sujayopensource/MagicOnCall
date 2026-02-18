package com.magiconcall.domain.llm;

/**
 * Port interface for LLM completions.
 * Adapters: MockLlmClient (default), future real provider implementations.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
