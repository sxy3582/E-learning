<template>
  <div>
    <button class="ai-agent-fab" type="button" @click="toggle" aria-label="AI 助手">
      <span v-if="!open">AI</span>
      <span v-else>×</span>
    </button>

    <section v-if="open" class="ai-agent-panel" role="dialog" aria-label="课程与题目助手">
      <header class="ai-agent-head">
        <strong>课程与题目助手</strong>
        <button class="ai-agent-close" type="button" @click="close">关闭</button>
      </header>

      <div class="ai-agent-chips">
        <button class="ai-agent-chip" type="button" @click="quickAsk('列出课程')">课程列表</button>
        <button class="ai-agent-chip" type="button" @click="quickAsk('编程题列表')">编程题列表</button>
        <button class="ai-agent-chip" type="button" @click="quickAsk('课程 11001 的章节')">课程章节</button>
        <button class="ai-agent-chip" type="button" @click="quickAsk('章节 12003 的练习题')">章节练习</button>
        <button class="ai-agent-chip" type="button" @click="quickAsk('帮助')">帮助</button>
      </div>

      <div class="ai-agent-messages" ref="listEl">
        <div v-for="m in messages" :key="m.id" class="ai-msg" :class="m.role">
          <div v-for="(line, idx) in m.lines" :key="idx">{{ line }}</div>

          <div v-if="m.links && m.links.length" class="ai-agent-links">
            <router-link v-for="(link, idx) in m.links" :key="idx" :to="link.to">{{ link.label }}</router-link>
          </div>

          <small v-if="m.hint">{{ m.hint }}</small>
        </div>
      </div>

      <form class="ai-agent-input" @submit.prevent="send">
        <input v-model="input" type="text" placeholder="问我：课程、章节、练习题、编程题..." :disabled="sending" />
        <button class="primary ai-agent-send" type="submit" :disabled="sending || !input.trim()">发送</button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { nextTick, ref } from "vue";
import http from "../api";

const open = ref(false);
const input = ref("");
const sending = ref(false);
const listEl = ref(null);

const nextId = (() => {
  let i = 1;
  return () => i++;
})();

const messages = ref([
  {
    id: nextId(),
    role: "assistant",
    lines: [
      "你好，我可以帮你查询课程、章节、练习题与编程题信息。",
      "你可以直接问：列出课程 / 课程 11001 的章节 / 章节 12003 的练习题 / 编程题列表"
    ],
    links: [],
    hint: ""
  }
]);

const cache = {
  courses: null,
  chaptersByCourseId: new Map(),
  questionsByChapterId: new Map(),
  codeProblems: null
};

function toggle() {
  open.value = !open.value;
  if (open.value) {
    nextTick(scrollToBottom);
  }
}

function close() {
  open.value = false;
}

function pushMessage(role, text, links = [], hint = "") {
  const lines = String(text)
    .split("\n")
    .map((x) => x.trim())
    .filter(Boolean);

  messages.value.push({
    id: nextId(),
    role,
    lines: lines.length ? lines : [""],
    links,
    hint
  });

  nextTick(scrollToBottom);
}

function scrollToBottom() {
  const el = listEl.value;
  if (!el) {
    return;
  }
  el.scrollTop = el.scrollHeight;
}

function quickAsk(text) {
  input.value = text;
  send();
}

function normalizeText(text) {
  return String(text || "").trim();
}

function extractId(text, prefix) {
  const pattern = new RegExp(`${prefix}\\s*([0-9]+)`, "i");
  const match = String(text).match(pattern);
  if (!match) {
    return null;
  }
  return Number(match[1]);
}

async function getCourses() {
  if (cache.courses) {
    return cache.courses;
  }
  cache.courses = await http.get("/api/course/list");
  return cache.courses;
}

async function getChapters(courseId) {
  if (cache.chaptersByCourseId.has(courseId)) {
    return cache.chaptersByCourseId.get(courseId);
  }
  const chapters = await http.get("/api/chapter/list", { params: { courseId } });
  cache.chaptersByCourseId.set(courseId, chapters);
  return chapters;
}

async function getQuestions(chapterId) {
  if (cache.questionsByChapterId.has(chapterId)) {
    return cache.questionsByChapterId.get(chapterId);
  }
  const questions = await http.get("/api/question/list", { params: { chapterId } });
  cache.questionsByChapterId.set(chapterId, questions);
  return questions;
}

async function getCodeProblems() {
  if (cache.codeProblems) {
    return cache.codeProblems;
  }
  cache.codeProblems = await http.get("/api/code-problem/list");
  return cache.codeProblems;
}

function buildCourseLinks(courses) {
  return (courses || []).slice(0, 8).map((c) => ({
    label: `${c.id} · ${c.title}`,
    to: `/course/${c.id}`
  }));
}

