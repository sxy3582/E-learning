<template>
  <section class="grid">
    <div class="course-detail-layout">
      <article class="card course-summary-card">
      <div class="course-cover-box detail-cover">
        <img
          v-if="course?.coverUrl"
          class="course-cover-img"
          :src="normalizeCoverUrl(course.coverUrl)"
          :alt="`${course.title}封面`"
        />
        <div v-else class="course-cover-placeholder">
          <span>{{ course?.category || "Course" }}</span>
          <strong>{{ course?.title || "课程详情" }}</strong>
        </div>
      </div>
      <p class="eyebrow">Course Detail</p>
      <h2 class="title">{{ course?.title || "课程详情" }}</h2>
      <p class="muted">{{ course?.intro || "暂无简介" }}</p>
      <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 18px;">
        <span class="status-pill">分类：{{ course?.category || "未分类" }}</span>
        <span class="status-pill">难度：{{ course?.difficulty || "未设置" }}</span>
      </div>
      <div class="course-progress-summary">
        <div>
          <strong>{{ progressStats.studied }}</strong>
          <span>已学习章节</span>
        </div>
        <div>
          <strong>{{ progressStats.practiceDone }}</strong>
          <span>已完成练习</span>
        </div>
        <div>
          <strong>{{ chapters.length }}</strong>
          <span>章节总数</span>
        </div>
      </div>
      </article>

      <article class="card">
        <h3 class="title">章节目录</h3>
        <div class="chapter-timeline">
          <div class="chapter-card" v-for="(chapter, index) in chapters" :key="chapter.id">
            <div class="chapter-order">{{ String(index + 1).padStart(2, "0") }}</div>
            <div class="chapter-main">
              <div class="chapter-title-row">
                <strong>{{ chapter.title }}</strong>
                <span class="status-pill">{{ getTypeLabel(chapter.contentType) }}</span>
              </div>
              <p class="muted">{{ previewContent(chapter.contentValue) }}</p>
              <div class="chapter-progress-row">
                <span :class="['progress-dot', getProgressState(chapter).className]">
                  {{ getProgressState(chapter).text }}
                </span>
                <span v-if="chapter.contentType === 'question'" class="muted">
                  {{ getPracticeText(chapter) }}
                </span>
              </div>
            </div>
            <div class="chapter-action">
              <router-link :to="getChapterTarget(chapter)">
                <button class="secondary">{{ getActionText(chapter.contentType) }}</button>
              </router-link>
            </div>
          </div>
        </div>
      </article>
    </div>
    <CommentPanel
      v-if="course?.id"
      target-type="COURSE"
      :target-id="course.id"
      title="课程评论区"
      placeholder="欢迎分享这门课程的学习体验、建议或问题"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import http from "../api";
import CommentPanel from "../components/CommentPanel.vue";

const route = useRoute();
const course = ref(null);
const chapters = ref([]);
const progressMap = ref({});

const progressStats = computed(() => {
  const values = Object.values(progressMap.value);
  return {
    studied: values.filter((item) => Number(item.chapterStudied) === 1).length,
    practiceDone: values.filter((item) => Number(item.practiceCompleted) === 1).length
  };
});

function previewContent(content) {
  if (!content) {
    return "暂无内容";
  }
  return content.length > 56 ? `${content.slice(0, 56)}...` : content;
}

function normalizeCoverUrl(url) {
  if (!url) {
    return "";
  }
  if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
    return url;
  }
  return url.startsWith("/") ? url : `/${url}`;
}

function getTypeLabel(type) {
  const map = {
    article: "文章学习",
    video: "视频学习",
    question: "章节练习",
    code_problem: "编程练习",
    algorithm_lab: "算法实验"
  };
  return map[type] || type;
}

function getActionText(type) {
  const map = {
    article: "开始学习",
    video: "观看学习",
    question: "进入练习",
    code_problem: "进入编程",
    algorithm_lab: "进入实验"
  };
  return map[type] || "进入";
}

function getChapterTarget(chapter) {
  if (chapter.contentType === "question") {
    return `/practice/${route.params.id}/${chapter.id}`;
  }
  if (chapter.contentType === "code_problem") {
    return chapter.contentValue || "/code-practice";
  }
  if (chapter.contentType === "algorithm_lab") {
    return chapter.contentValue || "/algorithm-lab";
  }
  return `/study/${route.params.id}/${chapter.id}`;
}

function getProgressState(chapter) {
  const progress = progressMap.value[chapter.id];
  if (!progress) {
    return { text: "未开始", className: "todo" };
  }
  if (chapter.contentType === "question") {
    return Number(progress.practiceCompleted) === 1
      ? { text: "练习已完成", className: "done" }
      : { text: "已学习，练习未完成", className: "doing" };
  }
  return Number(progress.chapterStudied) === 1
    ? { text: "已学习", className: "done" }
    : { text: "未开始", className: "todo" };
}

function getPracticeText(chapter) {
  const progress = progressMap.value[chapter.id];
  if (!progress || !progress.practiceCompleted) {
    return "尚未提交练习";
  }
  return `练习得分 ${progress.practiceScore}/${progress.practiceTotal}`;
}

onMounted(async () => {
  const id = route.params.id;
  course.value = await http.get(`/api/course/${id}`);
  chapters.value = await http.get("/api/chapter/list", { params: { courseId: id } });
  const progressList = await http.get("/api/progress/course", { params: { courseId: id } });
  const map = {};
  progressList.forEach((item) => {
    map[item.chapterId] = item;
  });
  progressMap.value = map;
});
</script>
