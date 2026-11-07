const canvas = document.getElementById("grid");
const ctx = canvas.getContext("2d");
function resize() {
  canvas.width = innerWidth;
  canvas.height = innerHeight;
}
addEventListener("resize", resize);
resize();
let t = 0;
(function draw() {
  t += 0.004;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = "rgba(0,229,255,0.07)";
  const gap = 42;
  for (let x = 0; x < canvas.width; x += gap) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, canvas.height);
    ctx.stroke();
  }
  for (let y = 0; y < canvas.height; y += gap) {
    ctx.beginPath();
    ctx.moveTo(0, y + Math.sin(t + y * 0.01) * 2);
    ctx.lineTo(canvas.width, y);
    ctx.stroke();
  }
  requestAnimationFrame(draw);
})();

const loginView = document.getElementById("login-view");
const dashView = document.getElementById("dash-view");
const loginError = document.getElementById("login-error");
let accessToken = sessionStorage.getItem("jarvis_admin_token") || "";
let refreshToken = sessionStorage.getItem("jarvis_admin_refresh") || "";

async function api(path, opts = {}) {
  const headers = { ...(opts.headers || {}) };
  if (!(opts.body instanceof FormData)) headers["Content-Type"] = "application/json";
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const res = await fetch(path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && refreshToken && path !== "/api/auth/refresh") {
    const r = await fetch("/api/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    const d = await r.json();
    if (r.ok) {
      accessToken = d.accessToken;
      refreshToken = d.refreshToken;
      sessionStorage.setItem("jarvis_admin_token", accessToken);
      sessionStorage.setItem("jarvis_admin_refresh", refreshToken);
      return api(path, opts);
    }
  }
  if (!res.ok) throw new Error(data.error || res.statusText);
  return data;
}

function showDash() {
  loginView.classList.add("hidden");
  dashView.classList.remove("hidden");
}
function showLogin() {
  dashView.classList.add("hidden");
  loginView.classList.remove("hidden");
}

document.getElementById("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  loginError.textContent = "";
  try {
    const data = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: document.getElementById("email").value,
        password: document.getElementById("password").value,
      }),
    });
    if (data.user.role !== "admin") {
      throw new Error("This account is not an administrator.");
    }
    accessToken = data.accessToken;
    refreshToken = data.refreshToken;
    sessionStorage.setItem("jarvis_admin_token", accessToken);
    sessionStorage.setItem("jarvis_admin_refresh", refreshToken);
    document.getElementById("who").textContent = data.user.email;
    showDash();
    loadAll();
  } catch (err) {
    loginError.textContent = err.message;
  }
});

document.getElementById("logout").addEventListener("click", async () => {
  try {
    await api("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  } catch { /* ignore */ }
  accessToken = "";
  refreshToken = "";
  sessionStorage.clear();
  showLogin();
});

document.querySelectorAll("nav button").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll("nav button").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    document.querySelectorAll(".tab").forEach((t) => t.classList.add("hidden"));
    document.getElementById(`tab-${btn.dataset.tab}`).classList.remove("hidden");
  });
});

function fmt(ts) {
  if (!ts) return "—";
  return new Date(ts).toLocaleString();
}

async function loadOverview() {
  const stats = await api("/api/admin/stats");
  const cards = [
    ["Users", stats.users],
    ["Active 24h", stats.active24h],
    ["Active 7d", stats.active7d],
    ["Suspended", stats.suspended],
    ["Messages", stats.messages],
    ["AI calls 24h", stats.chatsToday],
    ["Tokens", stats.tokens],
  ];
  document.getElementById("stat-cards").innerHTML = cards
    .map(([l, n]) => `<div class="card"><div class="n">${n}</div><div class="l">${l}</div></div>`)
    .join("");
  const health = await api("/api/admin/health");
  document.getElementById("health-json").textContent = JSON.stringify(health, null, 2);
  const pill = document.getElementById("health-pill");
  const ok = health.ok && health.gemini === "configured";
  pill.textContent = ok ? "ONLINE" : "DEGRADED";
  pill.classList.toggle("warn", !ok);
}

async function loadUsers() {
  const q = document.getElementById("user-q").value;
  const data = await api("/api/admin/users" + (q ? `?q=${encodeURIComponent(q)}` : ""));
  document.getElementById("user-body").innerHTML = data.users
    .map((u) => {
      const next = u.status === "suspended" ? "active" : "suspended";
      const label = u.status === "suspended" ? "REACTIVATE" : "SUSPEND";
      return `<tr>
        <td>${u.email}</td>
        <td>${u.displayName}</td>
        <td>${u.role}</td>
        <td>${u.status}</td>
        <td>${u.languagePref}</td>
        <td>${fmt(u.lastActive)}</td>
        <td><button data-id="${u.id}" data-status="${next}" class="status-btn">${label}</button></td>
      </tr>`;
    })
    .join("");
  document.querySelectorAll(".status-btn").forEach((b) => {
    b.addEventListener("click", async () => {
      if (!confirm(`Set status to ${b.dataset.status}?`)) return;
      await api(`/api/admin/users/${b.dataset.id}/status`, {
        method: "PUT",
        body: JSON.stringify({ status: b.dataset.status }),
      });
      loadUsers();
      loadOverview();
    });
  });
}

async function loadUsage() {
  const data = await api("/api/admin/usage");
  document.getElementById("usage-json").textContent = JSON.stringify(data, null, 2);
}

async function loadConfig() {
  const data = await api("/api/admin/config");
  const form = document.getElementById("config-form");
  for (const [k, v] of Object.entries(data.config || {})) {
    if (form.elements[k]) form.elements[k].value = v;
  }
  document.getElementById("secret-json").textContent = JSON.stringify(data.secrets, null, 2);
}

document.getElementById("config-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const config = Object.fromEntries(fd.entries());
  await api("/api/admin/config", { method: "PUT", body: JSON.stringify({ config }) });
  alert("Configuration saved");
  loadConfig();
});

document.getElementById("user-refresh").addEventListener("click", loadUsers);
document.getElementById("user-q").addEventListener("keydown", (e) => {
  if (e.key === "Enter") loadUsers();
});

async function loadAll() {
  try {
    await Promise.all([loadOverview(), loadUsers(), loadUsage(), loadConfig()]);
  } catch (err) {
    if (String(err.message).toLowerCase().includes("admin")) {
      sessionStorage.clear();
      showLogin();
    }
  }
}

(async function boot() {
  if (!accessToken) return;
  try {
    const me = await api("/api/auth/me");
    if (me.user.role !== "admin") throw new Error("not admin");
    document.getElementById("who").textContent = me.user.email;
    showDash();
    loadAll();
  } catch {
    sessionStorage.clear();
    showLogin();
  }
})();
