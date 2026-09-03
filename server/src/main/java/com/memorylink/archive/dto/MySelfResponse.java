package com.memorylink.archive.dto;

public record MySelfResponse(
        Long id,
        String name,
        boolean isDeceased,
        boolean aiPersonaEnabled
) {
}
