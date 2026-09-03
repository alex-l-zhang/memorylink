package com.memorylink.invite.dto;

import java.time.Instant;

public record InviteKeyResponse(
        String code,
        Long lovedOneId,
        String role,
        Instant expiresAt
) {
}
