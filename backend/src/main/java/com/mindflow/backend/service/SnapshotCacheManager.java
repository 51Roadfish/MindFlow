package com.mindflow.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.backend.mongo.ReviewSnapshot;
import com.mindflow.backend.mongo.ReviewSnapshotRepository;
import com.mindflow.backend.utils.IdempotentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Redis 懒加载管理器：活跃会话走 Redis，冷数据从 Mongo 加载回填。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotCacheManager {

    private final IdempotentUtil idempotentUtil;
    private final ReviewSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    /** 获取活跃会话的快照 JSON，Redis miss 则从 Mongo 加载 */
    public String getActiveSession(Long sessionId) {
        String cached = idempotentUtil.getSession(sessionId);
        if (cached != null) {
            return cached;
        }
        // Redis miss → 从 Mongo 懒加载
        Optional<ReviewSnapshot> snapshot = snapshotRepository.findBySessionId(sessionId);
        if (snapshot.isEmpty()) {
            return null;
        }
        try {
            String data = objectMapper.writeValueAsString(snapshot.get());
            idempotentUtil.cacheSession(sessionId, data);
            log.info("Lazy-loaded session {} from Mongo into Redis", sessionId);
            return data;
        } catch (Exception e) {
            log.error("Failed to serialize snapshot for session {}", sessionId, e);
            return null;
        }
    }

    /** 保存快照到 Mongo 并刷新 Redis 缓存 */
    public void saveSnapshot(ReviewSnapshot snapshot) {
        snapshotRepository.save(snapshot);
        try {
            String data = objectMapper.writeValueAsString(snapshot);
            idempotentUtil.cacheSession(snapshot.getSessionId(), data);
        } catch (Exception e) {
            log.error("Failed to cache session {} to Redis", snapshot.getSessionId(), e);
        }
    }

    /** 清除会话缓存（Redis + Mongo） */
    public void evictSession(Long sessionId) {
        idempotentUtil.removeSession(sessionId);
        snapshotRepository.deleteById("review_" + sessionId);
    }
}
