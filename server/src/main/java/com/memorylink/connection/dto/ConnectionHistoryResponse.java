package com.memorylink.connection.dto;

import java.time.Instant;

public record ConnectionHistoryResponse(
        Long id,
        Long targetId,
        String targetName,
        String relation,
        String inverseRelation,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}
