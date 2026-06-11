package com.elearning.backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionBatchSubmitRequest {
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    private List<AnswerItem> answers = new ArrayList<AnswerItem>();

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        @NotBlank(message = "答案不能为空")
        private String answerContent;
    }
}
