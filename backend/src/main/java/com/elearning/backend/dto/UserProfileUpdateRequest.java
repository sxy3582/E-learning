package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserProfileUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 128, message = "昵称不能超过128个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址不能超过500个字符")
    private String avatarUrl;

    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;
}
