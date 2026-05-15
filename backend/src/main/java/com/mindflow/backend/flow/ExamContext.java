package com.mindflow.backend.flow;

import com.mindflow.backend.dto.ExamQuestion;
import lombok.Data;

import java.util.List;

@Data
public class ExamContext {
    private Long userId;
    private List<Long> noteIds;
    private String notesContent;
    private int questionCount = 10;
    private String examTitle;
    private String weakAreas;  // 薄弱点描述，复习后针对性出卷时使用
    private List<ExamQuestion> generatedExam;
}
