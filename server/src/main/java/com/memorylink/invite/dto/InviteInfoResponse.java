package com.memorylink.invite.dto;

import java.time.Instant;

public record InviteInfoResponse(
        Long lovedOneId,
        String inviterName,
        String inviterPhone,
        String targetName,
        String role,
        Instant expiresAt
) {
}
