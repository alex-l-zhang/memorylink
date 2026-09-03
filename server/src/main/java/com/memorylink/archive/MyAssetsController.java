package com.memorylink.archive;

import com.memorylink.archive.dto.MediaResponse;
import com.memorylink.archive.dto.MySelfResponse;
import com.memorylink.common.ApiResponse;
import com.memorylink.consent.ConsentRecord;
import com.memorylink.consent.ConsentRecordRepository;
import com.memorylink.consent.dto.ConsentResponse;
import com.memorylink.security.SecurityUtils;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/my")
public class MyAssetsController {

    private final MyAssetsService myAssetsService;
    private final ConsentRecordRepository consentRecordRepository;

    public MyAssetsController(MyAssetsService myAssetsService,
                              ConsentRecordRepository consentRecordRepository) {
        this.myAssetsService = myAssetsService;
        this.consentRecordRepository = consentRecordRepository;
    }

    @GetMapping("/self-person")
    public ApiResponse<MySelfResponse> selfPerson() {
        return ApiResponse.ok(myAssetsService.selfPerson(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/media")
    public ApiResponse<List<MediaResponse>> listMedia() {
        return ApiResponse.ok(myAssetsService.listMedia(SecurityUtils.currentUser().userId()));
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaResponse> upload(@RequestParam("mediaType") String mediaType,
                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(myAssetsService.uploadMedia(
                SecurityUtils.currentUser().userId(), mediaType, file));
    }

    @DeleteMapping("/media/{mediaId}")
    public ApiResponse<Void> delete(@PathVariable Long mediaId) {
        myAssetsService.deleteMedia(SecurityUtils.currentUser().userId(), mediaId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/consents")
    public ApiResponse<List<ConsentResponse>> myConsents() {
        List<ConsentResponse> records = consentRecordRepository
                .findByConsentorContaining(SecurityUtils.currentUser().userId())
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
