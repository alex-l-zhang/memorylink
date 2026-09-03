package com.memorylink.invite;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.audit.AuditLog;
import com.memorylink.audit.AuditLogRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.invite.dto.ClaimResponse;
import com.memorylink.invite.dto.InviteKeyResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {

    public static final int CODE_ARCHIVE_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_INVALID = 2002;
    public static final int CODE_ALREADY_MEMBER = 3004;
    public static final int CODE_KEY_INVALID = 3005;

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> ROLES = Set.of("VIEWER", "EDITOR");
    private static final Set<String> RELATIONS = Set.of("SPOUSE", "CHILD", "GRANDCHILD", "SIBLING", "OTHER");

    private final InviteKeyRepository inviteKeyRepository;
    private final LovedOneRepository lovedOneRepository;
    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecureRandom random = new SecureRandom();

    public InviteService(InviteKeyRepository inviteKeyRepository,
                         LovedOneRepository lovedOneRepository,
                         FamilyService familyService,
                         FamilyMemberRepository familyMemberRepository,
                         AuditLogRepository auditLogRepository) {
        this.inviteKeyRepository = inviteKeyRepository;
        this.lovedOneRepository = lovedOneRepository;
        this.familyService = familyService;
        this.familyMemberRepository = familyMemberRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public InviteKeyResponse generate(Long userId, Long lovedOneId, String role, Integer hours) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (!familyService.canManage(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅家族创建者/共建者可生成邀请码");
        }
        String targetRole = role == null ? "VIEWER" : role.trim().toUpperCase();
        if (!ROLES.contains(targetRole)) {
            throw new BusinessException(CODE_INVALID, "角色仅支持 VIEWER/EDITOR");
        }
        int validHours = hours == null ? 72 : hours;
        if (validHours < 1 || validHours > 720) {
            throw new BusinessException(CODE_INVALID, "有效时长需在 1-720 小时之间");
        }

        String plain = randomCode();
        InviteKey key = new InviteKey();
        key.setLovedOneId(lovedOneId);
        key.setCodeHash(sha256Hex(plain));
        key.setCreatedBy(userId);
        key.setRole(targetRole);
        key.setExpiresAt(Instant.now().plusSeconds(validHours * 3600L));
        key.setMaxUses(1);
        key.setStatus("ACTIVE");
        inviteKeyRepository.save(key);
        audit("USER", userId, "INVITE_KEY_GENERATED", "lovedone:" + lovedOneId,
                Map.of("inviteKeyId", key.getId()));
        return new InviteKeyResponse(formatCode(plain), lovedOneId, targetRole, key.getExpiresAt());
    }

    @Transactional
    public ClaimResponse claim(Long userId, String code, String relation) {
        String normalized = normalize(code);
        InviteKey key = inviteKeyRepository.findFirstByCodeHashOrderByIdDesc(sha256Hex(normalized))
                .orElseThrow(() -> new BusinessException(CODE_KEY_INVALID, "邀请码无效或已过期"));
        if (!"ACTIVE".equals(key.getStatus())
                || key.getExpiresAt().isBefore(Instant.now())
                || key.getUsedCount() >= key.getMaxUses()) {
            throw new BusinessException(CODE_KEY_INVALID, "邀请码无效或已过期");
        }
        String targetRelation = relation == null ? "" : relation.trim().toUpperCase();
        if (!RELATIONS.contains(targetRelation)) {
            throw new BusinessException(CODE_INVALID, "关系仅支持 SPOUSE/CHILD/GRANDCHILD/SIBLING/OTHER");
        }
        LovedOne lovedOne = lovedOneRepository.findById(key.getLovedOneId())
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (familyMemberRepository.existsByFamilyIdAndUserId(lovedOne.getFamilyId(), userId)) {
            throw new BusinessException(CODE_ALREADY_MEMBER, "您已是该家族成员");
        }

        FamilyMember member = new FamilyMember();
        member.setFamilyId(lovedOne.getFamilyId());
        member.setUserId(userId);
        member.setRelation(targetRelation);
        member.setRole(key.getRole());
        member.setStatus("ACTIVE");
        member.setEvidenceStatus("SELF_DECLARED");
        member.setRelationSource("INVITE_KEY");
        familyMemberRepository.save(member);

        key.setUsedCount(key.getUsedCount() + 1);
        if (key.getUsedCount() >= key.getMaxUses()) {
            key.setStatus("USED");
            key.setUsedAt(Instant.now());
        }
        inviteKeyRepository.save(key);
        audit("USER", userId, "FAMILY_MEMBER_CLAIMED", "lovedone:" + key.getLovedOneId(),
                Map.of("inviteKeyId", key.getId(), "familyId", lovedOne.getFamilyId(), "relation", targetRelation));
        return new ClaimResponse(lovedOne.getFamilyId(), member.getRole(), targetRelation, "已通过邀请码加入纪念馆");
    }

    private void audit(String actorType, Long actorId, String action, String target, Map<String, Object> detail) {
        AuditLog log = new AuditLog();
        log.setActorType(actorType);
        log.setActorId(actorId);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private String formatCode(String plain) {
        String upper = plain.toUpperCase();
        return upper.substring(0, 4) + "-" + upper.substring(4, 8) + "-"
                + upper.substring(8, 12) + "-" + upper.substring(12, 16);
    }

    private String normalize(String code) {
        return code == null ? "" : code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
