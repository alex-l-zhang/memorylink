package com.memorylink.archive.dto;

import java.time.Instant;

public record MediaResponse(
        Long id,
        Long lovedOneId,
        String mediaType,
        String objectKey,
        Long sizeBytes,
        Instant createdAt,
        String url
) {
}
