package com.elearning.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("code_submission")
public class CodeSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long problemId;
    private String language;
    private Integer passed;
    private Integer passCount;
    private Integer totalCount;
    private Integer costMs;
    private LocalDateTime createTime;
}
