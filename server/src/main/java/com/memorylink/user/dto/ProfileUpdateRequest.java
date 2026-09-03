package com.memorylink.user.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProfileUpdateRequest(
        @Size(max = 50, message = "姓名过长")
        String name,

        @PastOrPresent(message = "出生日期不能晚于今天")
        LocalDate birthDate
) {
}
