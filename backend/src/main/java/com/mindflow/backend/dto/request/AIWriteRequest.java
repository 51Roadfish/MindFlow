package com.mindflow.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 续写请求")
public class AIWriteRequest {
    @NotBlank(message = "操作指令不能为空")
    @Schema(description = "操作指令（如：续写、润色）", example = "续写")
    private String action;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "要处理的文本内容", example = "微服务架构的优点有...")
    private String content;
}
