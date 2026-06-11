package com.elearning.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_progress")
public class LearningProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Integer chapterStudied;
    private Integer practiceCompleted;
    private Integer practiceScore;
    private Integer practiceTotal;
    private LocalDateTime lastStudyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
