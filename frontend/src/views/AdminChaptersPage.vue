<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Chapters</p>
        <h1>章节管理</h1>
        <p class="muted">按课程维护章节顺序和内容类型，支持文章、视频、练习入口、编程入口和算法入口。</p>
      </div>
      <button class="secondary" @click="resetForm">{{ form.id ? "切换为新增章节" : "新增章节" }}</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <article class="guide-card">
      <h3>章节内容配置说明</h3>
      <p>章节不是让管理员手写页面地址。文章直接填写正文；视频可上传文件；普通练习会按当前章节自动进入练习页；编程题和算法实验通过下方选择器生成入口。</p>
      <div class="guide-steps">
        <span>文章：填写正文即可</span>
        <span>视频：上传 mp4/webm/mov 或填写视频链接</span>
        <span>普通练习：保存章节后，到普通题管理为该章节出题</span>
        <span>编程题/算法实验：选择已有编程题或算法类型</span>
      </div>
    </article>

    <div class="admin-editor">
      <article class="card">
        <h2 class="title">{{ form.id ? "编辑章节" : "新增章节" }}</h2>
        <select v-model.number="form.courseId" @change="loadChapters">
          <option :value="null">请选择课程</option>
          <option v-for="course in courses" :value="course.id" :key="course.id">{{ course.title }}</option>
        </select>
        <input v-model.trim="form.title" placeholder="章节标题" />
        <div class="form-grid">
          <select v-model="form.contentType" @change="handleContentTypeChange">
            <option value="article">文章</option>
            <option value="video">视频</option>
            <option value="question">普通练习入口</option>
            <option value="code_problem">编程题入口</option>
            <option value="algorithm_lab">算法实验入口</option>
          </select>
          <input v-model.number="form.sortOrder" type="number" placeholder="排序" />
        </div>
        <textarea
          v-if="form.contentType === 'article' || form.contentType === 'video'"
          v-model.trim="form.contentValue"
          rows="6"
          :placeholder="contentPlaceholder"
        ></textarea>
        <label class="upload-box" v-if="form.contentType === 'video'">
          <span>{{ uploadingFile ? "视频上传中..." : "上传章节视频" }}</span>
          <small class="muted">支持常见视频格式。系统会自动尝试转码为 mp4(h264+aac)，提高用户端播放兼容性。</small>
          <input type="file" accept="video/*,.mkv,.flv" :disabled="uploadingFile" @change="uploadChapterFile" />
        </label>
        <p v-if="form.contentType === 'video' && form.contentValue" :class="['compatibility-tip', `level-${videoCompatibility.level}`]">
          兼容等级：{{ videoCompatibility.label }}。{{ videoCompatibility.message }}
        </p>
        <div class="guide-card compact" v-if="form.contentType === 'question'">
          <h3>普通练习入口</h3>
          <p>不需要填写页面地址。保存该章节后，到“普通题管理”选择这门课程和该章节，录入单选题或判断题即可。用户会从课程详情自动进入 `/practice/课程ID/章节ID`。</p>
        </div>
        <div v-if="form.contentType === 'code_problem'">
          <select v-model="form.contentValue">
            <option value="">请选择编程题</option>
            <option v-for="problem in codeProblems" :key="problem.id" :value="`/code-practice?problem=${problem.id}`">
              {{ problem.title }}
            </option>
          </select>
          <p class="muted">编程题内容和测试用例请先在“编程题管理”中维护，这里只负责把章节关联到某一道题。</p>
        </div>
        <div v-if="form.contentType === 'algorithm_lab'">
          <select v-model="form.contentValue">
            <option v-for="item in algorithmOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <p class="muted">算法可视化为系统内置功能，不需要上传文件。这里选择章节要进入哪一个实验。</p>
        </div>
        <div class="row-actions">
          <button class="primary" @click="saveChapter">保存章节</button>
          <button class="secondary" @click="resetForm">{{ form.id ? "取消编辑" : "重置表单" }}</button>
        </div>
      </article>

      <article class="card">
        <h2 class="title">章节列表</h2>
        <div class="admin-list">
          <div class="admin-row course-row" v-for="chapter in chapters" :key="chapter.id">
            <div>
              <strong>{{ chapter.title }}</strong>
              <div class="muted">类型：{{ chapter.contentType }} · 排序：{{ chapter.sortOrder }}</div>
              <small>{{ chapter.contentValue || "暂无内容" }}</small>
            </div>
            <div class="row-actions">
              <button class="secondary" @click="editChapter(chapter)">编辑</button>
              <button class="secondary danger" @click="deleteChapter(chapter)">删除</button>
            </div>
          </div>
          <p class="muted" v-if="!chapters.length">请选择课程后查看章节。</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import http from "../api";

