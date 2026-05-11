package com.mindflow.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteBookResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
