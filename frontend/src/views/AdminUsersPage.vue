<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Users</p>
        <h1>用户管理</h1>
        <p class="muted">管理普通用户的账号状态，必要时重置用户密码。</p>
      </div>
      <button class="secondary" @click="loadUsers">刷新列表</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <div class="data-panel">
      <div class="table-row table-head">
        <span>用户</span>
        <span>角色</span>
        <span>状态</span>
        <span>创建时间</span>
        <span>操作</span>
      </div>
      <div class="table-row" v-for="user in users" :key="user.id">
        <span>
          <strong>{{ user.username }}</strong>
          <small>{{ user.nickname || "未设置昵称" }}</small>
        </span>
        <span>{{ user.role }}</span>
        <span>
          <i class="status-pill" :class="{ 'muted-pill': user.banned }">
            {{ user.banned ? "已封禁" : "正常" }}
          </i>
        </span>
        <span>{{ user.createTime || "-" }}</span>
        <span class="row-actions">
          <button class="secondary" :disabled="user.role === 'ADMIN'" @click="toggleBan(user)">
            {{ user.banned ? "解封" : "封禁" }}
          </button>
          <button class="secondary" @click="resetPassword(user)">重置密码</button>
        </span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import http from "../api";

const users = ref([]);
const error = ref("");
const message = ref("");

async function loadUsers() {
  users.value = await http.get("/api/admin/user/list");
}

async function toggleBan(user) {
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/admin/user/ban", {
      userId: user.id,
      banned: user.banned ? 0 : 1
    });
    message.value = user.banned ? "用户已解封" : "用户已封禁";
    await loadUsers();
  } catch (e) {
    error.value = e.message;
  }
}

async function resetPassword(user) {
  const newPassword = window.prompt(`请输入 ${user.username} 的新密码（至少 6 位）`, "123456");
  if (!newPassword) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/admin/user/reset-password", {
      userId: user.id,
      newPassword
    });
    message.value = "密码已重置";
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(loadUsers);
</script>
