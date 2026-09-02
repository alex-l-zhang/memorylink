package com.memorylink.consent;

import com.memorylink.common.ApiResponse;
import com.memorylink.consent.dto.ConsentRequest;
import com.memorylink.consent.dto.ConsentResponse;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lovedones/{lovedOneId}")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping("/consent")
    public ApiResponse<ConsentResponse> create(@PathVariable Long lovedOneId,
                                               @Valid @RequestBody ConsentRequest request) {
        ConsentRecord record = consentService.create(
                SecurityUtils.currentUser().userId(), lovedOneId, request.consentType(), request.consentorIds());
        return ApiResponse.ok(toResponse(record));
    }

    @GetMapping("/consents")
    public ApiResponse<List<ConsentResponse>> list(@PathVariable Long lovedOneId) {
        List<ConsentResponse> records = consentService.list(SecurityUtils.currentUser().userId(), lovedOneId)
                .stream().map(this::toResponse).toList();
        return ApiResponse.ok(records);
    }

    private ConsentResponse toResponse(ConsentRecord record) {
        return new ConsentResponse(
                record.getId(),
                record.getLovedOneId(),
                record.getConsentType(),
                record.getConsentorIds(),
                record.getSignedAt(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }
}
