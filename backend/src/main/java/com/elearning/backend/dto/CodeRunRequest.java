package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CodeRunRequest {
    @NotBlank(message = "代码不能为空")
    private String sourceCode;
}
