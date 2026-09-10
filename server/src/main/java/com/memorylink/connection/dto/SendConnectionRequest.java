package com.memorylink.connection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SendConnectionRequest(
        @NotEmpty(message = "请选择联系人")
        List<Long> targetIds,

        @NotBlank(message = "请选择与联系人的关系")
        String relation,

        String inverseRelation
) {
}
