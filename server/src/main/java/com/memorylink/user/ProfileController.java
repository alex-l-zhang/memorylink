package com.memorylink.user;

import com.memorylink.common.ApiResponse;
import com.memorylink.security.SecurityUtils;
import com.memorylink.user.dto.ProfileResponse;
import com.memorylink.user.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> me() {
        return ApiResponse.ok(profileService.me(SecurityUtils.currentUser().userId()));
    }

    @PatchMapping("/me")
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(profileService.update(SecurityUtils.currentUser().userId(), request));
    }
}
