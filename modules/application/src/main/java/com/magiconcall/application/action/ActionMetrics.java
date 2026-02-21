package com.magiconcall.application.action;

public interface ActionMetrics {

    void recordProposal(String toolName, String riskLevel, String outcome);

    void recordApproval(String toolName);

    void recordRejection(String toolName);

    void recordExecution(String toolName, String status);

    void recordRetry(String toolName);
}
