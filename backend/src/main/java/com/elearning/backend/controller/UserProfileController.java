package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.UserProfileUpdateRequest;
import com.elearning.backend.entity.SysUser;
import com.elearning.backend.mapper.SysUserMapper;
import com.elearning.backend.util.AuthUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        AuthUtil.checkLogin();
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return ApiResponse.success(toSafeUser(user));
    }

    @PostMapping("/profile/update")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody @Validated UserProfileUpdateRequest request) {
        AuthUtil.checkLogin();
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setNickname(request.getNickname().trim());
        user.setAvatarUrl(normalize(request.getAvatarUrl()));
        user.setBio(normalize(request.getBio()));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ApiResponse.success("资料已更新", toSafeUser(user));
    }

    @PostMapping("/profile/avatar")
    public ApiResponse<Map<String, Object>> updateAvatar(@RequestBody Map<String, String> request) {
        AuthUtil.checkLogin();
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String avatarUrl = normalize(request.get("avatarUrl"));
        if (avatarUrl == null) {
            throw new BusinessException("头像地址不能为空");
        }
        user.setAvatarUrl(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ApiResponse.success("头像已更新", toSafeUser(user));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private Map<String, Object> toSafeUser(SysUser user) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("bio", user.getBio());
        data.put("role", user.getRole());
        data.put("banned", user.getBanned());
        data.put("createTime", user.getCreateTime());
        return data;
    }
}
