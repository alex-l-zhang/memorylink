package com.memorylink.qa;

import com.memorylink.archive.LovedOne;
import com.memorylink.archive.LovedOneRepository;
import com.memorylink.common.BusinessException;
import com.memorylink.common.UserAge;
import com.memorylink.consent.ConsentRecord;
import com.memorylink.consent.ConsentRecordRepository;
import com.memorylink.family.FamilyService;
import com.memorylink.qa.dto.ChatResponse;
import com.memorylink.user.User;
import com.memorylink.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QaService {

    public static final int CODE_ARCHIVE_NOT_FOUND = 3002;
    public static final int CODE_FORBIDDEN = 4001;
    public static final int CODE_AI_NOT_OPEN = 3006;
    public static final int CODE_ADULT_REQUIRED = 2003;

    private final LovedOneRepository lovedOneRepository;
    private final FamilyService familyService;
    private final ConversationRepository conversationRepository;
    private final DeepSeekGateway deepSeekGateway;
    private final SafetyFilter safetyFilter;
    private final ChatUsageService chatUsageService;
    private final ConsentRecordRepository consentRecordRepository;
    private final UserRepository userRepository;

    public QaService(LovedOneRepository lovedOneRepository,
                     FamilyService familyService,
                     ConversationRepository conversationRepository,
                     DeepSeekGateway deepSeekGateway,
                     SafetyFilter safetyFilter,
                     ChatUsageService chatUsageService,
                     ConsentRecordRepository consentRecordRepository,
                     UserRepository userRepository) {
        this.lovedOneRepository = lovedOneRepository;
        this.familyService = familyService;
        this.conversationRepository = conversationRepository;
        this.deepSeekGateway = deepSeekGateway;
        this.safetyFilter = safetyFilter;
        this.chatUsageService = chatUsageService;
        this.consentRecordRepository = consentRecordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatResponse chat(Long userId, Long lovedOneId, String question) {
        LovedOne lovedOne = requireAccess(userId, lovedOneId);
        requireEligible(userId, lovedOne);
        Conversation conversation = new Conversation();
        conversation.setLovedOneId(lovedOneId);
        conversation.setUserId(userId);
        conversation.setQuestion(question);

        var blocked = safetyFilter.inputBlock(question);
        if (blocked.isPresent()) {
            conversation.setAnswer(blocked.get());
            conversation.setAiFlag(false);
        } else if (safetyFilter.hasExtremeEmotion(question)) {
            conversation.setAnswer(safetyFilter.supportMessage());
            conversation.setAiFlag(false);
        } else {
            String systemPrompt = buildSystemPrompt(lovedOne);
            String answer = deepSeekGateway.complete(systemPrompt, question);
            var outputIssue = safetyFilter.outputIssue(answer);
            conversation.setAnswer(outputIssue.orElse(answer));
            conversation.setAiFlag(true);
        }

        Conversation saved = conversationRepository.save(conversation);
        String usageHint = chatUsageService.recordAndHint(userId);
        return new ChatResponse(
                saved.getId(), saved.getAnswer(), saved.isAiFlag(), usageHint, Instant.now());
    }

    private String buildSystemPrompt(LovedOne lovedOne) {
        String facts = String.join("\n",
                "姓名：" + nullToDash(lovedOne.getName()),
                "籍贯：" + nullToDash(lovedOne.getBirthPlace()),
                "生卒：" + (lovedOne.getBirthDate() == null ? "-" : lovedOne.getBirthDate())
                        + " 至 " + (lovedOne.getDeathDate() == null ? "-" : lovedOne.getDeathDate()),
                "生平简介：" + nullToDash(lovedOne.getBio()));
        String history = conversationRepository
                .findTop10ByLovedOneIdOrderByCreatedAtDesc(lovedOne.getId()).stream()
                .limit(5)
                .map(c -> "家人问：" + c.getQuestion() + "\n回应：" + nullToDash(c.getAnswer()))
                .collect(Collectors.joining("\n"));

        return """
                你在帮助一个家庭缅怀「%s」。请以温暖、克制的口吻，基于以下记忆档案回答问题：
                %s
                最近对话记录：
                %s
                规则：
                1. 只使用档案中已有的信息，不编造事实；
                2. 档案中没有的信息，请说"这部分家人没有留下记录"；
                3. 不处理金钱、法律、医疗等现实事务；
                4. 你是 AI 生成的记忆助手，不要声称自己是真人。
                """.formatted(lovedOne.getName(), facts, history.isEmpty() ? "（暂无）" : history);
    }

    private LovedOne requireAccess(Long userId, Long lovedOneId) {
        LovedOne lovedOne = lovedOneRepository.findById(lovedOneId)
                .orElseThrow(() -> new BusinessException(CODE_ARCHIVE_NOT_FOUND, "档案不存在"));
        if (!familyService.canAccess(userId, lovedOne.getFamilyId())) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该档案");
        }
        return lovedOne;
    }

    private void requireEligible(Long userId, LovedOne lovedOne) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CODE_ADULT_REQUIRED, "用户不存在"));
        if (!UserAge.isAdult(user)) {
            throw new BusinessException(CODE_ADULT_REQUIRED, "需年满 18 周岁且已完善出生日期后使用故事问答");
        }
        if (lovedOne.isDeceased()) {
            boolean consented = consentRecordRepository
                    .findFirstByLovedOneIdOrderByCreatedAtDesc(lovedOne.getId())
                    .filter(c -> "VALID".equals(c.getStatus()))
                    .isPresent();
            if (!consented) {
                throw new BusinessException(CODE_AI_NOT_OPEN,
                        "该档案尚未完成知情同意，暂不能开启故事问答，请由近亲属提交授权");
            }
        } else if (!lovedOne.isAiPersonaEnabled()) {
            throw new BusinessException(CODE_AI_NOT_OPEN,
                    "该成员尚未开启 AI 讲述，仅本人可开启");
        }
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
