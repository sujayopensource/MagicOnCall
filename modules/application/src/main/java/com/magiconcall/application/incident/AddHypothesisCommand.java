package com.magiconcall.application.incident;

public record AddHypothesisCommand(
    String title,
    String description,
    double confidence,
    String source
) {}
