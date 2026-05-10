package com.mindflow.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class NoteCreateRequest {
    private String title;
    private String content;
    private Long notebookId;
    private List<String> tags;
}
