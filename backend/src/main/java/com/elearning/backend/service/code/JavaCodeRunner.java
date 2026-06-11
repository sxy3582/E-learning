package com.elearning.backend.service.code;

import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.CodeProblem;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaCodeRunner extends AbstractProcessCodeRunner {

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public PreparedCodeProgram prepare(CodeProblem problem, String sourceCode, int argCount) {
        String methodName = normalizeMethodName(problem.getMethodName());
        Path workingDirectory = createWorkingDirectory(getLanguage());
        writeUtf8File(workingDirectory.resolve("Solution.java"), sourceCode);
        writeUtf8File(workingDirectory.resolve("JudgeMain.java"), buildJudgeMainSource(methodName));
        List<String> compileCommand = new ArrayList<String>();
        compileCommand.add("javac");
        compileCommand.add("-encoding");
        compileCommand.add("UTF-8");
        compileCommand.add("Solution.java");
        compileCommand.add("JudgeMain.java");
        compile(compileCommand, workingDirectory, "未检测到 javac，请确认 JDK 已正确安装并加入 PATH", "Java 编译失败：");
        return new JavaPreparedProgram(workingDirectory);
    }

    private String buildJudgeMainSource(String methodName) {
        return "import java.lang.reflect.Method;\n"
                + "public class JudgeMain {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        Solution solution = new Solution();\n"
                + "        Method target = null;\n"
                + "        for (Method method : Solution.class.getMethods()) {\n"
                + "            if (!method.getName().equals(\"" + methodName + "\")) {\n"
                + "                continue;\n"
                + "            }\n"
                + "            if (method.getParameterCount() != args.length) {\n"
                + "                continue;\n"
                + "            }\n"
                + "            boolean match = true;\n"
                + "            for (Class<?> paramType : method.getParameterTypes()) {\n"
                + "                if (!int.class.equals(paramType)) {\n"
                + "                    match = false;\n"
                + "                    break;\n"
                + "                }\n"
                + "            }\n"
                + "            if (match && int.class.equals(method.getReturnType())) {\n"
                + "                target = method;\n"
                + "                break;\n"
                + "            }\n"
                + "        }\n"
                + "        if (target == null) {\n"
                + "            throw new RuntimeException(\"未找到匹配方法: " + methodName + "(int...) -> int\");\n"
                + "        }\n"
                + "        Object[] invokeArgs = new Object[args.length];\n"
                + "        for (int i = 0; i < args.length; i++) {\n"
                + "            invokeArgs[i] = Integer.parseInt(args[i]);\n"
                + "        }\n"
                + "        Object result = target.invoke(solution, invokeArgs);\n"
                + "        System.out.print(result);\n"
                + "    }\n"
                + "}\n";
    }

    private String normalizeMethodName(String methodName) {
        if (methodName == null || !methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BusinessException("题目方法名配置无效");
        }
        return methodName;
    }

    private class JavaPreparedProgram implements PreparedCodeProgram {
        private final Path workingDirectory;

        private JavaPreparedProgram(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public int run(List<Integer> args) {
            List<String> command = new ArrayList<String>();
            command.add("java");
            command.add("-cp");
            command.add(workingDirectory.toAbsolutePath().toString());
            command.add("JudgeMain");
            for (Integer arg : args) {
                command.add(String.valueOf(arg));
            }
            CommandResult result = runCommand(command, workingDirectory, "未检测到 java，请确认 JDK/JRE 已正确安装并加入 PATH");
            return parseIntOutput(result);
        }

        @Override
        public void close() {
            deleteDirectory(workingDirectory);
        }
    }
}
