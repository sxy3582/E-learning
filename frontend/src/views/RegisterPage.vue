<template>
  <section class="auth-scene">
    <div class="auth-visual">
      <p class="eyebrow">Start Learning</p>
      <h1 class="auth-hero-title">建立属于你的学习节奏。</h1>
      <p class="auth-hero-copy">
        创建账号后，你可以从课程目录进入章节内容，完成基础练习，继续挑战 Java 编程题和算法实验。
      </p>
      <div class="auth-bento">
        <div class="auth-tile accent wide">
          <strong>学习路径</strong>
          <p>课程、章节、练习和实验按照学习顺序展开。</p>
        </div>
        <div class="auth-tile dark">
          <strong>Progress</strong>
          <pre class="code-snippet">chapter.done()
practice.submit()
code.accepted()</pre>
        </div>
        <div class="auth-tile">
          <strong>随时继续</strong>
          <p>个人中心会逐步记录课程进度、练习完成和编程通过情况。</p>
        </div>
      </div>
    </div>

    <div class="auth-panel-wrap">
      <div class="auth-panel">
        <p class="eyebrow">Create Account</p>
        <h2>创建学习账号</h2>
        <p class="muted">注册后自动进入普通用户身份，管理员账号由系统初始化。</p>
        <p class="error-text" v-if="error">{{ error }}</p>
        <label class="form-label">用户名</label>
        <input v-model.trim="form.username" placeholder="3-32 位用户名" />
        <label class="form-label">昵称</label>
        <input v-model.trim="form.nickname" placeholder="可选，用于页面展示" />
        <label class="form-label">密码</label>
        <input v-model.trim="form.password" type="password" placeholder="至少 6 位" @keyup.enter="submit" />
        <button class="primary full-button" @click="submit">创建账号</button>
        <p class="auth-switch">已有账号？ <router-link to="/login">返回登录</router-link></p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import http from "../api";

const router = useRouter();
const error = ref("");
const form = reactive({
  username: "",
  nickname: "",
  password: ""
});

async function submit() {
  error.value = "";
  try {
    await http.post("/api/auth/register", form);
    router.push("/login");
  } catch (e) {
    error.value = e.message;
  }
}
</script>