function buildChapterLinks(courseId, chapters) {
  return (chapters || []).slice(0, 10).map((ch) => ({
    label: `${ch.id} · ${ch.title}`,
    to: `/study/${courseId}/${ch.id}`
  }));
}

function buildCodeProblemLinks(problems) {
  return (problems || []).slice(0, 10).map((p) => ({
    label: `${p.id} · ${p.title}`,
    to: `/code-practice?problem=${p.id}`
  }));
}

async function answerQuestion(raw) {
  const q = normalizeText(raw);
  if (!q) {
    return { text: "请输入问题。", links: [] };
  }

  if (q.includes("帮助") || q.toLowerCase() === "help") {
    return {
      text: "我目前支持这些问法：\n- 列出课程 / 课程列表\n- 课程 11001 的章节\n- 章节 12003 的练习题\n- 编程题列表\n也可以输入课程名称关键词，我会尝试匹配。",
      links: []
    };
  }

  const courseId = extractId(q, "课程");
  const chapterId = extractId(q, "章节");

  if (q.includes("编程题")) {
    const wantsAnswer = ["答案", "解法", "怎么写", "实现", "代码"].some((k) => q.includes(k));
    if (wantsAnswer) {
      const server = await http.post("/api/agent/ask", { question: q });
      if (typeof server === "string") {
        return { text: server, links: [] };
      }
      return {
        text: server && server.answer ? server.answer : "我暂时无法回答这个问题。",
        links: server && server.links ? server.links : []
      };
    }

    const problems = await getCodeProblems();
    const keyword = q
      .replace(/编程题/g, "")
      .replace(/列表/g, "")
      .replace(/有哪些/g, "")
      .replace(/全部/g, "")
      .trim();

    if (keyword) {
      const matched = problems.filter((p) => String(p.title || "").includes(keyword));
      if (matched.length) {
        return {
          text: `匹配到 ${matched.length} 道编程题：`,
          links: buildCodeProblemLinks(matched)
        };
      }
      return {
        text: `未找到包含“${keyword}”的编程题。当前共有 ${problems.length} 道，可点击进入。`,
        links: buildCodeProblemLinks(problems)
      };
    }

    return {
      text: `已加载编程题：${problems.length} 道。点击可直接进入在线编程页。`,
      links: buildCodeProblemLinks(problems)
    };
  }

  if (chapterId && (q.includes("练习") || q.includes("题目") || q.includes("普通题"))) {
    const questions = await getQuestions(chapterId);
    const preview = questions
      .slice(0, 6)
      .map((it, idx) => `${idx + 1}. ${it.stem}`)
      .join("\n");
    return {
      text: `章节 ${chapterId} 共 ${questions.length} 道题。\n${preview}${questions.length > 6 ? "\n..." : ""}\n如需作答，请从章节页进入练习。`,
      links: [],
      hint: ""
    };
  }

  if (courseId && q.includes("章节")) {
    const chapters = await getChapters(courseId);
    return {
      text: `课程 ${courseId} 共 ${chapters.length} 个章节。点击即可进入对应章节学习页。`,
      links: buildChapterLinks(courseId, chapters)
    };
  }

  if (q.includes("课程") && (q.includes("列表") || q.includes("有哪些") || q.includes("全部"))) {
    const courses = await getCourses();
    return {
      text: `当前已发布课程：${courses.length} 门。点击可进入课程详情。`,
      links: buildCourseLinks(courses)
    };
  }

  if (q.includes("课程")) {
    const keyword = q.replace(/课程/g, "").trim();
    const courses = await getCourses();
    const matched = keyword
      ? courses.filter((c) => String(c.title || "").includes(keyword) || String(c.intro || "").includes(keyword))
      : [];

    if (matched.length) {
      return {
        text: `根据关键词“${keyword}”匹配到 ${matched.length} 门课程：`,
        links: buildCourseLinks(matched)
      };
    }
  }

  const server = await http.post("/api/agent/ask", { question: q });
  if (typeof server === "string") {
    return { text: server, links: [] };
  }
  return {
    text: server && server.answer ? server.answer : "我暂时无法回答这个问题。",
    links: server && server.links ? server.links : []
  };
}

async function send() {
  const text = input.value.trim();
  if (!text || sending.value) {
    return;
  }
  input.value = "";
  pushMessage("user", text);

  sending.value = true;
  try {
    const res = await answerQuestion(text);
    pushMessage("assistant", res.text || "", res.links || [], res.hint || "");
  } catch (e) {
    pushMessage("assistant", `请求失败：${e.message || e}`, [], "请确认后端已启动，并且已登录。");
  } finally {
    sending.value = false;
  }
}
</script>
