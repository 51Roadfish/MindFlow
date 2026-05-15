package com.mindflow.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewAnswerRequest {
    @NotBlank
    private String questionId;
    @NotBlank
    private String answer;
}
