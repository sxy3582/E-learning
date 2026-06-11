package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CodeProblemSaveRequest {
    private Long id;

    @NotBlank(message = "题目标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "支持语言不能为空")
    private String supportedLanguages;

    @NotBlank(message = "方法名不能为空")
    private String methodName;

    @NotBlank(message = "模板代码不能为空")
    private String templateCode;

    @Size(max = 65535, message = "Java 模板代码过长")
    private String templateCodeJava;

    @Size(max = 65535, message = "Python 模板代码过长")
    private String templateCodePython;

    @Size(max = 65535, message = "C++ 模板代码过长")
    private String templateCodeCpp;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
