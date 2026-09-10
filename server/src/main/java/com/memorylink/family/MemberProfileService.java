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

    public MemberProfileService(LovedOneRepository lovedOneRepository,
                                UserRepository userRepository) {
        this.lovedOneRepository = lovedOneRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LovedOne ensureMemberProfile(Long familyId, Long userId) {
        return lovedOneRepository.findFirstByFamilyIdAndUserId(familyId, userId)
                .orElseGet(() -> create(familyId, userId));
    }

    private LovedOne create(Long familyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_USER_NOT_FOUND, "用户不存在"));
        LovedOne person = new LovedOne();
        person.setFamilyId(familyId);
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
