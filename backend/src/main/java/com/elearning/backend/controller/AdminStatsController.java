package com.elearning.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.CodeSubmission;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.SysUser;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.CodeSubmissionMapper;
import com.elearning.backend.mapper.CourseMapper;
import com.elearning.backend.mapper.LearningProgressMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.SysUserMapper;
import com.elearning.backend.util.AuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;
    @Resource
    private LearningProgressMapper learningProgressMapper;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        AuthUtil.checkAdmin();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("userCount", sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "USER")));
        data.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()));
        data.put("chapterCount", chapterMapper.selectCount(new LambdaQueryWrapper<Chapter>()));
        data.put("questionCount", questionMapper.selectCount(new LambdaQueryWrapper<Question>()));
        data.put("codeSubmissionCount", codeSubmissionMapper.selectCount(new LambdaQueryWrapper<CodeSubmission>()));
        data.put("progressRecordCount", learningProgressMapper.selectCount(new LambdaQueryWrapper<LearningProgress>()));
        return ApiResponse.success(data);
    }
}
