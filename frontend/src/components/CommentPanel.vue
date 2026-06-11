<template>
  <article class="card comment-panel">
    <div class="comment-head">
      <div>
        <p class="eyebrow">Comments</p>
        <h3 class="title">{{ title }}</h3>
      </div>
      <button class="secondary" @click="loadComments" :disabled="loading || !targetId">刷新评论</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <div class="comment-editor">
      <textarea
        v-model.trim="draft"
        rows="4"
        maxlength="1000"
        :placeholder="placeholder"
      ></textarea>
      <div class="row-actions">
        <button class="primary" @click="saveComment" :disabled="saving || !targetId">
          {{ editingId ? "保存修改" : "发布评论" }}
        </button>
        <button class="secondary" v-if="editingId" @click="cancelEdit">取消编辑</button>
      </div>
    </div>

    <div class="comment-list">
      <div class="comment-item" v-for="item in comments" :key="item.id">
        <div class="comment-avatar">
          <img v-if="item.avatarUrl" :src="item.avatarUrl" alt="avatar" />
          <span v-else>{{ getInitial(item) }}</span>
        </div>
        <div class="comment-main">
          <div class="comment-meta">
            <strong>{{ item.nickname || item.username || "匿名用户" }}</strong>
            <span class="muted">{{ formatTime(item.updateTime || item.createTime) }}</span>
          </div>
          <p class="comment-content">{{ item.content }}</p>
          <div class="row-actions" v-if="item.canEdit || item.canDelete">
            <button class="secondary" v-if="item.canEdit" @click="startEdit(item)">编辑</button>
            <button class="secondary danger" v-if="item.canDelete" @click="removeComment(item)">删除</button>
          </div>
        </div>
      </div>
      <p class="muted" v-if="!loading && !comments.length">{{ emptyText }}</p>
    </div>
  </article>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import http from "../api";

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], default: null },
  title: { type: String, default: "评论区" },
  placeholder: { type: String, default: "写下你的看法、问题或建议" },
  emptyText: { type: String, default: "还没有评论，来发表第一条吧。" }
});

const comments = ref([]);
const draft = ref("");
const editingId = ref(null);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const message = ref("");

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

function getInitial(item) {
  const base = item.nickname || item.username || "U";
  return String(base).slice(0, 1).toUpperCase();
}

function cancelEdit() {
  editingId.value = null;
  draft.value = "";
}

function startEdit(item) {
  editingId.value = item.id;
  draft.value = item.content || "";
}

async function loadComments() {
  if (!props.targetId) {
    comments.value = [];
    return;
  }
  error.value = "";
  loading.value = true;
  try {
    comments.value = await http.get("/api/comment/list", {
      params: {
        targetType: props.targetType,
        targetId: props.targetId
      }
    });
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function saveComment() {
  if (!props.targetId) {
    return;
  }
  error.value = "";
  message.value = "";
  saving.value = true;
  try {
    await http.post("/api/comment/save", {
      id: editingId.value,
      targetType: props.targetType,
      targetId: Number(props.targetId),
      content: draft.value
    });
    message.value = editingId.value ? "评论已更新" : "评论已发布";
    cancelEdit();
    await loadComments();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}

async function removeComment(item) {
  if (!window.confirm("确认删除这条评论吗？")) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/comment/${item.id}`);
    if (editingId.value === item.id) {
      cancelEdit();
    }
    message.value = "评论已删除";
    await loadComments();
  } catch (e) {
    error.value = e.message;
  }
}

watch(() => [props.targetType, props.targetId], () => {
  cancelEdit();
  loadComments();
});

onMounted(loadComments);
</script>
