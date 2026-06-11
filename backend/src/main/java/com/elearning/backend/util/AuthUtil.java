package com.elearning.backend.util;

import cn.dev33.satoken.stp.StpUtil;
import com.elearning.backend.common.exception.BusinessException;

public class AuthUtil {

    private AuthUtil() {
    }

    public static void checkLogin() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("请先登录");
        }
    }

    public static void checkAdmin() {
        checkLogin();
        if (!StpUtil.hasRole("ADMIN")) {
            throw new BusinessException("仅管理员可操作");
        }
    }
}
