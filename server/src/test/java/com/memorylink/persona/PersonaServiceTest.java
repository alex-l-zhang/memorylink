package com.memorylink.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.audit.AuditLogRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.persona.dto.AiConsentResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private PersonaService personaService;

    @BeforeEach
    void setUp() {
        personaService = new PersonaService(lovedOneRepository, userRepository, auditLogRepository);
    }

    private User user(int age) {
        User user = new User();
        user.setId(1L);
        user.setBirthDate(LocalDate.now().minusYears(age));
        return user;
    }

    private LovedOne livingBound() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setUserId(1L);
        lovedOne.setDeceased(false);
        return lovedOne;
    }

    @Test
    void enableBySelfAdultSucceeds() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(livingBound()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(40)));
        when(lovedOneRepository.save(any(LovedOne.class))).thenAnswer(inv -> inv.getArgument(0));

        AiConsentResponse response = personaService.enable(1L, 1L);

        assertThat(response.enabled()).isTrue();
        verify(auditLogRepository).save(any());
    }

    @Test
    void enableForDeceasedRejected() {
        LovedOne deceased = livingBound();
        deceased.setDeceased(true);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(deceased));

        assertThatThrownBy(() -> personaService.enable(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("故人档案");
    }

    @Test
    void enableByOtherUserRejected() {
        LovedOne lovedOne = livingBound();
        lovedOne.setUserId(2L);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne));

        assertThatThrownBy(() -> personaService.enable(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅档案本人");
    }

    @Test
    void enableMinorRejected() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(livingBound()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(16)));

        assertThatThrownBy(() -> personaService.enable(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("年满 18");
    }
}
