package com.elearning.backend.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.entity.SysUser;
import com.elearning.backend.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, Long.valueOf(loginId.toString()));
        SysUser user = sysUserMapper.selectOne(wrapper);
        List<String> roles = new ArrayList<String>();
        if (user != null && user.getRole() != null) {
            roles.add(user.getRole());
        }
        return roles;
    }
}
