package com.elearning.backend.service.code;

import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.CodeProblem;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class CppCodeRunner extends AbstractProcessCodeRunner {

    @Override
    public String getLanguage() {
        return "cpp";
    }

    @Override
    public PreparedCodeProgram prepare(CodeProblem problem, String sourceCode, int argCount) {
        String methodName = normalizeMethodName(problem.getMethodName());
        Path workingDirectory = createWorkingDirectory(getLanguage());
        writeUtf8File(workingDirectory.resolve("solution.cpp"), sourceCode);
        writeUtf8File(workingDirectory.resolve("judge_main.cpp"), buildJudgeMainSource(methodName, argCount));
        List<String> compileCommand = new ArrayList<String>();
        compileCommand.add("g++");
        compileCommand.add("-std=c++14");
        compileCommand.add("-O2");
        compileCommand.add("judge_main.cpp");
        compileCommand.add("-o");
        compileCommand.add("judge_main.exe");
        compile(compileCommand, workingDirectory, "未检测到 g++，请先安装 MinGW-w64 或其他 C++ 编译器并加入 PATH", "C++ 编译失败：");
        return new CppPreparedProgram(workingDirectory);
    }

    private String buildJudgeMainSource(String methodName, int argCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("#include <iostream>\n");
        builder.append("#include <string>\n");
        builder.append("#include \"solution.cpp\"\n\n");
        builder.append("int main(int argc, char* argv[]) {\n");
        builder.append("    if (argc - 1 != ").append(argCount).append(") {\n");
        builder.append("        std::cerr << \"参数数量不匹配\";\n");
        builder.append("        return 1;\n");
        builder.append("    }\n");
        builder.append("    Solution solution;\n");
        builder.append("    int result = solution.").append(methodName).append("(");
        for (int i = 0; i < argCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("std::stoi(argv[").append(i + 1).append("])");
        }
        builder.append(");\n");
        builder.append("    std::cout << result;\n");
        builder.append("    return 0;\n");
        builder.append("}\n");
        return builder.toString();
    }

    private String normalizeMethodName(String methodName) {
        if (methodName == null || !methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BusinessException("题目方法名配置无效");
        }
        return methodName;
    }

    private class CppPreparedProgram implements PreparedCodeProgram {
        private final Path workingDirectory;

        private CppPreparedProgram(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public int run(List<Integer> args) {
            List<String> command = new ArrayList<String>();
            command.add(workingDirectory.resolve("judge_main.exe").toAbsolutePath().toString());
            for (Integer arg : args) {
                command.add(String.valueOf(arg));
            }
            CommandResult result = runCommand(command, workingDirectory, "未检测到 g++ 产物，C++ 可执行文件生成失败");
            return parseIntOutput(result);
        }

        @Override
        public void close() {
            deleteDirectory(workingDirectory);
        }
    }
}
