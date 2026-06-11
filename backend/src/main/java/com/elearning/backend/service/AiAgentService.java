package com.elearning.backend.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elearning.backend.common.exception.BusinessException;
import com.elearning.backend.entity.Chapter;
import com.elearning.backend.entity.CodeProblem;
import com.elearning.backend.entity.CodeSubmission;
import com.elearning.backend.entity.CodeTestCase;
import com.elearning.backend.entity.Course;
import com.elearning.backend.entity.LearningProgress;
import com.elearning.backend.entity.Question;
import com.elearning.backend.entity.QuestionOption;
import com.elearning.backend.mapper.ChapterMapper;
import com.elearning.backend.mapper.CodeProblemMapper;
import com.elearning.backend.mapper.CodeSubmissionMapper;
import com.elearning.backend.mapper.CodeTestCaseMapper;
import com.elearning.backend.mapper.CourseMapper;
import com.elearning.backend.mapper.LearningProgressMapper;
import com.elearning.backend.mapper.QuestionMapper;
import com.elearning.backend.mapper.QuestionOptionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAgentService {

    @Value("${deepseek.api-key:}")
    private String apiKeyFromConfig;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout-ms:20000}")
    private int timeoutMs;

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChapterMapper chapterMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper questionOptionMapper;
    @Resource
    private CodeProblemMapper codeProblemMapper;
    @Resource
    private CodeTestCaseMapper codeTestCaseMapper;
    @Resource
    private LearningProgressMapper learningProgressMapper;
    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;

    public Map<String, Object> ask(Long userId, String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BusinessException("问题不能为空");
        }

        Map<String, Object> codeDirect = tryResolveCodeProblem(q);
        if (codeDirect != null) {
            return codeDirect;
        }

        Map<String, Object> direct = tryResolveAnswerByName(q);
        if (direct != null) {
            return direct;
        }

        String context = buildContext(userId, q);
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BusinessException("AI 服务未配置，请设置 deepseek.api-key 或环境变量 DEEPSEEK_API_KEY");
        }

        String answer = callDeepSeek(apiKey.trim(), q, context);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("answer", answer);
        return result;
    }

    private String getApiKey() {
        if (apiKeyFromConfig != null && !apiKeyFromConfig.trim().isEmpty()) {
            return apiKeyFromConfig;
        }
        return System.getenv("DEEPSEEK_API_KEY");
    }

    private String callDeepSeek(String apiKey, String question, String context) {
        String url = normalizeBaseUrl(baseUrl) + "/v1/chat/completions";

        JSONArray messages = JSONUtil.createArray();
        messages.add(JSONUtil.createObj()
                .set("role", "system")
                .set("content",
                        "你是在线学习平台的课程与题目助教。你只能基于用户提供的上下文数据回答，不要编造不存在的课程、章节、题目或接口。\n"
                                + "如果上下文不足以回答，请明确说明缺少哪些信息，并引导用户给出课程ID、章节ID或题目关键词。\n"
                                + "回答尽量简洁、可操作，必要时用分点说明。"));
        messages.add(JSONUtil.createObj()
                .set("role", "user")
                .set("content", "用户问题：\n" + question + "\n\n系统数据上下文：\n" + context));

        JSONObject payload = JSONUtil.createObj();
        payload.set("model", model);
        payload.set("messages", messages);
        payload.set("temperature", 0.3);

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(timeoutMs)
                .body(payload.toString())
                .execute();

        if (response.getStatus() != 200) {
            throw new BusinessException("AI 服务调用失败");
        }

        JSONObject json = JSONUtil.parseObj(response.body());
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException("AI 服务返回异常");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? "" : message.getStr("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("AI 服务返回为空");
        }
        return content.trim();
    }

    private String buildContext(Long userId, String question) {
        StringBuilder sb = new StringBuilder();

        List<LearningProgress> progresses = learningProgressMapper.selectList(new LambdaQueryWrapper<LearningProgress>()
                .eq(LearningProgress::getUserId, userId));
        int studiedChapters = 0;
        int completedPractice = 0;
        int totalScore = 0;
        int totalPracticeCount = 0;
        for (LearningProgress p : progresses) {
            if (p.getChapterStudied() != null && p.getChapterStudied() == 1) {
                studiedChapters++;
            }
            if (p.getPracticeCompleted() != null && p.getPracticeCompleted() == 1) {
                completedPractice++;
            }
            totalScore += p.getPracticeScore() == null ? 0 : p.getPracticeScore();
            totalPracticeCount += p.getPracticeTotal() == null ? 0 : p.getPracticeTotal();
        }

        List<CodeSubmission> submissions = codeSubmissionMapper.selectList(new LambdaQueryWrapper<CodeSubmission>()
                .eq(CodeSubmission::getUserId, userId));
        int submitCount = submissions.size();
        int passedCount = 0;
        for (CodeSubmission s : submissions) {
            if (s.getPassed() != null && s.getPassed() == 1) {
                passedCount++;
            }
        }

        sb.append("用户学习统计：").append("\n");
        sb.append("- 已学习章节数：").append(studiedChapters).append("\n");
        sb.append("- 已完成练习次数：").append(completedPractice).append("\n");
        sb.append("- 累计练习得分：").append(totalScore).append(" / ").append(totalPracticeCount).append("\n");
        sb.append("- 编程题通过数：").append(passedCount).append(" / ").append(submitCount).append("\n\n");

        Long courseId = extractId(question, "课程");
        Long chapterId = extractId(question, "章节");
        Long problemId = extractId(question, "编程题");

        if (courseId != null) {
            Course course = courseMapper.selectById(courseId);
            if (course != null) {
                sb.append("课程：").append(course.getId()).append(" ").append(course.getTitle()).append("\n");
                sb.append("简介：").append(course.getIntro() == null ? "" : course.getIntro()).append("\n");
            } else {
                sb.append("课程：未找到 id=").append(courseId).append("\n");
            }
            List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                    .eq(Chapter::getCourseId, courseId)
                    .orderByAsc(Chapter::getSortOrder)
                    .orderByAsc(Chapter::getId));
            sb.append("该课程章节列表：").append(chapters.size()).append("\n");
            int limit = Math.min(12, chapters.size());
            for (int i = 0; i < limit; i++) {
                Chapter ch = chapters.get(i);
                sb.append("- ").append(ch.getId()).append(" ").append(ch.getTitle())
                        .append(" ").append(ch.getContentType()).append("\n");
            }
            sb.append("\n");
        }

        if (chapterId != null) {
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter != null) {
                sb.append("章节：").append(chapter.getId()).append(" ").append(chapter.getTitle())
                        .append(" ").append(chapter.getContentType()).append("\n");
                sb.append("内容：").append(trimLongText(chapter.getContentValue(), 600)).append("\n\n");
            } else {
                sb.append("章节：未找到 id=").append(chapterId).append("\n\n");
            }

            List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                    .eq(Question::getChapterId, chapterId)
                    .orderByAsc(Question::getSortOrder)
                    .orderByAsc(Question::getId));
            sb.append("该章节练习题：").append(questions.size()).append("\n");
            int limitQ = Math.min(6, questions.size());
            for (int i = 0; i < limitQ; i++) {
                Question q = questions.get(i);
                sb.append("题目 ").append(q.getId()).append(" ").append(q.getType()).append("\n");
                sb.append("题干：").append(trimLongText(q.getStem(), 280)).append("\n");

                if ("SINGLE".equalsIgnoreCase(q.getType())) {
                    List<QuestionOption> options = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                            .eq(QuestionOption::getQuestionId, q.getId())
                            .orderByAsc(QuestionOption::getSortOrder)
                            .orderByAsc(QuestionOption::getId));
                    int optionLimit = Math.min(6, options.size());
                    for (int j = 0; j < optionLimit; j++) {
                        QuestionOption opt = options.get(j);
                        sb.append("- ").append(opt.getOptionKey()).append(" ").append(trimLongText(opt.getOptionContent(), 120)).append("\n");
                    }
                }

                sb.append("参考答案：").append(trimLongText(q.getReferenceAnswer(), 80)).append("\n");
                sb.append("解析：").append(trimLongText(q.getAnalysis(), 240)).append("\n\n");
            }
        }

        if (problemId != null) {
            CodeProblem p = codeProblemMapper.selectById(problemId);
            if (p != null) {
                sb.append("编程题：").append(p.getId()).append(" ").append(p.getTitle()).append("\n");
                sb.append("方法名：").append(p.getMethodName()).append("\n");
                sb.append("描述：").append(trimLongText(p.getDescription(), 500)).append("\n");
                sb.append("模板代码：").append(trimLongText(p.getTemplateCode(), 600)).append("\n");

                List<CodeTestCase> samples = codeTestCaseMapper.selectList(new LambdaQueryWrapper<CodeTestCase>()
                        .eq(CodeTestCase::getProblemId, p.getId())
                        .eq(CodeTestCase::getIsSample, 1)
                        .orderByAsc(CodeTestCase::getSortOrder)
                        .orderByAsc(CodeTestCase::getId));
                if (!samples.isEmpty()) {
                    sb.append("样例用例：").append(samples.size()).append("\n");
                    int caseLimit = Math.min(4, samples.size());
                    for (int i = 0; i < caseLimit; i++) {
                        CodeTestCase c = samples.get(i);
                        sb.append("- input=").append(trimLongText(c.getInputJson(), 120))
                                .append(" expected=").append(trimLongText(c.getExpectedOutput(), 120)).append("\n");
                    }
                }
                sb.append("\n");
            } else {
                sb.append("编程题：未找到 id=").append(problemId).append("\n\n");
            }
        }

        if (courseId == null && chapterId == null && problemId == null) {
            List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getPublished, 1)
                    .orderByAsc(Course::getSortOrder)
                    .orderByDesc(Course::getId));
            sb.append("课程列表(节选)：").append("\n");
            int limit = Math.min(8, courses.size());
            for (int i = 0; i < limit; i++) {
                Course c = courses.get(i);
                sb.append("- ").append(c.getId()).append(" ").append(c.getTitle()).append("\n");
            }
            sb.append("\n");

            List<CodeProblem> problems = codeProblemMapper.selectList(new LambdaQueryWrapper<CodeProblem>()
                    .orderByAsc(CodeProblem::getSortOrder)
                    .orderByAsc(CodeProblem::getId));
            sb.append("编程题列表(节选)：").append("\n");
            int limitP = Math.min(8, problems.size());
            for (int i = 0; i < limitP; i++) {
                CodeProblem p = problems.get(i);
                sb.append("- ").append(p.getId()).append(" ").append(p.getTitle()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private Map<String, Object> tryResolveCodeProblem(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return null;
        }

        boolean related = q.contains("编程题") || q.contains("代码") || q.contains("解法") || q.contains("题目");
        if (!related) {
            return null;
        }

        boolean wantAnswer = q.contains("解法") || q.contains("答案") || q.contains("代码") || q.contains("怎么写") || q.contains("实现");
        Long problemId = extractId(q, "编程题");

        String titleKeyword = null;
        Matcher m1 = Pattern.compile("编程题\\s*[:：]?\\s*(.+)").matcher(q);
        if (m1.find()) {
            titleKeyword = m1.group(1);
        }
        titleKeyword = normalizeKeyword(titleKeyword);

        CodeProblem problem = null;
        if (problemId != null) {
            problem = codeProblemMapper.selectById(problemId);
        } else if (!titleKeyword.isEmpty()) {
            List<CodeProblem> problems = codeProblemMapper.selectList(new LambdaQueryWrapper<CodeProblem>()
                    .like(CodeProblem::getTitle, titleKeyword)
                    .orderByAsc(CodeProblem::getSortOrder)
                    .orderByAsc(CodeProblem::getId));
            if (!problems.isEmpty()) {
                problem = pickBestCodeProblem(problems, titleKeyword);
            }
        }

        if (problem == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("编程题：").append(problem.getId()).append(" ").append(problem.getTitle()).append("\n");
        sb.append("方法名：").append(problem.getMethodName()).append("\n");
        sb.append("描述：").append(trimLongText(problem.getDescription(), 520)).append("\n");
        sb.append("模板代码：").append(trimLongText(problem.getTemplateCode(), 720)).append("\n");

        List<CodeTestCase> samples = codeTestCaseMapper.selectList(new LambdaQueryWrapper<CodeTestCase>()
                .eq(CodeTestCase::getProblemId, problem.getId())
                .eq(CodeTestCase::getIsSample, 1)
                .orderByAsc(CodeTestCase::getSortOrder)
                .orderByAsc(CodeTestCase::getId));
        if (!samples.isEmpty()) {
            sb.append("样例用例：").append(samples.size()).append("\n");
            int limit = Math.min(6, samples.size());
            for (int i = 0; i < limit; i++) {
                CodeTestCase c = samples.get(i);
                sb.append("- input=").append(trimLongText(c.getInputJson(), 140))
                        .append(" expected=").append(trimLongText(c.getExpectedOutput(), 140)).append("\n");
            }
        }

        List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        links.add(link("进入在线编程", "/code-practice?problem=" + problem.getId()));

        if (!wantAnswer) {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("answer", sb.toString().trim());
            result.put("links", links);
            return result;
        }

        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BusinessException("AI 服务未配置，请设置 deepseek.api-key 或环境变量 DEEPSEEK_API_KEY");
        }

        String prompt = q;
        String answer = callDeepSeek(apiKey.trim(), prompt, sb.toString());

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("answer", answer);
        result.put("links", links);
        return result;
    }

    private CodeProblem pickBestCodeProblem(List<CodeProblem> problems, String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        for (CodeProblem p : problems) {
            if (p.getTitle() != null && p.getTitle().trim().equalsIgnoreCase(k)) {
                return p;
            }
        }
        return problems.get(0);
    }

    private Map<String, Object> tryResolveAnswerByName(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return null;
        }

        String chapterKeyword = null;
        String stemKeyword = null;

        Matcher m1 = Pattern.compile("章节\\s*[:：]?\\s*(.+?)\\s*(题目|问题)\\s*[:：]?\\s*(.+)").matcher(q);
        if (m1.find()) {
            chapterKeyword = m1.group(1);
            stemKeyword = m1.group(3);
        }

        if (chapterKeyword == null || stemKeyword == null) {
            Matcher m2 = Pattern.compile("在\\s*(.+?)\\s*章节.*?(题目|问题)\\s*[:：]?\\s*(.+)").matcher(q);
            if (m2.find()) {
                chapterKeyword = m2.group(1);
                stemKeyword = m2.group(3);
            }
        }

        chapterKeyword = normalizeKeyword(chapterKeyword);
        stemKeyword = normalizeKeyword(stemKeyword);

        if (chapterKeyword.isEmpty() || stemKeyword.isEmpty()) {
            return null;
        }

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .like(Chapter::getTitle, chapterKeyword)
                .orderByAsc(Chapter::getId));
        if (chapters.isEmpty()) {
            return null;
        }

        Chapter chapter = pickBestChapter(chapters, chapterKeyword);

        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getChapterId, chapter.getId())
                .like(Question::getStem, stemKeyword)
                .orderByAsc(Question::getSortOrder)
                .orderByAsc(Question::getId));
        if (questions.isEmpty()) {
            return null;
        }

        Question target = pickBestQuestion(questions, stemKeyword);

        String reference = target.getReferenceAnswer() == null ? "" : target.getReferenceAnswer().trim();
        String analysis = target.getAnalysis() == null ? "" : target.getAnalysis().trim();

        StringBuilder sb = new StringBuilder();
        sb.append("匹配到章节：").append(chapter.getId()).append(" ").append(chapter.getTitle()).append("\n");
        sb.append("匹配到题目：").append(target.getId()).append(" ").append(trimLongText(target.getStem(), 220)).append("\n");

        if ("SINGLE".equalsIgnoreCase(target.getType())) {
            sb.append("题型：单选题\n");
            sb.append("参考答案：").append(reference).append("\n");
            if (!reference.isEmpty()) {
                List<QuestionOption> opts = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, target.getId())
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId));
                for (QuestionOption opt : opts) {
                    if (reference.equalsIgnoreCase(opt.getOptionKey())) {
                        sb.append("正确选项：").append(opt.getOptionKey()).append(" ").append(trimLongText(opt.getOptionContent(), 160)).append("\n");
                        break;
                    }
                }
            }
        } else if ("JUDGE".equalsIgnoreCase(target.getType())) {
            sb.append("题型：判断题\n");
            sb.append("参考答案：").append(formatJudge(reference)).append("\n");
        } else {
            sb.append("题型：").append(target.getType()).append("\n");
            sb.append("参考答案：").append(reference).append("\n");
        }

        if (!analysis.isEmpty()) {
            sb.append("解析：").append(trimLongText(analysis, 360)).append("\n");
        }

        List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        links.add(link("进入章节学习", "/study/" + chapter.getCourseId() + "/" + chapter.getId()));
        links.add(link("进入章节练习", "/practice/" + chapter.getCourseId() + "/" + chapter.getId()));

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("answer", sb.toString().trim());
        result.put("links", links);
        return result;
    }

    private Map<String, String> link(String label, String to) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("label", label);
        map.put("to", to);
        return map;
    }

    private Chapter pickBestChapter(List<Chapter> chapters, String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        for (Chapter c : chapters) {
            if (c.getTitle() != null && c.getTitle().trim().equalsIgnoreCase(k)) {
                return c;
            }
        }
        return chapters.get(0);
    }

    private Question pickBestQuestion(List<Question> questions, String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        for (Question q : questions) {
            if (q.getStem() != null && q.getStem().trim().equalsIgnoreCase(k)) {
                return q;
            }
        }
        return questions.get(0);
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        v = v.replace("\"", "").replace("'", "");
        v = v.replace("的答案", "").replace("答案", "").replace("解析", "").trim();
        if (v.length() > 80) {
            v = v.substring(0, 80);
        }
        return v;
    }

    private String formatJudge(String value) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("TRUE".equals(v) || "T".equals(v) || "正确".equals(value) || "对".equals(value)) {
            return "正确";
        }
        if ("FALSE".equals(v) || "F".equals(v) || "错误".equals(value) || "错".equals(value)) {
            return "错误";
        }
        return value == null ? "" : value.trim();
    }

    private String trimLongText(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private Long extractId(String text, String prefix) {
        Pattern pattern = Pattern.compile(prefix + "\\s*([0-9]+)");
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) {
            return "https://api.deepseek.com";
        }
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
