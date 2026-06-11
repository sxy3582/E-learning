<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Comments</p>
        <h1>评论管理</h1>
        <p class="muted">集中查看课程、普通题和编程题评论，必要时删除不当内容。</p>
      </div>
      <button class="secondary" @click="loadComments">刷新列表</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <div class="form-grid">
      <select v-model="filters.targetType">
        <option value="">全部类型</option>
        <option value="COURSE">课程</option>
        <option value="QUESTION">普通题</option>
        <option value="CODE_PROBLEM">编程题</option>
      </select>
      <input v-model.trim="filters.targetId" placeholder="目标 ID，可选" />
    </div>
    <div class="row-actions" style="margin-bottom: 12px;">
      <button class="primary" @click="loadComments">筛选评论</button>
      <button class="secondary" @click="resetFilters">清空筛选</button>
    </div>

    <div class="admin-list">
      <article class="card comment-admin-card" v-for="item in comments" :key="item.id">
        <div class="comment-meta">
          <strong>{{ item.nickname || item.username || "匿名用户" }}</strong>
          <span class="muted">{{ item.targetType }} #{{ item.targetId }}</span>
          <span class="muted">{{ formatTime(item.updateTime || item.createTime) }}</span>
        </div>
        <p class="comment-content">{{ item.content }}</p>
        <div class="row-actions">
          <button class="secondary danger" @click="removeComment(item)">删除评论</button>
        </div>
      </article>
      <p class="muted" v-if="!comments.length">当前没有符合条件的评论。</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import http from "../api";

const comments = ref([]);
const error = ref("");
const message = ref("");
const filters = reactive({
  targetType: "",
  targetId: ""
});

function formatTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

async function loadComments() {
  error.value = "";
  try {
    comments.value = await http.get("/api/comment/admin/list", {
      params: {
        targetType: filters.targetType || undefined,
        targetId: filters.targetId ? Number(filters.targetId) : undefined
      }
    });
  } catch (e) {
    error.value = e.message;
  }
}

function resetFilters() {
  filters.targetType = "";
  filters.targetId = "";
  loadComments();
}

async function removeComment(item) {
  if (!window.confirm("确认删除这条评论吗？")) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/comment/${item.id}`);
    message.value = "评论已删除";
    await loadComments();
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(loadComments);
</script>
