package com.magiconcall.application.triage;

public class TokenBudgetExceededException extends RuntimeException {

    private final int estimatedTokens;
    private final int budget;

    public TokenBudgetExceededException(int estimatedTokens, int budget) {
        super("Estimated token usage %d exceeds budget %d".formatted(estimatedTokens, budget));
        this.estimatedTokens = estimatedTokens;
        this.budget = budget;
    }

    public int getEstimatedTokens() { return estimatedTokens; }
    public int getBudget() { return budget; }
}
