package com.mindflow.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.backend.domain.ReviewSession;
import com.mindflow.backend.dto.Question;
import com.mindflow.backend.dto.request.ReviewAnswerRequest;
import com.mindflow.backend.dto.request.ReviewStartRequest;
import com.mindflow.backend.dto.response.ReviewAnswerResponse;
import com.mindflow.backend.dto.response.ReviewSessionResponse;
import com.mindflow.backend.flow.ReviewContext;
import com.mindflow.backend.mongo.ReviewSnapshot;
import com.mindflow.backend.mongo.ReviewSnapshotRepository;
import com.mindflow.backend.repository.ReviewSessionRepository;
import com.mindflow.backend.utils.IdempotentUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewSessionRepository sessionRepository;
    private final ReviewSnapshotRepository snapshotRepository;
    private final SnapshotCacheManager snapshotCache;
    private final IdempotentUtil idempotentUtil;
    private final FlowExecutor flowExecutor;
    private final ObjectMapper objectMapper;

    @Value("${review.max-questions-per-session:20}")
    private int maxQuestions;

    /** 启动复习会话（原子：先生成题目，再写数据库，不留僵死会话） */
    @Transactional
    public ReviewSessionResponse start(ReviewStartRequest request, Long userId) {
        String noteIdsHash = hashNoteIds(request.getNoteIds());

        // 幂等检查：已有活跃会话则直接返回
        Optional<ReviewSession> existing = sessionRepository
                .findByUserIdAndNoteIdsHashAndStatus(userId, noteIdsHash, "IN_PROGRESS");
        if (existing.isPresent()) {
            ReviewSession session = existing.get();
            ReviewSnapshot snapshot = loadSnapshot(session.getId());
            if (snapshot != null && snapshot.getCurrentQuestionJson() != null) {
                return buildResponse(session, snapshot);
            }
            // 僵死会话：无有效题目，清理后重建
            log.warn("Stale session {} found without valid question, cleaning up and recreating", session.getId());
            snapshotCache.evictSession(session.getId());
            sessionRepository.delete(session);
            sessionRepository.flush();
        }

        // 第一步：先生成题目（无持久化副作用，失败不留痕迹）
        ReviewContext reviewCtx = new ReviewContext();
        reviewCtx.setUserId(userId);
        reviewCtx.setNoteIds(request.getNoteIds());
        try {
            flowExecutor.execute2Resp("review_start_chain", reviewCtx);
        } catch (Exception e) {
            log.error("Failed to generate review questions", e);
            throw new RuntimeException("Failed to generate first question", e);
        }

        // 第二步：创建会话（题目已生成，不会因 LLM 超时而僵死）
        ReviewSession session = new ReviewSession();
        session.setUserId(userId);
        try {
            session.setNoteIdsJson(objectMapper.writeValueAsString(request.getNoteIds()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize noteIds", e);
        }
        session.setNoteIdsHash(noteIdsHash);
        session.setStatus("IN_PROGRESS");
        session.setTotalQuestions(reviewCtx.getTurnCount());
        session.setAnsweredQuestions(0);
        session.setTotalScore(0);
        session = sessionRepository.save(session);

        // 第三步：保存 MongoDB 快照
        saveSnapshot(session, reviewCtx, null);

        Question current = getCurrentQuestion(reviewCtx);
        return ReviewSessionResponse.builder()
                .id(session.getId())
                .status(session.getStatus())
                .currentQuestion(current)
                .totalQuestions(reviewCtx.getTurnCount())
                .answeredQuestions(0)
                .totalScore(0)
                .maxQuestions(maxQuestions)
                .build();
    }

    /** 提交答案 */
    @Transactional
    public ReviewAnswerResponse answer(Long sessionId, ReviewAnswerRequest request, Long userId) {
        // 幂等检查：同一题不重复提交
        if (!idempotentUtil.markAnswer(sessionId, request.getQuestionId())) {
            throw new RuntimeException("This question has already been answered");
        }

        ReviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // 从缓存或 Mongo 恢复上下文
        ReviewContext reviewCtx = restoreContext(sessionId);
        reviewCtx.setUserAnswer(request.getAnswer());

        // 执行 LiteFlow 答题评分链
        try {
            flowExecutor.execute2Resp("review_answer_chain", reviewCtx);
        } catch (Exception e) {
            log.error("LiteFlow review_answer_chain failed", e);
            throw new RuntimeException("Failed to score answer", e);
        }

        // 更新会话
        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        session.setTotalScore(session.getTotalScore() + reviewCtx.getLastScore());

        // 构建响应
        ReviewAnswerResponse.ReviewAnswerResponseBuilder respBuilder = ReviewAnswerResponse.builder()
                .score(reviewCtx.getLastScore())
                .feedback(reviewCtx.getLastFeedback())
                .nextAction(reviewCtx.getNextAction());

        if ("complete".equals(reviewCtx.getNextAction())) {
            session.setStatus("COMPLETED");
            sessionRepository.save(session);

            // 生成总结
            String summary = String.format("复习结束！共回答 %d 题，总分 %d，平均分 %d。",
                    session.getAnsweredQuestions(), session.getTotalScore(),
                    session.getAnsweredQuestions() > 0
                            ? session.getTotalScore() / session.getAnsweredQuestions() : 0);
            respBuilder.summary(summary);

            // 保存 Mongo 快照（最终状态）
            saveSnapshot(session, reviewCtx, request);
            idempotentUtil.removeSession(sessionId);
            return respBuilder.build();
        }

        // 下一题（follow_up 或 next_question）
        Question nextQ = generateNextQuestion(reviewCtx);
        session.setTotalQuestions(session.getTotalQuestions() + 1);
        sessionRepository.save(session);

        reviewCtx.setUserAnswer(null);
        reviewCtx.setLastScore(0);
        reviewCtx.setLastFeedback(null);

        // 保存 Mongo 快照
        saveSnapshot(session, reviewCtx, request);

        respBuilder.nextQuestion(nextQ);
        return respBuilder.build();
    }

    /** 获取会话状态 */
    public ReviewSessionResponse getSession(Long sessionId, Long userId) {
        ReviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        ReviewSnapshot snapshot = snapshotRepository.findBySessionId(sessionId).orElse(null);
        return buildResponse(session, snapshot);
    }

    /** 结束复习 */
    @Transactional
    public ReviewSessionResponse endSession(Long sessionId, Long userId) {
        ReviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        session.setStatus("CANCELLED");
        sessionRepository.save(session);

        idempotentUtil.removeSession(sessionId);
        return buildResponse(session, null);
    }

    /** 历史记录 */
    public List<ReviewSession> history(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ===== 私有方法 =====

    private String hashNoteIds(List<Long> noteIds) {
        try {
            String sorted = noteIds.stream().sorted()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sorted.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash noteIds", e);
        }
    }

    private Question getCurrentQuestion(ReviewContext ctx) {
        List<Question> history = ctx.getHistory();
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }

    private Question generateNextQuestion(ReviewContext ctx) {
        try {
            flowExecutor.execute2Resp("review_start_chain", ctx);
        } catch (Exception e) {
            log.error("Failed to generate next question", e);
        }
        return getCurrentQuestion(ctx);
    }

    private ReviewContext restoreContext(Long sessionId) {
        String cached = snapshotCache.getActiveSession(sessionId);
        if (cached != null) {
            try {
                ReviewSnapshot snapshot = objectMapper.readValue(cached, ReviewSnapshot.class);
                return fromSnapshot(snapshot);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached session {}, reloading from Mongo", sessionId, e);
            }
        }
        // 从 Mongo 加载
        ReviewSnapshot snapshot = snapshotRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session snapshot not found"));
        return fromSnapshot(snapshot);
    }

    private ReviewContext fromSnapshot(ReviewSnapshot snapshot) {
        ReviewContext ctx = new ReviewContext();
        ctx.setSessionId(snapshot.getSessionId());
        ctx.setUserId(snapshot.getUserId());
        ctx.setNoteIds(snapshot.getNoteIds());
        ctx.setTurnCount(snapshot.getAnsweredQuestions());
        ctx.setNotesContent("");  // 不保存原始笔记内容在快照中，下一轮会重新加载
        if (snapshot.getCurrentQuestionJson() != null) {
            try {
                Question q = objectMapper.readValue(snapshot.getCurrentQuestionJson(), Question.class);
                ctx.getHistory().add(q);
            } catch (Exception e) {
                log.warn("Failed to parse current question JSON", e);
            }
        }
        return ctx;
    }

    /** 从 Redis 缓存或 Mongo 加载快照 */
    private ReviewSnapshot loadSnapshot(Long sessionId) {
        String cached = snapshotCache.getActiveSession(sessionId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ReviewSnapshot.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached snapshot for session {}", sessionId, e);
            }
        }
        return snapshotRepository.findBySessionId(sessionId).orElse(null);
    }

    private void saveSnapshot(ReviewSession session, ReviewContext ctx, ReviewAnswerRequest answerReq) {
        ReviewSnapshot snapshot = new ReviewSnapshot();
        snapshot.setId("review_" + session.getId());
        snapshot.setSessionId(session.getId());
        snapshot.setUserId(session.getUserId());
        snapshot.setNoteIds(ctx.getNoteIds());
        snapshot.setStatus(session.getStatus());
        snapshot.setTotalScore(session.getTotalScore());
        snapshot.setAnsweredQuestions(session.getAnsweredQuestions());
        snapshot.setUpdatedAt(LocalDateTime.now());

        Question current = getCurrentQuestion(ctx);
        if (current != null) {
            try {
                snapshot.setCurrentQuestionJson(objectMapper.writeValueAsString(current));
            } catch (Exception e) {
                log.warn("Failed to serialize current question", e);
            }
        }

        // 如果有答案，添加本轮记录
        if (answerReq != null && current != null) {
            ReviewSnapshot.ReviewTurn turn = new ReviewSnapshot.ReviewTurn();
            turn.setQuestionId(current.getQuestionId());
            turn.setQuestion(current.getQuestion());
            turn.setExpectedAnswer(current.getExpectedAnswer());
            turn.setUserAnswer(answerReq.getAnswer());
            turn.setScore(ctx.getLastScore());
            turn.setFeedback(ctx.getLastFeedback());
            turn.setAction(ctx.getNextAction());
            turn.setTimestamp(LocalDateTime.now());
            snapshot.getTurns().add(turn);
        }

        snapshotCache.saveSnapshot(snapshot);
    }

    private ReviewSessionResponse buildResponse(ReviewSession session, ReviewSnapshot snapshot) {
        Question current = null;
        if (snapshot != null && snapshot.getCurrentQuestionJson() != null) {
            try {
                current = objectMapper.readValue(snapshot.getCurrentQuestionJson(), Question.class);
            } catch (Exception e) {
                log.warn("Failed to parse current question", e);
            }
        }
        return ReviewSessionResponse.builder()
                .id(session.getId())
                .status(session.getStatus())
                .currentQuestion(current)
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(session.getAnsweredQuestions())
                .totalScore(session.getTotalScore())
                .maxQuestions(maxQuestions)
                .build();
    }
}
