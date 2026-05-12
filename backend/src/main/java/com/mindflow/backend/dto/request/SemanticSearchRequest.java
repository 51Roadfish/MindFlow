package com.mindflow.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "语义搜索请求")
public class SemanticSearchRequest {
    @NotBlank(message = "搜索关键词不能为空")
    @Schema(description = "搜索关键词", example = "如何优化数据库")
    private String query;
}
