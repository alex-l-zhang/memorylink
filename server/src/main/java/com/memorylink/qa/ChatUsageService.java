package com.memorylink.qa;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 故事问答使用时长提醒（防沉迷）：2 小时内高频使用给出休息提示。
 */
@Service
public class ChatUsageService {

    private final StringRedisTemplate redisTemplate;

    public ChatUsageService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String recordAndHint(Long userId) {
        try {
            String key = "qa:user:" + userId;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofHours(2));
            }
            if (count != null && count > 50 && count % 10 == 0) {
                return "你已连续使用一段时间了，先休息一下吧。";
            }
        } catch (Exception ignored) {
            // Redis 不可用时不影响问答主流程
        }
        return null;
    }
}
