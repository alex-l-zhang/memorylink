package com.memorylink.user;

import com.memorylink.common.BusinessException;
import com.memorylink.user.dto.ProfileResponse;
import com.memorylink.user.dto.ProfileUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    public static final int CODE_USER_NOT_FOUND = 3003;

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse me(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public ProfileResponse update(Long userId, ProfileUpdateRequest request) {
        User user = requireUser(userId);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        userRepository.save(user);
        return toResponse(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId(), user.getPhone(), user.getName(), user.getBirthDate());
    }
}
