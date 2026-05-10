package com.mindflow.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class NoteUpdateRequest {
    private String title;
    private String content;
    private Long notebookId;
    private Boolean isArchived;
    private List<String> tags;
}
