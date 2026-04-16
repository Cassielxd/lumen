<template>
  <div class="page-grid">
    <section class="glass-card dashboard-hero" :style="{ '--hero-from': currentClient.heroFrom, '--hero-to': currentClient.heroTo }">
      <div class="list-row" style="align-items: flex-start;">
        <div>
          <div class="client-chip" style="background: rgba(255,255,255,0.16); color: #fff;">
            {{ currentClient.audience }} / {{ currentClient.id }}
          </div>
          <h1 class="hero-title">{{ currentClient.label }}工作台</h1>
          <p class="hero-subtitle">
            {{ currentClient.description }}
            当前账号为 <strong>{{ authStore.username }}</strong>，登录方式为
            <strong>{{ authStore.grantType }}</strong>。
          </p>
        </div>
        <div class="card-actions">
          <el-button type="primary" plain @click="reloadRuntimeData">刷新会话</el-button>
        </div>
      </div>

      <div class="stats-grid" style="margin-top: 22px;">
        <el-card
          v-for="item in currentClient.metrics"
          :key="item.label"
          shadow="never"
          style="border-radius: 22px; border: 0; background: rgba(255, 255, 255, 0.14); color: #fff;"
        >
          <div style="font-size: 13px; opacity: 0.8;">{{ item.label }}</div>
          <div style="margin-top: 10px; font-size: 24px; font-weight: 700;">{{ item.value }}</div>
          <div style="margin-top: 8px; font-size: 13px; opacity: 0.84;">{{ item.note }}</div>
        </el-card>
      </div>
    </section>

    <section class="workspace-grid">
      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <span>当前 client 对应的业务内容</span>
            <el-tag round>{{ currentClient.label }}</el-tag>
          </div>
        </template>
        <div class="panel-grid">
          <el-card v-for="panel in currentClient.panels" :key="panel.title" shadow="hover" style="border-radius: 20px;">
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">{{ panel.title }}</div>
            <div class="section-subtitle" style="margin-top: 10px;">{{ panel.body }}</div>
          </el-card>
        </div>
      </el-card>

      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <span>当前会话</span>
            <div class="card-actions">
              <el-button size="small" @click="handleRevokeOthers">退出其他设备</el-button>
              <el-button size="small" type="primary" plain @click="reloadRuntimeData">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table :data="sessions" v-loading="sessionLoading" empty-text="当前没有会话数据" style="width: 100%;">
          <el-table-column label="SID" min-width="170">
            <template #default="{ row }">
              <div class="token-preview">{{ row.sid }}</div>
              <el-tag v-if="row.current" size="small" type="success" round style="margin-top: 6px;">当前设备</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="clientId" label="客户端" width="110" />
          <el-table-column prop="grantType" label="登录方式" width="120" />
          <el-table-column label="过期时间" min-width="180">
            <template #default="{ row }">
              <div>{{ formatDateTime(row.accessTokenExpiresAt) }}</div>
              <div class="muted-text" style="font-size: 12px;">刷新令牌：{{ formatDateTime(row.refreshTokenExpiresAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="设备信息" min-width="260">
            <template #default="{ row }">
              <div class="muted-text" style="font-size: 13px;">IP: {{ row.ipAddress || "-" }}</div>
              <div class="token-preview">{{ row.userAgent || "-" }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" :disabled="row.current" @click="handleRevokeSession(row.sid)">
                踢下线
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <section class="workspace-grid">
      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <span>令牌摘要</span>
            <el-tag round>{{ authStore.grantType }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="客户端">{{ currentClient.id }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ authStore.username }}</el-descriptions-item>
          <el-descriptions-item label="SID">{{ authStore.sid || "-" }}</el-descriptions-item>
          <el-descriptions-item label="登录时间">{{ formatDateTime(authStore.loggedInAt) }}</el-descriptions-item>
          <el-descriptions-item label="Access Token">
            <div class="token-preview">{{ authStore.accessToken }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="刷新令牌">
            <div class="token-preview">{{ authStore.refreshToken || "-" }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <span>Passkey 管理</span>
            <el-button type="primary" plain size="small" @click="handleRegisterPasskey">
              注册当前账号 Passkey
            </el-button>
          </div>
        </template>

        <el-empty v-if="!passkeys.length && !passkeyLoading" description="当前账号还没有 Passkey，可以直接在这里完成注册。" />

        <div v-else v-loading="passkeyLoading">
          <div v-for="item in passkeys" :key="item.credentialKey" class="passkey-row">
            <div class="list-row">
              <div>
                <div style="font-weight: 700; color: #0f172a;">{{ item.displayName || item.credentialKey }}</div>
                <div class="token-preview">{{ item.credentialKey }}</div>
              </div>
              <el-button link type="danger" @click="handleDeletePasskey(item.credentialKey)">删除</el-button>
            </div>
            <div class="section-subtitle" style="margin-top: 8px;">
              计数器：{{ item.payload?.signCount ?? "-" }} / 状态：{{ item.status }}
            </div>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import { describeRequestError } from "../api/http";
import { useAuthStore } from "../stores/auth";

const authStore = useAuthStore();
const currentClient = computed(() => authStore.currentClient);

const sessionLoading = ref(false);
const passkeyLoading = ref(false);
const sessions = ref([]);
const passkeys = ref([]);

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ");
}

async function loadSessions() {
  sessionLoading.value = true;
  try {
    sessions.value = await authStore.loadSessions();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    sessionLoading.value = false;
  }
}

async function loadPasskeys() {
  passkeyLoading.value = true;
  try {
    passkeys.value = await authStore.loadPasskeys();
  } catch (error) {
    passkeys.value = [];
    ElMessage.error(describeRequestError(error));
  } finally {
    passkeyLoading.value = false;
  }
}

async function reloadRuntimeData() {
  await Promise.all([loadSessions(), loadPasskeys()]);
}

async function handleRevokeSession(sid) {
  try {
    await ElMessageBox.confirm("确认让这个设备下线吗？", "会话撤销", { type: "warning" });
    await authStore.revokeSessionBySid(sid);
    ElMessage.success("会话已撤销");
    await loadSessions();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

async function handleRevokeOthers() {
  try {
    await ElMessageBox.confirm("确认退出当前账号的其他设备吗？", "会话管理", { type: "warning" });
    await authStore.revokeOtherCurrentSessions();
    ElMessage.success("其他设备已下线");
    await loadSessions();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

async function handleRegisterPasskey() {
  try {
    await authStore.registerPasskey();
    ElMessage.success("Passkey 注册完成");
    await loadPasskeys();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
}

async function handleDeletePasskey(credentialKey) {
  try {
    await ElMessageBox.confirm("确认删除这个 Passkey 吗？", "Passkey 管理", { type: "warning" });
    await authStore.deletePasskey(credentialKey);
    ElMessage.success("Passkey 已删除");
    await loadPasskeys();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

onMounted(async () => {
  await reloadRuntimeData();
});
</script>
