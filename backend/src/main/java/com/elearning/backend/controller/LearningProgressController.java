package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.CodeSubmission;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.SysUser;
import com.elearning.backend.entity.WrongBookEntry;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.CodeProblemMapper;
import com.elearning.backend.mapper.CodeSubmissionMapper;
import com.elearning.backend.mapper.CourseMapper;
import com.elearning.backend.mapper.LearningProgressMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.SysUserMapper;
import com.elearning.backend.mapper.WrongBookEntryMapper;
import com.elearning.backend.service.WrongBookService;
import com.elearning.backend.util.AuthUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/progress")
public class LearningProgressController {

    @Resource
    private LearningProgressMapper learningProgressMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;
    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private WrongBookEntryMapper wrongBookEntryMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private CodeProblemMapper codeProblemMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private WrongBookService wrongBookService;

    @PostMapping("/study")
    public ApiResponse<Void> markStudy(@RequestParam Long courseId, @RequestParam Long chapterId) {
        AuthUtil.checkLogin();
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        if (!chapter.getCourseId().equals(courseId)) {
            throw new BusinessException("章节不属于当前课程");
        }
        upsertProgress(StpUtil.getLoginIdAsLong(), courseId, chapterId, null, null);
        return ApiResponse.success("记录成功", null);
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myProgress() {
        AuthUtil.checkLogin();
        wrongBookService.backfillIfEmpty();
        return ApiResponse.success(buildUserProgress(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/admin/list")
    public ApiResponse<List<Map<String, Object>>> adminList() {
        AuthUtil.checkAdmin();
        wrongBookService.backfillIfEmpty();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "USER")
                .orderByDesc(SysUser::getId));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SysUser user : users) {
            Map<String, Object> row = buildUserProgress(user.getId());
            row.put("userId", user.getId());
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            result.add(row);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/course")
    public ApiResponse<List<LearningProgress>> courseProgress(@RequestParam Long courseId) {
        AuthUtil.checkLogin();
        List<LearningProgress> list = learningProgressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, StpUtil.getLoginIdAsLong())
                .eq(LearningProgress::getCourseId, courseId)
                .orderByAsc(LearningProgress::getChapterId));
        return ApiResponse.success(list);
    }

    @DeleteMapping("/wrong-book/{id}")
    public ApiResponse<Void> removeWrongBook(@PathVariable Long id) {
        AuthUtil.checkLogin();
        WrongBookEntry entry = wrongBookEntryMapper.selectById(id);
        if (entry == null || !entry.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("错题记录不存在");
        }
        wrongBookEntryMapper.deleteById(id);
        return ApiResponse.success("已移出错题单", null);
    }

    public void upsertProgress(Long userId, Long courseId, Long chapterId, Integer practiceScore, Integer practiceTotal) {
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
            progress.setPracticeCompleted(practiceScore == null ? 0 : 1);
            progress.setPracticeScore(practiceScore == null ? 0 : practiceScore);
            progress.setPracticeTotal(practiceTotal == null ? 0 : practiceTotal);
            progress.setLastStudyTime(now);
            progress.setCreateTime(now);
            progress.setUpdateTime(now);
            learningProgressMapper.insert(progress);
            return;
        }
        progress.setChapterStudied(1);
        progress.setLastStudyTime(now);
        if (practiceScore != null && practiceTotal != null) {
            progress.setPracticeCompleted(1);
            progress.setPracticeScore(practiceScore);
            progress.setPracticeTotal(practiceTotal);
        }
        progress.setUpdateTime(now);
        learningProgressMapper.updateById(progress);
    }

