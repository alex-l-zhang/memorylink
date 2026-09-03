package com.memorylink.archive;

import com.memorylink.archive.dto.OralHistoryResponse;
import com.memorylink.archive.dto.OralHistoryVisibilityRequest;
import com.memorylink.common.ApiResponse;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lovedones/{lovedOneId}/oral-histories")
public class OralHistoryController {

    private final OralHistoryService oralHistoryService;

    public OralHistoryController(OralHistoryService oralHistoryService) {
        this.oralHistoryService = oralHistoryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OralHistoryResponse> upload(@PathVariable Long lovedOneId,
                                                   @RequestParam("mediaType") String mediaType,
                                                   @RequestParam(value = "title", required = false) String title,
                                                   @RequestParam(value = "transcript", required = false) String transcript,
                                                   @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(oralHistoryService.upload(
                SecurityUtils.currentUser().userId(), lovedOneId, mediaType, title, transcript, file));
    }

    @GetMapping
    public ApiResponse<List<OralHistoryResponse>> list(@PathVariable Long lovedOneId) {
        return ApiResponse.ok(oralHistoryService.list(
                SecurityUtils.currentUser().userId(), lovedOneId));
    }

    @PatchMapping("/{oralHistoryId}")
    public ApiResponse<OralHistoryResponse> updateVisibility(@PathVariable Long lovedOneId,
                                                             @PathVariable Long oralHistoryId,
                                                             @Valid @RequestBody OralHistoryVisibilityRequest request) {
        return ApiResponse.ok(oralHistoryService.updateVisibility(
                SecurityUtils.currentUser().userId(), lovedOneId, oralHistoryId, request.visibility()));
    }

    @DeleteMapping("/{oralHistoryId}")
    public ApiResponse<Void> delete(@PathVariable Long lovedOneId, @PathVariable Long oralHistoryId) {
        oralHistoryService.delete(SecurityUtils.currentUser().userId(), lovedOneId, oralHistoryId);
        return ApiResponse.ok(null);
    }
}
