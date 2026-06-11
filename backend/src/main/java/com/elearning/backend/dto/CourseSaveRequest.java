package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CourseSaveRequest {
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称最多100字符")
    private String title;

    @Size(max = 500, message = "课程简介最多500字符")
    private String intro;

    @Size(max = 500, message = "封面URL最多500字符")
    private String coverUrl;

    @Size(max = 32, message = "难度最多32字符")
    private String difficulty;

    @Size(max = 32, message = "分类最多32字符")
    private String category;

    @NotNull(message = "发布状态不能为空")
    private Integer published;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
