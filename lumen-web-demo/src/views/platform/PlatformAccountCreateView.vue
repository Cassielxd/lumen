<template>
  <section class="page-grid">
    <div class="workspace-grid">
      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <div>
              <div style="font-size: 16px; font-weight: 700; color: #0f172a;">新建账号</div>
              <div class="section-subtitle">一个用户可绑定多个 client；当前版本角色和岗位仍然是用户级，不按 client 拆分。</div>
            </div>
            <el-button type="primary" :loading="saving" @click="submitForm">创建账号</el-button>
          </div>
        </template>

        <el-form label-position="top" :model="form">
          <div class="form-grid">
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" />
            </el-form-item>
            <el-form-item label="登录密码" required>
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="显示名">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="部门">
              <el-select v-model="form.deptId" clearable style="width: 100%;">
                <el-option v-for="item in departments" :key="item.deptId" :label="item.name" :value="item.deptId" />
              </el-select>
            </el-form-item>
            <el-form-item label="绑定 Client" required>
              <el-select v-model="form.clientIds" multiple collapse-tags collapse-tags-tooltip style="width: 100%;">
                <el-option v-for="item in clientOptions" :key="item.clientId" :label="item.clientId" :value="item.clientId" />
              </el-select>
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="form.role" multiple collapse-tags collapse-tags-tooltip style="width: 100%;">
                <el-option v-for="item in roles" :key="item.roleId" :label="item.roleName" :value="item.roleId" />
              </el-select>
            </el-form-item>
            <el-form-item label="岗位">
              <el-select v-model="form.post" multiple collapse-tags collapse-tags-tooltip style="width: 100%;">
                <el-option v-for="item in posts" :key="item.postId" :label="item.postName" :value="item.postId" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
      </el-card>

      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="list-row">
            <span>最近账号</span>
            <el-button type="primary" plain size="small" @click="loadUsers">刷新</el-button>
          </div>
        </template>

        <el-table :data="users" v-loading="userLoading" empty-text="暂无账号数据">
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="createTime" label="创建时间" min-width="180" />
        </el-table>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";

import { describeRequestError } from "../../api/http";
import {
  createUser,
  fetchClientPage,
  fetchDepartments,
  fetchPosts,
  fetchRoles,
  fetchUserPage
} from "../../api/platform";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();

const saving = ref(false);
const userLoading = ref(false);
const roles = ref([]);
const posts = ref([]);
const departments = ref([]);
const clientOptions = ref([]);
const users = ref([]);

const form = reactive(createEmptyForm());

function createEmptyForm() {
  return {
    username: "",
    password: "",
    name: "",
    nickname: "",
    phone: "",
    email: "",
    deptId: undefined,
    role: [],
    post: [],
    clientIds: ["app"]
  };
}

function resetForm() {
  Object.assign(form, createEmptyForm());
}

async function loadMeta() {
  const [roleData, departmentData, postData, clientPage] = await Promise.all([
    fetchRoles(authStore.accessToken),
    fetchDepartments(authStore.accessToken),
    fetchPosts(authStore.accessToken),
    fetchClientPage(authStore.accessToken)
  ]);
  roles.value = roleData || [];
  departments.value = departmentData || [];
  posts.value = postData || [];
  clientOptions.value = clientPage.records || [];
}

async function loadUsers() {
  userLoading.value = true;
  try {
    const page = await fetchUserPage(authStore.accessToken, { size: 8 });
    users.value = page.records || [];
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    userLoading.value = false;
  }
}

async function submitForm() {
  saving.value = true;
  try {
    await createUser(authStore.accessToken, form);
    ElMessage.success("账号创建成功");
    resetForm();
    await loadUsers();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  try {
    await loadMeta();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  }
  await loadUsers();
});
</script>
