export function getCurrentUser() {
  const raw = localStorage.getItem("currentUser");
  return raw ? JSON.parse(raw) : null;
}

export function setAuth(data) {
  if (data.token) {
    localStorage.setItem("token", data.token);
  }
  localStorage.setItem("currentUser", JSON.stringify(data));
}

export function clearAuth() {
  localStorage.removeItem("token");
  localStorage.removeItem("currentUser");
}
