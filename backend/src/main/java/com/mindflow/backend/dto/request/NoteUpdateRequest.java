package com.mindflow.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "更新笔记请求")
public class NoteUpdateRequest {
    @NotBlank(message = "笔记标题不能为空")
    @Schema(description = "笔记标题", example = "更新后的标题")
    private String title;

    @NotBlank(message = "笔记内容不能为空")
    @Schema(description = "笔记内容", example = "更新后的内容")
    private String content;

    @Schema(description = "所属笔记本ID", example = "1")
    private Long notebookId;

    @Schema(description = "是否归档", example = "false")
    private Boolean isArchived;
    private List<String> tags;
}
