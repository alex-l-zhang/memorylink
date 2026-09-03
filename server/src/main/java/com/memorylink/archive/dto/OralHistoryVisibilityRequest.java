package com.memorylink.archive.dto;

import jakarta.validation.constraints.NotBlank;

public record OralHistoryVisibilityRequest(
        @NotBlank(message = "visibility 不能为空")
        String visibility
) {
}
