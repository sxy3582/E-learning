package com.elearning.backend.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.CodeTestCase;
import com.elearning.backend.service.code.CodeRunner;
import com.elearning.backend.service.code.PreparedCodeProgram;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CodeJudgeService {
    private final Map<String, CodeRunner> runnerMap = new HashMap<String, CodeRunner>();

    public CodeJudgeService(List<CodeRunner> codeRunners) {
        for (CodeRunner codeRunner : codeRunners) {
            runnerMap.put(codeRunner.getLanguage().toLowerCase(Locale.ROOT), codeRunner);
        }
    }

    public Map<String, Object> runDemoAddProblem(String sourceCode) {
        int[][] testCases = new int[][]{{1, 2, 3}, {10, 20, 30}, {-5, 8, 3}, {0, 0, 0}};
        List<CodeTestCase> cases = new ArrayList<CodeTestCase>();
        for (int[] item : testCases) {
            CodeTestCase testCase = new CodeTestCase();
            testCase.setInputJson("[" + item[0] + "," + item[1] + "]");
            testCase.setExpectedOutput(String.valueOf(item[2]));
            cases.add(testCase);
        }
        CodeProblem problem = new CodeProblem();
        problem.setMethodName("add");
        return runWithCases(sourceCode, problem, cases, "java");
    }

    public Map<String, Object> runWithCases(String sourceCode, CodeProblem problem, List<CodeTestCase> cases) {
        return runWithCases(sourceCode, problem, cases, "java");
    }

    public Map<String, Object> runWithCases(String sourceCode, CodeProblem problem, List<CodeTestCase> cases, String language) {
        String normalizedLanguage = normalizeLanguage(language);
        validateSourceCode(sourceCode);
        CodeRunner runner = requireRunner(normalizedLanguage);

        List<JSONArray> rawInputs = new ArrayList<JSONArray>();
        List<List<Integer>> parsedArgsList = new ArrayList<List<Integer>>();
        Integer argCount = null;
        for (CodeTestCase item : cases) {
            JSONArray argsJson = JSONUtil.parseArray(item.getInputJson());
            List<Integer> args = parseArgs(argsJson);
            rawInputs.add(argsJson);
            parsedArgsList.add(args);
            if (argCount == null) {
                argCount = args.size();
            } else if (argCount.intValue() != args.size()) {
                throw new BusinessException("当前多语言执行器要求所有测试用例参数个数一致");
            }
        }

        List<Map<String, Object>> caseResults = new ArrayList<Map<String, Object>>();
        int passCount = 0;
        long start = System.currentTimeMillis();
        PreparedCodeProgram program = runner.prepare(problem, sourceCode, argCount == null ? 0 : argCount.intValue());
        try {
            for (int i = 0; i < cases.size(); i++) {
                CodeTestCase item = cases.get(i);
                List<Integer> args = parsedArgsList.get(i);
                int actual = program.run(args);
                boolean pass = String.valueOf(actual).equals(item.getExpectedOutput());
                if (pass) {
                    passCount++;
                }
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("input", rawInputs.get(i));
                row.put("expected", item.getExpectedOutput());
                row.put("actual", actual);
                row.put("pass", pass);
                caseResults.add(row);
            }
        } finally {
            program.close();
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("language", normalizedLanguage);
        result.put("passed", passCount == cases.size());
        result.put("passCount", passCount);
        result.put("total", cases.size());
        result.put("costMs", System.currentTimeMillis() - start);
        result.put("cases", caseResults);
        return result;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return "java";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private void validateSourceCode(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new BusinessException("代码不能为空");
        }
    }

    private CodeRunner requireRunner(String language) {
        CodeRunner runner = runnerMap.get(language);
        if (runner == null) {
            throw new BusinessException("当前未接入所选语言执行器: " + language);
        }
        return runner;
    }

    private List<Integer> parseArgs(JSONArray array) {
        List<Integer> args = new ArrayList<Integer>();
        for (int i = 0; i < array.size(); i++) {
            Object val = array.get(i);
            if (!(val instanceof Number)) {
                throw new BusinessException("当前仅支持整数参数测试用例");
            }
            args.add(((Number) val).intValue());
        }
        return args;
    }
}
