package com.memorylink.archive.dto;

import java.time.Instant;
import java.time.LocalDate;

public record LovedOneResponse(
        Long id,
        Long familyId,
        String name,
        LocalDate birthDate,
        LocalDate deathDate,
        String birthPlace,
        String bio,
        String status,
        Instant createdAt
) {
}
