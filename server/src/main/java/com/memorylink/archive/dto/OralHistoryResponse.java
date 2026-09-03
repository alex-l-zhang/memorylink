package com.memorylink.archive.dto;

import java.time.Instant;

public record OralHistoryResponse(
        Long id,
        Long lovedOneId,
        Long mediaFileId,
        String mediaType,
        String title,
        String transcript,
        String visibility,
        Long uploadedBy,
        String url,
        Instant createdAt
) {
}
