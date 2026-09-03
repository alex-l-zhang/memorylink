package com.memorylink.persona;

import com.memorylink.common.ApiResponse;
import com.memorylink.persona.dto.AiConsentResponse;
import com.memorylink.security.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lovedones/{lovedOneId}")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @PostMapping("/ai-consent")
    public ApiResponse<AiConsentResponse> enable(@PathVariable Long lovedOneId) {
        return ApiResponse.ok(personaService.enable(SecurityUtils.currentUser().userId(), lovedOneId));
    }

    @DeleteMapping("/ai-consent")
    public ApiResponse<AiConsentResponse> disable(@PathVariable Long lovedOneId) {
        return ApiResponse.ok(personaService.disable(SecurityUtils.currentUser().userId(), lovedOneId));
    }
}
