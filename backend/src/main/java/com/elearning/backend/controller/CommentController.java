package com.elearning.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.ApiResponse;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.dto.CommentSaveRequest;
import com.elearning.backend.entity.AppComment;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.SysUser;
import com.elearning.backend.mapper.AppCommentMapper;
import com.elearning.backend.mapper.CodeProblemMapper;
import com.elearning.backend.mapper.CourseMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.SysUserMapper;
import com.elearning.backend.util.AuthUtil;
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
@RequestMapping("/api/comment")
public class CommentController {

    @Resource
    private AppCommentMapper appCommentMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private CodeProblemMapper codeProblemMapper;

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam String targetType, @RequestParam Long targetId) {
        AuthUtil.checkLogin();
        String normalizedType = normalizeTargetType(targetType);
        validateTarget(normalizedType, targetId);
        return ApiResponse.success(buildCommentList(normalizedType, targetId, StpUtil.getLoginIdAsLong(), StpUtil.hasRole("ADMIN")));
    }

    @GetMapping("/admin/list")
    public ApiResponse<List<Map<String, Object>>> adminList(@RequestParam(required = false) String targetType,
                                                            @RequestParam(required = false) Long targetId) {
        AuthUtil.checkAdmin();
        LambdaQueryWrapper<AppComment> wrapper = new LambdaQueryWrapper<AppComment>()
                .orderByDesc(AppComment::getCreateTime)
                .orderByDesc(AppComment::getId);
        if (targetType != null && !targetType.trim().isEmpty()) {
            wrapper.eq(AppComment::getTargetType, normalizeTargetType(targetType));
        }
        if (targetId != null) {
            wrapper.eq(AppComment::getTargetId, targetId);
        }
        List<AppComment> comments = appCommentMapper.selectList(wrapper);
        return ApiResponse.success(toCommentViewList(comments, null, true));
    }

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(@RequestBody @Validated CommentSaveRequest request) {
        AuthUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        String normalizedType = normalizeTargetType(request.getTargetType());
        String content = normalizeContent(request.getContent());
        if (request.getId() == null) {
            validateTarget(normalizedType, request.getTargetId());
            AppComment comment = new AppComment();
            LocalDateTime now = LocalDateTime.now();
            comment.setUserId(userId);
            comment.setTargetType(normalizedType);
            comment.setTargetId(request.getTargetId());
            comment.setContent(content);
            comment.setCreateTime(now);
            comment.setUpdateTime(now);
            appCommentMapper.insert(comment);
            return ApiResponse.success("评论已发布", toCommentView(comment, sysUserMapper.selectById(userId), userId, admin));
        }

        AppComment comment = appCommentMapper.selectById(request.getId());
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!admin && !userId.equals(comment.getUserId())) {
            throw new BusinessException("只能修改自己的评论");
        }
        comment.setContent(content);
        comment.setUpdateTime(LocalDateTime.now());
        appCommentMapper.updateById(comment);
        return ApiResponse.success("评论已更新", toCommentView(comment, sysUserMapper.selectById(comment.getUserId()), userId, admin));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthUtil.checkLogin();
        AppComment comment = appCommentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        if (!admin && !userId.equals(comment.getUserId())) {
            throw new BusinessException("只能删除自己的评论");
        }
        appCommentMapper.deleteById(id);
        return ApiResponse.success("评论已删除", null);
    }

    private List<Map<String, Object>> buildCommentList(String targetType, Long targetId, Long currentUserId, boolean admin) {
        List<AppComment> comments = appCommentMapper.selectList(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getTargetType, targetType)
                .eq(AppComment::getTargetId, targetId)
                .orderByDesc(AppComment::getCreateTime)
                .orderByDesc(AppComment::getId));
        return toCommentViewList(comments, currentUserId, admin);
    }

    private List<Map<String, Object>> toCommentViewList(List<AppComment> comments, Long currentUserId, boolean admin) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (comments == null || comments.isEmpty()) {
            return result;
        }
        Set<Long> userIds = new HashSet<Long>();
        for (AppComment comment : comments) {
            userIds.add(comment.getUserId());
        }
        Map<Long, SysUser> userMap = new HashMap<Long, SysUser>();
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
        for (SysUser user : users) {
            userMap.put(user.getId(), user);
        }
        for (AppComment comment : comments) {
            result.add(toCommentView(comment, userMap.get(comment.getUserId()), currentUserId, admin));
        }
        return result;
    }

    private Map<String, Object> toCommentView(AppComment comment, SysUser user, Long currentUserId, boolean admin) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("id", comment.getId());
        row.put("userId", comment.getUserId());
        row.put("targetType", comment.getTargetType());
        row.put("targetId", comment.getTargetId());
        row.put("content", comment.getContent());
        row.put("createTime", comment.getCreateTime());
        row.put("updateTime", comment.getUpdateTime());
        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("avatarUrl", user == null ? "" : user.getAvatarUrl());
        boolean own = currentUserId != null && currentUserId.equals(comment.getUserId());
        row.put("canEdit", own);
        row.put("canDelete", admin || own);
        return row;
    }

    private String normalizeTargetType(String targetType) {
        String value = targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
        if (!"COURSE".equals(value) && !"QUESTION".equals(value) && !"CODE_PROBLEM".equals(value)) {
            throw new BusinessException("评论目标类型不支持");
        }
        return value;
    }

    private String normalizeContent(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (value.length() > 1000) {
            throw new BusinessException("评论内容不能超过1000个字符");
        }
        return value;
    }

    private void validateTarget(String targetType, Long targetId) {
        if (targetId == null) {
            throw new BusinessException("评论目标不存在");
        }
        if ("COURSE".equals(targetType)) {
            Course course = courseMapper.selectById(targetId);
            if (course == null) {
                throw new BusinessException("课程不存在");
            }
            return;
        }
        if ("QUESTION".equals(targetType)) {
            Question question = questionMapper.selectById(targetId);
            if (question == null) {
                throw new BusinessException("题目不存在");
            }
            return;
        }
        CodeProblem codeProblem = codeProblemMapper.selectById(targetId);
        if (codeProblem == null) {
            throw new BusinessException("编程题不存在");
        }
    }
}
