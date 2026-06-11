package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CommentSaveRequest {
    private Long id;

    @NotBlank(message = "评论目标类型不能为空")
    private String targetType;

    @NotNull(message = "评论目标ID不能为空")
    private Long targetId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;
}
