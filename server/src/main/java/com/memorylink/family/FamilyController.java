package com.memorylink.family;

import com.memorylink.common.ApiResponse;
import com.memorylink.family.dto.FamilyMemberResponse;
import com.memorylink.family.dto.FamilyResponse;
import com.memorylink.family.dto.MemberInviteRequest;
import com.memorylink.security.SecurityUtils;
import com.memorylink.security.UserPrincipal;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;
    private final UserRepository userRepository;

    public FamilyController(FamilyService familyService, UserRepository userRepository) {
        this.familyService = familyService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ApiResponse<FamilyResponse> myFamily() {
        UserPrincipal user = SecurityUtils.currentUser();
        Family family = familyService.getOrCreateDefaultFamily(user.userId(), user.phone());
        return ApiResponse.ok(toResponse(user.userId(), family));
    }

    @GetMapping("/{familyId}/members")
    public ApiResponse<List<FamilyMemberResponse>> members(@PathVariable Long familyId) {
        UserPrincipal user = SecurityUtils.currentUser();
        if (!familyService.canAccess(user.userId(), familyId)) {
            return ApiResponse.error(4001, "无权访问该家族");
        }
        return ApiResponse.ok(familyService.membersOf(familyId).stream()
                .map(m -> toMemberResponse(m, userRepository.findById(m.getUserId()).orElse(null)))
                .toList());
    }

    @PostMapping("/{familyId}/members")
    public ApiResponse<FamilyMemberResponse> invite(@PathVariable Long familyId,
                                                    @Valid @RequestBody MemberInviteRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        FamilyMember member = familyService.invite(user.userId(), familyId, request.phone(), request.role());
        User invited = userRepository.findById(member.getUserId()).orElse(null);
        return ApiResponse.ok(toMemberResponse(member, invited));
    }

    private FamilyResponse toResponse(Long userId, Family family) {
        String myRole = familyService.membersOf(family.getId()).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .map(FamilyMember::getRole)
                .orElse("VIEWER");
        List<FamilyMemberResponse> members = familyService.membersOf(family.getId()).stream()
                .map(m -> toMemberResponse(m, userRepository.findById(m.getUserId()).orElse(null)))
                .toList();
        return new FamilyResponse(family.getId(), family.getName(), myRole, members);
    }

    private FamilyMemberResponse toMemberResponse(FamilyMember member, User user) {
        return new FamilyMemberResponse(
                member.getUserId(),
                user == null ? null : user.getName(),
                user == null ? null : user.getPhone(),
                member.getRole(),
                member.getRelation(),
                member.getStatus()
        );
    }
}
