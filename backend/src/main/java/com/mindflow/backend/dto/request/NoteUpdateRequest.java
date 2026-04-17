package com.mindflow.backend.dto.request;

import lombok.Data;

@Data
public class NoteUpdateRequest {
    private String title;
    private String content;
    private Long notebookId;
    private Boolean isArchived;
}
