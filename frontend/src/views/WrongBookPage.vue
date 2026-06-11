<template>
  <section>
    <div class="section-head">
      <div>
        <p class="eyebrow">Wrong Book</p>
        <h1>错题单</h1>
        <p class="muted">作业题答错和编程题未通过时会自动加入，做对后会自动移出，也支持你手动移除。</p>
      </div>
      <button class="secondary" @click="loadData">刷新</button>
    </div>

    <p class="error-text" v-if="error">{{ error }}</p>

    <div class="grid grid-3">
      <article class="card metric-card">
        <p class="eyebrow">Wrong Count</p>
        <div class="metric-number">{{ progress.wrongBookCount || 0 }}</div>
        <p class="muted">当前错题总数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Practice</p>
        <div class="metric-number">{{ questionCount }}</div>
        <p class="muted">作业题错题数</p>
      </article>
      <article class="card metric-card">
        <p class="eyebrow">Code</p>
        <div class="metric-number">{{ codeCount }}</div>
        <p class="muted">编程题错题数</p>
      </article>
    </div>

    <article class="card wrong-book-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">My Wrong Book</p>
          <h2 class="title">错题列表</h2>
        </div>
      </div>
      <p class="muted" v-if="!progress.wrongBook || !progress.wrongBook.length">当前还没有错题记录。</p>
      <div class="admin-list" v-else>
        <div class="admin-row wrong-book-row" v-for="item in progress.wrongBook" :key="item.id">
          <div>
            <strong>{{ item.title }}</strong>
            <small>{{ item.typeLabel }} · {{ item.description || "暂无说明" }}</small>
          </div>
          <div class="row-actions">
            <button class="secondary" @click="openWrongItem(item)" :disabled="!item.routePath">{{ item.routeLabel || "前往题目" }}</button>
            <button class="secondary danger" @click="removeWrongItem(item)">移除</button>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import http from "../api";
import { applyWrongBookCount, applyWrongBookCountFromPayload, getWrongBookCount, onWrongBookCountChange } from "../wrongBookState";

const router = useRouter();
const error = ref("");
const progress = reactive({
  wrongBookCount: getWrongBookCount()
});
let stopListening = null;

const wrongBook = computed(() => (Array.isArray(progress.wrongBook) ? progress.wrongBook : []));
const questionCount = computed(() => wrongBook.value.filter((item) => item.targetType === "QUESTION").length);
const codeCount = computed(() => wrongBook.value.filter((item) => item.targetType === "CODE_PROBLEM").length);

async function loadData() {
  error.value = "";
  try {
    const data = await http.get("/api/progress/my");
    Object.assign(progress, data);
    applyWrongBookCountFromPayload(data);
  } catch (e) {
    error.value = e.message;
  }
}

function openWrongItem(item) {
  if (!item?.routePath) {
    return;
  }
  router.push(item.routePath);
}

async function removeWrongItem(item) {
  if (!window.confirm("确认将这道题从错题单移除吗？")) {
    return;
  }
  error.value = "";
  try {
    await http.delete(`/api/progress/wrong-book/${item.id}`);
    applyWrongBookCount(Math.max(0, (progress.wrongBookCount || 0) - 1));
    await loadData();
  } catch (e) {
    error.value = e.message;
  }
}

onMounted(() => {
  stopListening = onWrongBookCountChange((count) => {
    progress.wrongBookCount = count;
  });
  loadData();
});

onBeforeUnmount(() => {
  if (stopListening) {
    stopListening();
  }
});
</script>
