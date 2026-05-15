package com.mindflow.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.backend.domain.ExamPaper;
import com.mindflow.backend.domain.ReviewSession;
import com.mindflow.backend.dto.ExamQuestion;
import com.mindflow.backend.dto.request.ExamGenerateRequest;
import com.mindflow.backend.dto.response.ExamPaperResponse;
import com.mindflow.backend.flow.ExamContext;
import com.mindflow.backend.mongo.ReviewSnapshot;
import com.mindflow.backend.mongo.ReviewSnapshotRepository;
import com.mindflow.backend.repository.ExamPaperRepository;
import com.mindflow.backend.repository.ReviewSessionRepository;
import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamPaperRepository examPaperRepository;
    private final ReviewSessionRepository sessionRepository;
    private final ReviewSnapshotRepository snapshotRepository;
    private final FlowExecutor flowExecutor;
    private final ObjectMapper objectMapper;

    @Transactional
    public ExamPaperResponse generate(ExamGenerateRequest request, Long userId) {
        // 执行 LiteFlow 出卷链
        ExamContext examCtx = new ExamContext();
        examCtx.setUserId(userId);
        examCtx.setNoteIds(request.getNoteIds());
        examCtx.setQuestionCount(request.getQuestionCount() != null ? request.getQuestionCount() : 10);

        try {
            flowExecutor.execute2Resp("exam_generate_chain", examCtx);
        } catch (Exception e) {
            log.error("LiteFlow exam_generate_chain failed", e);
            throw new RuntimeException("Failed to generate exam paper", e);
        }

        List<ExamQuestion> questions = examCtx.getGeneratedExam();
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("No questions generated");
        }

        // 保存到 MySQL
        ExamPaper paper = new ExamPaper();
        paper.setUserId(userId);
        paper.setTitle(examCtx.getExamTitle() != null ? examCtx.getExamTitle() : "AI 考试");
        paper.setQuestionCount(questions.size());
        try {
            paper.setContent(objectMapper.writeValueAsString(questions));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize exam questions", e);
        }
        try {
            paper.setNoteIdsJson(objectMapper.writeValueAsString(request.getNoteIds()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize noteIds", e);
        }
        paper.setStatus("ACTIVE");
        paper = examPaperRepository.save(paper);

        return buildResponse(paper);
    }

    /** 复习后生成针对性试卷（针对低分知识点） */
    @Transactional
    public ExamPaperResponse generateFromReview(Long sessionId, Long userId) {
        // 验证会话所有权
        ReviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Review session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // 从 Mongo 加载快照
        ReviewSnapshot snapshot = snapshotRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Review snapshot not found"));

        // 提取弱项（score < 60 的轮次）
        String weakAreas = snapshot.getTurns().stream()
                .filter(t -> t.getScore() < 60)
                .map(t -> String.format(
                        "- 题目：%s\n  你的答案：%s\n  参考答案：%s\n  得分：%d",
                        t.getQuestion(), t.getUserAnswer(), t.getExpectedAnswer(), t.getScore()))
                .collect(Collectors.joining("\n"));

        if (weakAreas.isEmpty()) {
            throw new RuntimeException("No weak areas found — all answers scored 60 or above");
        }

        // 构建 ExamContext
        ExamContext examCtx = new ExamContext();
        examCtx.setUserId(userId);
        examCtx.setNoteIds(snapshot.getNoteIds());
        examCtx.setQuestionCount(5);
        examCtx.setWeakAreas(weakAreas);

        try {
            flowExecutor.execute2Resp("exam_generate_chain", examCtx);
        } catch (Exception e) {
            log.error("LiteFlow exam_generate_chain failed for review session {}", sessionId, e);
            throw new RuntimeException("Failed to generate targeted exam", e);
        }

        List<ExamQuestion> questions = examCtx.getGeneratedExam();
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("No questions generated");
        }

        ExamPaper paper = new ExamPaper();
        paper.setUserId(userId);
        paper.setTitle("薄弱点针对性试卷");
        paper.setQuestionCount(questions.size());
        try {
            paper.setContent(objectMapper.writeValueAsString(questions));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize exam questions", e);
        }
        try {
            paper.setNoteIdsJson(objectMapper.writeValueAsString(snapshot.getNoteIds()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize noteIds", e);
        }
        paper.setStatus("ACTIVE");
        paper = examPaperRepository.save(paper);

        return buildResponse(paper);
    }

    public ExamPaperResponse getExam(Long examId, Long userId) {
        ExamPaper paper = examPaperRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam paper not found"));
        if (!paper.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        return buildResponse(paper);
    }

    public List<ExamPaperResponse> listExams(Long userId) {
        return examPaperRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE")
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional
    public void deleteExam(Long examId, Long userId) {
        ExamPaper paper = examPaperRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam paper not found"));
        if (!paper.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        paper.setStatus("DELETED");
        examPaperRepository.save(paper);
    }

    @SuppressWarnings("unchecked")
    private ExamPaperResponse buildResponse(ExamPaper paper) {
        List<ExamQuestion> questions = null;
        if (paper.getContent() != null) {
            try {
                questions = objectMapper.readValue(paper.getContent(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ExamQuestion.class));
            } catch (Exception e) {
                log.warn("Failed to parse exam content for paper {}", paper.getId(), e);
            }
        }
        return ExamPaperResponse.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .questionCount(paper.getQuestionCount())
                .questions(questions)
                .createdAt(paper.getCreatedAt())
                .build();
    }
}
