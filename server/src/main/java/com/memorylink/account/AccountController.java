package com.memorylink.account;

import com.memorylink.account.dto.DeleteAccountRequest;
import com.memorylink.account.dto.DeletionPreviewResponse;
import com.memorylink.common.ApiResponse;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/deletion-preview")
    public ApiResponse<DeletionPreviewResponse> preview() {
        return ApiResponse.ok(accountService.preview(SecurityUtils.currentUser().userId()));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        accountService.delete(SecurityUtils.currentUser().userId(), request.password());
        return ApiResponse.ok(null);
    }
}
