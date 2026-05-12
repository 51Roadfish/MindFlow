package com.mindflow.backend.dto.request;

import lombok.Data;

@Data
public class NoteBookCreateRequest {
    private String name;
    private String description;
}
