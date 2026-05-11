package com.mindflow.backend.dto.request;

import lombok.Data;

@Data
public class NoteBookUpdateRequest {
    private String name;
    private String description;
}