const courses = ref([]);
const chapters = ref([]);
const codeProblems = ref([]);
const error = ref("");
const message = ref("");
const uploadingFile = ref(false);
const form = reactive(getEmptyForm());
const algorithmOptions = [
  { label: "冒泡排序实验", value: "/algorithm-lab?algo=bubble" },
  { label: "选择排序实验", value: "/algorithm-lab?algo=selection" },
  { label: "插入排序实验", value: "/algorithm-lab?algo=insertion" },
  { label: "二分查找实验", value: "/algorithm-lab?algo=binary" },
  { label: "线性查找实验", value: "/algorithm-lab?algo=linear" }
];

function getEmptyForm() {
  return {
    id: null,
    courseId: null,
    title: "",
    contentType: "article",
    contentValue: "",
    sortOrder: 1
  };
}

const contentPlaceholder = computed(() => {
  if (form.contentType === "article") {
    return "输入文章正文或段落内容";
  }
  if (form.contentType === "video") {
    return "输入视频 URL，例如 https://example.com/demo.mp4";
  }
  return "";
});

const videoCompatibility = computed(() => getVideoCompatibility(form.contentValue));

function handleContentTypeChange() {
  if (form.contentType === "question") {
    form.contentValue = "";
  }
  if (form.contentType === "code_problem" && !String(form.contentValue || "").startsWith("/code-practice?problem=")) {
    form.contentValue = codeProblems.value.length ? `/code-practice?problem=${codeProblems.value[0].id}` : "";
  }
  if (form.contentType === "algorithm_lab" && !algorithmOptions.some((item) => item.value === form.contentValue)) {
    form.contentValue = algorithmOptions[0].value;
  }
}

function getVideoCompatibility(url) {
  const ext = getVideoExtension(url);
  if ([".mp4", ".m4v", ".webm"].includes(ext)) {
    return { level: "high", label: "高", message: "大多数浏览器可直接播放。" };
  }
  if ([".mov", ".ogg"].includes(ext)) {
    return { level: "medium", label: "中", message: "部分浏览器可播放，建议优先转为 mp4(h264)。" };
  }
  if ([".avi", ".mkv", ".wmv", ".flv"].includes(ext)) {
    return { level: "low", label: "低", message: "很多浏览器无法直接播放，建议转为 mp4(h264) 后再上传。" };
  }
  if (!ext) {
    return { level: "medium", label: "未知", message: "当前链接没有明确扩展名，请手动验证播放效果。" };
  }
  return { level: "medium", label: "未知", message: "浏览器兼容性不确定，建议优先使用 mp4(h264)。" };
}

function getVideoExtension(url) {
  if (!url) {
    return "";
  }
  const normalized = String(url).toLowerCase().split("?")[0].split("#")[0];
  const index = normalized.lastIndexOf(".");
  if (index < 0) {
    return "";
  }
  return normalized.substring(index);
}

function assignForm(data) {
  Object.assign(form, data);
}

function resetForm() {
  const currentCourseId = form.courseId;
  assignForm({ ...getEmptyForm(), courseId: currentCourseId });
}

async function loadCourses() {
  courses.value = await http.get("/api/course/admin/list");
  if (!form.courseId && courses.value.length) {
    form.courseId = courses.value[0].id;
  }
}

async function loadCodeProblems() {
  codeProblems.value = await http.get("/api/code-problem/admin/list");
}

async function loadChapters() {
  chapters.value = [];
  if (!form.courseId) {
    return;
  }
  chapters.value = await http.get("/api/chapter/list", {
    params: { courseId: form.courseId }
  });
}

async function saveChapter() {
  error.value = "";
  message.value = "";
  if (!form.courseId) {
    error.value = "请先选择课程";
    return;
  }
  handleContentTypeChange();
  if (form.contentType === "code_problem" && !form.contentValue) {
    error.value = "请先在编程题管理中创建题目，再关联到章节";
    return;
  }
  try {
    await http.post("/api/chapter/admin/save", form);
    message.value = form.id ? "章节已更新" : "章节已新增";
    await loadChapters();
    resetForm();
  } catch (e) {
    error.value = e.message;
  }
}

async function uploadChapterFile(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) {
    return;
  }
  error.value = "";
  message.value = "";
  uploadingFile.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    const data = await http.post("/api/file/upload", formData);
    form.contentValue = data.url;
    message.value = data.note ? `章节视频已上传，${data.note}` : "章节视频已上传";
  } catch (e) {
    error.value = e.message;
  } finally {
    uploadingFile.value = false;
    event.target.value = "";
  }
}

function editChapter(chapter) {
  assignForm({ ...chapter });
}

async function deleteChapter(chapter) {
  if (!window.confirm(`确认删除章节「${chapter.title}」吗？`)) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/chapter/admin/${chapter.id}`);
    message.value = "章节已删除";
    await loadChapters();
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(async () => {
  await loadCourses();
  await loadCodeProblems();
  await loadChapters();
});
</script>
