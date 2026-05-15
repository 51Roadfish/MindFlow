package com.mindflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion {
    private int id;
    private String type;         // short_answer, multiple_choice
    private String question;
    private List<String> options;
    private String answer;
    private int points;
}
