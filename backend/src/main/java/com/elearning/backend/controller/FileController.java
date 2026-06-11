package com.elearning.backend.controller;

import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        AuthUtil.checkAdmin();
        return ApiResponse.success(storeFile(file, false));
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        AuthUtil.checkLogin();
        return ApiResponse.success(storeFile(file, true));
    }

    private Map<String, Object> storeFile(MultipartFile file, boolean imageOnly) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > (imageOnly ? 5L * 1024 * 1024 : MAX_FILE_SIZE)) {
            throw new BusinessException(imageOnly ? "头像不能超过5MB" : "文件不能超过200MB");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (imageOnly ? !isImageExtension(extension) : !isAllowedExtension(extension)) {
            throw new BusinessException(imageOnly ? "头像仅支持 jpg、jpeg、png、gif、webp" : "暂不支持该文件类型");
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDir = resolveUploadRoot().resolve(datePath);
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }
        Path finalFile = targetFile;
        String note = null;
        if (!imageOnly && isVideoExtension(extension)) {
            try {
                finalFile = transcodeVideoToMp4(targetFile);
                note = "已自动转码为 mp4(h264)，用户端兼容性更高";
                if (!finalFile.equals(targetFile)) {
                    Files.deleteIfExists(targetFile);
                }
            } catch (BusinessException e) {
                note = e.getMessage() + "，已保留原视频文件";
            } catch (IOException e) {
                note = "自动转码失败，已保留原视频文件";
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("originalName", originalName);
        result.put("fileName", finalFile.getFileName().toString());
        result.put("url", "/uploads/" + datePath + "/" + finalFile.getFileName().toString());
        result.put("size", file.getSize());
        if (note != null) {
            result.put("note", note);
        }
        return result;
    }

    private Path resolveUploadRoot() {
        Path workDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (workDir.getFileName() != null && "backend".equalsIgnoreCase(workDir.getFileName().toString())) {
            return workDir.getParent().resolve(uploadDir).normalize();
        }
        return workDir.resolve(uploadDir).normalize();
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".gif".equals(extension)
                || ".webp".equals(extension)
                || ".mp4".equals(extension)
                || ".webm".equals(extension)
                || ".mov".equals(extension)
                || ".m4v".equals(extension)
                || ".ogg".equals(extension)
                || ".avi".equals(extension)
                || ".mkv".equals(extension)
                || ".wmv".equals(extension)
                || ".flv".equals(extension)
                || ".pdf".equals(extension)
                || ".doc".equals(extension)
                || ".docx".equals(extension)
                || ".ppt".equals(extension)
                || ".pptx".equals(extension)
                || ".zip".equals(extension);
    }

    private boolean isImageExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".gif".equals(extension)
                || ".webp".equals(extension);
    }

    private boolean isVideoExtension(String extension) {
        return ".mp4".equals(extension)
                || ".webm".equals(extension)
                || ".mov".equals(extension)
                || ".m4v".equals(extension)
                || ".ogg".equals(extension)
                || ".avi".equals(extension)
                || ".mkv".equals(extension)
                || ".wmv".equals(extension)
                || ".flv".equals(extension);
    }

    private Path transcodeVideoToMp4(Path sourceFile) {
        String sourceName = sourceFile.getFileName().toString();
        String baseName = sourceName.contains(".")
                ? sourceName.substring(0, sourceName.lastIndexOf('.'))
                : sourceName;
        Path outputFile = sourceFile.getParent().resolve(baseName + "_h264.mp4");
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", sourceFile.toAbsolutePath().toString(),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-c:a", "aac",
                "-movflags", "+faststart",
                outputFile.toAbsolutePath().toString()
        );
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            consumeStream(process.getInputStream());
            boolean finished = process.waitFor(8, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("视频转码超时");
            }
            if (process.exitValue() != 0 || !Files.exists(outputFile)) {
                throw new BusinessException("自动转码不可用（请检查本机是否安装 ffmpeg）");
            }
            return outputFile;
        } catch (IOException e) {
            throw new BusinessException("自动转码不可用（请检查本机是否安装 ffmpeg）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("视频转码被中断");
        }
    }

    private void consumeStream(final InputStream inputStream) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                try {
                    while (inputStream.read(buffer) != -1) {
                        // drain ffmpeg output
                    }
                } catch (IOException e) {
                    // ignore
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
