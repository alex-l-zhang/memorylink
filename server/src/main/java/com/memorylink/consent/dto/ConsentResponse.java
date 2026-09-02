package com.memorylink.consent.dto;

import java.time.Instant;
import java.util.List;

public record ConsentResponse(
        Long id,
        Long lovedOneId,
        String consentType,
        List<Long> consentorIds,
        Instant signedAt,
        String status,
        Instant createdAt
) {
}
