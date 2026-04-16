<template>
  <section class="page-grid">
    <el-card class="glass-card" shadow="never">
      <template #header>
        <div class="list-row">
          <div>
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">Client 管理</div>
            <div class="section-subtitle">
              维护 OAuth 客户端、授权方式、展示信息、验证码策略和密码加密策略。
            </div>
          </div>
          <div class="card-actions">
            <el-button @click="loadClients">刷新</el-button>
            <el-button @click="handleSyncCache">同步缓存</el-button>
            <el-button type="primary" @click="openCreateDialog">新建 Client</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <el-input v-model="filters.clientId" clearable placeholder="按 clientId 搜索" style="max-width: 280px;" @keyup.enter="loadClients" />
        <el-button type="primary" plain @click="loadClients">搜索</el-button>
      </div>

      <el-table :data="clients" v-loading="loading" empty-text="暂无客户端数据">
        <el-table-column prop="clientId" label="客户端标识" min-width="140" />
        <el-table-column label="展示信息" min-width="220">
          <template #default="{ row }">
            <div style="font-weight: 700; color: #0f172a;">{{ row.displayName || row.clientId }}</div>
            <div class="muted-text" style="font-size: 12px; margin-top: 4px;">{{ row.audience || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="授权方式" min-width="260">
          <template #default="{ row }">
            <div class="tag-flow">
              <el-tag v-for="grant in normalizeGrantTypes(row.authorizedGrantTypes)" :key="grant" size="small">
                {{ getMethodLabel(grant) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="登录策略" min-width="220">
          <template #default="{ row }">
            <div class="tag-flow">
              <el-tag size="small" :type="row.requiresCaptcha ? 'danger' : 'success'">
                {{ row.requiresCaptcha ? "启用验证码" : "免验证码" }}
              </el-tag>
              <el-tag size="small" :type="row.encryptPassword ? 'warning' : 'info'">
                {{ row.encryptPassword ? "密码加密" : "密码明文" }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="240">
          <template #default="{ row }">
            <div class="muted-text">{{ row.description || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="令牌有效期" min-width="160">
          <template #default="{ row }">
            <div>Access：{{ row.accessTokenValidity || "-" }}</div>
            <div class="muted-text" style="font-size: 12px;">Refresh：{{ row.refreshTokenValidity || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="autoapprove" label="自动放行" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="card-actions">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Client' : '新建 Client'" width="840px">
      <el-form label-position="top" :model="form">
        <div class="form-grid">
          <el-form-item label="客户端标识" required>
            <el-input v-model="form.clientId" :disabled="Boolean(editingId)" />
          </el-form-item>
          <el-form-item label="客户端密钥" required>
            <el-input v-model="form.clientSecret" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="显示名称">
            <el-input v-model="form.displayName" placeholder="会员入口 / 平台运营" />
          </el-form-item>
          <el-form-item label="受众">
            <el-input v-model="form.audience" placeholder="会员 / 社区运营 / 平台运营" />
          </el-form-item>
          <el-form-item label="描述" class="form-span-2">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="用于登录页和客户端卡片展示" />
          </el-form-item>
          <el-form-item label="Scope" required>
            <el-input v-model="form.scope" placeholder="server" />
          </el-form-item>
          <el-form-item label="授权登录方式">
            <el-select v-model="form.authorizedGrantTypes" multiple collapse-tags collapse-tags-tooltip style="width: 100%;">
              <el-option v-for="item in loginMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="图形验证码">
            <el-switch v-model="form.requiresCaptcha" />
          </el-form-item>
          <el-form-item label="密码加密">
            <el-switch v-model="form.encryptPassword" />
          </el-form-item>
          <el-form-item label="Access Token 有效期">
            <el-input-number v-model="form.accessTokenValidity" :min="60" :step="300" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="Refresh Token 有效期">
            <el-input-number v-model="form.refreshTokenValidity" :min="300" :step="600" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="Redirect URI" class="form-span-2">
            <el-input v-model="form.webServerRedirectUri" placeholder="http://localhost:5173/callback" />
          </el-form-item>
          <el-form-item label="Authorities">
            <el-input v-model="form.authorities" placeholder="可选" />
          </el-form-item>
          <el-form-item label="Resource IDs">
            <el-input v-model="form.resourceIds" />
          </el-form-item>
          <el-form-item label="自动放行">
            <el-input v-model="form.autoapprove" placeholder="true / false" />
          </el-form-item>
          <el-form-item label="附加信息 JSON" class="form-span-2">
            <el-input
              v-model="form.additionalInformation"
              type="textarea"
              :rows="3"
              placeholder="保存时会保留未知字段。"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="card-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import { describeRequestError } from "../../api/http";
import { getMethodLabel, normalizeGrantTypes } from "../../data/clients";
import {
  parseClientAdditionalInformation,
  stringifyClientAdditionalInformation
} from "../../data/client-additional-information";
import { normalizeLoginMethodOptions } from "../../data/login-methods";
import {
  createClient,
  fetchClientPage,
  fetchLoginMethods,
  removeClients,
  syncClientCache,
  updateClient
} from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);
const clients = ref([]);
const loginMethodOptions = ref([]);
const filters = reactive({
  clientId: ""
});

const form = reactive(createEmptyForm());

function createEmptyForm() {
  return {
    id: undefined,
    clientId: "",
    clientSecret: "",
    displayName: "",
    audience: "",
    description: "",
    scope: "server",
    authorizedGrantTypes: ["password", "refresh_token"],
    requiresCaptcha: true,
    encryptPassword: true,
    accessTokenValidity: 7200,
    refreshTokenValidity: 604800,
    webServerRedirectUri: "",
    authorities: "",
    resourceIds: "",
    autoapprove: "true",
    additionalInformation: "{}"
  };
}

function resetForm() {
  Object.assign(form, createEmptyForm());
  editingId.value = null;
}

function decorateClientRow(row) {
  const metadata = parseClientAdditionalInformation(row.additionalInformation);
  return {
    ...row,
    displayName: metadata.displayName,
    audience: metadata.audience,
    description: metadata.description,
    requiresCaptcha: metadata.requiresCaptcha,
    encryptPassword: metadata.encryptPassword
  };
}

async function loadLoginMethodOptions() {
  loginMethodOptions.value = normalizeLoginMethodOptions(await fetchLoginMethods(authStore.accessToken));
}

async function loadClients() {
  loading.value = true;
  try {
    const page = await fetchClientPage(authStore.accessToken, {
      clientId: filters.clientId || undefined
    });
    clients.value = (page.records || []).map(decorateClientRow);
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  resetForm();
  const metadata = parseClientAdditionalInformation(row.additionalInformation);
  Object.assign(form, {
    ...row,
    authorizedGrantTypes: normalizeGrantTypes(row.authorizedGrantTypes),
    displayName: metadata.displayName,
    audience: metadata.audience,
    description: metadata.description,
    requiresCaptcha: metadata.requiresCaptcha,
    encryptPassword: metadata.encryptPassword,
    additionalInformation: row.additionalInformation || "{}"
  });
  editingId.value = row.id;
  dialogVisible.value = true;
}

async function submitForm() {
  saving.value = true;
  try {
    const normalizedGrantTypes = normalizeGrantTypes(form.authorizedGrantTypes);
    if (!normalizedGrantTypes.length) {
      throw new Error("至少选择一个授权登录方式");
    }

    const payload = {
      ...form,
      id: editingId.value || form.id,
      authorizedGrantTypes: normalizedGrantTypes,
      additionalInformation: stringifyClientAdditionalInformation(form.additionalInformation, {
        displayName: form.displayName,
        audience: form.audience,
        description: form.description,
        requiresCaptcha: form.requiresCaptcha,
        encryptPassword: form.encryptPassword
      })
    };

    if (editingId.value) {
      await updateClient(authStore.accessToken, payload);
    } else {
      await createClient(authStore.accessToken, payload);
    }

    await syncClientCache(authStore.accessToken);
    ElMessage.success("Client 保存成功");
    dialogVisible.value = false;
    await loadClients();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除客户端 ${row.clientId} 吗？`, "删除确认", { type: "warning" });
    await removeClients(authStore.accessToken, [row.id]);
    await syncClientCache(authStore.accessToken);
    ElMessage.success("Client 已删除");
    await loadClients();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

async function handleSyncCache() {
  try {
    await syncClientCache(authStore.accessToken);
    ElMessage.success("Client 缓存已同步");
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
}

onMounted(async () => {
  try {
    await loadLoginMethodOptions();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
  await loadClients();
});
</script>
