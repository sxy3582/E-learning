<template>
  <section class="auth-scene">
    <div class="auth-visual">
      <p class="eyebrow">E-Learning Lab</p>
      <h1 class="auth-hero-title">让每一次学习都有清晰路径。</h1>
      <p class="auth-hero-copy">
        从课程章节到练习反馈，从 Java 编程到算法动画，平台会把知识点、动手实践和学习进度串联起来。
        登录后继续你的学习节奏。
      </p>
      <div class="auth-bento">
        <div class="auth-tile dark wide">
          <strong>今日练习</strong>
          <pre class="code-snippet">public int fastPow(int a, int n) {
    int ans = 1;
    while (n > 0) {
        if ((n & 1) == 1) ans *= a;
        a *= a;
        n >>= 1;
    }
    return ans;
}</pre>
        </div>
        <div class="auth-tile">
          <strong>算法实验</strong>
          <div class="node-line">
            <i class="node"></i>
            <span></span>
            <i class="node"></i>
            <span></span>
            <i class="node"></i>
          </div>
        </div>
        <div class="auth-tile accent">
          <strong>学习路径</strong>
          <p>课程、章节、练习、编程题和进度记录依次连接。</p>
        </div>
      </div>
    </div>

    <div class="auth-panel-wrap">
      <div class="auth-panel">
        <p class="eyebrow">Welcome Back</p>
        <h2>欢迎回来</h2>
        <p class="muted">普通用户进入学习中心，管理员进入内容管理中心。</p>
        <p class="error-text" v-if="error">{{ error }}</p>
        <label class="form-label">用户名</label>
        <input v-model.trim="form.username" placeholder="请输入用户名" @keyup.enter="submit" />
        <label class="form-label">密码</label>
        <input v-model.trim="form.password" type="password" placeholder="请输入密码" @keyup.enter="submit" />
        <button class="primary full-button" @click="submit">进入平台</button>
        <p class="auth-switch">还没有账号？ <router-link to="/register">创建普通用户账号</router-link></p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "../api";
import { setAuth } from "../auth";

const emit = defineEmits(["auth-changed"]);
const router = useRouter();
const route = useRoute();
const error = ref("");
const form = reactive({
  username: "",
  password: ""
});

async function submit() {
  error.value = "";
  try {
    const data = await http.post("/api/auth/login", form);
    setAuth(data);
    emit("auth-changed");
    const redirect = route.query.redirect || (data.role === "ADMIN" ? "/admin" : "/");
    router.push(redirect);
  } catch (e) {
    error.value = e.message;
  }
}
</script>
