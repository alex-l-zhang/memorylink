package com.memorylink.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LovedOneRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名过长")
        String name,

        LocalDate birthDate,

        LocalDate deathDate,

        @Size(max = 100, message = "籍贯过长")
        String birthPlace,

        String bio
) {
}
