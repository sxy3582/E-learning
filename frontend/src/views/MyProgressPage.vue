<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">My Progress</p>
        <h1>我的学习进度</h1>
        <p class="muted">汇总课程学习、章节完成、练习成绩与编程提交情况。</p>
      </div>
      <button class="secondary" @click="loadData">刷新</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <div class="grid grid-3">
      <article class="card metric-card">
        <p class="eyebrow">Courses</p>
        <div class="metric-number">{{ progress.studiedCourses || 0 }}</div>
        <p class="muted">已学习课程数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Chapters</p>
        <div class="metric-number">{{ progress.studiedChapters || 0 }}</div>
        <p class="muted">已学习章节数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Practice</p>
        <div class="metric-number">{{ progress.practiceScoreTotal || 0 }}/{{ progress.practiceQuestionTotal || 0 }}</div>
        <p class="muted">累计练习得分</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Practice Done</p>
        <div class="metric-number">{{ progress.completedPractice || 0 }}</div>
        <p class="muted">已完成练习章节</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Code Passed</p>
        <div class="metric-number">{{ progress.codePassedCount || 0 }}</div>
        <p class="muted">编程通过次数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Code Submissions</p>
        <div class="metric-number">{{ progress.codeSubmissionCount || 0 }}</div>
        <p class="muted">编程提交次数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Wrong Book</p>
        <div class="metric-number">{{ progress.wrongBookCount || 0 }}</div>
        <p class="muted">错题单条目数</p>
      </article>
    </div>

  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import http from "../api";
import { applyWrongBookCountFromPayload, getWrongBookCount, onWrongBookCountChange } from "../wrongBookState";

const error = ref("");
const progress = reactive({
  wrongBookCount: getWrongBookCount()
});
let stopListening = null;

async function loadData() {
  error.value = "";
  try {
    const data = await http.get("/api/progress/my");
    Object.assign(progress, data);
    applyWrongBookCountFromPayload(data);
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(() => {
  stopListening = onWrongBookCountChange((count) => {
    progress.wrongBookCount = count;
  });
  loadData();
});

onBeforeUnmount(() => {
  if (stopListening) {
    stopListening();
  }
});
</script>
