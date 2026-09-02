package com.memorylink.family.dto;

import java.util.List;

public record FamilyResponse(
        Long id,
        String name,
        String myRole,
        List<FamilyMemberResponse> members
) {
}
