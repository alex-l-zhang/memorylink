package com.memorylink.connection.dto;

import java.time.Instant;

public record ConnectionRequestResponse(
        Long id,
        String requesterName,
        Integer birthYear,
        Integer birthMonth,
        String birthPlace,
        String relation,
        String status,
        Instant createdAt
) {
}
