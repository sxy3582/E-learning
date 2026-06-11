package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ChapterSaveRequest {
    private Long id;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "章节标题不能为空")
    @Size(max = 100, message = "章节标题最多100字符")
    private String title;

    @NotBlank(message = "内容类型不能为空")
    @Size(max = 20, message = "内容类型最多20字符")
    private String contentType;

    private String contentValue;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
