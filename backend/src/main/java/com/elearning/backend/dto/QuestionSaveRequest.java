package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionSaveRequest {
    private Long id;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotBlank(message = "题目类型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    private String analysis;
    private String referenceAnswer;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;

    private List<OptionItem> options = new ArrayList<OptionItem>();

    @Data
    public static class OptionItem {
        private Long id;

        @NotBlank(message = "选项标识不能为空")
        @Size(max = 8, message = "选项标识长度不能超过8")
        private String optionKey;

        @NotBlank(message = "选项内容不能为空")
        private String optionContent;

        @NotNull(message = "是否正确不能为空")
        private Integer isCorrect;

        @NotNull(message = "选项排序不能为空")
        private Integer sortOrder;
    }
}
