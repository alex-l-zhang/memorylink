package com.memorylink.notification;

import com.memorylink.common.ApiResponse;
import com.memorylink.notification.dto.NotificationListResponse;
import com.memorylink.security.SecurityUtils;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<NotificationListResponse> list() {
        return ApiResponse.ok(notificationService.list(SecurityUtils.currentUser().userId()));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        String relation = body == null ? null : body.get("relation");
        notificationService.confirm(SecurityUtils.currentUser().userId(), id, relation);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        notificationService.reject(SecurityUtils.currentUser().userId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/confirm-all")
    public ApiResponse<Map<String, Integer>> confirmAll() {
        int count = notificationService.confirmAll(SecurityUtils.currentUser().userId());
        return ApiResponse.ok(Map.of("confirmed", count));
    }
}
