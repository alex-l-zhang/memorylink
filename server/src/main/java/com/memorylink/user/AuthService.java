package com.memorylink.user;

import com.memorylink.common.BusinessException;
import com.memorylink.config.JwtService;
import com.memorylink.user.dto.AuthResponse;
import com.memorylink.user.dto.LoginRequest;
import com.memorylink.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final int CODE_PHONE_EXISTS = 3001;
    public static final int CODE_BAD_CREDENTIALS = 1003;
    public static final int CODE_PASSWORD_MISMATCH = 2002;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        return new AuthResponse(token, user.getId(), user.getPhone(), user.getName());
    }
}
