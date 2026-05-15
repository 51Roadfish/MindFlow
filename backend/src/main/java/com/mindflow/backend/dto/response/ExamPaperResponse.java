package com.mindflow.backend.dto.response;

import com.mindflow.backend.dto.ExamQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPaperResponse {
    private Long id;
    private String title;
    private int questionCount;
    private List<ExamQuestion> questions;
    private LocalDateTime createdAt;
}
