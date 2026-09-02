package com.memorylink.qa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafetyFilterTest {

    private final SafetyFilter safetyFilter = new SafetyFilter();

    @Test
    void inputBlockCatchesRealWorldRequests() {
        assertThat(safetyFilter.inputBlock("能帮我借点钱吗")).isPresent();
        assertThat(safetyFilter.inputBlock("爷爷最喜欢吃什么")).isEmpty();
    }

    @Test
    void extremeEmotionDetected() {
        assertThat(safetyFilter.hasExtremeEmotion("有时候真的不想活了")).isTrue();
        assertThat(safetyFilter.hasExtremeEmotion("今天天气怎么样")).isFalse();
    }

    @Test
    void outputIssueReplacesUnsafeAnswer() {
        assertThat(safetyFilter.outputIssue("我可以帮你转账")).isPresent();
        assertThat(safetyFilter.outputIssue("爷爷小时候住在上海")).isEmpty();
    }
}
