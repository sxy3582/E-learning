<template>
  <section class="overview-page admin-overview-page">
    <div class="overview-hero admin-hero">
      <article>
        <p class="eyebrow">Admin Console</p>
        <h1>把课程内容配置成完整的学习闭环。</h1>
        <p>
          管理员的核心任务不是堆功能，而是维护“课程 - 章节 - 练习 - 编程题 - 算法实验 - 学习统计”这条链路，让普通用户登录后能顺着路径学习。
        </p>
        <div class="hero-actions">
          <router-link to="/admin/courses"><button class="primary">维护课程</button></router-link>
          <router-link to="/admin/chapters"><button class="secondary">配置章节</button></router-link>
        </div>
      </article>
      <aside class="overview-focus-card">
        <span class="status-pill">平台资源</span>
        <strong>{{ stats.courseCount || 0 }}</strong>
        <p>已维护课程，包含 {{ stats.chapterCount || 0 }} 个章节内容。</p>
      </aside>
    </div>

    <div class="overview-metrics">
      <article class="overview-metric">
        <span>普通用户</span>
        <strong>{{ stats.userCount || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>课程</span>
        <strong>{{ stats.courseCount || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>章节</span>
        <strong>{{ stats.chapterCount || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>普通题</span>
        <strong>{{ stats.questionCount || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>编程提交</span>
        <strong>{{ stats.codeSubmissionCount || 0 }}</strong>
      </article>
      <article class="overview-metric">
        <span>进度记录</span>
        <strong>{{ stats.progressRecordCount || 0 }}</strong>
      </article>
    </div>

    <div class="overview-section-title">
      <div>
        <p class="eyebrow">Content Workflow</p>
        <h2>后台配置流程</h2>
      </div>
      <p class="muted">按这个顺序配置，前台用户看到的学习路径最完整。</p>
    </div>

    <div class="learning-path admin-path">
      <article class="path-card">
        <span>01</span>
        <h3>课程框架</h3>
        <p>创建课程、设置分类难度、上传封面并决定是否发布。</p>
        <router-link to="/admin/courses">课程管理</router-link>
      </article>
      <article class="path-card">
        <span>02</span>
        <h3>章节内容</h3>
        <p>配置文章、视频、普通练习入口、编程题入口和算法实验入口。</p>
        <router-link to="/admin/chapters">章节管理</router-link>
      </article>
      <article class="path-card">
        <span>03</span>
        <h3>练习资源</h3>
        <p>为练习章节录入单选、判断题，保存解析与正确答案。</p>
        <router-link to="/admin/questions">普通题管理</router-link>
      </article>
      <article class="path-card is-highlight">
        <span>04</span>
        <h3>在线编程</h3>
        <p>维护 Java 编程题、测试用例，并通过内置算法实验增强展示效果。</p>
        <router-link to="/admin/code-problems">编程题管理</router-link>
      </article>
    </div>

    <div class="overview-section-title">
      <div>
        <p class="eyebrow">Quick Access</p>
        <h2>常用管理入口</h2>
      </div>
      <p class="muted">这里放高频操作，避免管理员在菜单里反复寻找。</p>
    </div>

    <div class="admin-action-grid">
      <article class="admin-action-card">
        <h3>用户与权限</h3>
        <p>查看用户状态，处理封禁、解封和重置密码。</p>
        <router-link to="/admin/users"><button class="secondary">管理用户</button></router-link>
      </article>
      <article class="admin-action-card">
        <h3>课程内容</h3>
        <p>课程负责入口，章节负责内容，二者共同组成用户学习路径。</p>
        <router-link to="/admin/chapters"><button class="secondary">配置章节</button></router-link>
      </article>
      <article class="admin-action-card">
        <h3>题库与判题</h3>
        <p>普通题用于章节反馈，编程题用于展示在线 Java 判题能力。</p>
        <div class="row-actions">
          <router-link to="/admin/questions"><button class="secondary">普通题</button></router-link>
          <router-link to="/admin/code-problems"><button class="secondary">编程题</button></router-link>
        </div>
      </article>
      <article class="admin-action-card">
        <h3>学习进度</h3>
        <p>查看用户学习、练习和编程提交情况，确认平台闭环是否被使用。</p>
        <router-link to="/admin/progress"><button class="secondary">查看统计</button></router-link>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import http from "../api";

const stats = reactive({});

onMounted(async () => {
  try {
    const data = await http.get("/api/admin/stats/overview");
    Object.assign(stats, data);
  } catch (e) {
    // ignore stats loading failure in overview page
  }
});
</script>
