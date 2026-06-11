package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AiAskRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题长度不能超过1000")
    private String question;
}
