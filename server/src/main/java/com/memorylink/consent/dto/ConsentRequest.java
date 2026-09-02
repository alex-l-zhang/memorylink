package com.memorylink.consent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ConsentRequest(
        @NotBlank(message = "consentType 不能为空")
        String consentType,

        @NotEmpty(message = "确认人不能为空")
        List<Long> consentorIds
) {
}
