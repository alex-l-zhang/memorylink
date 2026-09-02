package com.memorylink.family.dto;

public record FamilyMemberResponse(
        Long userId,
        String name,
        String phone,
        String role,
        String relation,
        String status
) {
}