    private Map<String, Object> buildUserProgress(Long userId) {
        List<LearningProgress> list = learningProgressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, userId)
                .orderByDesc(LearningProgress::getLastStudyTime));
        Set<Long> courseSet = new HashSet<Long>();
        int studiedChapters = 0;
        int completedPractice = 0;
        int practiceScoreTotal = 0;
        int practiceQuestionTotal = 0;
        for (LearningProgress item : list) {
            if (item.getCourseId() != null) {
                courseSet.add(item.getCourseId());
            }
            if (Integer.valueOf(1).equals(item.getChapterStudied())) {
                studiedChapters++;
            }
            if (Integer.valueOf(1).equals(item.getPracticeCompleted())) {
                completedPractice++;
                practiceScoreTotal += item.getPracticeScore() == null ? 0 : item.getPracticeScore();
                practiceQuestionTotal += item.getPracticeTotal() == null ? 0 : item.getPracticeTotal();
            }
        }
        List<CodeSubmission> submissions = codeSubmissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                .eq(CodeSubmission::getUserId, userId));
        int codeSubmissionCount = submissions.size();
        int codePassedCount = 0;
        for (CodeSubmission submission : submissions) {
            if (Integer.valueOf(1).equals(submission.getPassed())) {
                codePassedCount++;
            }
        }
        List<Map<String, Object>> wrongBook = buildWrongBook(userId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("studiedCourses", courseSet.size());
        result.put("studiedChapters", studiedChapters);
        result.put("completedPractice", completedPractice);
        result.put("practiceScoreTotal", practiceScoreTotal);
        result.put("practiceQuestionTotal", practiceQuestionTotal);
        result.put("codeSubmissionCount", codeSubmissionCount);
        result.put("codePassedCount", codePassedCount);
        result.put("wrongBookCount", wrongBook.size());
        result.put("wrongBook", wrongBook);
        result.put("latestStudyTime", list.isEmpty() ? null : list.get(0).getLastStudyTime());
        return result;
    }

    private List<Map<String, Object>> buildWrongBook(Long userId) {
        List<WrongBookEntry> entries = wrongBookEntryMapper.selectList(new LambdaQueryWrapper<WrongBookEntry>()
                .eq(WrongBookEntry::getUserId, userId)
                .orderByDesc(WrongBookEntry::getUpdateTime)
                .orderByDesc(WrongBookEntry::getId));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (WrongBookEntry entry : entries) {
            Map<String, Object> row = buildWrongBookRow(entry);
            if (row != null) {
                result.add(row);
            }
        }
        return result;
    }

    private Map<String, Object> buildWrongBookRow(WrongBookEntry entry) {
        if ("QUESTION".equals(entry.getTargetType())) {
            Question question = questionMapper.selectById(entry.getTargetId());
            if (question == null) {
                return null;
            }
            Chapter chapter = chapterMapper.selectById(question.getChapterId());
            Course course = chapter == null ? null : courseMapper.selectById(chapter.getCourseId());
            Map<String, Object> row = createBaseWrongBookRow(entry, "作业题", question.getStem());
            row.put("description", chapter == null ? "所属章节已被删除" : "章节：" + chapter.getTitle());
            row.put("routePath", chapter == null || course == null ? null : "/practice/" + course.getId() + "/" + chapter.getId());
            row.put("routeLabel", "去练习页");
            return row;
        }
        if ("CODE_PROBLEM".equals(entry.getTargetType())) {
            CodeProblem problem = codeProblemMapper.selectById(entry.getTargetId());
            if (problem == null) {
                return null;
            }
            Map<String, Object> row = createBaseWrongBookRow(entry, "编程题", problem.getTitle());
            row.put("description", problem.getDescription());
            row.put("routePath", "/code-practice?problem=" + problem.getId());
            row.put("routeLabel", "去编程页");
            return row;
        }
        return null;
    }

    private Map<String, Object> createBaseWrongBookRow(WrongBookEntry entry, String typeLabel, String title) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("id", entry.getId());
        row.put("targetType", entry.getTargetType());
        row.put("targetId", entry.getTargetId());
        row.put("typeLabel", typeLabel);
        row.put("title", title);
        row.put("createTime", entry.getCreateTime());
        row.put("updateTime", entry.getUpdateTime());
        return row;
    }
}
