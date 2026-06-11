package com.elearning.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.entity.CodeSubmission;
import com.elearning.backend.entity.UserAnswer;
import com.elearning.backend.entity.WrongBookEntry;
import com.elearning.backend.mapper.CodeSubmissionMapper;
import com.elearning.backend.mapper.UserAnswerMapper;
import com.elearning.backend.mapper.WrongBookEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WrongBookService {
    @Resource
    private WrongBookEntryMapper wrongBookEntryMapper;
    @Resource
    private UserAnswerMapper userAnswerMapper;
    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;

    public void addQuestionWrong(Long userId, Long questionId) {
        upsert(userId, "QUESTION", questionId);
    }

    public void removeQuestionWrong(Long userId, Long questionId) {
        remove(userId, "QUESTION", questionId);
    }

    public void addCodeProblemWrong(Long userId, Long problemId) {
        upsert(userId, "CODE_PROBLEM", problemId);
    }

    public void removeCodeProblemWrong(Long userId, Long problemId) {
        remove(userId, "CODE_PROBLEM", problemId);
    }

    public int countByUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        backfillIfEmpty();
        Long count = wrongBookEntryMapper.selectCount(new LambdaQueryWrapper<WrongBookEntry>()
                .eq(WrongBookEntry::getUserId, userId));
        return count == null ? 0 : count.intValue();
    }

    @Transactional(rollbackFor = Exception.class)
    public void backfillIfEmpty() {
        Long existingCount = wrongBookEntryMapper.selectCount(new LambdaQueryWrapper<WrongBookEntry>());
        if (existingCount != null && existingCount > 0) {
            return;
        }

        List<UserAnswer> wrongAnswers = userAnswerMapper.selectList(new LambdaQueryWrapper<UserAnswer>()
                .eq(UserAnswer::getIsCorrect, 0)
                .orderByAsc(UserAnswer::getId));
        for (UserAnswer answer : wrongAnswers) {
            addQuestionWrong(answer.getUserId(), answer.getQuestionId());
        }

        List<CodeSubmission> submissions = codeSubmissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                .isNotNull(CodeSubmission::getProblemId)
                .orderByDesc(CodeSubmission::getCreateTime)
                .orderByDesc(CodeSubmission::getId));
        Map<String, CodeSubmission> latestSubmissionMap = new HashMap<String, CodeSubmission>();
        for (CodeSubmission submission : submissions) {
            String key = buildCodeProblemKey(submission.getUserId(), submission.getProblemId());
            if (!latestSubmissionMap.containsKey(key)) {
                latestSubmissionMap.put(key, submission);
            }
        }
        for (CodeSubmission submission : latestSubmissionMap.values()) {
            if (Integer.valueOf(1).equals(submission.getPassed())) {
                continue;
            }
            addCodeProblemWrong(submission.getUserId(), submission.getProblemId());
        }
    }

    private void upsert(Long userId, String targetType, Long targetId) {
        if (userId == null || targetId == null) {
            return;
        }
        WrongBookEntry existing = wrongBookEntryMapper.selectOne(new LambdaQueryWrapper<WrongBookEntry>()
                .eq(WrongBookEntry::getUserId, userId)
                .eq(WrongBookEntry::getTargetType, targetType)
                .eq(WrongBookEntry::getTargetId, targetId)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            WrongBookEntry entry = new WrongBookEntry();
            entry.setUserId(userId);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setCreateTime(now);
            entry.setUpdateTime(now);
            wrongBookEntryMapper.insert(entry);
            return;
        }
        existing.setUpdateTime(now);
        wrongBookEntryMapper.updateById(existing);
    }

    private void remove(Long userId, String targetType, Long targetId) {
        if (userId == null || targetId == null) {
            return;
        }
        wrongBookEntryMapper.delete(new LambdaQueryWrapper<WrongBookEntry>()
                .eq(WrongBookEntry::getUserId, userId)
                .eq(WrongBookEntry::getTargetType, targetType)
                .eq(WrongBookEntry::getTargetId, targetId));
    }

    private String buildCodeProblemKey(Long userId, Long problemId) {
        return String.valueOf(userId) + "_" + String.valueOf(problemId);
    }
}
