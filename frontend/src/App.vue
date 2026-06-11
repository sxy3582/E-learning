<template>
  <div class="app-shell">
    <header class="top-nav" v-if="!isAuthPage && !isAdminPage">
      <router-link :to="homePath" class="brand">
        <span class="brand-mark">JP</span>
        <span>Java 编程实训平台</span>
      </router-link>
      <nav class="menu">
        <router-link to="/">学习首页</router-link>
        <router-link to="/courses">课程</router-link>
        <router-link to="/my-progress">学习进度</router-link>
        <router-link to="/wrong-book">错题单</router-link>
        <router-link to="/code-practice">在线编程</router-link>
        <router-link to="/algorithm-lab">算法实验室</router-link>
        <router-link to="/login" v-if="!user">登录</router-link>
        <router-link to="/register" v-if="!user">注册</router-link>
        <router-link v-if="user" to="/my-profile" class="user-chip user-chip-link">
          <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="avatar" class="nav-avatar" />
          <span>{{ user.nickname || user.username }} · 个人中心</span>
        </router-link>
        <button v-if="user" class="link-btn" @click="logout">退出</button>
      </nav>
    </header>

    <div v-if="isAdminPage" class="admin-shell">
      <aside class="admin-sidebar">
        <router-link :to="homePath" class="brand admin-brand">
          <span class="brand-mark">JP</span>
          <span>Java 编程实训平台</span>
        </router-link>
        <div class="admin-user">{{ user.nickname || user.username }}</div>
        <nav class="admin-menu">
          <router-link to="/admin">管理总览</router-link>
          <router-link to="/admin/users">用户管理</router-link>
          <router-link to="/admin/courses">课程管理</router-link>
          <router-link to="/admin/chapters">章节管理</router-link>
          <router-link to="/admin/questions">普通题管理</router-link>
          <router-link to="/admin/code-problems">编程题管理</router-link>
          <router-link to="/admin/comments">评论管理</router-link>
          <router-link to="/admin/progress">学习进度</router-link>
        </nav>
        <button class="link-btn admin-logout" @click="logout">退出</button>
      </aside>
      <main class="page admin-page">
        <router-view @auth-changed="syncUser" />
      </main>
    </div>

    <main v-else :class="isAuthPage ? 'auth-page-wrap' : 'page'">
      <router-view @auth-changed="syncUser" />
    </main>
    <AiAgentWidget v-if="!isAuthPage" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AiAgentWidget from "./components/AiAgentWidget.vue";
import http from "./api";
import { clearAuth, getCurrentUser } from "./auth";

const router = useRouter();
const route = useRoute();
const user = ref(getCurrentUser());
const isAuthPage = computed(() => route.path === "/login" || route.path === "/register");
const isAdminPage = computed(() => !!user.value && user.value.role === "ADMIN" && route.path.startsWith("/admin"));
const homePath = computed(() => (user.value && user.value.role === "ADMIN" ? "/admin" : "/"));

function syncUser() {
  user.value = getCurrentUser();
}

async function validateSession() {
  if (isAuthPage.value || !user.value) {
    return;
  }
  try {
    await http.get("/api/auth/me");
  } catch (error) {
    // api 拦截器会统一清理本地登录态并跳转登录页
  }
}

async function logout() {
  try {
    await http.post("/api/auth/logout");
  } catch (error) {
    // token 过期时也直接清理本地状态
  } finally {
    clearAuth();
    syncUser();
    router.push("/login");
  }
}

onMounted(validateSession);
watch(() => route.fullPath, validateSession);
</script>
