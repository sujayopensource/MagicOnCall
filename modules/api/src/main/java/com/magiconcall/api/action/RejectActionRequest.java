package com.magiconcall.api.action;

import jakarta.validation.constraints.NotBlank;

public record RejectActionRequest(@NotBlank String rejectedBy, String reason) {}
