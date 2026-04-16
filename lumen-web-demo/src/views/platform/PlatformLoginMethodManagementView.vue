<template>
  <section class="page-grid">
    <el-card class="glass-card" shadow="never">
      <template #header>
        <div class="list-row">
          <div>
            <div style="font-size: 16px; font-weight: 700; color: #0f172a;">登录方式管理</div>
            <div class="section-subtitle">这里只维护 grant_types 字典目录，client 是否启用仍由 authorized_grant_types 决定。</div>
          </div>
          <div class="card-actions">
            <el-button @click="loadLoginMethods">刷新</el-button>
            <el-button type="primary" @click="openCreateDialog">新增登录方式</el-button>
          </div>
        </div>
      </template>

      <el-table :data="loginMethods" v-loading="loading" empty-text="暂无登录方式">
        <el-table-column prop="value" label="Grant Type" min-width="180" />
        <el-table-column prop="label" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="remarks" label="备注" min-width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="card-actions">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑登录方式' : '新增登录方式'" width="640px">
      <el-form label-position="top" :model="form">
        <div class="form-grid">
          <el-form-item label="Grant Type" required>
            <el-input v-model="form.value" :disabled="Boolean(editingId)" placeholder="例如 magic_link" />
          </el-form-item>
          <el-form-item label="显示名称" required>
            <el-input v-model="form.label" placeholder="例如 魔法链接" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remarks" />
          </el-form-item>
          <el-form-item label="描述" class="form-span-2">
            <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { normalizeLoginMethodOptions } from "../../data/login-methods";
import {
  createLoginMethod,
  fetchLoginMethods,
  removeLoginMethod,
  updateLoginMethod
} from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);
const loginMethods = ref([]);
const form = reactive(createEmptyForm());

function createEmptyForm() {
  return {
    id: undefined,
    value: "",
    label: "",
    description: "",
    sortOrder: 0,
    remarks: ""
  };
}

function resetForm() {
  Object.assign(form, createEmptyForm());
  editingId.value = null;
}

async function loadLoginMethods() {
  loading.value = true;
  try {
    loginMethods.value = normalizeLoginMethodOptions(await fetchLoginMethods(authStore.accessToken));
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
  Object.assign(form, row);
  editingId.value = row.id;
  dialogVisible.value = true;
}

async function submitForm() {
  saving.value = true;
  try {
    const payload = { ...form };
    if (editingId.value) {
      await updateLoginMethod(authStore.accessToken, payload);
    } else {
      await createLoginMethod(authStore.accessToken, payload);
    }
    ElMessage.success("登录方式保存成功");
    dialogVisible.value = false;
    await loadLoginMethods();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除登录方式 ${row.value} 吗？`, "删除确认", { type: "warning" });
    await removeLoginMethod(authStore.accessToken, row.id);
    ElMessage.success("登录方式已删除");
    await loadLoginMethods();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(describeRequestError(error));
    }
  }
}

onMounted(async () => {
  await loadLoginMethods();
});
</script>
