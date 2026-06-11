<template>
  <section class="grid">
    <div class="grid grid-2">
      <article class="card">
        <h2 class="title">在线编程</h2>
        <select v-model.number="selectedProblemId" @change="handleProblemChange">
          <option v-for="p in problems" :key="p.id" :value="p.id">{{ p.title }}</option>
        </select>
        <select v-model="selectedLanguage" @change="handleLanguageChange">
          <option v-for="lang in supportedLanguages" :key="lang" :value="lang">{{ languageLabelMap[lang] || lang }}</option>
        </select>
        <p>题目描述：{{ currentProblem?.description || "请先选择题目" }}</p>
        <p>当前语言：<code>{{ languageLabelMap[selectedLanguage] || "-" }}</code></p>
        <p>方法名：<code>{{ currentProblem?.methodName || "-" }}</code></p>
        <p style="color: #7a5a37;">小屏可浏览，建议使用 PC 获得最佳体验。编辑器支持 Tab 缩进。</p>
        <div class="code-toolbar">
          <button class="secondary" @click="formatCode">整理缩进</button>
          <button class="secondary" @click="restoreTemplate" :disabled="!currentProblem || !selectedLanguage">恢复模板</button>
        </div>
        <textarea
          ref="codeEditor"
          v-model="code"
          class="code-editor"
          rows="18"
          spellcheck="false"
          @keydown.tab.prevent="insertTab"
          @keydown.ctrl.enter.prevent="runCode"
        ></textarea>
        <button class="primary" @click="runCode">运行判题</button>
        <pre class="error-text compile-error" v-if="error">{{ error }}</pre>
      </article>
      <article class="card">
        <h3 class="title">运行结果</h3>
        <p v-if="!result">还没有运行记录。</p>
        <template v-else>
          <p>是否通过：{{ result.passed ? "通过" : "未通过" }}</p>
          <p>通过数：{{ result.passCount }}/{{ result.total }}</p>
          <p>耗时：{{ result.costMs }} ms</p>
          <p>错题单：{{ wrongBookCount }} 条</p>
          <div v-for="(item, idx) in result.cases" :key="idx" style="margin-bottom: 10px;">
            <strong>Case {{ idx + 1 }}</strong>
            <div>输入：{{ item.input.join(", ") }}</div>
            <div>期望：{{ item.expected }}，实际：{{ item.actual }}</div>
            <div>结果：{{ item.pass ? "通过" : "未通过" }}</div>
          </div>
        </template>
      </article>
    </div>
    <CommentPanel
      v-if="currentProblem?.id"
      target-type="CODE_PROBLEM"
      :target-id="currentProblem.id"
      title="编程题评论区"
      placeholder="可以讨论题意、测试点、实现思路和踩坑经验"
      empty-text="这道编程题还没有评论。"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import http from "../api";
import CommentPanel from "../components/CommentPanel.vue";
import { applyWrongBookCountFromPayload, getWrongBookCount } from "../wrongBookState";

const route = useRoute();
const problems = ref([]);
const selectedProblemId = ref(null);
const selectedLanguage = ref("java");
const code = ref("");
const codeEditor = ref(null);
const error = ref("");
const result = ref(null);
const wrongBookCount = ref(getWrongBookCount());
const currentProblem = computed(() => problems.value.find((x) => x.id === selectedProblemId.value));
const languageLabelMap = {
  java: "Java",
  python: "Python",
  cpp: "C++"
};
const supportedLanguages = computed(() => {
  const raw = currentProblem.value?.supportedLanguages;
  if (!raw) {
    return ["java"];
  }
  return raw
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);
});

async function runCode() {
  error.value = "";
  result.value = null;
  try {
    result.value = await http.post("/api/code/run-problem", {
      problemId: selectedProblemId.value,
      language: selectedLanguage.value,
      sourceCode: code.value
    });
    const latestWrongBookCount = applyWrongBookCountFromPayload(result.value);
    wrongBookCount.value = latestWrongBookCount === null ? wrongBookCount.value : latestWrongBookCount;
  } catch (e) {
    error.value = e.message;
  }
}

function insertTab() {
  const textarea = codeEditor.value;
  if (!textarea) {
    return;
  }
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  code.value = `${code.value.slice(0, start)}    ${code.value.slice(end)}`;
  requestAnimationFrame(() => {
    textarea.selectionStart = start + 4;
    textarea.selectionEnd = start + 4;
  });
}

function restoreTemplate() {
  code.value = resolveTemplateByLanguage();
}

function formatCode() {
  let indent = 0;
  code.value = code.value
    .split("\n")
    .map((line) => {
      const trimmed = line.trim();
      if (!trimmed) {
        return "";
      }
      const startsWithClosingBrace = trimmed.startsWith("}");
      const lineIndent = startsWithClosingBrace ? Math.max(0, indent - 1) : indent;
      const formatted = `${"    ".repeat(lineIndent)}${trimmed}`;
      const opens = (trimmed.match(/\{/g) || []).length;
      const closes = (trimmed.match(/\}/g) || []).length;
      const consumedClosingBrace = startsWithClosingBrace ? 1 : 0;
      indent = Math.max(0, lineIndent + opens - Math.max(0, closes - consumedClosingBrace));
      return formatted;
    })
    .join("\n");
}

function handleProblemChange() {
  const current = currentProblem.value;
  if (current) {
    const available = supportedLanguages.value;
    if (!available.includes(selectedLanguage.value)) {
      selectedLanguage.value = available[0] || "java";
    }
    code.value = resolveTemplateByLanguage();
  }
}

function handleLanguageChange() {
  if (!supportedLanguages.value.includes(selectedLanguage.value)) {
    return;
  }
  code.value = resolveTemplateByLanguage();
}

function resolveTemplateByLanguage() {
  const current = currentProblem.value;
  if (!current) {
    return "";
  }
  if (selectedLanguage.value === "python") {
    return current.templateCodePython || current.templateCode || "";
  }
  if (selectedLanguage.value === "cpp") {
    return current.templateCodeCpp || current.templateCode || "";
  }
  return current.templateCodeJava || current.templateCode || "";
}

onMounted(async () => {
  problems.value = await http.get("/api/code-problem/list");
  if (problems.value.length) {
    const queryProblemId = Number(route.query.problem);
    const matched = problems.value.find((item) => item.id === queryProblemId);
    selectedProblemId.value = matched ? matched.id : problems.value[0].id;
    handleProblemChange();
  }
});
</script>
