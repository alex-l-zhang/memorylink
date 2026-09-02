package com.memorylink.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.memorylink.common.BusinessException;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private UserRepository userRepository;

    private FamilyService familyService;

    @BeforeEach
    void setUp() {
        familyService = new FamilyService(familyRepository, familyMemberRepository, userRepository);
    }

    @Test
    void getOrCreateDefaultFamilyCreatesFamilyAndOwnerMembership() {
        when(familyRepository.findFirstByCreatorIdOrderByIdAsc(1L)).thenReturn(Optional.empty());
        Family family = new Family();
        family.setId(1L);
        family.setName("小明 的家族");
        family.setCreatorId(1L);
        when(familyRepository.save(any(Family.class))).thenReturn(family);
        when(familyMemberRepository.existsByFamilyIdAndUserId(1L, 1L)).thenReturn(false);

        Family result = familyService.getOrCreateDefaultFamily(1L, "小明");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void inviteByNonOwnerIsForbidden() {
        Family family = new Family();
        family.setId(1L);
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        FamilyMember viewer = new FamilyMember();
        viewer.setFamilyId(1L);
        viewer.setUserId(1L);
        viewer.setRole("VIEWER");
        viewer.setStatus("ACTIVE");
        when(familyMemberRepository.findByFamilyIdAndUserId(1L, 1L)).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> familyService.invite(1L, 1L, "13800138002", "EDITOR"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家族创建者");
    }

    @Test
    void inviteSuccess() {
        Family family = new Family();
        family.setId(1L);
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        FamilyMember owner = new FamilyMember();
        owner.setFamilyId(1L);
        owner.setUserId(1L);
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        when(familyMemberRepository.findByFamilyIdAndUserId(1L, 1L)).thenReturn(Optional.of(owner));

        User invited = new User();
        invited.setId(2L);
        invited.setPhone("13800138002");
        when(userRepository.findByPhone("13800138002")).thenReturn(Optional.of(invited));
        when(familyMemberRepository.existsByFamilyIdAndUserId(1L, 2L)).thenReturn(false);
        FamilyMember saved = new FamilyMember();
        saved.setId(1L);
        saved.setFamilyId(1L);
        saved.setUserId(2L);
        saved.setRole("EDITOR");
        saved.setStatus("ACTIVE");
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(saved);

        FamilyMember member = familyService.invite(1L, 1L, "13800138002", "EDITOR");

        assertThat(member.getUserId()).isEqualTo(2L);
        assertThat(member.getRole()).isEqualTo("EDITOR");
    }
}
