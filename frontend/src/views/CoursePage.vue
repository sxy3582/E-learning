<template>
  <section>
    <div class="hero-board">
      <article class="hero-card">
        <p class="eyebrow">Course Library</p>
        <h1>选择一门课程，进入章节学习。</h1>
        <p class="muted" style="color: #d9cbb8;">课程模块作为学习闭环入口，后续会连接练习、编程题和进度记录。</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Published</p>
        <div class="metric-number">{{ list.length }}</div>
        <p class="muted">已发布课程</p>
      </article>
    </div>
    <div class="grid grid-2">
      <article class="card feature-card" v-for="course in list" :key="course.id">
        <div class="course-cover-box">
          <img
            v-if="course.coverUrl"
            class="course-cover-img"
            :src="normalizeCoverUrl(course.coverUrl)"
            :alt="`${course.title}封面`"
          />
          <div v-else class="course-cover-placeholder">
            <span>{{ course.category || "Course" }}</span>
            <strong>{{ course.title }}</strong>
          </div>
        </div>
        <p class="eyebrow">{{ course.category || "Course" }}</p>
        <h3>{{ course.title }}</h3>
        <p class="muted">{{ course.intro || "暂无简介" }}</p>
        <span class="status-pill">{{ course.difficulty || "未设置" }}</span>
        <router-link :to="`/course/${course.id}`">
          <button class="secondary" style="margin-top: 16px;">进入学习</button>
        </router-link>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import http from "../api";

const list = ref([]);

function normalizeCoverUrl(url) {
  if (!url) {
    return "";
  }
  if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
    return url;
  }
  return url.startsWith("/") ? url : `/${url}`;
}

onMounted(async () => {
  list.value = await http.get("/api/course/list");
});
</script>
