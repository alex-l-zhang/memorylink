package com.memorylink.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名过长")
        String name,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 位")
        String password,

        @NotBlank(message = "请再次输入密码")
        String confirmPassword
) {
}
