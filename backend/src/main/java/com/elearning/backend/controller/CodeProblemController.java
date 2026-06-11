package com.elearning.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.CodeProblemSaveRequest;
import com.elearning.backend.dto.CodeTestCaseSaveRequest;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.CodeTestCase;
import com.elearning.backend.entity.AppComment;
import com.elearning.backend.mapper.AppCommentMapper;
import com.elearning.backend.mapper.CodeProblemMapper;
import com.elearning.backend.mapper.CodeTestCaseMapper;
import com.elearning.backend.util.AuthUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/code-problem")
public class CodeProblemController {

    @Resource
    private CodeProblemMapper codeProblemMapper;
    @Resource
    private CodeTestCaseMapper codeTestCaseMapper;
    @Resource
    private AppCommentMapper appCommentMapper;

    @GetMapping("/list")
    public ApiResponse<List<CodeProblem>> list() {
        AuthUtil.checkLogin();
        return ApiResponse.success(codeProblemMapper.selectList(new LambdaQueryWrapper<CodeProblem>()
                .orderByAsc(CodeProblem::getSortOrder)
                .orderByAsc(CodeProblem::getId)));
    }

    @GetMapping("/admin/list")
    public ApiResponse<List<CodeProblem>> adminList() {
        AuthUtil.checkAdmin();
        return ApiResponse.success(codeProblemMapper.selectList(new LambdaQueryWrapper<CodeProblem>()
                .orderByAsc(CodeProblem::getSortOrder)
                .orderByAsc(CodeProblem::getId)));
    }

    @PostMapping("/admin/save")
    public ApiResponse<Void> save(@RequestBody @Validated CodeProblemSaveRequest request) {
        AuthUtil.checkAdmin();
        normalizeRequest(request);
        LocalDateTime now = LocalDateTime.now();
        if (request.getId() == null) {
            CodeProblem problem = new CodeProblem();
            BeanUtils.copyProperties(request, problem);
            problem.setCreateTime(now);
            problem.setUpdateTime(now);
            codeProblemMapper.insert(problem);
        } else {
            CodeProblem problem = codeProblemMapper.selectById(request.getId());
            if (problem == null) {
                throw new BusinessException("编程题不存在");
            }
            BeanUtils.copyProperties(request, problem);
            problem.setUpdateTime(now);
            codeProblemMapper.updateById(problem);
        }
        return ApiResponse.success("保存成功", null);
    }

    private void normalizeRequest(CodeProblemSaveRequest request) {
        request.setSupportedLanguages(normalizeLanguages(request.getSupportedLanguages()));
        String javaTemplate = normalizeTemplate(request.getTemplateCodeJava());
        String pythonTemplate = normalizeTemplate(request.getTemplateCodePython());
        String cppTemplate = normalizeTemplate(request.getTemplateCodeCpp());
        request.setTemplateCodeJava(javaTemplate);
        request.setTemplateCodePython(pythonTemplate);
        request.setTemplateCodeCpp(cppTemplate);
        if (javaTemplate == null || javaTemplate.isEmpty()) {
            throw new BusinessException("Java 模板代码不能为空");
        }
        request.setTemplateCode(javaTemplate);
    }

    private String normalizeLanguages(String supportedLanguages) {
        if (supportedLanguages == null || supportedLanguages.trim().isEmpty()) {
            throw new BusinessException("请至少选择一种支持语言");
        }
        String[] rawItems = supportedLanguages.split(",");
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<String>();
        for (String rawItem : rawItems) {
            String item = rawItem == null ? "" : rawItem.trim().toLowerCase(Locale.ROOT);
            if (item.isEmpty()) {
                continue;
            }
            if (!"java".equals(item) && !"python".equals(item) && !"cpp".equals(item)) {
                throw new BusinessException("存在不支持的语言: " + item);
            }
            values.add(item);
        }
        if (values.isEmpty()) {
            throw new BusinessException("请至少选择一种支持语言");
        }
        return String.join(",", values);
    }

    private String normalizeTemplate(String template) {
        if (template == null) {
            return null;
        }
        String value = template.replace("\r\n", "\n").trim();
        return value.isEmpty() ? null : value;
    }

    @DeleteMapping("/admin/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUtil.checkAdmin();
        codeProblemMapper.deleteById(id);
        appCommentMapper.delete(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getTargetType, "CODE_PROBLEM")
                .eq(AppComment::getTargetId, id));
        codeTestCaseMapper.delete(new LambdaQueryWrapper<CodeTestCase>()
                .eq(CodeTestCase::getProblemId, id));
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/test-case/list")
    public ApiResponse<List<CodeTestCase>> testCaseList(@RequestParam Long problemId) {
        AuthUtil.checkAdmin();
        return ApiResponse.success(codeTestCaseMapper.selectList(new LambdaQueryWrapper<CodeTestCase>()
                .eq(CodeTestCase::getProblemId, problemId)
                .orderByAsc(CodeTestCase::getSortOrder)
                .orderByAsc(CodeTestCase::getId)));
    }

    @PostMapping("/test-case/save")
    public ApiResponse<Void> saveTestCase(@RequestBody @Validated CodeTestCaseSaveRequest request) {
        AuthUtil.checkAdmin();
        LocalDateTime now = LocalDateTime.now();
        if (request.getId() == null) {
            CodeTestCase testCase = new CodeTestCase();
            BeanUtils.copyProperties(request, testCase);
            testCase.setCreateTime(now);
            testCase.setUpdateTime(now);
            codeTestCaseMapper.insert(testCase);
        } else {
            CodeTestCase testCase = codeTestCaseMapper.selectById(request.getId());
            if (testCase == null) {
                throw new BusinessException("测试用例不存在");
            }
            BeanUtils.copyProperties(request, testCase);
            testCase.setUpdateTime(now);
            codeTestCaseMapper.updateById(testCase);
        }
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/test-case/{id}")
    public ApiResponse<Void> deleteTestCase(@PathVariable Long id) {
        AuthUtil.checkAdmin();
        codeTestCaseMapper.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
