package com.memorylink.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.qa.dto.ChatResponse;
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

    private QaService qaService;

    @BeforeEach
    void setUp() {
        qaService = new QaService(lovedOneRepository, familyService, conversationRepository,
                deepSeekGateway, new SafetyFilter(), chatUsageService);
    }

    private LovedOne lovedOne() {
        LovedOne lovedOne = new LovedOne();
        lovedOne.setId(1L);
        lovedOne.setFamilyId(9L);
        lovedOne.setName("张爷爷");
        lovedOne.setBirthPlace("上海");
        return lovedOne;
    }

    @Test
    void chatReturnsGatewayAnswerAndSavesConversation() {
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne()));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);
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
        when(lovedOneRepository.findById(1L)).thenReturn(Optional.of(lovedOne()));
        when(familyService.canAccess(1L, 9L)).thenReturn(true);

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
}
