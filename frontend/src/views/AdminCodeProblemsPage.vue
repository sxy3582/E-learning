<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Code Problems</p>
        <h1>编程题管理</h1>
        <p class="muted">维护编程题描述、方法名、支持语言、三套模板代码与测试用例。</p>
      </div>
      <button class="secondary" @click="resetForm">{{ form.id ? "切换为新增题目" : "新增题目" }}</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <p class="success-text" v-if="message">{{ message }}</p>

    <article class="guide-card">
      <h3>编程题维护流程</h3>
      <p>编程题由题目、方法名、支持语言、多语言模板代码和测试用例组成。章节管理只负责把某个章节关联到这里维护好的编程题，用户提交代码后会按测试用例自动判题。</p>
      <div class="guide-steps">
        <span>1. 创建题目并填写用户需要实现的方法名</span>
        <span>2. 至少填写 Java 模板，可按需补充 Python 与 C++ 模板</span>
        <span>3. 模板代码需与题目方法名一致，类名统一使用 Solution</span>
        <span>4. 回到章节管理，把编程题章节关联到对应题目</span>
      </div>
    </article>

    <div class="admin-editor">
      <article class="card">
        <h2 class="title">{{ form.id ? "编辑编程题" : "新增编程题" }}</h2>
        <input v-model.trim="form.title" placeholder="题目名称" />
        <textarea v-model.trim="form.description" rows="4" placeholder="题目描述"></textarea>
        <div class="form-grid">
          <input v-model.trim="form.methodName" placeholder="方法名，例如 add" />
          <input v-model.number="form.sortOrder" type="number" placeholder="排序" />
        </div>
        <div class="language-check-group">
          <label v-for="lang in languageOptions" :key="lang.value" class="check-chip">
            <input type="checkbox" :value="lang.value" v-model="form.supportedLanguageList" />
            <span>{{ lang.label }}</span>
          </label>
        </div>
        <label class="form-label">Java 模板</label>
        <textarea v-model="form.templateCodeJava" rows="8" placeholder="Java 模板代码"></textarea>
        <label class="form-label">Python 模板</label>
        <textarea v-model="form.templateCodePython" rows="8" placeholder="Python 模板代码"></textarea>
        <label class="form-label">C++ 模板</label>
        <textarea v-model="form.templateCodeCpp" rows="8" placeholder="C++ 模板代码"></textarea>
        <div class="row-actions">
          <button class="primary" @click="saveProblem">保存题目</button>
          <button class="secondary" @click="resetForm">{{ form.id ? "取消编辑" : "重置表单" }}</button>
        </div>
      </article>

      <article class="card">
        <h2 class="title">题目列表</h2>
        <div class="admin-list">
          <div class="admin-row course-row" v-for="item in problems" :key="item.id">
            <div>
              <strong>{{ item.title }}</strong>
              <small>方法：{{ item.methodName }}</small>
              <small>语言：{{ formatSupportedLanguages(item.supportedLanguages) }}</small>
            </div>
            <div class="row-actions">
              <button class="secondary" @click="editProblem(item)">编辑</button>
              <button class="secondary" @click="openCases(item)">测试用例</button>
              <button class="secondary danger" @click="deleteProblem(item)">删除</button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <article class="card" v-if="currentProblem">
      <h2 class="title">测试用例：{{ currentProblem.title }}</h2>
      <div class="form-grid">
        <input v-model.trim="caseForm.inputJson" placeholder="输入 JSON，如 [1,2]" />
        <input v-model.trim="caseForm.expectedOutput" placeholder="期望输出，如 3" />
      </div>
      <div class="form-grid">
        <select v-model.number="caseForm.isSample">
          <option :value="1">样例</option>
          <option :value="0">普通</option>
        </select>
        <input v-model.number="caseForm.sortOrder" type="number" placeholder="排序" />
      </div>
      <div class="row-actions">
        <button class="primary" @click="saveCase">保存用例</button>
        <button class="secondary" @click="resetCaseForm">{{ caseForm.id ? "取消编辑" : "重置" }}</button>
      </div>
      <div class="admin-list" style="margin-top: 14px;">
        <div class="admin-row" v-for="c in cases" :key="c.id">
          <div>
            <strong>{{ c.inputJson }} => {{ c.expectedOutput }}</strong>
            <small>{{ c.isSample ? "样例" : "普通" }}</small>
          </div>
          <div class="row-actions">
            <button class="secondary" @click="editCase(c)">编辑</button>
            <button class="secondary danger" @click="deleteCase(c)">删除</button>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import http from "../api";

const problems = ref([]);
const cases = ref([]);
const currentProblem = ref(null);
const error = ref("");
const message = ref("");
const languageOptions = [
  { value: "java", label: "Java" },
  { value: "python", label: "Python" },
  { value: "cpp", label: "C++" }
];

