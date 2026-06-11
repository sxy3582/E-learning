<template>
  <section class="study-layout">
    <article class="card chapter-nav">
      <p class="eyebrow">Study</p>
      <h2 class="title">{{ course?.title || "章节学习" }}</h2>
      <p class="muted">{{ course?.intro || "请选择章节继续学习。" }}</p>
      <div class="admin-list">
        <router-link
          v-for="item in chapters"
          :key="item.id"
          :to="`/study/${route.params.courseId}/${item.id}`"
          class="chapter-link"
          :class="{ active: Number(route.params.chapterId) === item.id }"
        >
          <strong>{{ item.title }}</strong>
          <small>{{ getTypeLabel(item.contentType) }}</small>
        </router-link>
      </div>
    </article>

    <article class="card chapter-content">
      <p class="eyebrow">Chapter</p>
      <h2 class="title">{{ chapter?.title || "加载中..." }}</h2>
      <p class="error-text" v-if="error">{{ error }}</p>
      <template v-if="chapter">
        <template v-if="chapter.contentType === 'article'">
          <div class="article-box">{{ chapter.contentValue || "暂无文章内容" }}</div>
        </template>

        <template v-else-if="chapter.contentType === 'video'">
          <video
            v-if="isVideoFile(chapter.contentValue) && !videoError"
            controls
            class="study-video"
            :src="normalizeMediaUrl(chapter.contentValue)"
            @error="videoError = true"
          ></video>
          <iframe
            v-else-if="isEmbedVideo(chapter.contentValue)"
            class="study-video study-iframe"
            :src="normalizeMediaUrl(chapter.contentValue)"
            allowfullscreen
            scrolling="no"
            frameborder="0"
          ></iframe>
          <div v-else class="article-box">
            <p v-if="videoError">当前视频无法在浏览器内直接播放，可能是编码不兼容。</p>
            <p>视频链接：</p>
            <a :href="normalizeMediaUrl(chapter.contentValue)" target="_blank" rel="noreferrer">{{ normalizeMediaUrl(chapter.contentValue) }}</a>
          </div>
        </template>

        <template v-else-if="chapter.contentType === 'code_problem'">
          <p class="muted">本章节包含编程练习，点击进入编程页开始提交。</p>
          <router-link :to="chapter.contentValue || '/code-practice'"><button class="primary">进入在线编程</button></router-link>
        </template>

        <template v-else-if="chapter.contentType === 'algorithm_lab'">
          <p class="muted">本章节关联算法实验，点击进入实验室进行动画观察。</p>
          <router-link :to="chapter.contentValue || '/algorithm-lab'"><button class="primary">进入算法实验室</button></router-link>
        </template>

        <template v-else-if="chapter.contentType === 'question'">
          <p class="muted">本章节包含普通练习，点击进入后即可作答并查看解析。</p>
          <router-link :to="`/practice/${route.params.courseId}/${chapter.id}`"><button class="primary">进入普通练习</button></router-link>
        </template>

        <template v-else>
          <div class="article-box">{{ chapter.contentValue || "暂无可展示内容" }}</div>
        </template>
      </template>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import http from "../api";

const route = useRoute();
const course = ref(null);
const chapter = ref(null);
const chapters = ref([]);
const error = ref("");
const videoError = ref(false);

function getTypeLabel(type) {
  const map = {
    article: "文章",
    video: "视频",
    question: "普通练习",
    code_problem: "编程题",
    algorithm_lab: "算法实验"
  };
  return map[type] || type;
}

function isVideoFile(url) {
  if (!url) {
    return false;
  }
  const lower = url.toLowerCase();
  return lower.endsWith(".mp4")
    || lower.endsWith(".webm")
    || lower.endsWith(".ogg")
    || lower.endsWith(".mov")
    || lower.endsWith(".m4v")
    || lower.endsWith(".avi")
    || lower.endsWith(".mkv")
    || lower.endsWith(".wmv")
    || lower.endsWith(".flv");
}

function isEmbedVideo(url) {
  if (!url) {
    return false;
  }
  return url.includes("player.bilibili.com") || url.includes("youtube.com/embed");
}

function normalizeMediaUrl(url) {
  if (!url) {
    return "";
  }
  if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
    return url;
  }
  return url.startsWith("/") ? url : `/${url}`;
}

async function loadCourseAndChapters() {
  const courseId = route.params.courseId;
  course.value = await http.get(`/api/course/${courseId}`);
  chapters.value = await http.get("/api/chapter/list", { params: { courseId } });
}

async function loadCurrentChapter() {
  error.value = "";
  chapter.value = null;
  videoError.value = false;
  try {
    chapter.value = await http.get(`/api/chapter/${route.params.chapterId}`);
    await http.post("/api/progress/study", null, {
      params: {
        courseId: route.params.courseId,
        chapterId: route.params.chapterId
      }
    });
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(async () => {
  await loadCourseAndChapters();
  await loadCurrentChapter();
});

watch(
  () => route.params.chapterId,
  async () => {
    await loadCurrentChapter();
  }
);
</script>
