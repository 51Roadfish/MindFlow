package com.mindflow.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private List<String> tags;
    private Long notebookId;
    private Boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
