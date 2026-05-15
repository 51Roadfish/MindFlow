package com.mindflow.backend.flow;

import com.mindflow.backend.dto.Question;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewContext {
    private Long sessionId;
    private Long userId;
    private List<Long> noteIds;
    private String notesContent;
    private String userAnswer;
    private int lastScore;
    private String lastFeedback;
    private String nextAction;   // follow_up | next_question | complete
    private int turnCount;
    private List<Question> history = new ArrayList<>();
}
