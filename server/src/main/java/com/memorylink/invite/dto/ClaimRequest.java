package com.memorylink.invite.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimRequest(
        @NotBlank(message = "邀请码不能为空")
        String code,

        @NotBlank(message = "关系不能为空")
        String relation
) {
}
