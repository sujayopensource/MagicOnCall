package com.magiconcall.api.action;

import jakarta.validation.constraints.NotBlank;

public record ApproveActionRequest(@NotBlank String approvedBy) {}
