package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserBanRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "封禁状态不能为空")
    private Integer banned;
}
