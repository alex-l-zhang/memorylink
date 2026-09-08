package com.memorylink.connection.dto;

public record UserCandidateResponse(
        Long id,
        String name,
        Integer birthYear,
        Integer birthMonth,
        String birthPlace
) {
}
