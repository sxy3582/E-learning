package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.ChapterSaveRequest;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.QuestionOption;
import com.elearning.backend.entity.UserAnswer;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.CourseMapper;
import com.elearning.backend.mapper.LearningProgressMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.QuestionOptionMapper;
import com.elearning.backend.mapper.UserAnswerMapper;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chapter")
public class ChapterController {

    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper questionOptionMapper;
    @Resource
    private UserAnswerMapper userAnswerMapper;
    @Resource
    private LearningProgressMapper learningProgressMapper;
    @Resource
    private CourseMapper courseMapper;

    @GetMapping("/list")
    public ApiResponse<List<Chapter>> list(@RequestParam Long courseId) {
        AuthUtil.checkLogin();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!StpUtil.hasRole("ADMIN") && !Integer.valueOf(1).equals(course.getPublished())) {
            throw new BusinessException("课程暂未发布");
        }
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, courseId)
                .orderByAsc(Chapter::getSortOrder)
                .orderByAsc(Chapter::getId));
        return ApiResponse.success(chapters);
    }

    @GetMapping("/{id}")
    public ApiResponse<Chapter> detail(@PathVariable Long id) {
        AuthUtil.checkLogin();
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        Course course = courseMapper.selectById(chapter.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!StpUtil.hasRole("ADMIN") && !Integer.valueOf(1).equals(course.getPublished())) {
            throw new BusinessException("课程暂未发布");
        }
        return ApiResponse.success(chapter);
    }

    @PostMapping("/admin/save")
    public ApiResponse<Chapter> save(@RequestBody @Validated ChapterSaveRequest request) {
        AuthUtil.checkAdmin();
        LocalDateTime now = LocalDateTime.now();
        Chapter chapter;
        if (request.getId() == null) {
            chapter = new Chapter();
            BeanUtils.copyProperties(request, chapter);
            chapter.setCreateTime(now);
            chapter.setUpdateTime(now);
            chapterMapper.insert(chapter);
        } else {
            chapter = chapterMapper.selectById(request.getId());
            if (chapter == null) {
                throw new BusinessException("章节不存在");
            }
            BeanUtils.copyProperties(request, chapter);
            chapter.setUpdateTime(now);
            chapterMapper.updateById(chapter);
        }
        return ApiResponse.success(chapter);
    }

    @DeleteMapping("/admin/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUtil.checkAdmin();
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getChapterId, id));
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        if (!questionIds.isEmpty()) {
            questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, questionIds));
            userAnswerMapper.delete(new LambdaQueryWrapper<UserAnswer>()
                    .in(UserAnswer::getQuestionId, questionIds));
            questionMapper.delete(new LambdaQueryWrapper<Question>()
                    .in(Question::getId, questionIds));
        }
        learningProgressMapper.delete(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getChapterId, id));
        chapterMapper.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
