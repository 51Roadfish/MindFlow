package com.mindflow.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReviewStartRequest {
    @NotEmpty
    private List<Long> noteIds;
    private List<String> tags;
}
