package com.memorylink.user.dto;

import java.time.LocalDate;

public record ProfileResponse(
        Long id,
        String phone,
        String name,
        LocalDate birthDate
) {
}
