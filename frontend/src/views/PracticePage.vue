<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Practice</p>
        <h1>章节练习</h1>
        <p class="muted">完成本章节练习，立即查看结果与解析。</p>
      </div>
      <div class="row-actions">
        <button class="secondary" @click="goBackStudy">返回章节学习</button>
        <button class="secondary" @click="loadQuestions">刷新题目</button>
      </div>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>
    <div class="row-actions" style="margin-bottom: 12px;">
      <button class="primary" :disabled="locked || !questions.length" @click="submitAll">
        {{ locked ? "已提交，不可修改" : "提交全部答案" }}
      </button>
      <span class="muted" v-if="summary.total">得分：{{ summary.score }}/{{ summary.total }}</span>
      <span class="muted" v-if="wrongBookCount || locked">错题单：{{ wrongBookCount || 0 }} 条</span>
    </div>

    <div class="admin-list">
      <article class="card" v-for="(q, idx) in questions" :key="q.id">
        <p class="eyebrow">Question {{ idx + 1 }}</p>
        <h3 class="title">{{ q.stem }}</h3>

        <div v-if="q.type === 'SINGLE'" class="admin-list">
          <label v-for="opt in q.options" :key="opt.id" class="option-item">
            <input
              type="radio"
              :name="`q-${q.id}`"
              :value="opt.optionKey"
              v-model="answers[q.id]"
              :disabled="locked"
            />
            <span>{{ opt.optionKey }}. {{ opt.optionContent }}</span>
          </label>
        </div>

        <div v-else-if="q.type === 'JUDGE'" class="admin-list">
          <p class="muted">请判断以下说法是否正确：</p>
          <div class="judge-option-row">
            <label class="option-item judge-choice">
              <input type="radio" :name="`q-${q.id}`" value="TRUE" v-model="answers[q.id]" :disabled="locked" />
              <span>正确</span>
            </label>
            <label class="option-item judge-choice">
              <input type="radio" :name="`q-${q.id}`" value="FALSE" v-model="answers[q.id]" :disabled="locked" />
              <span>错误</span>
            </label>
          </div>
        </div>

        <div class="result-panel" v-if="resultMap[q.id]">
          <p><strong>结果：</strong>{{ resultMap[q.id].message }}</p>
          <p><strong>参考答案：</strong>{{ getReadableAnswer(q, resultMap[q.id].referenceAnswer) }}</p>
          <p><strong>你的答案：</strong>{{ getReadableAnswer(q, answers[q.id]) }}</p>
          <p><strong>解析：</strong>{{ resultMap[q.id].analysis || "暂无解析" }}</p>
        </div>

        <CommentPanel
          target-type="QUESTION"
          :target-id="q.id"
          title="题目评论区"
          placeholder="可以讨论题意、易错点或解题思路"
          empty-text="这道题还没有评论。"
        />
      </article>
      <p class="muted" v-if="!questions.length">当前章节还没有题目。</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "../api";
import CommentPanel from "../components/CommentPanel.vue";
import { applyWrongBookCountFromPayload, getWrongBookCount } from "../wrongBookState";

const route = useRoute();
const router = useRouter();
const questions = ref([]);
const error = ref("");
const answers = reactive({});
const resultMap = reactive({});
const locked = ref(false);
const wrongBookCount = ref(getWrongBookCount());
const summary = reactive({
  score: 0,
  total: 0
});

function normalizeJudge(answer) {
  const value = String(answer || "").trim().toUpperCase();
  if (["TRUE", "正确", "对", "T"].includes(value)) {
    return "TRUE";
  }
  if (["FALSE", "错误", "错", "F"].includes(value)) {
    return "FALSE";
  }
  return value;
}

function getReadableAnswer(question, answer) {
  if (!answer) {
    return "-";
  }
  if (question.type === "JUDGE") {
    return normalizeJudge(answer) === "TRUE" ? "正确" : "错误";
  }
  const key = String(answer).trim().toUpperCase();
  const option = (question.options || []).find((item) => String(item.optionKey || "").trim().toUpperCase() === key);
  return option ? `${key}. ${option.optionContent}` : key;
}

async function loadQuestions() {
  error.value = "";
  questions.value = await http.get("/api/question/list", {
    params: { chapterId: route.params.chapterId }
  });
  locked.value = questions.value.length > 0 && questions.value.every((q) => Number(q.submitted) === 1);
  questions.value.forEach((q) => {
    answers[q.id] = q.myAnswer || answers[q.id] || "";
    if (q.submitted) {
      resultMap[q.id] = {
        message: Number(q.myCorrect) === 1 ? "回答正确" : "回答错误",
        referenceAnswer: q.referenceAnswer,
        analysis: q.analysis
      };
    }
  });
  if (locked.value) {
    summary.total = questions.value.length;
    summary.score = questions.value.filter((q) => Number(q.myCorrect) === 1).length;
  }
}

async function submitAll() {
  if (locked.value) {
    return;
  }
  error.value = "";
  for (const q of questions.value) {
    const value = (answers[q.id] || "").trim();
    if (!value) {
      error.value = "请确认所有题目都已填写后再提交";
      return;
    }
  }
  try {
    const payload = {
      chapterId: Number(route.params.chapterId),
      answers: questions.value.map((q) => ({
        questionId: q.id,
        answerContent: answers[q.id]
      }))
    };
    const result = await http.post("/api/question/submit-batch", payload);
    summary.score = result.score;
    summary.total = result.total;
    const latestWrongBookCount = applyWrongBookCountFromPayload(result);
    wrongBookCount.value = latestWrongBookCount === null ? wrongBookCount.value : latestWrongBookCount;
    result.results.forEach((item) => {
      resultMap[item.questionId] = item;
    });
    locked.value = true;
  } catch (e) {
    error.value = e.message;
  }
}

function goBackStudy() {
  router.push(`/study/${route.params.courseId}/${route.params.chapterId}`);
}

onMounted(loadQuestions);
</script>
