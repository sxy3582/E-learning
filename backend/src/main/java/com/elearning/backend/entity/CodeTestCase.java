package com.elearning.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("code_test_case")
public class CodeTestCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private String inputJson;
    private String expectedOutput;
    private Integer isSample;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
