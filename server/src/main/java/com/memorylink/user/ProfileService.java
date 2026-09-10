package com.memorylink.user;

import com.memorylink.common.BusinessException;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.user.dto.ProfileResponse;
import com.memorylink.user.dto.ProfileUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    public static final int CODE_USER_NOT_FOUND = 3003;

    private final UserRepository userRepository;
    private final LovedOneRepository lovedOneRepository;

    public ProfileService(UserRepository userRepository, LovedOneRepository lovedOneRepository) {
        this.userRepository = userRepository;
        this.lovedOneRepository = lovedOneRepository;
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
        if (request.birthPlace() != null && !request.birthPlace().isBlank()) {
            user.setBirthPlace(request.birthPlace().trim());
        }
        if (request.gender() != null && !request.gender().isBlank()) {
            user.setGender(request.gender().trim().toUpperCase());
        }
        userRepository.save(user);
        // 同步本人在各家族的成员档案卡（姓名/出生年月/籍贯）
        for (var person : lovedOneRepository.findByUserId(userId)) {
            if (!person.effectiveDeceased()) {
                person.setName(user.getName());
                person.setBirthDate(user.getBirthDate());
                person.setBirthPlace(user.getBirthPlace());
                lovedOneRepository.save(person);
            }
        }
        return toResponse(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId(), user.getPhone(), user.getName(),
                user.getBirthDate(), user.getBirthPlace(), user.getGender());
    }
}
