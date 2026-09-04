package com.memorylink.account.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "请输入密码确认")
        String password,

        boolean confirm
) {
    @AssertTrue(message = "请确认已阅读注销说明")
    public boolean isConfirm() {
        return confirm;
    }
}
