<template>
  <div class="console-shell">
    <aside class="glass-card console-sidebar">
      <div class="console-brand">
        <div class="client-chip">Lumen 控制台</div>
        <h2>认证与会话控制台</h2>
        <p>{{ authStore.currentClient.label }} 已登录，可以按 client 切换不同内容和治理入口。</p>
      </div>

      <el-menu :default-active="route.path" router class="console-menu">
        <template v-for="item in menuItems" :key="item.index">
          <el-sub-menu v-if="item.children" :index="item.index">
            <template #title>{{ item.label }}</template>
            <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
              {{ child.label }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">{{ item.label }}</el-menu-item>
        </template>
      </el-menu>
    </aside>

    <main class="console-main">
      <header class="glass-card console-topbar">
        <div>
          <div class="muted-text" style="font-size: 13px;">当前登录</div>
          <div class="console-title-row">
            <h1>{{ authStore.currentClient.label }}</h1>
            <el-tag round>{{ authStore.username }}</el-tag>
            <el-tag type="warning" round>{{ authStore.grantType }}</el-tag>
          </div>
        </div>
        <div class="card-actions">
          <el-button @click="goToLogin">切换 client</el-button>
          <el-button type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";

import { buildConsoleMenu } from "../../data/console";
import { useAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const menuItems = computed(() => buildConsoleMenu(authStore.selectedClientId));

async function handleLogout() {
  await authStore.logout();
  ElMessage.success("已退出登录");
  await router.push({ name: "login" });
}

async function goToLogin() {
  await authStore.logout();
  await router.push({ name: "login" });
}
</script>
