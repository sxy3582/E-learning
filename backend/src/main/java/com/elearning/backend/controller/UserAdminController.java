package com.elearning.backend.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.ResetPasswordRequest;
import com.elearning.backend.dto.UserBanRequest;
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
import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
public class UserAdminController {

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public ApiResponse<List<SysUser>> list() {
        AuthUtil.checkAdmin();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId));
        for (SysUser user : users) {
            user.setPassword(null);
        }
        return ApiResponse.success(users);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Validated ResetPasswordRequest request) {
        AuthUtil.checkAdmin();
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(DigestUtil.sha256Hex(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ApiResponse.success("重置成功", null);
    }

    @PostMapping("/ban")
    public ApiResponse<Void> ban(@RequestBody @Validated UserBanRequest request) {
        AuthUtil.checkAdmin();
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员账号不可封禁");
        }
        user.setBanned(request.getBanned() == null ? 0 : request.getBanned());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ApiResponse.success("操作成功", null);
    }
}
