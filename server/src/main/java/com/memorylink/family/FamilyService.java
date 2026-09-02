package com.memorylink.family;

import com.memorylink.common.BusinessException;
import com.memorylink.user.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyService {

    public static final int CODE_FAMILY_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_USER_NOT_FOUND = 3003;
    public static final int CODE_ALREADY_MEMBER = 3004;
    public static final int CODE_INVALID_ROLE = 2002;

    private static final Set<String> INVITABLE_ROLES = Set.of("EDITOR", "VIEWER");

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public FamilyService(FamilyRepository familyRepository,
                         FamilyMemberRepository familyMemberRepository,
                         UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Family getOrCreateDefaultFamily(Long userId, String displayName) {
        Family family = familyRepository.findFirstByCreatorIdOrderByIdAsc(userId)
                .orElseGet(() -> {
                    Family created = new Family();
                    created.setName((displayName == null ? "我的" : displayName) + "的家族");
                    created.setCreatorId(userId);
                    created.setStatus("ACTIVE");
                    return familyRepository.save(created);
                });
        ensureCreatorMembership(family, userId);
        return family;
    }

    @Transactional(readOnly = true)
    public boolean canAccess(Long userId, Long familyId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long userId, Long familyId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(m -> "OWNER".equals(m.getRole()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<FamilyMember> membersOf(Long familyId) {
        return familyMemberRepository.findByFamilyId(familyId);
    }

    @Transactional(readOnly = true)
    public List<FamilyMember> membershipsOf(Long userId) {
        return familyMemberRepository.findByUserId(userId);
    }

    @Transactional
    public FamilyMember invite(Long operatorUserId, Long familyId, String phone, String role) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException(CODE_FAMILY_NOT_FOUND, "家族不存在"));
        if (!isOwner(operatorUserId, family.getId())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅家族创建者可邀请成员");
        }
        String targetRole = role == null ? "EDITOR" : role.trim().toUpperCase();
        if (!INVITABLE_ROLES.contains(targetRole)) {
            throw new BusinessException(CODE_INVALID_ROLE, "角色仅支持 EDITOR/VIEWER");
        }
        var invited = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "该手机号未注册"));
        if (familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), invited.getId())) {
            throw new BusinessException(CODE_ALREADY_MEMBER, "该用户已是家庭成员");
        }
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(invited.getId());
        member.setRole(targetRole);
        member.setStatus("ACTIVE");
        return familyMemberRepository.save(member);
    }

    private void ensureCreatorMembership(Family family, Long userId) {
        if (!familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), userId)) {
            FamilyMember owner = new FamilyMember();
            owner.setFamilyId(family.getId());
            owner.setUserId(userId);
            owner.setRelation("创建者");
            owner.setRole("OWNER");
            owner.setStatus("ACTIVE");
            familyMemberRepository.save(owner);
        }
    }
}
