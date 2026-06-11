<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Profile Center</p>
        <h1>个人中心</h1>
        <p class="muted">查看账号信息，维护昵称、头像、头像地址和个人简介。</p>
      </div>
      <button class="secondary" @click="loadProfile">刷新资料</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <div class="profile-layout">
      <article class="card">
        <div v-if="displayAvatarUrl">
          <img :src="displayAvatarUrl" alt="avatar" class="profile-avatar" />
        </div>
        <div v-else class="profile-cover">
          {{ initials }}
        </div>

        <h2 class="title" style="margin-top: 16px;">{{ profile.nickname || profile.username || "未登录用户" }}</h2>
        <p class="muted">@{{ profile.username || "-" }}</p>
        <p class="muted">{{ profile.bio || "这个人很低调，还没有写个人简介。" }}</p>

        <div class="profile-metrics">
          <div>
            <strong>{{ profile.role || "-" }}</strong>
            <span class="muted">账号角色</span>
          </div>
          <div>
            <strong>{{ profile.createTime ? formatDate(profile.createTime) : "-" }}</strong>
            <span class="muted">注册时间</span>
          </div>
        </div>
      </article>

      <article class="card">
        <h2 class="title">编辑资料</h2>
        <label class="form-label">昵称</label>
        <input v-model.trim="form.nickname" maxlength="128" placeholder="请输入展示昵称" />

        <label class="form-label">头像</label>
        <div class="upload-box profile-upload-box">
          <span>{{ uploading ? "头像上传中..." : "选择图片后自动上传" }}</span>
          <small class="muted">支持 jpg / jpeg / png / gif / webp，大小不超过 5MB</small>
          <input type="file" accept="image/*" :disabled="uploading" @change="uploadAvatar" />
        </div>
        <input v-model.trim="form.avatarUrl" maxlength="500" placeholder="也可以直接填写图片 URL" />

        <label class="form-label">个人简介</label>
        <textarea
          v-model.trim="form.bio"
          class="profile-bio"
          maxlength="500"
          rows="7"
          placeholder="介绍一下你的学习方向、兴趣或目标"
        ></textarea>

        <div class="row-actions">
          <button class="primary" @click="saveProfile">保存资料</button>
          <button class="secondary" @click="resetForm">恢复当前资料</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import http from "../api";
import { setAuth } from "../auth";

const error = ref("");
const message = ref("");
const uploading = ref(false);
const profile = reactive({
  id: null,
  username: "",
  nickname: "",
  avatarUrl: "",
  bio: "",
  role: "",
  createTime: ""
});
const form = reactive({
  nickname: "",
  avatarUrl: "",
  bio: ""
});

const initials = computed(() => {
  const base = profile.nickname || profile.username || "U";
  return String(base).slice(0, 1).toUpperCase();
});
const displayAvatarUrl = computed(() => form.avatarUrl || profile.avatarUrl || "");

function resetForm() {
  form.nickname = profile.nickname || "";
  form.avatarUrl = profile.avatarUrl || "";
  form.bio = profile.bio || "";
}

function applyProfile(data) {
  profile.id = data.id || null;
  profile.username = data.username || "";
  profile.nickname = data.nickname || "";
  profile.avatarUrl = data.avatarUrl || "";
  profile.bio = data.bio || "";
  profile.role = data.role || "";
  profile.createTime = data.createTime || "";
  resetForm();
}

function formatDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString();
}

async function loadProfile() {
  error.value = "";
  message.value = "";
  try {
    const data = await http.get("/api/user/profile");
    applyProfile(data);
    setAuth({
      ...JSON.parse(localStorage.getItem("currentUser") || "{}"),
      ...data
    });
  } catch (e) {
    error.value = e.message;
  }
}

async function saveProfile() {
  error.value = "";
  message.value = "";
  try {
    const data = await http.post("/api/user/profile/update", {
      nickname: form.nickname,
      avatarUrl: form.avatarUrl,
      bio: form.bio
    });
    applyProfile(data);
    setAuth({
      ...JSON.parse(localStorage.getItem("currentUser") || "{}"),
      ...data
    });
    message.value = "资料已保存";
  } catch (e) {
    error.value = e.message;
  }
}

async function uploadAvatar(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) {
    return;
  }
  error.value = "";
  message.value = "";
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    const uploadData = await http.post("/api/file/upload-avatar", formData, {
      headers: { "Content-Type": "multipart/form-data" }
    });
    const saved = await http.post("/api/user/profile/avatar", {
      avatarUrl: uploadData.url || ""
    });
    applyProfile(saved);
    setAuth({
      ...JSON.parse(localStorage.getItem("currentUser") || "{}"),
      ...saved
    });
    message.value = "头像已上传并自动保存";
  } catch (e) {
    error.value = e.message;
  } finally {
    uploading.value = false;
    event.target.value = "";
  }
}

onMounted(loadProfile);
</script>
