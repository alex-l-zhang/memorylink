package com.memorylink.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.audit.AuditLogRepository;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.invite.dto.ClaimResponse;
import com.memorylink.invite.dto.InviteKeyResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteKeyRepository inviteKeyRepository;
    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private FamilyMemberRepository familyMemberRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    private InviteService service;

    @BeforeEach
    void setUp() {
        service = new InviteService(inviteKeyRepository, lovedOneRepository,
                familyService, familyMemberRepository, auditLogRepository, userRepository);
    }

    private LovedOne lovedOne(Long familyId) {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(familyId);
        lovedOne.setName("张爷爷");
        return lovedOne;
    }

    private InviteKey activeKey(Instant expiresAt) {
        InviteKey key = new InviteKey();
        key.setId(3L);
        key.setLovedOneId(1L);
        key.setCreatedBy(1L);
        key.setCodeHash("abc");
        key.setRole("VIEWER");
        key.setExpiresAt(expiresAt);
        key.setMaxUses(1);
        key.setStatus("ACTIVE");
        return key;
    }

    @Test
    void generateRejectsNonManager() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(9L)));
        when(familyService.canManage(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> service.generate(1L, 1L, "VIEWER", 72))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅家族创建者/共建者");
    }

    @Test
    void generateReturns16CharCodeAndStoresHash() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(9L)));
        when(familyService.canManage(1L, 9L)).thenReturn(true);
        when(inviteKeyRepository.save(any(InviteKey.class))).thenAnswer(invocation -> {
            InviteKey key = invocation.getArgument(0);
            key.setId(3L);
            return key;
        });

        InviteKeyResponse response = service.generate(1L, 1L, "VIEWER", 72);

        assertThat(response.code().replace("-", "")).hasSize(16);
        assertThat(response.role()).isEqualTo("VIEWER");
        ArgumentCaptor<InviteKey> captor = ArgumentCaptor.forClass(InviteKey.class);
        verify(inviteKeyRepository).save(captor.capture());
        String stored = captor.getValue().getCodeHash();
        assertThat(stored).hasSize(64);
        assertThat(stored).isNotEqualTo(response.code().replace("-", ""));
    }

    @Test
    void claimSuccessAddsMemberAndMarksKeyUsed() {
        InviteKey key = activeKey(Instant.now().plusSeconds(3600));
        when(inviteKeyRepository.findFirstByCodeHashOrderByIdDesc(anyString())).thenReturn(Optional.of(key));
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(9L)));
        when(familyMemberRepository.existsByFamilyIdAndUserId(9L, 7L)).thenReturn(false);
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inviteKeyRepository.save(any(InviteKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ClaimResponse response = service.claim(7L, "ABCD-EFGH-JKLM-NPQR", "CHILD");

        assertThat(response.familyId()).isEqualTo(9L);
        assertThat(response.role()).isEqualTo("VIEWER");
        assertThat(response.relation()).isEqualTo("CHILD");
        ArgumentCaptor<FamilyMember> memberCaptor = ArgumentCaptor.forClass(FamilyMember.class);
        verify(familyMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRelationSource()).isEqualTo("INVITE_KEY");
        assertThat(memberCaptor.getValue().getEvidenceStatus()).isEqualTo("SELF_DECLARED");
        assertThat(key.getStatus()).isEqualTo("USED");
        assertThat(key.getUsedCount()).isEqualTo(1);
    }

    @Test
    void claimRejectsExpiredKey() {
        InviteKey key = activeKey(Instant.now().minusSeconds(60));
        when(inviteKeyRepository.findFirstByCodeHashOrderByIdDesc(anyString())).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service.claim(7L, "ABCD-EFGH-JKLM-NPQR", "CHILD"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效或已过期");
    }

    @Test
    void infoReturnsInviterAndTarget() {
        InviteKey key = activeKey(Instant.now().plusSeconds(3600));
        when(inviteKeyRepository.findFirstByCodeHashOrderByIdDesc(anyString())).thenReturn(Optional.of(key));
        User inviter = new User();
        inviter.setId(1L);
        inviter.setName("李建国");
        inviter.setPhone("13800138001");
        when(userRepository.findById(1L)).thenReturn(Optional.of(inviter));
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(9L)));

        var response = service.info("ABCD-EFGH-JKLM-NPQR");

        assertThat(response.inviterName()).isEqualTo("李建国");
        assertThat(response.inviterPhone()).isEqualTo("138****8001");
        assertThat(response.targetName()).isEqualTo("张爷爷");
    }
}
