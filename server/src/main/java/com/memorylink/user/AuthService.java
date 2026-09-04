package com.memorylink.user;

import com.memorylink.common.BusinessException;
import com.memorylink.config.JwtService;
import com.memorylink.audit.AuditService;
import com.memorylink.user.dto.AuthResponse;
import com.memorylink.user.dto.LoginRequest;
import com.memorylink.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class AuthService {

    public static final int CODE_PHONE_EXISTS = 3001;
    public static final int CODE_BAD_CREDENTIALS = 1003;
    public static final int CODE_PASSWORD_MISMATCH = 2002;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(CODE_PASSWORD_MISMATCH, "两次输入的密码不一致");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(CODE_PHONE_EXISTS, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getPhone(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getPhone(), user.getName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(CODE_BAD_CREDENTIALS, "手机号或密码错误"));
        String token = jwtService.generateToken(user.getId(), user.getPhone(), user.getRole());
        auditService.log("USER", user.getId(), "LOGIN", "user:" + user.getId(),
                Map.of("phone", mask(user.getPhone())));
        return new AuthResponse(token, user.getId(), user.getPhone(), user.getName());
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
