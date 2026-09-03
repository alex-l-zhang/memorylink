package com.memorylink.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.consent.ConsentRecord;
import com.memorylink.consent.ConsentRecordRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.qa.dto.ChatResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QaServiceTest {

    @Mock
    private LovedOneRepository lovedOneRepository;
    @Mock
    private FamilyService familyService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private DeepSeekGateway deepSeekGateway;
    @Mock
    private ChatUsageService chatUsageService;
    @Mock
    private ConsentRecordRepository consentRecordRepository;
    @Mock
    private UserRepository userRepository;

    private QaService qaService;

    @BeforeEach
    void setUp() {
        qaService = new QaService(lovedOneRepository, familyService, conversationRepository,
                deepSeekGateway, new SafetyFilter(), chatUsageService,
                consentRecordRepository, userRepository);
    }

    private User adultUser() {
        User user = new User();
        user.setId(1L);
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        return user;
    }

    private LovedOne livingEnabledPerson() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        lovedOne.setName("张爷爷");
        lovedOne.setBirthPlace("上海");
        lovedOne.setAiPersonaEnabled(true);
        return lovedOne;
    }

    private LovedOne deceasedPerson() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        lovedOne.setName("张爷爷");
        lovedOne.setDeathDate(LocalDate.of(2020, 1, 1));
        return lovedOne;
    }

    private void stubEligibleLiving() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(livingEnabledPerson()));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adultUser()));
    }

    @Test
    void chatReturnsGatewayAnswerAndSavesConversation() {
        stubEligibleLiving();
        when(conversationRepository.findTop10ByLovedOneIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(deepSeekGateway.complete(anyString(), anyString()))
                .thenReturn("爷爷小时候最喜欢在弄堂里和邻居下棋。");

        Conversation saved = new Conversation();
        saved.setId(7L);
        saved.setAnswer("爷爷小时候最喜欢在弄堂里和邻居下棋。");
        saved.setAiFlag(true);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(chatUsageService.recordAndHint(1L)).thenReturn(null);

        ChatResponse response = qaService.chat(1L, 1L, "爷爷小时候最喜欢做什么？");

        assertThat(response.conversationId()).isEqualTo(7L);
        assertThat(response.answer()).contains("下棋");
        assertThat(response.aiFlag()).isTrue();
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void blockedQuestionDoesNotCallGateway() {
        stubEligibleLiving();
        Conversation saved = new Conversation();
        saved.setId(8L);
        saved.setAnswer("这个问题涉及现实事务，超出了记忆助手的能力范围，建议咨询相关专业机构。");
        saved.setAiFlag(false);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(chatUsageService.recordAndHint(1L)).thenReturn(null);

        ChatResponse response = qaService.chat(1L, 1L, "能帮我借点钱吗");

        assertThat(response.aiFlag()).isFalse();
        assertThat(response.answer()).contains("现实事务");
        verifyNoInteractions(deepSeekGateway);
    }

    @Test
    void deceasedWithoutConsentRejected() {
        LovedOne deceased = deceasedPerson();
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(deceased));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adultUser()));
        when(consentRecordRepository.findFirstByLovedOneIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> qaService.chat(1L, 1L, "你好"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("知情同意");
    }

    @Test
    void deceasedWithValidConsentAllowed() {
        LovedOne deceased = deceasedPerson();
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(deceased));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adultUser()));
        ConsentRecord consent = new ConsentRecord();
        consent.setStatus("VALID");
        when(consentRecordRepository.findFirstByLovedOneIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(consent));
        when(conversationRepository.findTop10ByLovedOneIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(deepSeekGateway.complete(anyString(), anyString())).thenReturn("回答");
        Conversation saved = new Conversation();
        saved.setId(1L);
        saved.setAnswer("回答");
        saved.setAiFlag(true);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(chatUsageService.recordAndHint(1L)).thenReturn(null);

        ChatResponse response = qaService.chat(1L, 1L, "你好");

        assertThat(response.answer()).isEqualTo("回答");
    }

    @Test
    void livingWithoutAiConsentRejected() {
        LovedOne living = livingEnabledPerson();
        living.setAiPersonaEnabled(false);
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(living));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adultUser()));

        assertThatThrownBy(() -> qaService.chat(1L, 1L, "你好"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未开启");
    }
}
