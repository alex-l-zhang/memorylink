package com.memorylink.user.dto;

public record AuthResponse(String token, Long userId, String phone, String name) {
}
