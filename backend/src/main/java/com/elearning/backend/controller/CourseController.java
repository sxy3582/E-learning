package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.CourseSaveRequest;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.AppComment;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.QuestionOption;
import com.elearning.backend.entity.UserAnswer;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.AppCommentMapper;
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
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Resource
    private CourseMapper courseMapper;
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
    private AppCommentMapper appCommentMapper;

    @GetMapping("/list")
    public ApiResponse<List<Course>> list() {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getPublished, 1)
                .orderByAsc(Course::getSortOrder)
                .orderByDesc(Course::getId));
        return ApiResponse.success(courses);
    }

    @GetMapping("/admin/list")
    public ApiResponse<List<Course>> adminList() {
        AuthUtil.checkAdmin();
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByAsc(Course::getSortOrder)
                .orderByDesc(Course::getId));
        return ApiResponse.success(courses);
    }

    @GetMapping("/{id}")
    public ApiResponse<Course> detail(@PathVariable Long id) {
        AuthUtil.checkLogin();
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!StpUtil.hasRole("ADMIN") && !Integer.valueOf(1).equals(course.getPublished())) {
            throw new BusinessException("课程暂未发布");
        }
        return ApiResponse.success(course);
    }

    @PostMapping("/admin/save")
    public ApiResponse<Course> save(@RequestBody @Validated CourseSaveRequest request) {
        AuthUtil.checkAdmin();
        LocalDateTime now = LocalDateTime.now();
        Course course;
        if (request.getId() == null) {
            course = new Course();
            BeanUtils.copyProperties(request, course);
            course.setCreateTime(now);
            course.setUpdateTime(now);
            courseMapper.insert(course);
        } else {
            course = courseMapper.selectById(request.getId());
            if (course == null) {
                throw new BusinessException("课程不存在");
            }
            BeanUtils.copyProperties(request, course);
            course.setUpdateTime(now);
            courseMapper.updateById(course);
        }
        return ApiResponse.success(course);
    }

    @DeleteMapping("/admin/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUtil.checkAdmin();
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, id));
        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
        if (!chapterIds.isEmpty()) {
            List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .in(Question::getChapterId, chapterIds));
            List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
            if (!questionIds.isEmpty()) {
                appCommentMapper.delete(new LambdaQueryWrapper<AppComment>()
                        .eq(AppComment::getTargetType, "QUESTION")
                        .in(AppComment::getTargetId, questionIds));
                questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds));
                userAnswerMapper.delete(new LambdaQueryWrapper<UserAnswer>()
                        .in(UserAnswer::getQuestionId, questionIds));
                questionMapper.delete(new LambdaQueryWrapper<Question>()
                        .in(Question::getId, questionIds));
            }
            learningProgressMapper.delete(new LambdaQueryWrapper<LearningProgress>()
                    .in(LearningProgress::getChapterId, chapterIds));
            chapterMapper.delete(new LambdaQueryWrapper<Chapter>()
                    .in(Chapter::getId, chapterIds));
        }
        appCommentMapper.delete(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getTargetType, "COURSE")
                .eq(AppComment::getTargetId, id));
        courseMapper.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
