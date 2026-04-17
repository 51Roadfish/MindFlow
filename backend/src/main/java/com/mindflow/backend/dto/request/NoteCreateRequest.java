package com.mindflow.backend.dto.request;

import lombok.Data;

@Data
public class NoteCreateRequest {
    private String title;
    private String content;
    private Long notebookId;
}
