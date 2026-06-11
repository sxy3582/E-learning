<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Questions</p>
        <h1>普通题管理</h1>
        <p class="muted">维护单选和判断题，支持章节维度管理。</p>
      </div>
      <button class="secondary" @click="resetForm">{{ form.id ? "切换为新增题目" : "新增题目" }}</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <article class="guide-card">
      <h3>普通练习维护流程</h3>
      <p>普通题绑定到“练习类型”的章节。用户进入该章节练习后必须一次性完成全部题目，提交后系统自动判分并写入学习进度。</p>
      <div class="guide-steps">
        <span>1. 先在章节管理中新建“普通练习入口”章节</span>
        <span>2. 在这里选择课程和章节</span>
        <span>3. 单选题只需勾选正确选项，判断题直接点选“正确/错误”</span>
        <span>4. 保存后用户即可从课程详情进入练习</span>
      </div>
    </article>

    <div class="admin-editor">
      <article class="card">
        <h2 class="title">{{ form.id ? "编辑题目" : "新增题目" }}</h2>
        <div class="form-grid">
          <select v-model.number="selectedCourseId" @change="handleCourseChange">
            <option :value="null">选择课程</option>
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
          <select v-model.number="form.chapterId" @change="loadQuestions">
            <option :value="null">选择章节</option>
            <option v-for="chapter in chapters" :key="chapter.id" :value="chapter.id">{{ chapter.title }}</option>
          </select>
        </div>

        <div class="form-grid">
          <select v-model="form.type" @change="handleTypeChange">
            <option value="SINGLE">单选题</option>
            <option value="JUDGE">判断题</option>
          </select>
          <input v-model.number="form.sortOrder" type="number" placeholder="排序" />
        </div>

        <textarea v-model.trim="form.stem" rows="3" placeholder="题干"></textarea>
        <div v-if="form.type === 'SINGLE'" class="judge-answer-box">
          <p class="muted">单选题答案自动取“正确选项”，无需单独填写参考答案。</p>
          <p class="eyebrow">当前答案：{{ getSingleReference(form.options) }}</p>
        </div>
        <div v-else class="judge-answer-box">
          <p class="muted">判断题请选择标准答案：</p>
          <div class="judge-option-row">
            <label class="option-item judge-choice">
              <input type="radio" value="TRUE" v-model="form.referenceAnswer" />
              <span>正确</span>
            </label>
            <label class="option-item judge-choice">
              <input type="radio" value="FALSE" v-model="form.referenceAnswer" />
              <span>错误</span>
            </label>
          </div>
        </div>
        <textarea v-model.trim="form.analysis" rows="2" placeholder="题目解析"></textarea>

        <div v-if="form.type === 'SINGLE'" class="option-list">
          <div class="option-edit-row" v-for="(opt, idx) in form.options" :key="idx">
            <input v-model.trim="opt.optionKey" placeholder="选项标识，如 A" />
            <input v-model.trim="opt.optionContent" placeholder="选项内容" />
            <label class="option-item">
              <input type="radio" :name="'correct-option'" :checked="opt.isCorrect === 1" @change="setCorrect(idx)" />
              <span>正确答案</span>
            </label>
          </div>
          <div class="row-actions">
            <button class="secondary" @click="addOption">添加选项</button>
            <button class="secondary" @click="removeOption" :disabled="form.options.length <= 2">移除选项</button>
          </div>
        </div>

        <div class="row-actions">
          <button class="primary" @click="saveQuestion">保存题目</button>
          <button class="secondary" @click="resetForm">{{ form.id ? "取消编辑" : "重置表单" }}</button>
        </div>
      </article>

      <article class="card">
        <h2 class="title">题目列表</h2>
        <div class="admin-list">
          <div class="admin-row course-row" v-for="q in questions" :key="q.id">
            <div>
              <strong>[{{ q.type }}] {{ q.stem }}</strong>
              <small>答案：{{ formatReferenceAnswer(q) }}</small>
            </div>
            <div class="row-actions">
              <button class="secondary" @click="editQuestion(q)">编辑</button>
              <button class="secondary danger" @click="deleteQuestion(q)">删除</button>
            </div>
          </div>
          <p class="muted" v-if="!questions.length">{{ form.chapterId ? "当前章节暂无题目。" : "请选择章节后查看题目。" }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import http from "../api";

const courses = ref([]);
const chapters = ref([]);
const questions = ref([]);
const selectedCourseId = ref(null);
const error = ref("");
const message = ref("");
const form = reactive(getEmptyForm());

function getDefaultOptions() {
  return [
    { optionKey: "A", optionContent: "", isCorrect: 1, sortOrder: 1 },
    { optionKey: "B", optionContent: "", isCorrect: 0, sortOrder: 2 }
  ];
}

function getEmptyForm() {
  return {
    id: null,
    chapterId: null,
    type: "SINGLE",
    stem: "",
    analysis: "",
    referenceAnswer: "",
    sortOrder: 1,
    options: getDefaultOptions()
  };
}

function assignForm(data) {
  Object.assign(form, data);
}

function resetForm() {
  assignForm({
    ...getEmptyForm(),
    chapterId: form.chapterId
  });
  handleTypeChange();
}

function addOption() {
  form.options.push({
    optionKey: String.fromCharCode(65 + form.options.length),
    optionContent: "",
    isCorrect: 0,
    sortOrder: form.options.length + 1
  });
}

function removeOption() {
  if (form.options.length <= 2) {
    return;
  }
  form.options.pop();
}

function setCorrect(index) {
  form.options.forEach((item, idx) => {
    item.isCorrect = idx === index ? 1 : 0;
  });
}

function handleTypeChange() {
  if (form.type === "SINGLE") {
    if (!form.options || form.options.length < 2) {
      form.options = getDefaultOptions();
      return;
    }
    if (!form.options.some((item) => Number(item.isCorrect) === 1)) {
      form.options[0].isCorrect = 1;
    }
    return;
  }
  form.options = [];
  form.referenceAnswer = normalizeJudgeValue(form.referenceAnswer) || "TRUE";
}

function normalizeJudgeValue(value) {
  const v = String(value || "").trim().toUpperCase();
  if (["TRUE", "正确", "对", "T"].includes(v)) {
    return "TRUE";
  }
  if (["FALSE", "错误", "错", "F"].includes(v)) {
    return "FALSE";
  }
  return "";
}

function getSingleReference(options) {
  const current = (options || []).find((item) => Number(item.isCorrect) === 1);
  return current?.optionKey || "-";
}

function normalizeSingleOptions(options = [], referenceAnswer = "") {
  const result = options.map((item, idx) => ({
    optionKey: item.optionKey || String.fromCharCode(65 + idx),
    optionContent: item.optionContent || "",
    isCorrect: Number(item.isCorrect) === 1 ? 1 : 0,
    sortOrder: item.sortOrder || idx + 1
  }));
  if (result.length < 2) {
    return getDefaultOptions();
  }
  if (!result.some((item) => item.isCorrect === 1)) {
    const reference = String(referenceAnswer || "").trim().toUpperCase();
    const matchIndex = result.findIndex((item) => String(item.optionKey || "").trim().toUpperCase() === reference);
    result[matchIndex >= 0 ? matchIndex : 0].isCorrect = 1;
  }
  return result;
}

function formatReferenceAnswer(question) {
  if (!question) {
    return "-";
  }
  if (question.type === "JUDGE") {
    return normalizeJudgeValue(question.referenceAnswer) === "TRUE" ? "正确" : "错误";
  }
  const key = String(question.referenceAnswer || "").trim();
  const option = (question.options || []).find((item) => String(item.optionKey || "").trim().toUpperCase() === key.toUpperCase());
  if (!key) {
    return "-";
  }
  return option ? `${key}（${option.optionContent}）` : key;
}

async function loadCourses() {
  courses.value = await http.get("/api/course/admin/list");
  if (!selectedCourseId.value && courses.value.length) {
    selectedCourseId.value = courses.value[0].id;
  }
}

async function handleCourseChange() {
  form.chapterId = null;
  questions.value = [];
  await loadChapters();
}

async function loadChapters() {
  if (!selectedCourseId.value) {
    chapters.value = [];
    return;
  }
  chapters.value = await http.get("/api/chapter/list", {
    params: { courseId: selectedCourseId.value }
  });
  if (!form.chapterId && chapters.value.length) {
    form.chapterId = chapters.value[0].id;
  }
  await loadQuestions();
}

async function loadQuestions() {
  questions.value = [];
  if (!form.chapterId) {
    return;
  }
  questions.value = await http.get("/api/question/admin/list", {
    params: { chapterId: form.chapterId }
  });
}

async function saveQuestion() {
  error.value = "";
  message.value = "";
  if (!form.chapterId) {
    error.value = "请先选择章节";
    return;
  }
  try {
    const payload = {
      ...form,
      referenceAnswer: form.type === "JUDGE" ? normalizeJudgeValue(form.referenceAnswer) : "",
      options: form.type === "SINGLE" ? form.options : []
    };
    await http.post("/api/question/admin/save", payload);
    message.value = form.id ? "题目已更新" : "题目已新增";
    await loadQuestions();
    resetForm();
  } catch (e) {
    error.value = e.message;
  }
}

function editQuestion(question) {
  assignForm({
    id: question.id,
    chapterId: question.chapterId,
    type: question.type,
    stem: question.stem,
    analysis: question.analysis,
    referenceAnswer: question.referenceAnswer,
    sortOrder: question.sortOrder,
    options: normalizeSingleOptions(question.options, question.referenceAnswer)
  });
  if (form.type === "JUDGE") {
    form.options = [];
    form.referenceAnswer = normalizeJudgeValue(question.referenceAnswer) || "TRUE";
  }
}

async function deleteQuestion(question) {
  if (!window.confirm("确认删除该题目吗？")) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/question/admin/${question.id}`);
    message.value = "题目已删除";
    await loadQuestions();
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(async () => {
  await loadCourses();
  await loadChapters();
  handleTypeChange();
});
</script>
