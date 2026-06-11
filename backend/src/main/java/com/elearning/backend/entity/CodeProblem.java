package com.elearning.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("code_problem")
public class CodeProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String supportedLanguages;
    private String methodName;
    private String templateCode;
    private String templateCodeJava;
    private String templateCodePython;
    private String templateCodeCpp;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
