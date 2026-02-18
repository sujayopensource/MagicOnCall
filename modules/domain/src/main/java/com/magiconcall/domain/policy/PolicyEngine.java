package com.magiconcall.domain.policy;

/**
 * Core policy evaluation interface.
 * Deterministic-first: rule-based evaluation before any LLM fallback.
 */
public interface PolicyEngine {

    PolicyDecision evaluate(PolicyContext context);

    boolean supports(String action);
}
