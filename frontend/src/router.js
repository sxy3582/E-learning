import { createRouter, createWebHistory } from "vue-router";
import { getCurrentUser } from "./auth";

import HomePage from "./views/HomePage.vue";
import LoginPage from "./views/LoginPage.vue";
import RegisterPage from "./views/RegisterPage.vue";
import CoursePage from "./views/CoursePage.vue";
import CourseDetailPage from "./views/CourseDetailPage.vue";
import AdminPage from "./views/AdminPage.vue";
import AdminUsersPage from "./views/AdminUsersPage.vue";
import AdminCoursesPage from "./views/AdminCoursesPage.vue";
import AdminChaptersPage from "./views/AdminChaptersPage.vue";
import AdminQuestionsPage from "./views/AdminQuestionsPage.vue";
import AdminProgressPage from "./views/AdminProgressPage.vue";
import AdminCodeProblemsPage from "./views/AdminCodeProblemsPage.vue";
import AdminCommentsPage from "./views/AdminCommentsPage.vue";
import CodePracticePage from "./views/CodePracticePage.vue";
import AlgorithmLabPage from "./views/AlgorithmLabPage.vue";
import ChapterStudyPage from "./views/ChapterStudyPage.vue";
import PracticePage from "./views/PracticePage.vue";
import MyProgressPage from "./views/MyProgressPage.vue";
import MyProfilePage from "./views/MyProfilePage.vue";
import WrongBookPage from "./views/WrongBookPage.vue";

const routes = [
  { path: "/", component: HomePage },
  { path: "/login", component: LoginPage, meta: { public: true } },
  { path: "/register", component: RegisterPage, meta: { public: true } },
  { path: "/courses", component: CoursePage },
  { path: "/course/:id", component: CourseDetailPage },
  { path: "/study/:courseId/:chapterId", component: ChapterStudyPage },
  { path: "/practice/:courseId/:chapterId", component: PracticePage },
  { path: "/my-progress", component: MyProgressPage },
  { path: "/wrong-book", component: WrongBookPage },
  { path: "/my-profile", component: MyProfilePage },
  { path: "/code-practice", component: CodePracticePage },
  { path: "/algorithm-lab", component: AlgorithmLabPage },
  { path: "/admin", component: AdminPage, meta: { admin: true } },
  { path: "/admin/users", component: AdminUsersPage, meta: { admin: true } },
  { path: "/admin/courses", component: AdminCoursesPage, meta: { admin: true } },
  { path: "/admin/chapters", component: AdminChaptersPage, meta: { admin: true } },
  { path: "/admin/questions", component: AdminQuestionsPage, meta: { admin: true } },
  { path: "/admin/progress", component: AdminProgressPage, meta: { admin: true } },
  { path: "/admin/code-problems", component: AdminCodeProblemsPage, meta: { admin: true } },
  { path: "/admin/comments", component: AdminCommentsPage, meta: { admin: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const user = getCurrentUser();

  if (!to.meta.public && !user) {
    next({ path: "/login", query: { redirect: to.fullPath } });
    return;
  }

  if (to.meta.public && user) {
    next(user.role === "ADMIN" ? "/admin" : "/");
    return;
  }

  if (to.path === "/" && user && user.role === "ADMIN") {
    next("/admin");
    return;
  }

  if (to.meta.admin && user.role !== "ADMIN") {
    next("/");
    return;
  }

  next();
});

export default router;
