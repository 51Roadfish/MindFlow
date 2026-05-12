package com.mindflow.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI 聊天请求")
public class AIChatRequest {
    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户输入的问题", example = "什么是大语言模型？")
    private String question;
}
