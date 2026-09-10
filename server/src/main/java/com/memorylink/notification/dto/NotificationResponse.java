package com.memorylink.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        Long relationshipId,
        Long otherUserId,
        String otherName,
        String title,
        String body,
        String suggestedRelation,
        String status,
        Instant createdAt
) {
}
