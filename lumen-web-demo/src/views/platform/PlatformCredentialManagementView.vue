<template>
  <section class="page-grid">
    <el-card class="glass-card" shadow="never">
      <template #header>
        <div class="list-row">
          <div>
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">凭证与标识治理</div>
            <div class="section-subtitle">
              统一查看账号凭证状态，并治理登录标识。密码、OTP、Passkey 和标识都按账号维度管理。
            </div>
          </div>
          <div class="card-actions">
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" @click="loadRows">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar-row">
        <el-select v-model="filters.clientId" clearable placeholder="按 Client 筛选" style="width: 180px;">
          <el-option v-for="item in clientOptions" :key="item.clientId" :label="item.clientId" :value="item.clientId" />
        </el-select>
        <el-input
          v-model="filters.loginName"
          clearable
          placeholder="按用户名筛选"
          style="max-width: 220px;"
          @keyup.enter="loadRows"
        />
        <el-input
          v-model="filters.phone"
          clearable
          placeholder="按手机号筛选"
          style="max-width: 220px;"
          @keyup.enter="loadRows"
        />
        <el-button type="primary" plain @click="loadRows">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" empty-text="暂无治理数据">
        <el-table-column type="expand" width="56">
          <template #default="{ row }">
            <div class="identifier-panel">
              <div class="list-row" style="margin-bottom: 12px;">
                <div>
                  <div style="font-weight: 700; color: #0f172a;">账号标识</div>
                  <div class="section-subtitle">主标识来自当前账号资料，扩展标识可在这里补录或删除。</div>
                </div>
                <el-button size="small" type="primary" plain @click="openIdentifierDialog(row)">添加标识</el-button>
              </div>

              <el-table :data="row.identifiers || []" size="small" empty-text="暂无标识数据">
                <el-table-column prop="identifierType" label="类型" width="120">
                  <template #default="{ row: identifier }">
                    <el-tag size="small" effect="plain">{{ identifier.identifierType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="identifierValue" label="标识值" min-width="220" />
                <el-table-column label="主标识" width="100">
                  <template #default="{ row: identifier }">
                    <el-tag size="small" :type="identifier.primaryFlag === '1' ? 'success' : 'info'">
                      {{ identifier.primaryFlag === "1" ? "是" : "否" }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="verifiedAt" label="校验时间" min-width="180" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row: identifier }">
                    <el-tag size="small" :type="identifier.status === '0' ? 'success' : 'warning'">
                      {{ identifier.status === "0" ? "正常" : "停用" }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row: identifier }">
                    <el-button
                      link
                      type="danger"
                      :disabled="identifier.primaryFlag === '1'"
                      @click="handleRemoveIdentifier(row, identifier)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="clientId" label="Client" width="120" />
        <el-table-column prop="loginName" label="用户名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="账号状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.accountStatus === '0' ? 'success' : 'warning'">
              {{ row.accountStatus === "0" ? "正常" : "锁定" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密码" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.passwordStatus === '0' ? 'success' : 'info'">
              {{ row.passwordStatus === "0" ? "已配置" : "未配置" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="OTP" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.otpStatus === '0' ? 'success' : 'info'">
              {{ row.otpStatus === "0" ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Passkey" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.passkeyCount > 0 ? 'success' : 'info'">
              {{ row.passkeyCount || 0 }} 个
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标识" min-width="220">
          <template #default="{ row }">
            <div class="tag-flow">
              <el-tag
                v-for="identifier in row.identifiers || []"
                :key="identifier.identifierId"
                size="small"
                :type="identifier.primaryFlag === '1' ? 'success' : 'info'"
              >
                {{ identifier.identifierType }}: {{ identifier.identifierValue }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="latestVerifiedAt" label="最近校验" min-width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="card-actions">
              <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
              <el-button
                link
                :type="row.otpStatus === '0' ? 'warning' : 'success'"
                @click="handleToggleOtp(row)"
              >
                {{ row.otpStatus === "0" ? "停用 OTP" : "启用 OTP" }}
              </el-button>
              <el-button link type="danger" :disabled="!row.passkeyCount" @click="handleClearPasskeys(row)">
                清空 Passkey
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="identifierDialogVisible" title="添加账号标识" width="520px">
      <el-form label-position="top" :model="identifierForm">
        <el-form-item label="账号">
          <el-input :model-value="identifierTargetLabel" disabled />
        </el-form-item>
        <el-form-item label="标识类型" required>
          <el-select v-model="identifierForm.identifierType" style="width: 100%;">
            <el-option label="用户名" value="USERNAME" />
            <el-option label="手机号" value="PHONE" />
            <el-option label="邮箱" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="标识值" required>
          <el-input v-model="identifierForm.identifierValue" placeholder="请输入标识值" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="card-actions">
          <el-button @click="identifierDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="identifierSaving" @click="handleSaveIdentifier">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

import { describeRequestError } from "../../api/http";
import {
  clearAccountPasskeys,
  fetchClientPage,
  fetchAccountCredentialGovernance,
  fetchAccountIdentifiers,
  removeAccountIdentifier,
  resetAccountPassword,
  saveAccountIdentifier,
  updateAccountOtpStatus
} from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const loading = ref(false);
const rows = ref([]);
const clientOptions = ref([]);
const identifierDialogVisible = ref(false);
const identifierSaving = ref(false);
const identifierTargetRow = ref(null);

const filters = reactive({
  clientId: "",
  loginName: "",
  phone: ""
});

const identifierForm = reactive({
  accountId: undefined,
  identifierType: "EMAIL",
  identifierValue: ""
});

const identifierTargetLabel = computed(() => {
  if (!identifierTargetRow.value) {
    return "";
  }
  return `${identifierTargetRow.value.clientId} / ${identifierTargetRow.value.loginName}`;
});

function resetFilters() {
  filters.clientId = "";
  filters.loginName = "";
  filters.phone = "";
  loadRows();
}

function resetIdentifierForm() {
  identifierForm.accountId = undefined;
  identifierForm.identifierType = "EMAIL";
  identifierForm.identifierValue = "";
  identifierTargetRow.value = null;
}

async function loadClientOptions() {
  const page = await fetchClientPage(authStore.accessToken);
  clientOptions.value = page.records || [];
}

async function loadRows() {
  loading.value = true;
  try {
    rows.value = await fetchAccountCredentialGovernance(authStore.accessToken, {
      clientId: filters.clientId || undefined,
      loginName: filters.loginName || undefined,
      phone: filters.phone || undefined
    });
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    loading.value = false;
  }
}

async function reloadIdentifiers(row) {
  row.identifiers = await fetchAccountIdentifiers(authStore.accessToken, row.accountId);
}

function openIdentifierDialog(row) {
  resetIdentifierForm();
  identifierTargetRow.value = row;
  identifierForm.accountId = row.accountId;
  identifierDialogVisible.value = true;
}

async function handleSaveIdentifier() {
  identifierSaving.value = true;
  try {
    await saveAccountIdentifier(authStore.accessToken, {
      accountId: identifierForm.accountId,
      identifierType: identifierForm.identifierType,
      identifierValue: identifierForm.identifierValue
    });
    ElMessage.success("账号标识已保存");
    identifierDialogVisible.value = false;
    if (identifierTargetRow.value) {
      await reloadIdentifiers(identifierTargetRow.value);
    }
    await loadRows();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    identifierSaving.value = false;
  }
}

async function handleRemoveIdentifier(row, identifier) {
  try {
    await ElMessageBox.confirm(
      `确认删除 ${identifier.identifierType}:${identifier.identifierValue} 吗？`,
      "删除标识",
      { type: "warning" }
    );
    await removeAccountIdentifier(authStore.accessToken, identifier.identifierId);
    ElMessage.success("账号标识已删除");
    await reloadIdentifiers(row);
    await loadRows();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

async function handleResetPassword(row) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.clientId}/${row.loginName} 的新密码`, "重置密码", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      inputType: "password",
      inputPattern: /^.{6,}$/,
      inputErrorMessage: "密码至少 6 位"
    });
    await resetAccountPassword(authStore.accessToken, {
      accountId: row.accountId,
      newPassword: value
    });
    ElMessage.success("密码已重置");
    await loadRows();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

async function handleToggleOtp(row) {
  try {
    const nextStatus = row.otpStatus === "0" ? "9" : "0";
    await updateAccountOtpStatus(authStore.accessToken, {
      accountId: row.accountId,
      status: nextStatus
    });
    ElMessage.success(nextStatus === "0" ? "OTP 已启用" : "OTP 已停用");
    await loadRows();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
}

async function handleClearPasskeys(row) {
  try {
    await ElMessageBox.confirm(`确认清空 ${row.clientId}/${row.loginName} 的全部 Passkey 吗？`, "Passkey 治理", {
      type: "warning"
    });
    await clearAccountPasskeys(authStore.accessToken, row.accountId);
    ElMessage.success("Passkey 已清空");
    await loadRows();
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
  await loadRows();
});
</script>
