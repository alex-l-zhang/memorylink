package com.memorylink.archive;

import com.memorylink.archive.dto.OralHistoryResponse;
import com.memorylink.common.ApiResponse;
import com.memorylink.security.SecurityUtils;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/my/oral-histories")
public class MyOralHistoryController {

    private final OralHistoryService oralHistoryService;

    public MyOralHistoryController(OralHistoryService oralHistoryService) {
        this.oralHistoryService = oralHistoryService;
    }

    @GetMapping
    public ApiResponse<List<OralHistoryResponse>> listMine() {
        return ApiResponse.ok(oralHistoryService.listMine(SecurityUtils.currentUser().userId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OralHistoryResponse> uploadMine(
            @RequestParam("mediaType") String mediaType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "transcript", required = false) String transcript,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(oralHistoryService.uploadMine(
                SecurityUtils.currentUser().userId(), mediaType, title, transcript, file));
    }
}
