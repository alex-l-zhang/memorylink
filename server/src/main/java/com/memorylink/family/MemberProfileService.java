package com.memorylink.family;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家族成员档案卡：让家族里的每个人都以"在世成员档案"的形式出现在记忆档案列表中，
 * 保证家庭成员之间可以互相看到对方的基础信息（姓名/出生年月/籍贯）。
 */
@Service
public class MemberProfileService {

    public static final int CODE_USER_NOT_FOUND = 3003;

    private final LovedOneRepository lovedOneRepository;
    private final UserRepository userRepository;
    private final FamilyService familyService;

    public MemberProfileService(LovedOneRepository lovedOneRepository,
                                UserRepository userRepository,
                                FamilyService familyService) {
        this.lovedOneRepository = lovedOneRepository;
        this.userRepository = userRepository;
        this.familyService = familyService;
    }

    /** 每位用户仅保留一份本人档案（单份存储，家族展示由成员关系动态带出）。 */
    @Transactional
    public LovedOne ensureSelfProfile(Long userId) {
        return lovedOneRepository.findFirstByUserIdOrderByIdAsc(userId)
                .filter(person -> !person.effectiveDeceased())
                .orElseGet(() -> create(userId));
    }

    private LovedOne create(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
        Family family = familyService.getOrCreateDefaultFamily(userId, user.getName());
        LovedOne person = new LovedOne();
        person.setFamilyId(family.getId());
        person.setName(user.getName() == null || user.getName().isBlank() ? "家族成员" : user.getName());
        person.setBirthDate(user.getBirthDate());
        person.setBirthPlace(user.getBirthPlace());
        person.setCreatedBy(userId);
        person.setUserId(userId);
        person.setDeceased(false);
        person.setStatus("ACTIVE");
        return lovedOneRepository.save(person);
    }
}
