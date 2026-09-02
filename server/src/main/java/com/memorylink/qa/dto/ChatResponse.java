package com.memorylink.qa.dto;

import java.time.Instant;

public record ChatResponse(
        Long conversationId,
        String answer,
        boolean aiFlag,
        String usageHint,
        Instant createdAt
) {
}
