package com.mindflow.backend.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotentUtil {

    private final StringRedisTemplate redisTemplate;

    private static final String ANSWER_KEY = "review:session:%d:q:%s";
    private static final String ACTIVE_SESSION_KEY = "review:session:%d:active";
    private static final long ANSWER_TTL_SECONDS = 3600;
    private static final long ACTIVE_TTL_MINUTES = 30;

    /** 标记本题已答，返回 true 表示第一次提交，false 表示重复提交 */
    public boolean markAnswer(Long sessionId, String questionId) {
        String key = ANSWER_KEY.formatted(sessionId, questionId);
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, "1", ANSWER_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(absent);
    }

    /** 刷新活跃会话 TTL */
    public void refreshSession(Long sessionId) {
        String key = ACTIVE_SESSION_KEY.formatted(sessionId);
        redisTemplate.expire(key, ACTIVE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 缓存活跃会话数据 */
    public void cacheSession(Long sessionId, String data) {
        String key = ACTIVE_SESSION_KEY.formatted(sessionId);
        redisTemplate.opsForValue().set(key, data, ACTIVE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 获取缓存的会话数据 */
    public String getSession(Long sessionId) {
        String key = ACTIVE_SESSION_KEY.formatted(sessionId);
        return redisTemplate.opsForValue().get(key);
    }

    /** 清除会话缓存 */
    public void removeSession(Long sessionId) {
        String key = ACTIVE_SESSION_KEY.formatted(sessionId);
        redisTemplate.delete(key);
    }
}
