package com.memorylink.invite;

import com.memorylink.common.ApiResponse;
import com.memorylink.invite.dto.ClaimRequest;
import com.memorylink.invite.dto.ClaimResponse;
import com.memorylink.invite.dto.InviteKeyRequest;
import com.memorylink.invite.dto.InviteKeyResponse;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/lovedones/{lovedOneId}/invite-key")
    public ApiResponse<InviteKeyResponse> generate(@PathVariable Long lovedOneId,
                                                   @RequestBody(required = false) InviteKeyRequest request) {
        String role = request == null ? null : request.role();
        Integer hours = request == null ? null : request.hours();
        return ApiResponse.ok(inviteService.generate(
                SecurityUtils.currentUser().userId(), lovedOneId, role, hours));
    }

    @PostMapping("/invites/claim")
    public ApiResponse<ClaimResponse> claim(@Valid @RequestBody ClaimRequest request) {
        return ApiResponse.ok(inviteService.claim(
                SecurityUtils.currentUser().userId(), request.code(), request.relation()));
    }
}
