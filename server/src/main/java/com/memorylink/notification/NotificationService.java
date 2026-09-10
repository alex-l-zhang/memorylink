package com.memorylink.notification;

import com.memorylink.audit.AuditService;
import com.memorylink.common.BusinessException;
import com.memorylink.connection.FamilyRelationship;
import com.memorylink.connection.FamilyRelationshipRepository;
import com.memorylink.connection.RelationCatalog;
import com.memorylink.connection.RelationshipVisibility;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.notification.dto.NotificationListResponse;
import com.memorylink.notification.dto.NotificationResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    public static final int CODE_NOT_FOUND = 3002;
    public static final int CODE_INVALID = 2002;

    private static final String TYPE_RELATION_CONFIRM = "RELATION_CONFIRM";
    private static final List<String> PENDING_STATUSES = List.of("UNREAD", "READ");

    private final NotificationRepository notificationRepository;
    private final FamilyRelationshipRepository relationshipRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationService(NotificationRepository notificationRepository,
                               FamilyRelationshipRepository relationshipRepository,
                               FamilyMemberRepository familyMemberRepository,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.relationshipRepository = relationshipRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long userId) {
        List<NotificationResponse> items = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
        long unread = notificationRepository
                .countByRecipientIdAndStatusIn(userId, List.of("UNREAD"));
        return new NotificationListResponse(items, unread);
    }

    @Transactional
    public void confirm(Long userId, Long notificationId, String relation) {
        confirmInternal(userId, notificationId,
                relation == null || relation.isBlank()
                        ? RelationCatalog.FALLBACK : relation.trim().toUpperCase());
    }

    @Transactional
    public int confirmAll(Long userId) {
        List<Notification> pending = notificationRepository
                .findByRecipientIdAndStatusInOrderByCreatedAtDesc(userId, PENDING_STATUSES).stream()
                .filter(n -> TYPE_RELATION_CONFIRM.equals(n.getType()))
                .toList();
        for (Notification notification : pending) {
            confirmInternal(userId, notification.getId(), suggestedOrDefault(notification));
        }
        return pending.size();
    }

    /**
     * 从关系图谱等处直接确认某一侧关系：不要求存在站内消息，只要该关系属于我。
     */
    @Transactional
    public void confirmByRelationship(Long userId, Long relationshipId, String relation) {
        String value = relation == null || relation.isBlank()
                ? RelationCatalog.FALLBACK : relation.trim().toUpperCase();
        if (!RelationCatalog.isValid(value)) {
            throw new BusinessException(CODE_INVALID, "请选择有效的亲属关系");
        }
        FamilyRelationship relationship = requireOwnRelationship(userId, relationshipId);
        applyConfirmation(userId, relationship, value);
        markPendingNotifications(userId, relationshipId, "ACTIONED");
        auditService.log("USER", userId, "RELATION_CONFIRMED",
                "relationship:" + relationshipId, Map.of("relation", value));
    }

    /** 从关系图谱等处拒绝某一侧关系：关系保留但对我隐藏，对方之后可再申请确认。 */
    @Transactional
    public void rejectByRelationship(Long userId, Long relationshipId) {
        FamilyRelationship relationship = requireOwnRelationship(userId, relationshipId);
        applyRejection(userId, relationship);
        markPendingNotifications(userId, relationshipId, "REJECTED");
        auditService.log("USER", userId, "RELATION_REJECTED",
                "relationship:" + relationshipId, Map.of());
    }

    @Transactional
    public void reject(Long userId, Long notificationId) {
        Notification notification = requireNotification(userId, notificationId);
        FamilyRelationship relationship = requireRelationship(notification);
        applyRejection(userId, relationship);
        notification.setStatus("REJECTED");
        notification.setActionedAt(Instant.now());
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
        auditService.log("USER", userId, "RELATION_REJECTED",
                "relationship:" + relationship.getId(), Map.of());
    }

    /**
     * 成员入族后的扇出：为新人 × 每位现有成员建立（或恢复）关系，并给尚未确认的一侧发站内消息。
     *
     * @param familyId             新加入的家族
     * @param newcomerId           新人（自己）
     * @param knownPeerId          已知关系的对象（邀请码创建人 / 同意联系请求的发起人），可为 null
     * @param newcomerDeclaration  新人对 knownPeer 的自述："我是你的 X"（例如"我是你的女儿"）
     * @param newcomerSideOverride 新人自己对 knownPeer 的称谓（旧流程已收集时使用："你是我的 Y"），可为 null
     */
    @Transactional
    public void fanoutOnJoin(Long familyId, Long newcomerId, Long knownPeerId,
                             String newcomerDeclaration, String newcomerSideOverride) {
        List<Long> memberIds = familyMemberRepository.findByFamilyId(familyId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(FamilyMember::getUserId)
                .distinct()
                .toList();
        String declaration = blankToNull(newcomerDeclaration);
        String override = blankToNull(newcomerSideOverride);
        for (Long memberId : memberIds) {
            if (Objects.equals(memberId, newcomerId)) {
                continue;
            }
            FamilyRelationship relationship = ensureRelationship(newcomerId, memberId);
            boolean knownPeer = Objects.equals(memberId, knownPeerId);
            if (knownPeer && declaration != null) {
                // 老人这一侧是确定的：新人自述"我是你的 X" → 老人对新人就是 X
                setSideState(relationship, memberId, RelationshipVisibility.ACTIVE,
                        RelationCatalog.normalizeLegacy(declaration));
                if (override != null) {
                    setSideState(relationship, newcomerId, RelationshipVisibility.ACTIVE,
                            RelationCatalog.normalizeLegacy(override));
                }
            }
            relationshipRepository.save(relationship);
            notifyIfPending(relationship, memberId, null);
            notifyIfPending(relationship, newcomerId,
                    knownPeer ? RelationCatalog.suggest(declaration) : null);
        }
    }

    private void confirmInternal(Long userId, Long notificationId, String relation) {
        if (!RelationCatalog.isValid(relation)) {
            throw new BusinessException(CODE_INVALID, "请选择有效的亲属关系");
        }
        Notification notification = requireNotification(userId, notificationId);
        FamilyRelationship relationship = requireRelationship(notification);
        applyConfirmation(userId, relationship, relation);
        notification.setStatus("ACTIONED");
        notification.setActionedAt(Instant.now());
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        notificationRepository.save(notification);
        auditService.log("USER", userId, "RELATION_CONFIRMED",
                "relationship:" + relationship.getId(), Map.of("relation", relation));
    }

    /** 确认我这一侧的关系：写入我对对方的称谓并置为 ACTIVE。 */
    private void applyConfirmation(Long userId, FamilyRelationship relationship, String relation) {
        relationship.setStatus(RelationshipVisibility.ACTIVE);
        setSideState(relationship, userId, RelationshipVisibility.ACTIVE, relation);
        relationshipRepository.save(relationship);
    }

    /** 拒绝我这一侧的关系：保留技术关系，仅对我隐藏。 */
    private void applyRejection(Long userId, FamilyRelationship relationship) {
        setSideState(relationship, userId, RelationshipVisibility.REJECTED, null);
        relationshipRepository.save(relationship);
    }

    private void markPendingNotifications(Long userId, Long relationshipId, String status) {
        Instant now = Instant.now();
        for (Notification notification : notificationRepository
                .findByRecipientIdAndStatusInOrderByCreatedAtDesc(userId, PENDING_STATUSES)) {
            if (!relationshipId.equals(notification.getRelationshipId())) {
                continue;
            }
            notification.setStatus(status);
            notification.setActionedAt(now);
            notification.setReadAt(now);
            notificationRepository.save(notification);
        }
    }

    private FamilyRelationship requireOwnRelationship(Long userId, Long relationshipId) {
        FamilyRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "关系不存在"));
        if (!RelationshipVisibility.isMine(relationship, userId)) {
            throw new BusinessException(4001, "无权操作该关系");
        }
        return relationship;
    }

    private String suggestedOrDefault(Notification notification) {
        String suggested = notification.getSuggestedRelation();
        return suggested == null || suggested.isBlank()
                ? RelationCatalog.FALLBACK : suggested;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private Notification requireNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "消息不存在"));
        if (!TYPE_RELATION_CONFIRM.equals(notification.getType())) {
            throw new BusinessException(CODE_INVALID, "该消息不支持此操作");
        }
        return notification;
    }

    private FamilyRelationship requireRelationship(Notification notification) {
        Long relationshipId = notification.getRelationshipId();
        if (relationshipId == null) {
            throw new BusinessException(CODE_NOT_FOUND, "关系不存在");
        }
        return relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new BusinessException(CODE_NOT_FOUND, "关系不存在"));
    }

    private FamilyRelationship ensureRelationship(Long userIdA, Long userIdB) {
        return relationshipRepository.findByUserAIdAndUserBId(userIdA, userIdB)
                .or(() -> relationshipRepository.findByUserBIdAndUserAId(userIdA, userIdB))
                .map(existing -> {
                    if (RelationshipVisibility.REMOVED.equals(existing.getStatus())) {
                        existing.setStatus(RelationshipVisibility.ACTIVE);
                        existing.setAStatus(RelationshipVisibility.PENDING);
                        existing.setBStatus(RelationshipVisibility.PENDING);
                        existing.setRelationAToB(null);
                        existing.setRelationBToA(null);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    FamilyRelationship created = new FamilyRelationship();
                    created.setUserAId(userIdA);
                    created.setUserBId(userIdB);
                    created.setStatus(RelationshipVisibility.ACTIVE);
                    created.setAStatus(RelationshipVisibility.PENDING);
                    created.setBStatus(RelationshipVisibility.PENDING);
                    return created;
                });
    }

    private void notifyIfPending(FamilyRelationship relationship, Long recipientId, String suggestion) {
        if (!RelationshipVisibility.PENDING.equals(
                RelationshipVisibility.sideStatus(relationship, recipientId))) {
            return;
        }
        boolean exists = notificationRepository
                .existsByRecipientIdAndRelationshipIdAndStatusIn(
                        recipientId, relationship.getId(), PENDING_STATUSES);
        if (exists) {
            return;
        }
        Long otherUserId = RelationshipVisibility.otherId(relationship, recipientId);
        String otherName = userRepository.findById(otherUserId)
                .map(User::getName).orElse("家族成员");
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(TYPE_RELATION_CONFIRM);
        notification.setRelationshipId(relationship.getId());
        notification.setOtherUserId(otherUserId);
        notification.setTitle("家族成员关系确认");
        notification.setBody(otherName + " 已加入家族，请确认对方是你的谁");
        notification.setSuggestedRelation(suggestion);
        notification.setStatus("UNREAD");
        notificationRepository.save(notification);
    }

    private void setSideState(FamilyRelationship relationship, Long userId, String status, String relation) {
        Instant now = Instant.now();
        if (relationship.getUserAId().equals(userId)) {
            relationship.setAStatus(status);
            if (relation != null) {
                relationship.setRelationAToB(relation);
            }
            if (RelationshipVisibility.ACTIVE.equals(status)) {
                relationship.setAConfirmedAt(now);
            }
        } else if (relationship.getUserBId().equals(userId)) {
            relationship.setBStatus(status);
            if (relation != null) {
                relationship.setRelationBToA(relation);
            }
            if (RelationshipVisibility.ACTIVE.equals(status)) {
                relationship.setBConfirmedAt(now);
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        String otherName = notification.getOtherUserId() == null ? null
                : userRepository.findById(notification.getOtherUserId())
                        .map(User::getName).orElse("已注销用户");
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRelationshipId(),
                notification.getOtherUserId(),
                otherName,
                notification.getTitle(),
                notification.getBody(),
                notification.getSuggestedRelation(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
