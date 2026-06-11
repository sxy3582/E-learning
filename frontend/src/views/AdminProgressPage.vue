<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Progress</p>
        <h1>学习进度</h1>
        <p class="muted">查看每个用户的学习行为与练习完成情况。</p>
      </div>
      <button class="secondary" @click="loadData">刷新</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <div class="data-panel">
      <div class="table-row table-head progress-table">
        <span>用户</span>
        <span>已学课程</span>
        <span>已学章节</span>
        <span>练习完成</span>
        <span>练习得分</span>
        <span>编程通过/提交</span>
      </div>
      <div class="table-row progress-table" v-for="row in rows" :key="row.userId">
        <span>
          <strong>{{ row.username }}</strong>
          <small>{{ row.nickname || "-" }}</small>
        </span>
        <span>{{ row.studiedCourses }}</span>
        <span>{{ row.studiedChapters }}</span>
        <span>{{ row.completedPractice }}</span>
        <span>{{ row.practiceScoreTotal }}/{{ row.practiceQuestionTotal }}</span>
        <span>{{ row.codePassedCount }}/{{ row.codeSubmissionCount }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import http from "../api";

const rows = ref([]);
const error = ref("");

async function loadData() {
  error.value = "";
  try {
    rows.value = await http.get("/api/progress/admin/list");
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(loadData);
</script>
