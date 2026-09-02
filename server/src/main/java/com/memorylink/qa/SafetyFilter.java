package com.memorylink.qa;

import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 输入/输出安全过滤：拦截敏感现实事务请求与自伤倾向，兜底输出不当内容。
 */
@Component
public class SafetyFilter {

    private static final Set<String> BLOCKED_TOKENS = Set.of(
            "转账", "汇款", "借钱", "借点钱", "打钱", "验证码", "银行卡", "支付密码", "遗产分割", "法律文书", "替我做决定"
    );

    private static final Set<String> EXTREME_EMOTION_TOKENS = Set.of(
            "不想活", "活不下去", "想自杀", "自杀", "活着没意思", "撑不下去", "伤害自己"
    );

    private static final String SUPPORT_MESSAGE =
            "听到你这样说，我心里很担心。你并不孤单，请一定联系身边信任的人，或拨打心理援助热线 12356 寻求专业帮助。";

    public Optional<String> inputBlock(String question) {
        if (question == null) {
            return Optional.empty();
        }
        for (String token : BLOCKED_TOKENS) {
            if (question.contains(token)) {
                return Optional.of("这个问题涉及现实事务，超出了记忆助手的能力范围，建议咨询相关专业机构。");
            }
        }
        return Optional.empty();
    }

    public boolean hasExtremeEmotion(String question) {
        if (question == null) {
            return false;
        }
        for (String token : EXTREME_EMOTION_TOKENS) {
            if (question.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public String supportMessage() {
        return SUPPORT_MESSAGE;
    }

    public Optional<String> outputIssue(String answer) {
        if (answer == null) {
            return Optional.empty();
        }
        for (String token : BLOCKED_TOKENS) {
            if (answer.contains(token)) {
                return Optional.of("这个问题涉及现实事务，我无法回应，建议咨询专业机构。");
            }
        }
        return Optional.empty();
    }
}
