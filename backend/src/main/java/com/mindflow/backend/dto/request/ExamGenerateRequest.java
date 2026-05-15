package com.mindflow.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ExamGenerateRequest {
    @NotEmpty
    private List<Long> noteIds;
    private List<String> tags;
    private Integer questionCount = 10;
}
