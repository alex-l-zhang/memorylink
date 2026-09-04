package com.memorylink.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.memorylink.common.BusinessException;
import com.memorylink.config.JwtService;
import com.memorylink.audit.AuditService;
import com.memorylink.user.dto.AuthResponse;
import com.memorylink.user.dto.LoginRequest;
import com.memorylink.user.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("please-change-me-in-production-0123456789abcdef", 7200);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, auditService);
    }

    @Test
    void registerDuplicatedPhoneFails() {
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("13800138000", "小明", "secret123", "secret123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已注册");
    }

    @Test
    void registerSuccessReturnsToken() {
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        User saved = new User();
        saved.setId(1L);
        saved.setPhone("13800138000");
        saved.setName("小明");
        saved.setRole("USER");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse response = authService.register(
                new RegisterRequest("13800138000", "小明", "secret123", "secret123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.phone()).isEqualTo("13800138000");
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("13800138000", "小明", "secret123", "different")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不一致");
    }

    @Test
    void loginWrongPasswordFails() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setName("小明");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("13800138000", "wrong-pass")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("错误");
    }
}
