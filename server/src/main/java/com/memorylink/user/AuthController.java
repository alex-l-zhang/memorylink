package com.memorylink.user;

import com.memorylink.common.ApiResponse;
import com.memorylink.user.dto.AuthResponse;
import com.memorylink.user.dto.LoginRequest;
import com.memorylink.user.dto.RegisterRequest;
import com.memorylink.audit.AuditService;
import com.memorylink.security.SecurityUtils;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        var user = SecurityUtils.currentUser();
        auditService.log("USER", user.userId(), "LOGOUT", "user:" + user.userId(), Map.of());
        return ApiResponse.ok(null);
    }
}
