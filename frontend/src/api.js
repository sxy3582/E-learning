import axios from "axios";

const http = axios.create({
  baseURL: "/",
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = token;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const result = response.data;
    if (result.code !== 0) {
      handleAuthExpired(result.message);
      return Promise.reject(new Error(result.message || "请求失败"));
    }
    return result.data;
  },
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      handleAuthExpired("登录状态已失效");
    }
    return Promise.reject(error);
  }
);

function handleAuthExpired(message = "") {
  const text = String(message);
  const authErrorTexts = ["请先登录", "未登录", "登录状态已失效", "token无效", "token已过期"];
  const isAuthError = authErrorTexts.some((keyword) => text.includes(keyword));
  if (!isAuthError) {
    return;
  }
  localStorage.removeItem("token");
  localStorage.removeItem("currentUser");
  const current = window.location.pathname + window.location.search;
  if (window.location.pathname !== "/login" && window.location.pathname !== "/register") {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`;
  }
}

export default http;
