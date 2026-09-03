package com.memorylink.persona;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.audit.AuditLog;
import com.memorylink.audit.AuditLogRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.common.UserAge;
import com.memorylink.persona.dto.AiConsentResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonaService {

    public static final int CODE_ARCHIVE_NOT_FOUND = 3002;
    public static final int CODE_REQUIRE_CONSENT = 3006;
    public static final int CODE_NOT_BOUND_TO_SELF = 3007;
    public static final int CODE_ADULT_REQUIRED = 2003;

    private final LovedOneRepository lovedOneRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public PersonaService(LovedOneRepository lovedOneRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository) {
        this.lovedOneRepository = lovedOneRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AiConsentResponse enable(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (lovedOne.effectiveDeceased()) {
            throw new BusinessException(CODE_REQUIRE_CONSENT, "该档案为故人档案，请由近亲属完成知情同意");
        }
        if (!userId.equals(lovedOne.getUserId())) {
            throw new BusinessException(CODE_NOT_BOUND_TO_SELF, "仅档案本人可开启 AI 讲述");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_ADULT_REQUIRED, "用户不存在"));
        if (!UserAge.isAdult(user)) {
            throw new BusinessException(CODE_ADULT_REQUIRED, "需年满 18 周岁且已完善出生日期");
        }
        lovedOne.setAiPersonaEnabled(true);
        lovedOne.setAiEnabledBy(userId);
        lovedOne.setAiEnabledAt(Instant.now());
        lovedOneRepository.save(lovedOne);
        audit(userId, "AI_CONSENT_ENABLED", lovedOneId);
        return new AiConsentResponse(lovedOneId, true);
    }

    @Transactional
    public AiConsentResponse disable(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (lovedOne.effectiveDeceased()) {
            throw new BusinessException(CODE_REQUIRE_CONSENT, "故人档案请走知情同意/关闭流程");
        }
        if (!userId.equals(lovedOne.getUserId())) {
            throw new BusinessException(CODE_NOT_BOUND_TO_SELF, "仅档案本人可关闭 AI 讲述");
        }
        lovedOne.setAiPersonaEnabled(false);
        lovedOneRepository.save(lovedOne);
        audit(userId, "AI_CONSENT_DISABLED", lovedOneId);
        return new AiConsentResponse(lovedOneId, false);
    }

    private void audit(Long actorId, String action, Long lovedOneId) {
        AuditLog log = new AuditLog();
        log.setActorType("USER");
        log.setActorId(actorId);
        log.setAction(action);
        log.setTarget("lovedone:" + lovedOneId);
        log.setDetail(Map.of("lovedOneId", lovedOneId));
        auditLogRepository.save(log);
    }
}
