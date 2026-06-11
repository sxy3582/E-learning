package com.elearning.backend.service.code;

import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.CodeProblem;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PythonCodeRunner extends AbstractProcessCodeRunner {

    @Override
    public String getLanguage() {
        return "python";
    }

    @Override
    public PreparedCodeProgram prepare(CodeProblem problem, String sourceCode, int argCount) {
        String methodName = normalizeMethodName(problem.getMethodName());
        Path workingDirectory = createWorkingDirectory(getLanguage());
        writeUtf8File(workingDirectory.resolve("solution.py"), sourceCode);
        writeUtf8File(workingDirectory.resolve("judge_main.py"), buildJudgeMainSource(methodName));
        List<String> compileCommand = new ArrayList<String>();
        compileCommand.add("python");
        compileCommand.add("-m");
        compileCommand.add("py_compile");
        compileCommand.add("solution.py");
        compile(compileCommand, workingDirectory, "未检测到 python，请确认 Python 已正确安装并加入 PATH", "Python 编译失败：");
        return new PythonPreparedProgram(workingDirectory);
    }

    private String buildJudgeMainSource(String methodName) {
        return "from solution import Solution\n"
                + "import sys\n\n"
                + "solution = Solution()\n"
                + "target = getattr(solution, '" + methodName + "', None)\n"
                + "if target is None:\n"
                + "    raise RuntimeError('未找到匹配方法: " + methodName + "(int...) -> int')\n"
                + "args = [int(item) for item in sys.argv[1:]]\n"
                + "result = target(*args)\n"
                + "if not isinstance(result, int):\n"
                + "    raise RuntimeError('返回值必须是 int')\n"
                + "print(result, end='')\n";
    }

    private String normalizeMethodName(String methodName) {
        if (methodName == null || !methodName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new BusinessException("题目方法名配置无效");
        }
        return methodName;
    }

    private class PythonPreparedProgram implements PreparedCodeProgram {
        private final Path workingDirectory;

        private PythonPreparedProgram(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public int run(List<Integer> args) {
            List<String> command = new ArrayList<String>();
            command.add("python");
            command.add("judge_main.py");
            for (Integer arg : args) {
                command.add(String.valueOf(arg));
            }
            CommandResult result = runCommand(command, workingDirectory, "未检测到 python，请确认 Python 已正确安装并加入 PATH");
            return parseIntOutput(result);
        }

        @Override
        public void close() {
            deleteDirectory(workingDirectory);
        }
    }
}
