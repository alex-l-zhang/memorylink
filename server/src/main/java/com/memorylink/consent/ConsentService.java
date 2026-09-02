package com.memorylink.consent;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

    public static final int CODE_ARCHIVE_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_INVALID = 2002;

    private static final Set<String> CONSENT_TYPES = Set.of("PRE_AUTHORIZED", "TWO_RELATIVES");

    private final ConsentRecordRepository consentRecordRepository;
    private final LovedOneRepository lovedOneRepository;
    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepository;

    public ConsentService(ConsentRecordRepository consentRecordRepository,
                          LovedOneRepository lovedOneRepository,
                          FamilyService familyService,
                          FamilyMemberRepository familyMemberRepository) {
        this.consentRecordRepository = consentRecordRepository;
        this.lovedOneRepository = lovedOneRepository;
        this.familyService = familyService;
        this.familyMemberRepository = familyMemberRepository;
    }

    @Transactional
    public ConsentRecord create(Long userId, Long lovedOneId, String consentType, List<Long> consentorIds) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        String type = consentType == null ? "" : consentType.trim().toUpperCase();
        if (!CONSENT_TYPES.contains(type)) {
            throw new BusinessException(CODE_INVALID, "consentType 仅支持 PRE_AUTHORIZED/TWO_RELATIVES");
        }
        List<Long> distinct = consentorIds == null ? List.of()
                : consentorIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        int required = "TWO_RELATIVES".equals(type) ? 2 : 1;
        if (distinct.size() < required) {
            throw new BusinessException(CODE_INVALID,
                    required == 2 ? "两名近亲共同确认至少需要 2 位确认人" : "至少需要 1 位确认人");
        }
        for (Long consentorId : distinct) {
            boolean member = familyMemberRepository.findByFamilyIdAndUserId(lovedOne.getFamilyId(), consentorId)
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .isPresent();
            if (!member) {
                throw new BusinessException(CODE_INVALID, "确认人必须为家族成员");
            }
        }
        ConsentRecord record = new ConsentRecord();
        record.setLovedOneId(lovedOneId);
        record.setConsentType(type);
        record.setConsentorIds(distinct);
        record.setSignedAt(Instant.now());
        record.setStatus("VALID");
        return consentRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> list(Long userId, Long lovedOneId) {
        requireAccess(userId, lovedOneId);
        return consentRecordRepository.findByLovedOneIdOrderByCreatedAtDesc(lovedOneId);
    }

    private LovedOne requireAccess(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (!familyService.canAccess(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该档案");
        }
        return lovedOne;
    }
}
