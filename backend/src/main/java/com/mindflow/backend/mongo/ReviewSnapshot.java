package com.mindflow.backend.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "review_snapshots")
public class ReviewSnapshot {

    @Id
    private String id;  // "review_<sessionId>"

    private Long sessionId;
    private Long userId;
    private List<Long> noteIds;
    private String status;  // IN_PROGRESS, COMPLETED, CANCELLED

    private List<ReviewTurn> turns = new ArrayList<>();
    private String currentQuestionJson;  // JSON of current Question
    private int totalScore;
    private int answeredQuestions;
    private LocalDateTime updatedAt;

    @Data
    public static class ReviewTurn {
        private String questionId;
        private String question;
        private String expectedAnswer;
        private String userAnswer;
        private int score;
        private String feedback;
        private String action;  // follow_up, next_question, complete
        private LocalDateTime timestamp;
    }
}
