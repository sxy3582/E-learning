package com.elearning.backend.service.code;

import com.elearning.backend.common.exception.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class AbstractProcessCodeRunner implements CodeRunner {
    private static final long COMPILE_TIMEOUT_MS = 10000L;
    private static final long RUN_TIMEOUT_MS = 2000L;

    protected Path createWorkingDirectory(String language) {
        try {
            Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "elearning-judge", language + "-" + UUID.randomUUID().toString());
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new BusinessException("创建临时代码目录失败");
        }
    }

    protected void writeUtf8File(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("写入代码文件失败: " + file.getFileName());
        }
    }

    protected CommandResult compile(List<String> command, Path workingDirectory, String missingMessage, String failPrefix) {
        CommandResult result = execute(command, workingDirectory, COMPILE_TIMEOUT_MS, missingMessage, "编译超时");
        if (result.getExitCode() != 0) {
            throw new BusinessException(failPrefix + "\n" + pickMessage(result));
        }
        return result;
    }

    protected CommandResult runCommand(List<String> command, Path workingDirectory, String missingMessage) {
        CommandResult result = execute(command, workingDirectory, RUN_TIMEOUT_MS, missingMessage, "执行超时");
        if (result.getExitCode() != 0) {
            throw new BusinessException("执行失败：\n" + pickMessage(result));
        }
        return result;
    }

    protected String pickMessage(CommandResult result) {
        String stderr = safeTrim(result.getStderr());
        if (!stderr.isEmpty()) {
            return stderr;
        }
        String stdout = safeTrim(result.getStdout());
        if (!stdout.isEmpty()) {
            return stdout;
        }
        return "没有可用的错误输出";
    }

    protected String requireTextOutput(CommandResult result) {
        String text = safeTrim(result.getStdout());
        if (text.isEmpty()) {
            String message = safeTrim(result.getStderr());
            if (message.isEmpty()) {
                message = "程序没有输出结果";
            }
            throw new BusinessException(message);
        }
        return text;
    }

    protected int parseIntOutput(CommandResult result) {
        String text = requireTextOutput(result);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new BusinessException("程序输出不是整数：\n" + text);
        }
    }

    protected void deleteDirectory(Path workingDirectory) {
        if (workingDirectory == null || !Files.exists(workingDirectory)) {
            return;
        }
        try {
            Files.walk(workingDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private CommandResult execute(List<String> command, Path workingDirectory, long timeoutMs, String missingMessage, String timeoutMessage) {
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(new ArrayList<String>(command));
            builder.directory(workingDirectory.toFile());
            process = builder.start();
        } catch (IOException e) {
            throw new BusinessException(missingMessage);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = executor.submit(new StreamCollector(process.getInputStream()));
        Future<String> stderrFuture = executor.submit(new StreamCollector(process.getErrorStream()));
        try {
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(timeoutMessage);
            }
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            return new CommandResult(process.exitValue(), stdout, stderr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BusinessException("执行被中断");
        } catch (ExecutionException e) {
            process.destroyForcibly();
            throw new BusinessException("读取执行输出失败");
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new BusinessException("读取执行输出超时");
        } finally {
            executor.shutdownNow();
        }
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private static class StreamCollector implements Callable<String> {
        private final InputStream inputStream;

        private StreamCollector(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public String call() throws Exception {
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    builder.append('\n');
                }
                builder.append(line);
                first = false;
            }
            return builder.toString();
        }
    }

    protected static class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }
}
