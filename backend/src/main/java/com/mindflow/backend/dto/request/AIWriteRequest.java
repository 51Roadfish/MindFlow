package com.mindflow.backend.dto.request;

import lombok.Data;

@Data
public class AIWriteRequest {
    private String action;
    private String content;
}
