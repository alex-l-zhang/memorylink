package com.memorylink.connection.dto;

public record RelationshipResponse(
        Long id,
        Long otherUserId,
        String otherName,
        String relationFromMe,
        String relationFromOther
) {
}
