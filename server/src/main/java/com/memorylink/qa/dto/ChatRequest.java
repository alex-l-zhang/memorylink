package com.memorylink.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题过长")
        String question
) {
}
