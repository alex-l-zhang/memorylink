package com.memorylink.connection;

import com.memorylink.audit.AuditService;
import com.memorylink.common.BusinessException;
import com.memorylink.connection.dto.ConnectionRequestResponse;
import com.memorylink.connection.dto.ConnectionHistoryResponse;
import com.memorylink.connection.dto.GraphNodeResponse;
import com.memorylink.connection.dto.RelationshipGraphResponse;
import com.memorylink.connection.dto.RelationshipResponse;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.notification.NotificationService;
import com.memorylink.family.FamilyRepository;
import com.memorylink.connection.dto.UserCandidateResponse;
import com.memorylink.family.Family;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.family.MemberProfileService;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {

    public static final int CODE_INVALID = 2002;
    public static final int CODE_USER_NOT_FOUND = 3003;
    public static final int CODE_ALREADY = 3008;
    public static final int CODE_INVERSE_REQUIRED = 3009;
    public static final int CODE_FORBIDDEN = 4001;

    private final UserRepository userRepository;
    private final ConnectionRequestRepository requestRepository;
    private final FamilyRelationshipRepository relationshipRepository;
    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepository;
    private final AuditService auditService;
    private final MemberProfileService memberProfileService;
    private final LovedOneRepository lovedOneRepository;
    private final FamilyRepository familyRepository;
    private final NotificationService notificationService;

    public ConnectionService(UserRepository userRepository,
                             ConnectionRequestRepository requestRepository,
                             FamilyRelationshipRepository relationshipRepository,
                             FamilyService familyService,
                             FamilyMemberRepository familyMemberRepository,
                             AuditService auditService,
                             MemberProfileService memberProfileService,
                             LovedOneRepository lovedOneRepository,
                             FamilyRepository familyRepository,
                             NotificationService notificationService) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.relationshipRepository = relationshipRepository;
        this.familyService = familyService;
        this.familyMemberRepository = familyMemberRepository;
        this.auditService = auditService;
        this.memberProfileService = memberProfileService;
        this.lovedOneRepository = lovedOneRepository;
        this.familyRepository = familyRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<UserCandidateResponse> search(Long userId, String name) {
        String key = name == null ? "" : name.trim();
        if (key.isEmpty()) {
            throw new BusinessException(CODE_INVALID, "请输入完整姓名进行搜索");
        }
        Set<Long> related = relatedIds(userId);
        return userRepository.findByName(key).stream()
                .filter(u -> !u.getId().equals(userId) && !related.contains(u.getId()))
                .map(u -> new UserCandidateResponse(
                        u.getId(), u.getName(),
                        u.getBirthDate() == null ? null : u.getBirthDate().getYear(),
                        u.getBirthDate() == null ? null : u.getBirthDate().getMonthValue(),
                        u.getBirthPlace()))
                .toList();
    }

    @Transactional
    public int send(Long userId, List<Long> targetIds, String relation, String inverseRelation) {
        String rel = relation == null ? "" : relation.trim().toUpperCase();
        String inverse = inverseRelation == null || inverseRelation.isBlank()
                ? null : inverseRelation.trim().toUpperCase();
        if (!RelationCatalog.isValid(rel) || (inverse != null && !RelationCatalog.isValid(inverse))) {
            throw new BusinessException(CODE_INVALID, "请选择有效的亲属关系");
        }
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
        int sent = 0;
        for (Long targetId : targetIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (targetId.equals(userId)) {
                continue;
            }
            User target = userRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "联系人不存在"));
            ensureNotConnected(userId, targetId, target.getName());
            ConnectionRequest request = requestRepository
                    .findFirstByRequesterIdAndTargetIdOrderByIdDesc(userId, targetId)
                    .orElseGet(() -> requestRepository
                            .findFirstByTargetIdAndRequesterIdOrderByIdDesc(userId, targetId)
                            .orElseGet(ConnectionRequest::new));
            request.setRequesterId(userId);
            request.setTargetId(targetId);
            request.setRequesterName(me.getName());
            if (me.getBirthDate() != null) {
                request.setBirthYear(me.getBirthDate().getYear());
                request.setBirthMonth(me.getBirthDate().getMonthValue());
            }
            request.setBirthPlace(me.getBirthPlace());
            request.setRelation(rel);
            request.setInverseRelation(inverse);
            request.setStatus("PENDING");
            request.setRespondedAt(null);
            requestRepository.save(request);
            sent++;
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public List<ConnectionRequestResponse> incoming(Long userId) {
        return requestRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionRequestResponse> received(Long userId) {
        return requestRepository.findByTargetIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionHistoryResponse> outgoing(Long userId) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId).stream()
                .map(request -> {
                    String targetName = userRepository.findById(request.getTargetId())
                            .map(User::getName).orElse("已注销用户");
                    return new ConnectionHistoryResponse(
                            request.getId(),
                            request.getTargetId(),
                            targetName,
                            request.getRelation(),
                            request.getInverseRelation(),
                            request.getStatus(),
                            request.getCreatedAt(),
                            request.getRespondedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RelationshipResponse> relationships(Long userId) {
        return allOfMine(userId).stream()
                .filter(r -> RelationshipVisibility.visibleTo(r, userId))
                .map(r -> {
                    Long otherId = RelationshipVisibility.otherId(r, userId);
                    String otherName = userRepository.findById(otherId)
                            .map(User::getName).orElse("已注销用户");
                    return new RelationshipResponse(
                            r.getId(),
                            otherId,
                            otherName,
                            RelationshipVisibility.relationFromMe(r, userId),
                            RelationshipVisibility.relationFromOther(r, userId),
                            RelationshipVisibility.myStatus(r, userId));
                })
                .toList();
    }

    /**
     * 以自己为中心的家族关系图谱。
     * 默认只返回我已确认的家人；includePending=true 时附带"待确认"节点（半透明展示）。
     * includeExtended=true 时附带"家人的家人"（二级节点，带关系路径，用于顺藤摸瓜发起建立联系）。
     */
    @Transactional(readOnly = true)
    public RelationshipGraphResponse graph(Long userId, boolean includePending, boolean includeExtended) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
        List<GraphNodeResponse> nodes = new java.util.ArrayList<>();
        long pending = 0;
        Set<Long> directIds = new java.util.HashSet<>();
        directIds.add(userId);
        for (FamilyRelationship relationship : allOfMine(userId)) {
            String myStatus = RelationshipVisibility.myStatus(relationship, userId);
            if (myStatus == null || RelationshipVisibility.REMOVED.equals(myStatus)) {
                continue;
            }
            boolean mine = RelationshipVisibility.ACTIVE.equals(myStatus);
            if (!mine) {
                pending++;
                if (!includePending) {
                    continue;
                }
            }
            Long otherId = RelationshipVisibility.otherId(relationship, userId);
            directIds.add(otherId);
            User other = userRepository.findById(otherId).orElse(null);
            String otherStatus = RelationshipVisibility.sideStatus(relationship, otherId);
            nodes.add(new GraphNodeResponse(
                    otherId,
                    other == null ? "已注销用户" : other.getName(),
                    other == null || other.getBirthDate() == null ? null : other.getBirthDate().getYear(),
                    other == null || other.getBirthDate() == null ? null : other.getBirthDate().getMonthValue(),
                    other == null ? null : other.getBirthPlace(),
                    other == null ? null : other.getGender(),
                    relationship.getId(),
                    RelationshipVisibility.relationFromMe(relationship, userId),
                    mine ? RelationshipVisibility.relationFromOther(relationship, userId) : null,
                    myStatus,
                    otherStatus,
                    !mine || !RelationshipVisibility.ACTIVE.equals(otherStatus),
                    false,
                    false,
                    null,
                    null,
                    null,
                    false));
        }
        if (includeExtended) {
            nodes.addAll(extendedNodes(userId, nodes, directIds));
        }
        GraphNodeResponse self = new GraphNodeResponse(
                me.getId(), me.getName(),
                me.getBirthDate() == null ? null : me.getBirthDate().getYear(),
                me.getBirthDate() == null ? null : me.getBirthDate().getMonthValue(),
                me.getBirthPlace(), me.getGender(),
                null, null, null,
                RelationshipVisibility.ACTIVE, RelationshipVisibility.ACTIVE, false, true,
                false, null, null, null, false);
        return new RelationshipGraphResponse(self, nodes, pending);
    }

    /**
     * 二级节点："我已确认的家人"所确认的家人，且我与 TA 之间还没有关系。
     * 只返回能给出关系路径的（例如"张耘嫣的母亲"），便于用户顺藤摸瓜发起建立联系。
     */
    private List<GraphNodeResponse> extendedNodes(Long userId, List<GraphNodeResponse> directNodes,
                                                  Set<Long> directIds) {
        List<GraphNodeResponse> extended = new java.util.ArrayList<>();
        Set<Long> seen = new java.util.HashSet<>();
        for (GraphNodeResponse direct : directNodes) {
            if (direct.extended() || direct.userId() == null) {
                continue;
            }
            for (FamilyRelationship relationship : allOfMine(direct.userId())) {
                if (!RelationshipVisibility.visibleTo(relationship, direct.userId())) {
                    continue;
                }
                Long otherId = RelationshipVisibility.otherId(relationship, direct.userId());
                if (otherId == null || directIds.contains(otherId) || !seen.add(otherId)) {
                    continue;
                }
                if (hasAnyRelationshipWith(userId, otherId)) {
                    continue;
                }
                User other = userRepository.findById(otherId).orElse(null);
                if (other == null) {
                    continue;
                }
                extended.add(new GraphNodeResponse(
                        otherId,
                        other.getName(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "EXTENDED",
                        null,
                        false,
                        false,
                        true,
                        direct.userId(),
                        direct.name(),
                        RelationshipVisibility.relationFromMe(relationship, direct.userId()),
                        requestRepository.existsByRequesterIdAndTargetIdAndStatus(
                                userId, otherId, "PENDING")));
            }
        }
        return extended;
    }

    private boolean hasAnyRelationshipWith(Long userId, Long otherId) {
        return relationshipRepository.findByUserAIdAndUserBId(userId, otherId)
                .filter(r -> !RelationshipVisibility.REMOVED.equals(r.getStatus()))
                .or(() -> relationshipRepository.findByUserBIdAndUserAId(userId, otherId)
                        .filter(r -> !RelationshipVisibility.REMOVED.equals(r.getStatus())))
                .isPresent();
    }

    private List<FamilyRelationship> allOfMine(Long userId) {
        List<FamilyRelationship> all = new java.util.ArrayList<>();
        all.addAll(relationshipRepository.findByUserAId(userId));
        all.addAll(relationshipRepository.findByUserBId(userId));
        return all;
    }

    @Transactional
    public void removeRelationship(Long userId, Long relationshipId) {
        FamilyRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new BusinessException(CODE_INVALID, "关系不存在"));
        if (!relationship.getUserAId().equals(userId) && !relationship.getUserBId().equals(userId)) {
            throw new BusinessException(CODE_FORBIDDEN, "无权解除该关系");
        }
        Long userAId = relationship.getUserAId();
        Long userBId = relationship.getUserBId();
        Long targetId = userBId.equals(userId) ? userAId : userBId;
        // 解除关系后，清理由这段关系产生的家族成员关系与成员卡（双向）
        dropDerivedMembership(userAId, userBId);
        dropDerivedMembership(userBId, userAId);
        relationship.setStatus("REMOVED");
        relationshipRepository.save(relationship);
        requestRepository.findFirstByRequesterIdAndTargetIdOrderByIdDesc(userId, targetId)
                .or(() -> requestRepository
                        .findFirstByTargetIdAndRequesterIdOrderByIdDesc(userId, targetId))
                .ifPresent(request -> {
                    request.setStatus("REMOVED");
                    requestRepository.save(request);
                });
        // 若被解除方失去唯一档案卡，则在本人默认家族重建"自己"的档案卡
        if (lovedOneRepository.findByUserId(targetId).isEmpty()) {
            memberProfileService.ensureSelfProfile(targetId);
        }
        auditService.log("USER", userId, "RELATIONSHIP_REMOVED", "relationship:" + relationshipId,
                Map.of("otherUserId", targetId));
    }

    /**
     * 解除关系时清理"由关系产生"的成员关系（邀请码 / 建立联系 / 关系确认），
     * 只删除 owner 与 other 共同所在的家族里的记录，避免误删用户自己创建家族的成员身份。
     */
    private void dropDerivedMembership(Long ownerId, Long otherId) {
        if (ownerId == null || otherId == null) {
            return;
        }
        familyRepository.findFirstByCreatorIdOrderByIdAsc(ownerId).ifPresent(family -> {
            familyMemberRepository.findByFamilyIdAndUserId(family.getId(), otherId)
                    .filter(m -> isDerivedSource(m.getRelationSource()))
                    .ifPresent(familyMemberRepository::delete);
            lovedOneRepository.findFirstByFamilyIdAndUserId(family.getId(), otherId)
                    .ifPresent(lovedOneRepository::delete);
        });
        for (com.memorylink.family.FamilyMember membership
                : familyMemberRepository.findByUserId(otherId)) {
            if (!isDerivedSource(membership.getRelationSource())) {
                continue;
            }
            if (familyMemberRepository.findByFamilyIdAndUserId(membership.getFamilyId(), ownerId)
                    .isPresent()) {
                familyMemberRepository.delete(membership);
            }
        }
    }

    private static boolean isDerivedSource(String relationSource) {
        return "CONNECTION_REQUEST".equals(relationSource) || "RELATION_CONFIRM".equals(relationSource);
    }

    @Transactional
    public void accept(Long userId, Long requestId, String inverseOverride) {
        ConnectionRequest request = requestRepository
                .findByIdAndTargetIdAndStatus(requestId, userId, "PENDING")
                .orElseThrow(() -> new BusinessException(CODE_INVALID, "请求不存在或已处理"));
        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "发起人不存在"));
        if (inverseOverride == null || inverseOverride.isBlank()) {
            throw new BusinessException(CODE_INVERSE_REQUIRED, "请选择你与对方的关系后再同意");
        }
        String finalInverse = inverseOverride.trim().toUpperCase();
        if (!RelationCatalog.isValid(finalInverse)) {
            throw new BusinessException(CODE_INVALID, "请选择有效的亲属关系");
        }
        Family family = familyService.getOrCreateDefaultFamily(requester.getId(), requester.getName());
        if (!familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), userId)) {
            FamilyMember member = new FamilyMember();
            member.setFamilyId(family.getId());
            member.setUserId(userId);
            member.setRelation(finalInverse);
            member.setRole("VIEWER");
            member.setStatus("ACTIVE");
            member.setEvidenceStatus("SELF_DECLARED");
            member.setRelationSource("CONNECTION_REQUEST");
            familyMemberRepository.save(member);
        }
        memberProfileService.ensureSelfProfile(requester.getId());
        memberProfileService.ensureSelfProfile(userId);
        FamilyRelationship relationship = relationshipRepository
                .findByUserAIdAndUserBId(requester.getId(), userId)
                .or(() -> relationshipRepository.findByUserBIdAndUserAId(requester.getId(), userId))
                .orElseGet(FamilyRelationship::new);
        relationship.setUserAId(requester.getId());
        relationship.setUserBId(userId);
        // A=发起人, B=被联系人
        // relationAToB：B 是 A 的谁（B 同意时给出"我是你的 Y" → A 眼中 B 是 Y）
        // relationBToA：A 是 B 的谁（A 发起时给出"我是你的 X"）
        relationship.setRelationAToB(RelationCatalog.normalizeLegacy(finalInverse));
        relationship.setRelationBToA(RelationCatalog.normalizeLegacy(request.getRelation()));
        relationship.setStatus(RelationshipVisibility.ACTIVE);
        Instant now = Instant.now();
        relationship.setAStatus(RelationshipVisibility.ACTIVE);
        relationship.setBStatus(RelationshipVisibility.ACTIVE);
        relationship.setAConfirmedAt(now);
        relationship.setBConfirmedAt(now);
        relationshipRepository.save(relationship);
        // 被联系人加入发起人的家族后，与家族其他成员自动建立"待确认"关系
        notificationService.fanoutOnJoin(family.getId(), userId, requester.getId(), null, null);
        request.setStatus("ACCEPTED");
        request.setRespondedAt(Instant.now());
        request.setInverseRelation(finalInverse);
        requestRepository.save(request);
        auditService.log("USER", userId, "CONNECTION_ACCEPTED", "connection:" + requestId,
                Map.of("requesterId", requester.getId()));
    }

    @Transactional
    public void reject(Long userId, Long requestId) {
        ConnectionRequest request = requestRepository
                .findByIdAndTargetIdAndStatus(requestId, userId, "PENDING")
                .orElseThrow(() -> new BusinessException(CODE_INVALID, "请求不存在或已处理"));
        request.setStatus("REJECTED");
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);
    }

    private Set<Long> relatedIds(Long userId) {
        Set<Long> ids = relationshipRepository.findByUserAId(userId).stream()
                .filter(ConnectionService::notRemoved)
                .map(FamilyRelationship::getUserBId).collect(Collectors.toSet());
        ids.addAll(relationshipRepository.findByUserBId(userId).stream()
                .filter(ConnectionService::notRemoved)
                .map(FamilyRelationship::getUserAId).collect(Collectors.toSet()));
        ids.addAll(requestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
                .map(ConnectionRequest::getTargetId).collect(Collectors.toSet()));
        ids.addAll(requestRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
                .map(ConnectionRequest::getRequesterId).collect(Collectors.toSet()));
        return ids;
    }

    private void ensureNotConnected(Long userId, Long targetId, String targetName) {
        FamilyRelationship existing = relationshipRepository.findByUserAIdAndUserBId(userId, targetId)
                .or(() -> relationshipRepository.findByUserBIdAndUserAId(userId, targetId))
                .filter(ConnectionService::notRemoved)
                .orElse(null);
        if (existing != null) {
            if (RelationshipVisibility.visibleTo(existing, userId)) {
                throw new BusinessException(CODE_ALREADY, "你与「" + targetName + "」已建立联系");
            }
            throw new BusinessException(CODE_ALREADY,
                    "你与「" + targetName + "」的关系待确认，请到消息中心或关系图谱处理");
        }
        if (requestRepository.existsByRequesterIdAndTargetIdAndStatus(userId, targetId, "PENDING")
                || requestRepository.existsByTargetIdAndRequesterIdAndStatus(userId, targetId, "PENDING")) {
            throw new BusinessException(CODE_ALREADY, "你与「" + targetName + "」已有待处理的联系请求");
        }
    }

    private static boolean notRemoved(FamilyRelationship relationship) {
        return !RelationshipVisibility.REMOVED.equals(relationship.getStatus());
    }


    private ConnectionRequestResponse toResponse(ConnectionRequest request) {
        return new ConnectionRequestResponse(
                request.getId(),
                request.getRequesterName(),
                request.getBirthYear(),
                request.getBirthMonth(),
                request.getBirthPlace(),
                request.getRelation(),
                request.getInverseRelation(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt()
        );
    }
}