const form = reactive({
  id: null,
  title: "",
  description: "",
  supportedLanguageList: ["java", "python", "cpp"],
  methodName: "add",
  templateCode: "",
  templateCodeJava: "public class Solution {\n    public int add(int a, int b) {\n        return 0;\n    }\n}",
  templateCodePython: "class Solution:\n    def add(self, a, b):\n        return 0\n",
  templateCodeCpp: "#include <bits/stdc++.h>\nusing namespace std;\n\nclass Solution {\npublic:\n    int add(int a, int b) {\n        return 0;\n    }\n};\n",
  sortOrder: 1
});

const caseForm = reactive({
  id: null,
  problemId: null,
  inputJson: "",
  expectedOutput: "",
  isSample: 1,
  sortOrder: 1
});

function resetForm() {
  Object.assign(form, {
    id: null,
    title: "",
    description: "",
    supportedLanguageList: ["java", "python", "cpp"],
    methodName: "add",
    templateCode: "",
    templateCodeJava: "public class Solution {\n    public int add(int a, int b) {\n        return 0;\n    }\n}",
    templateCodePython: "class Solution:\n    def add(self, a, b):\n        return 0\n",
    templateCodeCpp: "#include <bits/stdc++.h>\nusing namespace std;\n\nclass Solution {\npublic:\n    int add(int a, int b) {\n        return 0;\n    }\n};\n",
    sortOrder: 1
  });
}

function resetCaseForm() {
  Object.assign(caseForm, {
    id: null,
    problemId: currentProblem.value ? currentProblem.value.id : null,
    inputJson: "",
    expectedOutput: "",
    isSample: 1,
    sortOrder: 1
  });
}

async function loadProblems() {
  problems.value = await http.get("/api/code-problem/admin/list");
}

async function saveProblem() {
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/code-problem/admin/save", buildPayload());
    message.value = form.id ? "题目已更新" : "题目已新增";
    await loadProblems();
    resetForm();
  } catch (e) {
    error.value = e.message;
  }
}

function editProblem(item) {
  Object.assign(form, {
    id: item.id,
    title: item.title || "",
    description: item.description || "",
    supportedLanguageList: parseSupportedLanguages(item.supportedLanguages),
    methodName: item.methodName || "add",
    templateCode: item.templateCode || "",
    templateCodeJava: item.templateCodeJava || item.templateCode || "",
    templateCodePython: item.templateCodePython || "",
    templateCodeCpp: item.templateCodeCpp || "",
    sortOrder: item.sortOrder || 1
  });
}

async function deleteProblem(item) {
  if (!window.confirm("确认删除该编程题吗？")) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/code-problem/admin/${item.id}`);
    message.value = "题目已删除";
    await loadProblems();
    if (currentProblem.value && currentProblem.value.id === item.id) {
      currentProblem.value = null;
      cases.value = [];
    }
  } catch (e) {
    error.value = e.message;
  }
}

async function openCases(item) {
  currentProblem.value = item;
  resetCaseForm();
  cases.value = await http.get("/api/code-problem/test-case/list", { params: { problemId: item.id } });
}

async function saveCase() {
  if (!currentProblem.value) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.post("/api/code-problem/test-case/save", caseForm);
    message.value = caseForm.id ? "用例已更新" : "用例已新增";
    await openCases(currentProblem.value);
  } catch (e) {
    error.value = e.message;
  }
}

function editCase(item) {
  Object.assign(caseForm, item);
}

async function deleteCase(item) {
  if (!window.confirm("确认删除该测试用例吗？")) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    await http.delete(`/api/code-problem/test-case/${item.id}`);
    message.value = "用例已删除";
    await openCases(currentProblem.value);
  } catch (e) {
    error.value = e.message;
  }
}

function buildPayload() {
  const languages = parseSupportedLanguages(form.supportedLanguageList.join(","));
  return {
    id: form.id,
    title: form.title,
    description: form.description,
    supportedLanguages: languages.join(","),
    methodName: form.methodName,
    templateCode: form.templateCodeJava || form.templateCode || "",
    templateCodeJava: form.templateCodeJava,
    templateCodePython: form.templateCodePython,
    templateCodeCpp: form.templateCodeCpp,
    sortOrder: form.sortOrder
  };
}

function parseSupportedLanguages(value) {
  const raw = Array.isArray(value) ? value : String(value || "").split(",");
  const allowed = ["java", "python", "cpp"];
  const list = raw
    .map((item) => String(item).trim().toLowerCase())
    .filter((item, index, arr) => item && allowed.includes(item) && arr.indexOf(item) === index);
  return list.length ? list : ["java"];
}

function formatSupportedLanguages(value) {
  return parseSupportedLanguages(value)
    .map((item) => languageOptions.find((lang) => lang.value === item)?.label || item)
    .join(" / ");
}

onMounted(loadProblems);
</script>
