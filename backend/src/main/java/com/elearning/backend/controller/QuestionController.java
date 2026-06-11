package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.QuestionBatchSubmitRequest;
import com.elearning.backend.dto.QuestionSaveRequest;
import com.elearning.backend.dto.QuestionVO;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.AppComment;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.QuestionOption;
import com.elearning.backend.entity.UserAnswer;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.AppCommentMapper;
import com.elearning.backend.mapper.LearningProgressMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.QuestionOptionMapper;
import com.elearning.backend.mapper.UserAnswerMapper;
import com.elearning.backend.service.WrongBookService;
import com.elearning.backend.util.AuthUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper questionOptionMapper;
    @Resource
    private UserAnswerMapper userAnswerMapper;
    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private LearningProgressMapper learningProgressMapper;
    @Resource
    private AppCommentMapper appCommentMapper;
    @Resource
    private WrongBookService wrongBookService;

    @GetMapping("/list")
    public ApiResponse<List<QuestionVO>> list(@RequestParam Long chapterId) {
        AuthUtil.checkLogin();
        List<QuestionVO> list = buildQuestionList(chapterId, false, StpUtil.getLoginIdAsLong());
        return ApiResponse.success(list);
    }

    @GetMapping("/admin/list")
    public ApiResponse<List<QuestionVO>> adminList(@RequestParam Long chapterId) {
        AuthUtil.checkAdmin();
        List<QuestionVO> list = buildQuestionList(chapterId, true, null);
        return ApiResponse.success(list);
    }

    @PostMapping("/submit-batch")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> submitBatch(@RequestBody @Validated QuestionBatchSubmitRequest request) {
        AuthUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new BusinessException("请完整填写后再提交");
        }

        Chapter chapter = chapterMapper.selectById(request.getChapterId());
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }

        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getChapterId, request.getChapterId())
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId));
        if (questions.isEmpty()) {
            throw new BusinessException("当前章节还没有题目");
        }

        Map<Long, Question> questionMap = new HashMap<Long, Question>();
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
        }
        if (request.getAnswers().size() != questions.size()) {
            throw new BusinessException("请确保所有题目都已作答");
        }

        List<UserAnswer> exists = userAnswerMapper.selectList(new LambdaQueryWrapper<UserAnswer>()
                .eq(UserAnswer::getUserId, userId)
                .in(UserAnswer::getQuestionId, questionMap.keySet()));
        if (!exists.isEmpty()) {
            throw new BusinessException("本章节练习已提交，答案不可修改");
        }

        Map<Long, QuestionBatchSubmitRequest.AnswerItem> answerMap = new HashMap<Long, QuestionBatchSubmitRequest.AnswerItem>();
        for (QuestionBatchSubmitRequest.AnswerItem item : request.getAnswers()) {
            answerMap.put(item.getQuestionId(), item);
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> singleQuestionIds = new HashSet<Long>();
        for (Question question : questions) {
            if ("SINGLE".equalsIgnoreCase(question.getType())) {
                singleQuestionIds.add(question.getId());
            }
        }
        Map<Long, String> singleReferenceMap = new HashMap<Long, String>();
        if (!singleQuestionIds.isEmpty()) {
            List<QuestionOption> correctOptions = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, singleQuestionIds)
                    .eq(QuestionOption::getIsCorrect, 1));
            for (QuestionOption option : correctOptions) {
                singleReferenceMap.put(option.getQuestionId(), option.getOptionKey());
            }
        }

        int correctCount = 0;
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Question question : questions) {
            QuestionBatchSubmitRequest.AnswerItem item = answerMap.get(question.getId());
            if (item == null || item.getAnswerContent() == null || item.getAnswerContent().trim().isEmpty()) {
                throw new BusinessException("请确保所有题目都已作答");
            }
            String userAnswer = item.getAnswerContent().trim();
            String reference = question.getReferenceAnswer() == null ? "" : question.getReferenceAnswer().trim();
            if ("SINGLE".equalsIgnoreCase(question.getType()) && singleReferenceMap.containsKey(question.getId())) {
                reference = singleReferenceMap.get(question.getId());
            }
            if ("JUDGE".equalsIgnoreCase(question.getType())) {
                reference = normalizeJudge(reference);
            }
            Integer isCorrect = normalize(userAnswer).equals(normalize(reference)) ? 1 : 0;
            if (isCorrect == 1) {
                correctCount++;
            }
            UserAnswer answer = new UserAnswer();
            answer.setUserId(userId);
            answer.setQuestionId(question.getId());
            answer.setAnswerContent(userAnswer);
            answer.setIsCorrect(isCorrect);
            answer.setCreateTime(now);
            answer.setUpdateTime(now);
            userAnswerMapper.insert(answer);
            if (isCorrect == 0) {
                wrongBookService.addQuestionWrong(userId, question.getId());
            } else {
                wrongBookService.removeQuestionWrong(userId, question.getId());
            }

            Map<String, Object> row = new HashMap<String, Object>();
            row.put("questionId", question.getId());
            row.put("correct", isCorrect);
            row.put("analysis", question.getAnalysis());
            row.put("referenceAnswer", reference);
            row.put("message", isCorrect == 1 ? "回答正确" : "回答错误");
            results.add(row);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("score", correctCount);
        result.put("total", questions.size());
        result.put("submitted", 1);
        result.put("results", results);
        result.put("wrongBookCount", wrongBookService.countByUser(userId));

        upsertProgress(userId, chapter.getCourseId(), chapter.getId(), correctCount, questions.size());
        return ApiResponse.success(result);
    }

    @PostMapping("/admin/save")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> save(@RequestBody @Validated QuestionSaveRequest request) {
        AuthUtil.checkAdmin();
        validateQuestionRequest(request);

        LocalDateTime now = LocalDateTime.now();
        String type = request.getType() == null ? "" : request.getType().toUpperCase(Locale.ROOT);
        String normalizedReference = normalizeReferenceAnswer(request, type);
        Question question;
        if (request.getId() == null) {
            question = new Question();
            BeanUtils.copyProperties(request, question);
            question.setType(type);
            question.setReferenceAnswer(normalizedReference);
            question.setCreateTime(now);
            question.setUpdateTime(now);
            questionMapper.insert(question);
        } else {
            question = questionMapper.selectById(request.getId());
            if (question == null) {
                throw new BusinessException("题目不存在");
            }
            BeanUtils.copyProperties(request, question);
            question.setType(type);
            question.setReferenceAnswer(normalizedReference);
            question.setUpdateTime(now);
            questionMapper.updateById(question);
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId()));
        }

        if ("SINGLE".equalsIgnoreCase(question.getType())) {
            for (QuestionSaveRequest.OptionItem item : request.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setOptionKey(item.getOptionKey());
                option.setOptionContent(item.getOptionContent());
                option.setIsCorrect(item.getIsCorrect());
                option.setSortOrder(item.getSortOrder());
                option.setCreateTime(now);
                option.setUpdateTime(now);
                questionOptionMapper.insert(option);
            }
        }
        return ApiResponse.success("保存成功", null);
    }

    @DeleteMapping("/admin/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUtil.checkAdmin();
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        questionMapper.deleteById(id);
        appCommentMapper.delete(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getTargetType, "QUESTION")
                .eq(AppComment::getTargetId, id));
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, id));
        return ApiResponse.success("删除成功", null);
    }

    private void validateQuestionRequest(QuestionSaveRequest request) {
        String type = request.getType() == null ? "" : request.getType().toUpperCase(Locale.ROOT);
        if (!"SINGLE".equals(type) && !"JUDGE".equals(type)) {
            throw new BusinessException("题型仅支持 SINGLE/JUDGE");
        }
        if ("SINGLE".equals(type)) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BusinessException("单选题至少需要两个选项");
            }
            int correctCount = 0;
            for (QuestionSaveRequest.OptionItem item : request.getOptions()) {
                if (item.getIsCorrect() != null && item.getIsCorrect() == 1) {
                    correctCount++;
                }
            }
            if (correctCount != 1) {
                throw new BusinessException("单选题必须且仅有一个正确选项");
            }
            return;
        }
        String judgeAnswer = normalizeJudge(request.getReferenceAnswer());
        if (!"TRUE".equals(judgeAnswer) && !"FALSE".equals(judgeAnswer)) {
            throw new BusinessException("判断题答案只能是 正确 或 错误");
        }
    }

    private List<QuestionVO> buildQuestionList(Long chapterId, boolean includeAnswer, Long userId) {
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getChapterId, chapterId)
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId));
        Map<Long, UserAnswer> userAnswerMap = new HashMap<Long, UserAnswer>();
        if (userId != null && !questions.isEmpty()) {
            List<Long> questionIds = new ArrayList<Long>();
            for (Question question : questions) {
                questionIds.add(question.getId());
            }
            List<UserAnswer> answers = userAnswerMapper.selectList(new LambdaQueryWrapper<UserAnswer>()
                    .eq(UserAnswer::getUserId, userId)
                    .in(UserAnswer::getQuestionId, questionIds));
            for (UserAnswer answer : answers) {
                userAnswerMap.put(answer.getQuestionId(), answer);
            }
        }

        List<QuestionVO> list = new ArrayList<QuestionVO>();
        for (Question question : questions) {
            QuestionVO vo = new QuestionVO();
            BeanUtils.copyProperties(question, vo);
            if (!includeAnswer) {
                vo.setReferenceAnswer(null);
            }

            List<QuestionOption> options = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId())
                    .orderByAsc(QuestionOption::getSortOrder)
                    .orderByAsc(QuestionOption::getId));
            List<QuestionVO.OptionVO> optionVOS = new ArrayList<QuestionVO.OptionVO>();
            for (QuestionOption option : options) {
                QuestionVO.OptionVO optionVO = new QuestionVO.OptionVO();
                BeanUtils.copyProperties(option, optionVO);
                if (!includeAnswer) {
                    optionVO.setIsCorrect(null);
                }
                optionVOS.add(optionVO);
            }
            vo.setOptions(optionVOS);
            UserAnswer myAnswer = userAnswerMap.get(question.getId());
            if (myAnswer != null) {
                vo.setMyAnswer(myAnswer.getAnswerContent());
                vo.setMyCorrect(myAnswer.getIsCorrect());
                vo.setSubmitted(1);
                if (!includeAnswer) {
                    vo.setReferenceAnswer(question.getReferenceAnswer());
                }
            } else {
                vo.setSubmitted(0);
            }
            list.add(vo);
        }
        return list;
    }

    private void upsertProgress(Long userId, Long courseId, Long chapterId, Integer score, Integer total) {
        LocalDateTime now = LocalDateTime.now();
        LearningProgress progress = learningProgressMapper.selectOne(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, userId)
                .eq(LearningProgress::getChapterId, chapterId));
        if (progress == null) {
            progress = new LearningProgress();
            progress.setUserId(userId);
            progress.setCourseId(courseId);
            progress.setChapterId(chapterId);
            progress.setChapterStudied(1);
            progress.setPracticeCompleted(1);
            progress.setPracticeScore(score);
            progress.setPracticeTotal(total);
            progress.setLastStudyTime(now);
            progress.setCreateTime(now);
            progress.setUpdateTime(now);
            learningProgressMapper.insert(progress);
            return;
        }
        progress.setChapterStudied(1);
        progress.setPracticeCompleted(1);
        progress.setPracticeScore(score);
        progress.setPracticeTotal(total);
        progress.setLastStudyTime(now);
        progress.setUpdateTime(now);
        learningProgressMapper.updateById(progress);
    }

    private String normalize(String source) {
        return source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeReferenceAnswer(QuestionSaveRequest request, String type) {
        if ("SINGLE".equals(type)) {
            if (request.getOptions() == null) {
                return "";
            }
            for (QuestionSaveRequest.OptionItem option : request.getOptions()) {
                if (option.getIsCorrect() != null && option.getIsCorrect() == 1) {
                    return option.getOptionKey();
                }
            }
            return "";
        }
        return normalizeJudge(request.getReferenceAnswer());
    }

    private String normalizeJudge(String source) {
        String value = normalize(source);
        if ("正确".equals(value) || "对".equals(value) || "T".equals(value)) {
            return "TRUE";
        }
        if ("错误".equals(value) || "错".equals(value) || "F".equals(value)) {
            return "FALSE";
        }
        return value;
    }
}
