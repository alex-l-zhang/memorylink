package com.memorylink.qa;

import com.memorylink.common.ApiResponse;
import com.memorylink.qa.dto.ChatRequest;
import com.memorylink.qa.dto.ChatResponse;
import com.memorylink.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lovedones/{lovedOneId}")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@PathVariable Long lovedOneId,
                                          @Valid @RequestBody ChatRequest request) {
        ChatResponse response = qaService.chat(
                SecurityUtils.currentUser().userId(), lovedOneId, request.question());
        return ApiResponse.ok(response);
    }
}
