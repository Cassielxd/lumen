<template>
  <section class="page-grid">
    <el-card class="glass-card" shadow="never">
      <template #header>
        <div class="list-row">
          <div>
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">审计日志</div>
            <div class="section-subtitle">统一查看登录、会话撤销、Client 变更、凭证治理和账号标识治理的操作轨迹。</div>
          </div>
          <div class="card-actions">
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" @click="loadRows">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <el-select v-model="filters.logType" clearable placeholder="按日志类型筛选" style="width: 180px;">
          <el-option label="正常" value="0" />
          <el-option label="异常" value="9" />
        </el-select>
        <el-input
          v-model="filters.title"
          clearable
          placeholder="按标题筛选"
          style="max-width: 240px;"
          @keyup.enter="loadRows"
        />
        <el-input
          v-model="filters.createBy"
          clearable
          placeholder="按操作人筛选"
          style="max-width: 180px;"
          @keyup.enter="loadRows"
        />
        <el-input
          v-model="filters.requestUri"
          clearable
          placeholder="按请求 URI 筛选"
          style="max-width: 240px;"
          @keyup.enter="loadRows"
        />
        <el-button type="primary" plain @click="loadRows">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" empty-text="暂无审计日志">
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.logType === '9' ? 'danger' : 'success'">
              {{ row.logType === "9" ? "异常" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="createBy" label="操作人" width="120" />
        <el-table-column prop="requestUri" label="请求 URI" min-width="220" />
        <el-table-column prop="method" label="方法" width="110" />
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">
            {{ row.time ? `${row.time} ms` : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="remoteAddr" label="IP" width="140" />
        <el-table-column prop="createTime" label="时间" min-width="180" />
        <el-table-column label="详情" min-width="280">
          <template #default="{ row }">
            <div class="token-preview">{{ row.exception || row.params || "-" }}</div>
          </template>
        </el-table-column>
      </el-table>

      <div class="list-row" style="margin-top: 16px; justify-content: flex-end;">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";

import { describeRequestError } from "../../api/http";
import { fetchLogPage } from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const loading = ref(false);
const rows = ref([]);
const filters = reactive({
  logType: "",
  title: "",
  createBy: "",
  requestUri: ""
});
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
});

function resetFilters() {
  filters.logType = "";
  filters.title = "";
  filters.createBy = "";
  filters.requestUri = "";
  pagination.current = 1;
  loadRows();
}

async function loadRows() {
  loading.value = true;
  try {
    const page = await fetchLogPage(authStore.accessToken, {
      current: pagination.current,
      size: pagination.size,
      logType: filters.logType || undefined,
      title: filters.title || undefined,
      createBy: filters.createBy || undefined,
      requestUri: filters.requestUri || undefined
    });
    rows.value = page.records || [];
    pagination.total = page.total || 0;
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    loading.value = false;
  }
}

function handleCurrentChange(current) {
  pagination.current = current;
  loadRows();
}

function handleSizeChange(size) {
  pagination.size = size;
  pagination.current = 1;
  loadRows();
}

onMounted(async () => {
  await loadRows();
});
</script>
