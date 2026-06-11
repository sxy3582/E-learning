package com.elearning.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionVO {
    private Long id;
    private Long chapterId;
    private String type;
    private String stem;
    private String analysis;
    private String referenceAnswer;
    private Integer sortOrder;
    private String myAnswer;
    private Integer myCorrect;
    private Integer submitted;
    private List<OptionVO> options = new ArrayList<OptionVO>();

    @Data
    public static class OptionVO {
        private Long id;
        private String optionKey;
        private String optionContent;
        private Integer isCorrect;
        private Integer sortOrder;
    }
}
