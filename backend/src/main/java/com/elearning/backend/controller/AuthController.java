package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.AuthRequest;
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
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private SysUserMapper sysUserMapper;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody @Validated AuthRequest request) {
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null) {
            throw new BusinessException("用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(DigestUtil.sha256Hex(request.getPassword()));
        user.setNickname(request.getNickname() == null || request.getNickname().trim().isEmpty()
                ? request.getUsername()
                : request.getNickname().trim());
        user.setRole("USER");
        user.setBanned(0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        sysUserMapper.insert(user);
        return ApiResponse.success("注册成功", toSafeUser(user));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Validated AuthRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !DigestUtil.sha256Hex(request.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (Integer.valueOf(1).equals(user.getBanned())) {
            throw new BusinessException("账号已被封禁");
        }
        StpUtil.login(user.getId());
        Map<String, Object> data = toSafeUser(user);
        data.put("token", StpUtil.getTokenValue());
        return ApiResponse.success("登录成功", data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        AuthUtil.checkLogin();
        StpUtil.logout();
        return ApiResponse.success("退出成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        AuthUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return ApiResponse.success(toSafeUser(user));
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
