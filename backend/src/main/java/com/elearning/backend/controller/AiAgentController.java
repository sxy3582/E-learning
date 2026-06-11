package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.dto.AiAskRequest;
import com.elearning.backend.service.AiAgentService;
import com.elearning.backend.util.AuthUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AiAgentController {

    @Resource
    private AiAgentService aiAgentService;

    @PostMapping("/ask")
    public ApiResponse<Map<String, Object>> ask(@RequestBody @Validated AiAskRequest request) {
        AuthUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(aiAgentService.ask(userId, request.getQuestion()));
    }
}
