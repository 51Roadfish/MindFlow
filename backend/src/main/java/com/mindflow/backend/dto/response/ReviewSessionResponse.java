package com.mindflow.backend.dto.response;

import com.mindflow.backend.dto.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSessionResponse {
    private Long id;
    private String status;
    private Question currentQuestion;
    private int totalQuestions;
    private int answeredQuestions;
    private int totalScore;
    private int maxQuestions;
}
