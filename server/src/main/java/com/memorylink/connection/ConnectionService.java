package com.memorylink.connection;

import com.memorylink.audit.AuditService;
import com.memorylink.common.BusinessException;
import com.memorylink.connection.dto.ConnectionRequestResponse;
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

    private final UserRepository userRepository;
    private final ConnectionRequestRepository requestRepository;
    private final FamilyRelationshipRepository relationshipRepository;
    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepository;
    private final AuditService auditService;
    private final MemberProfileService memberProfileService;

    public ConnectionService(UserRepository userRepository,
                             ConnectionRequestRepository requestRepository,
                             FamilyRelationshipRepository relationshipRepository,
                             FamilyService familyService,
                             FamilyMemberRepository familyMemberRepository,
                             AuditService auditService,
                             MemberProfileService memberProfileService) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.relationshipRepository = relationshipRepository;
        this.familyService = familyService;
        this.familyMemberRepository = familyMemberRepository;
        this.auditService = auditService;
        this.memberProfileService = memberProfileService;
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
        String inverse = inverseRelation == null ? "" : inverseRelation.trim().toUpperCase();
        if (!RelationCatalog.isValid(rel) || !RelationCatalog.isValid(inverse)) {
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
            ConnectionRequest request = new ConnectionRequest();
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

    @Transactional
    public void accept(Long userId, Long requestId) {
        ConnectionRequest request = requestRepository
                .findByIdAndTargetIdAndStatus(requestId, userId, "PENDING")
                .orElseThrow(() -> new BusinessException(CODE_INVALID, "请求不存在或已处理"));
        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "发起人不存在"));
        Family family = familyService.getOrCreateDefaultFamily(requester.getId(), requester.getName());
        if (!familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), userId)) {
            FamilyMember member = new FamilyMember();
            member.setFamilyId(family.getId());
            member.setUserId(userId);
            member.setRelation(request.getInverseRelation() == null
                    ? RelationCatalog.normalizeLegacy(inverse(request.getRelation()))
                    : request.getInverseRelation());
            member.setRole("VIEWER");
            member.setStatus("ACTIVE");
            member.setEvidenceStatus("SELF_DECLARED");
            member.setRelationSource("CONNECTION_REQUEST");
            familyMemberRepository.save(member);
        }
        memberProfileService.ensureMemberProfile(family.getId(), requester.getId());
        memberProfileService.ensureMemberProfile(family.getId(), userId);
        FamilyRelationship relationship = new FamilyRelationship();
        relationship.setUserAId(requester.getId());
        relationship.setUserBId(userId);
        relationship.setRelationAToB(RelationCatalog.normalizeLegacy(request.getRelation()));
        relationship.setRelationBToA(request.getInverseRelation() == null
                ? RelationCatalog.normalizeLegacy(inverse(request.getRelation()))
                : RelationCatalog.normalizeLegacy(request.getInverseRelation()));
        relationship.setStatus("ACTIVE");
        relationshipRepository.save(relationship);
        request.setStatus("ACCEPTED");
        request.setRespondedAt(Instant.now());
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
                .map(FamilyRelationship::getUserBId).collect(Collectors.toSet());
        ids.addAll(relationshipRepository.findByUserBId(userId).stream()
                .map(FamilyRelationship::getUserAId).collect(Collectors.toSet()));
        ids.addAll(requestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
                .map(ConnectionRequest::getTargetId).collect(Collectors.toSet()));
        ids.addAll(requestRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(userId, "PENDING").stream()
                .map(ConnectionRequest::getRequesterId).collect(Collectors.toSet()));
        return ids;
    }

    private void ensureNotConnected(Long userId, Long targetId, String targetName) {
        if (relationshipRepository.findByUserAIdAndUserBId(userId, targetId).isPresent()
                || relationshipRepository.findByUserBIdAndUserAId(userId, targetId).isPresent()) {
            throw new BusinessException(CODE_ALREADY, "你与「" + targetName + "」已建立联系");
        }
        if (requestRepository.existsByRequesterIdAndTargetIdAndStatus(userId, targetId, "PENDING")
                || requestRepository.existsByTargetIdAndRequesterIdAndStatus(userId, targetId, "PENDING")) {
            throw new BusinessException(CODE_ALREADY, "你与「" + targetName + "」已有待处理的联系请求");
        }
    }

    private String inverse(String relation) {
        return switch (relation) {
            case "PARENT" -> "CHILD";
            case "CHILD" -> "PARENT";
            case "GRANDPARENT" -> "GRANDCHILD";
            case "GRANDCHILD" -> "GRANDPARENT";
            default -> relation;
        };
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
                request.getCreatedAt()
        );
    }
}
