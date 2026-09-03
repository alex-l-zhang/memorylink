package com.memorylink.archive;

import com.memorylink.archive.dto.LovedOneRequest;
import com.memorylink.archive.dto.LovedOneResponse;
import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.common.ApiResponse;
import com.memorylink.security.SecurityUtils;
import com.memorylink.security.UserPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/lovedones")
public class LovedOneController {

    private final LovedOneService lovedOneService;

    public LovedOneController(LovedOneService lovedOneService) {
        this.lovedOneService = lovedOneService;
    }

    @PostMapping
    public ApiResponse<LovedOneResponse> create(@Valid @RequestBody LovedOneRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        return ApiResponse.ok(lovedOneService.create(user.userId(), user.phone(), request));
    }

    @GetMapping
    public ApiResponse<List<LovedOneResponse>> list() {
        return ApiResponse.ok(lovedOneService.list(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<LovedOneResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(lovedOneService.get(SecurityUtils.currentUser().userId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<LovedOneResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody LovedOneRequest request) {
        return ApiResponse.ok(lovedOneService.update(SecurityUtils.currentUser().userId(), id, request));
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaResponse> upload(@PathVariable Long id,
                                             @RequestParam("mediaType") String mediaType,
                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(lovedOneService.uploadMedia(SecurityUtils.currentUser().userId(), id, mediaType, file));
    }

    @GetMapping("/{id}/media")
    public ApiResponse<List<MediaResponse>> listMedia(@PathVariable Long id) {
        return ApiResponse.ok(lovedOneService.listMedia(SecurityUtils.currentUser().userId(), id));
    }

    @GetMapping("/{id}/media/{mediaId}/url")
    public ApiResponse<Map<String, String>> mediaUrl(@PathVariable Long id, @PathVariable Long mediaId) {
        String url = lovedOneService.mediaUrl(SecurityUtils.currentUser().userId(), id, mediaId);
        return ApiResponse.ok(Map.of("url", url));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ApiResponse<Void> deleteMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        lovedOneService.deleteMedia(SecurityUtils.currentUser().userId(), id, mediaId);
        return ApiResponse.ok(null);
    }
}
