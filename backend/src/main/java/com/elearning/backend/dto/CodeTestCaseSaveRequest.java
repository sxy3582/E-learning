package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CodeTestCaseSaveRequest {
    private Long id;

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    @NotBlank(message = "输入不能为空")
    private String inputJson;

    @NotBlank(message = "期望输出不能为空")
    private String expectedOutput;

    @NotNull(message = "是否样例不能为空")
    private Integer isSample;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
