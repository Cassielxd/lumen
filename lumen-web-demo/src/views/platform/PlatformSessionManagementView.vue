<template>
  <section class="page-grid">
    <el-card class="glass-card" shadow="never">
      <template #header>
        <div class="list-row">
          <div>
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">会话治理</div>
            <div class="section-subtitle">平台侧统一查看各个 client 的登录会话，并可按 sid 直接撤销。</div>
          </div>
          <div class="card-actions">
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" @click="loadSessions">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <el-select v-model="filters.clientId" clearable placeholder="按 client 筛选" style="width: 180px;">
          <el-option v-for="item in clientOptions" :key="item.clientId" :label="item.clientId" :value="item.clientId" />
        </el-select>
        <el-input
          v-model="filters.principalName"
          clearable
          placeholder="按用户名筛选"
          style="max-width: 220px;"
          @keyup.enter="loadSessions"
        />
        <el-select v-model="filters.status" clearable placeholder="按状态筛选" style="width: 160px;">
          <el-option label="正常" value="0" />
          <el-option label="已撤销" value="9" />
        </el-select>
        <el-button type="primary" plain @click="loadSessions">查询</el-button>
      </div>

      <el-table :data="sessions" v-loading="loading" empty-text="暂无会话数据">
        <el-table-column prop="principalName" label="用户名" min-width="120" />
        <el-table-column prop="clientId" label="Client" width="120" />
        <el-table-column prop="grantType" label="登录方式" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === "0" ? "正常" : "已撤销" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" min-width="140" />
        <el-table-column label="设备信息" min-width="240">
          <template #default="{ row }">
            <div class="muted-text">{{ row.userAgent || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="lastActiveTime" label="最后活跃" min-width="180" />
        <el-table-column label="撤销时间" min-width="180">
          <template #default="{ row }">
            {{ row.logoutTime || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="sid" label="SID" min-width="220" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="row.status !== '0'" @click="handleRevoke(row)">
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import { describeRequestError } from "../../api/http";
import { fetchClientPage, fetchPlatformSessions, revokePlatformSession } from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const loading = ref(false);
const sessions = ref([]);
const clientOptions = ref([]);
const filters = reactive({
  clientId: "",
  principalName: "",
  status: "0"
});

function resetFilters() {
  filters.clientId = "";
  filters.principalName = "";
  filters.status = "0";
  loadSessions();
}

async function loadClientOptions() {
  const page = await fetchClientPage(authStore.accessToken);
  clientOptions.value = page.records || [];
}

async function loadSessions() {
  loading.value = true;
  try {
    sessions.value = await fetchPlatformSessions(authStore.accessToken, {
      clientId: filters.clientId || undefined,
      principalName: filters.principalName || undefined,
      status: filters.status || undefined
    });
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    loading.value = false;
  }
}

async function handleRevoke(row) {
  try {
    await ElMessageBox.confirm(`确认撤销会话 ${row.sid} 吗？`, "会话撤销", { type: "warning" });
    await revokePlatformSession(authStore.accessToken, row.sid);
    ElMessage.success("会话已撤销");
    await loadSessions();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

onMounted(async () => {
  try {
    await loadClientOptions();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
  await loadSessions();
});
</script>
