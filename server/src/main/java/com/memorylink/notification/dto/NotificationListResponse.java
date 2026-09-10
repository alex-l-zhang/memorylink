package com.memorylink.notification.dto;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> items, long unread) {
}
