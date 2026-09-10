package com.memorylink.notification;

import com.memorylink.audit.AuditService;
import com.memorylink.common.BusinessException;
import com.memorylink.connection.FamilyRelationship;
import com.memorylink.connection.FamilyRelationshipRepository;
import com.memorylink.connection.RelationCatalog;
import com.memorylink.connection.RelationshipVisibility;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.family.MemberProfileService;
import com.memorylink.notification.dto.NotificationListResponse;
import com.memorylink.notification.dto.NotificationResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final LovedOneRepository lovedOneRepository;
    private final MemberProfileService memberProfileService;

    public NotificationService(NotificationRepository notificationRepository,
                               FamilyRelationshipRepository relationshipRepository,
                               FamilyMemberRepository familyMemberRepository,
                               UserRepository userRepository,
                               AuditService auditService,
                               LovedOneRepository lovedOneRepository,
                               MemberProfileService memberProfileService) {
        this.notificationRepository = notificationRepository;
        this.relationshipRepository = relationshipRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.lovedOneRepository = lovedOneRepository;
        this.memberProfileService = memberProfileService;
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
     * 成员入族后的扇出：让"新人的家族网络"与"已知对象的家族网络"互相认识。
     *
     * <p>家族关系是一张网而不是孤岛：一个人进入网络时，网络里的所有人都应收到确认消息。
     * 网络 = 从该人出发，沿"共同家族成员 + 已确认关系"做广度优先遍历得到的整个连通图；
     * 这样"张耘嘉进入张力的家族网络"时，张力的女儿张耘嫣、以及张耘嫣的母亲袁蓉（二跳）
     * 都会收到消息，而不是只有直接相关的那几个人。
     *
     * @param familyId             本次加入的家族
     * @param newcomerId           新加入该家族的人
     * @param knownPeerId          已知关系的对象（邀请码创建人 / 联系请求的发起人），可为 null
     * @param newcomerDeclaration  新人对 knownPeer 的自述："我是你的 X"（例如"我是你的女儿"）
     * @param newcomerSideOverride 新人自己对 knownPeer 的称谓（旧流程已收集时使用："你是我的 Y"），可为 null
     */
    @Transactional
    public void fanoutOnJoin(Long familyId, Long newcomerId, Long knownPeerId,
                             String newcomerDeclaration, String newcomerSideOverride) {
        String declaration = blankToNull(newcomerDeclaration);
        String override = blankToNull(newcomerSideOverride);
        Set<Long> newcomerNetwork = networkOf(newcomerId, familyId);
        Set<Long> peerNetwork = networkOf(knownPeerId, familyId);
        Set<Long> peers = new LinkedHashSet<>(peerNetwork);
        peers.remove(newcomerId);
        peers.remove(null);

        // 一、新人与对方网络里的每个人建立（或恢复）关系
        for (Long peerId : peers) {
            boolean anchor = Objects.equals(peerId, knownPeerId);
            FamilyRelationship relationship = ensureRelationship(newcomerId, peerId);
            if (anchor && declaration != null) {
                // 老人这一侧是确定的：新人自述"我是你的 X" → 老人对新人就是 X
                setSideState(relationship, peerId, RelationshipVisibility.ACTIVE,
                        RelationCatalog.normalizeLegacy(declaration));
                if (override != null) {
                    setSideState(relationship, newcomerId, RelationshipVisibility.ACTIVE,
                            RelationCatalog.normalizeLegacy(override));
                }
            }
            relationshipRepository.save(relationship);
            notifyIfPending(relationship, peerId, null);
            notifyIfPending(relationship, newcomerId,
                    anchor ? RelationCatalog.suggest(declaration) : null);
        }

        // 二、已知对象与新人的网络成员互相认识
        if (knownPeerId != null) {
            for (Long memberId : newcomerNetwork) {
                if (memberId.equals(newcomerId) || memberId.equals(knownPeerId)) {
                    continue;
                }
                FamilyRelationship relationship = ensureRelationship(knownPeerId, memberId);
                relationshipRepository.save(relationship);
                notifyIfPending(relationship, knownPeerId, null);
                notifyIfPending(relationship, memberId, null);
            }
        }
    }

    /** 网络规模上限：超过时退化为"一跳圈子"，避免一次入族产生海量消息。 */
    private static final int MAX_NETWORK_SIZE = 200;

    /**
     * 某个人的"家族网络"：从该人出发做广度优先遍历，
     * 每一步展开"所在家族的全部成员"与"已确认关系的联系人"，得到整个连通图。
     * 规模超过 {@link #MAX_NETWORK_SIZE} 时退化为 {@link #circleOf}（只取一跳）。
     */
    private Set<Long> networkOf(Long userId, Long extraFamilyId) {
        Set<Long> oneHop = circleOf(userId, extraFamilyId);
        if (oneHop.isEmpty()) {
            return oneHop;
        }
        Set<Long> visited = new LinkedHashSet<>(oneHop);
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>(oneHop);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long next : circleOf(current, null)) {
                if (visited.add(next)) {
                    if (visited.size() > MAX_NETWORK_SIZE) {
                        return oneHop;
                    }
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    /** 某个人的"一跳圈子"：自己 + 所在家族的全部成员 + 已确认关系的联系人。 */
    private Set<Long> circleOf(Long userId, Long extraFamilyId) {
        Set<Long> circle = new LinkedHashSet<>();
        if (userId == null) {
            return circle;
        }
        circle.add(userId);
        Set<Long> familyIds = new LinkedHashSet<>();
        if (extraFamilyId != null) {
            familyIds.add(extraFamilyId);
        }
        familyMemberRepository.findByUserId(userId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(FamilyMember::getFamilyId)
                .forEach(familyIds::add);
        for (Long familyId : familyIds) {
            familyMemberRepository.findByFamilyId(familyId).stream()
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .map(FamilyMember::getUserId)
                    .forEach(circle::add);
        }
        for (FamilyRelationship relationship : allRelationshipsOf(userId)) {
            if (RelationshipVisibility.visibleTo(relationship, userId)) {
                Long otherId = RelationshipVisibility.otherId(relationship, userId);
                if (otherId != null) {
                    circle.add(otherId);
                }
            }
        }
        return circle;
    }

    private List<FamilyRelationship> allRelationshipsOf(Long userId) {
        List<FamilyRelationship> all = new java.util.ArrayList<>();
        all.addAll(relationshipRepository.findByUserAId(userId));
        all.addAll(relationshipRepository.findByUserBId(userId));
        return all;
    }

    /**
     * 确认关系后让双方真正进入同一个家族，否则确认了也看不到对方的档案。
     * 仅当双方尚无共同家族时，补一条"由关系确认产生"的成员关系。
     */
    private void ensureSharedFamily(Long userId, Long otherUserId, String myRelation) {
        if (otherUserId == null || sharesFamily(userId, otherUserId)) {
            return;
        }
        memberProfileService.ensureSelfProfile(otherUserId);
        Long targetFamilyId = lovedOneRepository.findFirstByUserIdOrderByIdAsc(otherUserId)
                .map(LovedOne::getFamilyId)
                .orElse(null);
        if (targetFamilyId == null) {
            return;
        }
        FamilyMember member = new FamilyMember();
        member.setFamilyId(targetFamilyId);
        member.setUserId(userId);
        member.setRelation(myRelation);
        member.setRole("VIEWER");
        member.setStatus("ACTIVE");
        member.setEvidenceStatus("SELF_DECLARED");
        member.setRelationSource("RELATION_CONFIRM");
        familyMemberRepository.save(member);
        memberProfileService.ensureSelfProfile(userId);
    }

    private boolean sharesFamily(Long userId, Long otherUserId) {
        Set<Long> mine = familyMemberRepository.findByUserId(userId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(FamilyMember::getFamilyId)
                .collect(Collectors.toSet());
        return !mine.isEmpty() && familyMemberRepository.findByUserId(otherUserId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(FamilyMember::getFamilyId)
                .anyMatch(mine::contains);
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
        ensureSharedFamily(userId, RelationshipVisibility.otherId(relationship, userId), relation);
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
