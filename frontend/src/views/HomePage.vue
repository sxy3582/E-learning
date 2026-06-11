<template>
  <section class="overview-page">
    <div class="overview-hero learner-hero">
      <article>
        <p class="eyebrow">Learning Center</p>
        <h1>把课程、练习、代码和算法实验连成一条学习路径。</h1>
        <p>
          这里不是简单的课程列表，而是一个个人学习工作台：先进入课程章节，再完成练习和 Java 编程题，最后通过算法动画理解过程。
        </p>
        <div class="hero-actions">
          <router-link to="/courses"><button class="primary">继续课程学习</button></router-link>
          <router-link to="/my-progress"><button class="secondary">查看学习进度</button></router-link>
        </div>
      </article>
      <aside class="overview-focus-card">
        <span class="status-pill">当前进度</span>
        <strong>{{ progress.studiedChapters || 0 }}</strong>
        <p>已学习章节。{{ progress.latestStudyTime ? `最近学习：${progress.latestStudyTime}` : "先选择一门课程开始记录。" }}</p>
      </aside>
    </div>

    <div class="overview-metrics">
      <article class="overview-metric">
        <span>已学课程</span>
        <strong>{{ progress.studiedCourses || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>练习完成</span>
        <strong>{{ progress.completedPractice || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>练习得分</span>
        <strong>{{ progress.practiceScoreTotal || 0 }}/{{ progress.practiceQuestionTotal || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>编程通过</span>
        <strong>{{ progress.codePassedCount || 0 }}</strong>
      </article>
    </div>

    <div class="overview-section-title">
      <div>
        <p class="eyebrow">How To Learn</p>
        <h2>推荐学习流程</h2>
      </div>
      <p class="muted">面向普通学习者，先保证学得下去，再通过两个亮点模块加深理解。</p>
    </div>

    <div class="learning-path">
      <article class="path-card">
        <span>01</span>
        <h3>进入课程</h3>
        <p>按课程目录学习文章或视频章节，建立知识主线。</p>
        <router-link to="/courses">选择课程</router-link>
      </article>
      <article class="path-card">
        <span>02</span>
        <h3>完成章节练习</h3>
        <p>单选和判断题会即时判分，帮助确认当前章节是否掌握。</p>
        <router-link to="/courses">从章节进入</router-link>
      </article>
      <article class="path-card is-highlight">
        <span>03</span>
        <h3>在线编程</h3>
        <p>像力扣一样补全 `Solution` 方法，运行测试用例并查看友好错误提示。</p>
        <router-link to="/code-practice">开始编程</router-link>
      </article>
      <article class="path-card is-highlight">
        <span>04</span>
        <h3>算法可视化实验</h3>
        <p>观察排序和查找过程，理解比较、交换、指针移动这些抽象步骤。</p>
        <router-link to="/algorithm-lab">打开实验室</router-link>
      </article>
    </div>

    <div class="overview-feature-grid">
      <article class="feature-panel dark">
        <p class="eyebrow">Core Highlight</p>
        <h3>编程题运行与判题</h3>
        <p>平台内置 Java 编译、测试用例执行、通过数统计和提交记录，适合展示“学完马上练”的闭环。</p>
      </article>
      <article class="feature-panel">
        <p class="eyebrow">Core Highlight</p>
        <h3>算法可视化</h3>
        <p>冒泡、选择、插入、二分、线性查找等过程可单步观察，降低算法学习门槛。</p>
      </article>
      <article class="feature-panel">
        <p class="eyebrow">Next Action</p>
        <h3>{{ progress.latestStudyTime ? "继续巩固" : "从第一门课开始" }}</h3>
        <p>{{ progress.latestStudyTime ? "你已经产生学习记录，可以查看详细进度并继续练习。" : "还没有学习记录，建议先从课程目录进入第一章。" }}</p>
        <router-link :to="progress.latestStudyTime ? '/my-progress' : '/courses'">
          <button class="secondary">{{ progress.latestStudyTime ? "查看进度" : "开始学习" }}</button>
        </router-link>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import http from "../api";

const progress = reactive({
  studiedCourses: 0,
  studiedChapters: 0,
  completedPractice: 0,
  codePassedCount: 0,
  codeSubmissionCount: 0,
  practiceScoreTotal: 0,
  practiceQuestionTotal: 0,
  latestStudyTime: null
});

onMounted(async () => {
  try {
    const data = await http.get("/api/progress/my");
    Object.assign(progress, data);
  } catch (e) {
    // ignore dashboard progress failure to avoid blocking home rendering
  }
});
</script>
