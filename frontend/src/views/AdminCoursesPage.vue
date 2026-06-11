<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Courses</p>
        <h1>课程管理</h1>
        <p class="muted">维护课程基础信息、分类、难度、排序和发布状态。</p>
      </div>
      <button class="secondary" @click="resetForm">{{ form.id ? "切换为新增课程" : "新增课程" }}</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <article class="guide-card">
      <h3>课程维护流程</h3>
      <p>先创建课程基础信息，再到“章节管理”为课程添加文章、视频、练习、编程题或算法实验入口。课程发布后，普通用户才能在课程页看到。</p>
      <div class="guide-steps">
        <span>1. 填写课程名称、简介、分类和难度</span>
        <span>2. 可上传封面图，系统自动生成可访问地址</span>
        <span>3. 保存课程后进入章节管理补充学习内容</span>
        <span>4. 确认内容完整后设置为发布</span>
      </div>
    </article>

    <div class="admin-editor">
      <article class="card">
        <h2 class="title">{{ form.id ? "编辑课程" : "新增课程" }}</h2>
        <input v-model.trim="form.title" placeholder="课程名称" />
        <textarea v-model.trim="form.intro" rows="3" placeholder="课程简介"></textarea>
        <input v-model.trim="form.coverUrl" placeholder="封面图 URL（可选）" />
        <label class="upload-box">
          <span>{{ uploadingCover ? "封面上传中..." : "上传课程封面" }}</span>
          <small class="muted">支持 jpg、png、webp、gif，上传后会自动填入封面地址。</small>
          <input type="file" accept="image/*" :disabled="uploadingCover" @change="uploadCourseCover" />
        </label>
        <div class="form-grid">
          <input v-model.trim="form.category" placeholder="分类，例如：后端开发" />
          <select v-model="form.difficulty">
            <option value="初级">初级</option>
            <option value="中级">中级</option>
            <option value="高级">高级</option>
          </select>
          <input v-model.number="form.sortOrder" type="number" placeholder="排序" />
          <select v-model.number="form.published">
            <option :value="1">发布</option>
            <option :value="0">下架</option>
          </select>
        </div>
        <div class="row-actions">
          <button class="primary" @click="saveCourse">保存课程</button>
          <button class="secondary" @click="resetForm">{{ form.id ? "取消编辑" : "重置表单" }}</button>
        </div>
      </article>

      <article class="card">
        <h2 class="title">课程列表</h2>
        <div class="admin-list">
          <div class="admin-row course-row" v-for="course in courses" :key="course.id">
            <div class="course-row-main">
              <div class="course-cover-mini">
                <img
                  v-if="course.coverUrl"
                  class="course-cover-mini-img"
                  :src="normalizeCoverUrl(course.coverUrl)"
                  :alt="`${course.title}封面`"
                />
                <div v-else class="course-cover-mini-placeholder">{{ course.category || "Course" }}</div>
              </div>
              <div>
                <strong>{{ course.title }}</strong>
                <div class="muted">{{ course.category || "未分类" }} · {{ course.difficulty || "未设置" }}</div>
                <small>{{ course.intro || "暂无简介" }}</small>
              </div>
            </div>
            <div class="row-actions">
              <span class="status-pill" :class="{ 'muted-pill': !course.published }">
                {{ course.published ? "已发布" : "未发布" }}
              </span>
              <button class="secondary" @click="editCourse(course)">编辑</button>
              <button class="secondary" @click="togglePublish(course)">
                {{ course.published ? "下架" : "发布" }}
              </button>
              <button class="secondary danger" @click="deleteCourse(course)">删除</button>
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import http from "../api";

const courses = ref([]);
const error = ref("");
const message = ref("");
const uploadingCover = ref(false);
const form = reactive(getEmptyForm());

function getEmptyForm() {
  return {
    id: null,
    title: "",
    intro: "",
    coverUrl: "",
    difficulty: "初级",
    category: "",
    published: 1,
    sortOrder: 1
  };
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

function assignForm(data) {
  Object.assign(form, data);
}

function resetForm() {
  assignForm(getEmptyForm());
}

async function loadCourses() {
  courses.value = await http.get("/api/course/admin/list");
}

async function saveCourse() {
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/course/admin/save", form);
    message.value = form.id ? "课程已更新" : "课程已新增";
    resetForm();
    await loadCourses();
  } catch (e) {
    error.value = e.message;
  }
}

async function uploadCourseCover(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) {
    return;
  }
  error.value = "";
  message.value = "";
  uploadingCover.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    const data = await http.post("/api/file/upload", formData);
    form.coverUrl = data.url;
    message.value = "封面已上传";
  } catch (e) {
    error.value = e.message;
  } finally {
    uploadingCover.value = false;
    event.target.value = "";
  }
}

function editCourse(course) {
  assignForm({ ...getEmptyForm(), ...course });
}

async function togglePublish(course) {
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/course/admin/save", {
      ...course,
      published: course.published ? 0 : 1
    });
    message.value = course.published ? "课程已下架" : "课程已发布";
    await loadCourses();
  } catch (e) {
    error.value = e.message;
  }
}

async function deleteCourse(course) {
  if (!window.confirm(`确认删除课程「${course.title}」吗？`)) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/course/admin/${course.id}`);
    message.value = "课程已删除";
    await loadCourses();
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(loadCourses);
</script>
