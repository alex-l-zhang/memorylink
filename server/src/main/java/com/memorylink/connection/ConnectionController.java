package com.memorylink.connection;

import com.memorylink.common.ApiResponse;
import com.memorylink.connection.dto.ConnectionRequestResponse;
import com.memorylink.connection.dto.ConnectionHistoryResponse;
import com.memorylink.connection.dto.AcceptConnectionRequest;
import com.memorylink.connection.dto.RelationshipGraphResponse;
import com.memorylink.connection.dto.RelationshipResponse;
import com.memorylink.connection.dto.SendConnectionRequest;
import com.memorylink.connection.dto.UserCandidateResponse;
import com.memorylink.notification.NotificationService;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final NotificationService notificationService;

    public ConnectionController(ConnectionService connectionService,
                                NotificationService notificationService) {
        this.connectionService = connectionService;
        this.notificationService = notificationService;
    }

    @GetMapping("/search")
    public ApiResponse<List<UserCandidateResponse>> search(@RequestParam("name") String name) {
        return ApiResponse.ok(connectionService.search(SecurityUtils.currentUser().userId(), name));
    }

    @PostMapping("/requests")
    public ApiResponse<Map<String, Integer>> send(@Valid @RequestBody SendConnectionRequest request) {
        int sent = connectionService.send(
                SecurityUtils.currentUser().userId(), request.targetIds(),
                request.relation(), request.inverseRelation());
        return ApiResponse.ok(Map.of("sent", sent));
    }

    @GetMapping("/requests/incoming")
    public ApiResponse<List<ConnectionRequestResponse>> incoming() {
        return ApiResponse.ok(connectionService.incoming(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/requests/received")
    public ApiResponse<List<ConnectionRequestResponse>> received() {
        return ApiResponse.ok(connectionService.received(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/requests/outgoing")
    public ApiResponse<List<ConnectionHistoryResponse>> outgoing() {
        return ApiResponse.ok(connectionService.outgoing(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/relationships")
    public ApiResponse<List<RelationshipResponse>> relationships() {
        return ApiResponse.ok(connectionService.relationships(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/graph")
    public ApiResponse<RelationshipGraphResponse> graph(
            @RequestParam(value = "includePending", defaultValue = "false") boolean includePending,
            @RequestParam(value = "includeExtended", defaultValue = "false") boolean includeExtended) {
        return ApiResponse.ok(connectionService.graph(
                SecurityUtils.currentUser().userId(), includePending, includeExtended));
    }

    @PostMapping("/relationships/{relationshipId}/confirm")
    public ApiResponse<Void> confirmRelationship(@PathVariable Long relationshipId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String relation = body == null ? null : body.get("relation");
        notificationService.confirmByRelationship(
                SecurityUtils.currentUser().userId(), relationshipId, relation);
        return ApiResponse.ok(null);
    }

    @PostMapping("/relationships/{relationshipId}/reject")
    public ApiResponse<Void> rejectRelationship(@PathVariable Long relationshipId) {
        notificationService.rejectByRelationship(
                SecurityUtils.currentUser().userId(), relationshipId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/relationships/{relationshipId}")
    public ApiResponse<Void> removeRelationship(@PathVariable Long relationshipId) {
        connectionService.removeRelationship(SecurityUtils.currentUser().userId(), relationshipId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<Void> accept(@PathVariable Long requestId,
                                    @RequestBody(required = false) AcceptConnectionRequest request) {
        connectionService.accept(SecurityUtils.currentUser().userId(), requestId,
                request == null ? null : request.inverseRelation());
        return ApiResponse.ok(null);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long requestId) {
        connectionService.reject(SecurityUtils.currentUser().userId(), requestId);
        return ApiResponse.ok(null);
    }
}
