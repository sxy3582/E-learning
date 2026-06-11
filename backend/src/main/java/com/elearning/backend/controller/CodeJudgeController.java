package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.CodeProblemRunRequest;
import com.elearning.backend.dto.CodeRunRequest;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.CodeSubmission;
import com.elearning.backend.entity.CodeTestCase;
import com.elearning.backend.mapper.CodeProblemMapper;
import com.elearning.backend.mapper.CodeSubmissionMapper;
import com.elearning.backend.mapper.CodeTestCaseMapper;
import com.elearning.backend.service.CodeJudgeService;
import com.elearning.backend.service.WrongBookService;
import com.elearning.backend.util.AuthUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/code")
public class CodeJudgeController {

    @Resource
    private CodeJudgeService codeJudgeService;
    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;
    @Resource
    private CodeProblemMapper codeProblemMapper;
    @Resource
    private CodeTestCaseMapper codeTestCaseMapper;
    @Resource
    private WrongBookService wrongBookService;

    @PostMapping("/run-demo")
    public ApiResponse<Map<String, Object>> runDemo(@RequestBody @Validated CodeRunRequest request) {
        AuthUtil.checkLogin();
        Map<String, Object> result = codeJudgeService.runDemoAddProblem(request.getSourceCode());
        saveSubmission(result, "java", null);
        return ApiResponse.success(result);
    }

    @PostMapping("/run-problem")
    public ApiResponse<Map<String, Object>> runProblem(@RequestBody @Validated CodeProblemRunRequest request) {
        AuthUtil.checkLogin();
        CodeProblem problem = codeProblemMapper.selectById(request.getProblemId());
        if (problem == null) {
            throw new BusinessException("编程题不存在");
        }
        String language = normalizeLanguage(request.getLanguage());
        validateSupportedLanguage(problem, language);
        java.util.List<CodeTestCase> cases = codeTestCaseMapper.selectList(new LambdaQueryWrapper<CodeTestCase>()
                .eq(CodeTestCase::getProblemId, request.getProblemId())
                .orderByAsc(CodeTestCase::getSortOrder)
                .orderByAsc(CodeTestCase::getId));
        if (cases.isEmpty()) {
            throw new BusinessException("该编程题尚未配置测试用例");
        }
        Map<String, Object> result = codeJudgeService.runWithCases(request.getSourceCode(), problem, cases, language);
        saveSubmission(result, language, request.getProblemId());
        return ApiResponse.success(result);
    }

    private void saveSubmission(Map<String, Object> result, String language, Long problemId) {
        CodeSubmission submission = new CodeSubmission();
        Long userId = StpUtil.getLoginIdAsLong();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setLanguage(language);
        submission.setPassed(Boolean.TRUE.equals(result.get("passed")) ? 1 : 0);
        submission.setPassCount((Integer) result.get("passCount"));
        submission.setTotalCount((Integer) result.get("total"));
        submission.setCostMs(((Long) result.get("costMs")).intValue());
        submission.setCreateTime(LocalDateTime.now());
        codeSubmissionMapper.insert(submission);
        if (problemId != null) {
            if (Boolean.TRUE.equals(result.get("passed"))) {
                wrongBookService.removeCodeProblemWrong(userId, problemId);
            } else {
                wrongBookService.addCodeProblemWrong(userId, problemId);
            }
        }
        result.put("wrongBookCount", wrongBookService.countByUser(userId));
    }

    private String normalizeLanguage(String language) {
        String value = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        if (!"java".equals(value) && !"python".equals(value) && !"cpp".equals(value)) {
            throw new BusinessException("当前仅支持 Java、Python、C++");
        }
        return value;
    }

    private void validateSupportedLanguage(CodeProblem problem, String language) {
        String raw = problem.getSupportedLanguages();
        if (raw == null || raw.trim().isEmpty()) {
            if ("java".equals(language)) {
                return;
            }
            throw new BusinessException("该题目当前仅支持 Java");
        }
        List<String> languages = Arrays.asList(raw.toLowerCase(Locale.ROOT).split("\\s*,\\s*"));
        if (!languages.contains(language)) {
            throw new BusinessException("该题目暂不支持所选语言: " + language);
        }
    }
}
