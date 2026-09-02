package com.memorylink.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.family.FamilyMember;
import com.memorylink.family.FamilyMemberRepository;
import com.memorylink.family.FamilyService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentRecordRepository consentRecordRepository;
    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private FamilyMemberRepository familyMemberRepository;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        consentService = new ConsentService(
                consentRecordRepository, lovedOneRepository, familyService, familyMemberRepository);
    }

    private LovedOne lovedOne(Long id, Long familyId) {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(id);
        lovedOne.setFamilyId(familyId);
        return lovedOne;
    }

    private FamilyMember activeMember(Long userId) {
        FamilyMember member = new FamilyMember();
        member.setUserId(userId);
        member.setStatus("ACTIVE");
        return member;
    }

    @Test
    void createRequiresTwoRelativesToHaveTwoConsentors() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(1L, 9L)));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> consentService.create(1L, 1L, "TWO_RELATIVES", List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少需要 2 位确认人");
    }

    @Test
    void createSavesValidConsentWhenAllConsentorsAreMembers() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(1L, 9L)));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyIdAndUserId(9L, 1L))
                .thenReturn(Optional.of(activeMember(1L)));
        when(familyMemberRepository.findByFamilyIdAndUserId(9L, 2L))
                .thenReturn(Optional.of(activeMember(2L)));
        ConsentRecord saved = new ConsentRecord();
        saved.setId(1L);
        saved.setLovedOneId(1L);
        saved.setConsentType("TWO_RELATIVES");
        saved.setConsentorIds(List.of(1L, 2L));
        saved.setStatus("VALID");
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenReturn(saved);

        ConsentRecord record = consentService.create(1L, 1L, "TWO_RELATIVES", List.of(1L, 2L));

        assertThat(record.getConsentType()).isEqualTo("TWO_RELATIVES");
        assertThat(record.getConsentorIds()).containsExactly(1L, 2L);
    }

    @Test
    void createForbiddenWhenNotFamilyMember() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne(1L, 9L)));
        when(familyService.canAccess(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> consentService.create(1L, 1L, "TWO_RELATIVES", List.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
    }
}
