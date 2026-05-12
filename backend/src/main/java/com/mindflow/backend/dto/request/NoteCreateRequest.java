package com.mindflow.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "创建笔记请求")
public class NoteCreateRequest {
    @NotBlank(message = "笔记标题不能为空")
    @Schema(description = "笔记标题", example = "Spring Boot 学习笔记")
    private String title;

    @NotBlank(message = "笔记内容不能为空")
    @Schema(description = "笔记内容", example = "这是笔记的详细内容...")
    private String content;

    @NotNull(message = "所属笔记本 ID 不能为空")
    @Schema(description = "所属笔记本ID", example = "1")
    private Long notebookId;
    private List<String> tags;
}
