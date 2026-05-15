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
public class ReviewAnswerResponse {
    private int score;
    private String feedback;
    private String nextAction;     // follow_up, next_question, complete
    private Question nextQuestion;
    private String summary;         // 复习总结（complete 时返回）
}
