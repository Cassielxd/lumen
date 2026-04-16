<template>
  <div class="page-shell">
    <div class="login-layout">
      <section class="glass-card login-banner">
        <div class="client-chip">OAuth2 + 显式会话 + 动态客户端目录</div>
        <h1 class="section-title" style="margin-top: 18px;">多客户端登录演示</h1>
        <p class="section-subtitle" style="max-width: 620px;">
          客户端列表、授权方式、验证码和密码加密策略都从后端公开客户端目录动态加载。
        </p>

        <el-skeleton :loading="clientLoading" animated>
          <template #template>
            <div class="page-grid" style="margin-top: 28px;">
              <el-skeleton-item variant="rect" style="height: 120px; border-radius: 22px;" />
              <el-skeleton-item variant="rect" style="height: 120px; border-radius: 22px;" />
            </div>
          </template>

          <template #default>
            <div class="page-grid" style="margin-top: 28px;">
              <div
                v-for="item in clients"
                :key="item.id"
                :class="['client-option', { 'is-active': selectedClientId === item.id }]"
                @click="selectClient(item.id)"
              >
                <div class="list-row">
                  <div>
                    <h3>{{ item.label }}</h3>
                    <p>{{ item.description }}</p>
                  </div>
                  <el-tag round :type="selectedClientId === item.id ? 'warning' : 'info'">{{ item.id }}</el-tag>
                </div>
                <div class="tag-flow" style="margin-top: 12px;">
                  <el-tag v-for="method in item.methods" :key="method" size="small" effect="plain">
                    {{ methodLabels[method] || method }}
                  </el-tag>
                  <el-tag size="small" effect="plain" :type="item.requiresCaptcha ? 'danger' : 'success'">
                    {{ item.requiresCaptcha ? "启用验证码" : "免验证码" }}
                  </el-tag>
                  <el-tag size="small" effect="plain" :type="item.encryptPassword ? 'warning' : 'info'">
                    {{ item.encryptPassword ? "密码加密" : "密码明文" }}
                  </el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-skeleton>
      </section>

      <section class="glass-card login-panel">
        <div class="list-row" style="align-items: flex-start;">
          <div>
            <div class="client-chip">{{ currentClient.audience }}</div>
            <h2 class="section-title" style="margin-top: 14px;">{{ currentClient.label }}</h2>
            <p class="section-subtitle">{{ currentClient.description }}</p>
          </div>
          <el-tag type="warning" round>{{ currentClient.id || "未选择" }}</el-tag>
        </div>

        <el-alert
          style="margin-top: 20px;"
          type="info"
          :closable="false"
          show-icon
          title="演示账号：admin / 123456，短信验证码手机号：17034642999"
        />

        <template v-if="availableMethods.length">
          <el-tabs v-model="activeMethod" style="margin-top: 20px;">
            <el-tab-pane
              v-for="method in availableMethods"
              :key="method"
              :label="methodLabels[method] || method"
              :name="method"
            />
          </el-tabs>

          <el-form
            v-if="activeMethod === 'password'"
            label-position="top"
            :model="passwordForm"
            @submit.prevent="handlePasswordLogin"
          >
            <el-form-item label="用户名">
              <el-input v-model="passwordForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>

            <template v-if="currentClient.requiresCaptcha">
              <div class="captcha-row">
                <el-form-item label="图形验证码">
                  <el-input v-model="passwordForm.code" placeholder="请输入计算结果" />
                </el-form-item>
                <div>
                  <div class="muted-text" style="font-size: 13px; margin-bottom: 8px;">点击图片刷新</div>
                  <img v-if="captchaUrl" :src="captchaUrl" alt="captcha" class="captcha-image" @click="refreshCaptcha" />
                </div>
              </div>
            </template>

            <div class="card-actions">
              <el-button
                type="primary"
                :loading="passwordLoading"
                :disabled="!currentClient.clientSecret"
                @click="handlePasswordLogin"
              >
                密码登录
              </el-button>
              <el-button v-if="currentClient.requiresCaptcha" @click="refreshCaptcha">刷新验证码</el-button>
            </div>
          </el-form>

          <el-form v-else-if="activeMethod === 'otp'" label-position="top" :model="otpForm" @submit.prevent="handleOtpLogin">
            <el-form-item label="手机号">
              <el-input v-model="otpForm.mobile" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="短信验证码">
              <el-input v-model="otpForm.code" placeholder="请输入短信验证码" />
            </el-form-item>
            <div class="card-actions">
              <el-button :loading="otpSendLoading" :disabled="!currentClient.clientSecret" @click="handleSendOtp">
                发送验证码
              </el-button>
              <el-button type="primary" :loading="otpLoading" :disabled="!currentClient.clientSecret" @click="handleOtpLogin">
                验证码登录
              </el-button>
            </div>
          </el-form>

          <el-form v-else label-position="top" :model="passkeyForm" @submit.prevent="handlePasskeyLogin">
            <el-form-item label="用户名">
              <el-input v-model="passkeyForm.username" placeholder="请输入已注册 Passkey 的用户名" />
            </el-form-item>
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              title="Passkey 依赖浏览器 WebAuthn 能力，并要求当前客户端账号已完成 Passkey 注册。"
            />
            <div class="card-actions" style="margin-top: 18px;">
              <el-button
                type="primary"
                :loading="passkeyLoading"
                :disabled="!currentClient.clientSecret"
                @click="handlePasskeyLogin"
              >
                Passkey 登录
              </el-button>
            </div>
          </el-form>
        </template>

        <el-alert
          v-else
          style="margin-top: 20px;"
          type="warning"
          :closable="false"
          show-icon
          title="当前客户端未开放任何浏览器登录方式。"
        />

        <el-alert
          v-if="!currentClient.clientSecret"
          style="margin-top: 16px;"
          type="warning"
          :closable="false"
          show-icon
          title="当前客户端未暴露 clientSecret，不能直接在浏览器演示登录。"
        />

        <el-card shadow="never" style="margin-top: 24px; border-radius: 22px;">
          <template #header>
            <div class="list-row">
              <span>当前客户端特征</span>
              <el-tag round>{{ currentClient.id }}</el-tag>
            </div>
          </template>
          <div class="stats-grid">
            <div v-for="item in currentClient.metrics" :key="item.label">
              <div class="muted-text" style="font-size: 13px;">{{ item.label }}</div>
              <div style="margin-top: 6px; font-size: 20px; font-weight: 700; color: #0f172a;">{{ item.value }}</div>
              <div class="muted-text" style="margin-top: 6px; font-size: 13px;">{{ item.note }}</div>
            </div>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

import { createCaptchaUrl } from "../api/auth";
import { describeRequestError } from "../api/http";
import { resolveDefaultDashboardPath } from "../data/console";
import { BROWSER_LOGIN_METHODS, METHOD_LABELS } from "../data/clients";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

const methodLabels = METHOD_LABELS;

const clients = computed(() => authStore.availableClients);
const selectedClientId = computed(() => authStore.selectedClientId);
const currentClient = computed(() => authStore.currentClient);
const availableMethods = computed(() =>
  currentClient.value.methods.filter((method) => BROWSER_LOGIN_METHODS.includes(method))
);

const clientLoading = ref(false);
const passwordLoading = ref(false);
const otpSendLoading = ref(false);
const otpLoading = ref(false);
const passkeyLoading = ref(false);

const activeMethod = ref("password");
const captchaRandomStr = ref("");
const captchaUrl = ref("");

const passwordForm = ref({
  username: "admin",
  password: "123456",
  code: ""
});

const otpForm = ref({
  mobile: "17034642999",
  code: ""
});

const passkeyForm = ref({
  username: "admin"
});

function nextRandomStr() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `captcha-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function refreshCaptcha() {
  captchaRandomStr.value = nextRandomStr();
  captchaUrl.value = createCaptchaUrl(captchaRandomStr.value);
  passwordForm.value.code = "";
}

function syncMethodWithClient() {
  if (!availableMethods.value.length) {
    activeMethod.value = "";
  } else if (!availableMethods.value.includes(activeMethod.value)) {
    activeMethod.value = availableMethods.value[0];
  }

  if (currentClient.value.requiresCaptcha && availableMethods.value.includes("password")) {
    refreshCaptcha();
  } else {
    captchaRandomStr.value = "";
    captchaUrl.value = "";
    passwordForm.value.code = "";
  }
}

watch(
  () => authStore.selectedClientId,
  () => syncMethodWithClient(),
  { immediate: true }
);

watch(activeMethod, (method) => {
  if (method === "password" && currentClient.value.requiresCaptcha && !captchaUrl.value) {
    refreshCaptcha();
  }
});

function selectClient(clientId) {
  authStore.setSelectedClient(clientId);
}

async function loadClients() {
  clientLoading.value = true;
  try {
    await authStore.loadClientCatalog();
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    clientLoading.value = false;
  }
}

async function handlePasswordLogin() {
  passwordLoading.value = true;
  try {
    await authStore.loginWithPassword({
      ...passwordForm.value,
      randomStr: captchaRandomStr.value
    });
    ElMessage.success("密码登录成功");
    await router.push(resolveDefaultDashboardPath(authStore.selectedClientId));
  } catch (error) {
    ElMessage.error(describeRequestError(error));
    if (currentClient.value.requiresCaptcha) {
      refreshCaptcha();
    }
  } finally {
    passwordLoading.value = false;
  }
}

async function handleSendOtp() {
  otpSendLoading.value = true;
  try {
    const result = await authStore.sendOtpCode(otpForm.value.mobile);
    if (result?.code) {
      otpForm.value.code = result.code;
    }
    ElMessage.success(result?.reused ? "验证码复用成功" : "验证码已生成并回填");
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    otpSendLoading.value = false;
  }
}

async function handleOtpLogin() {
  otpLoading.value = true;
  try {
    await authStore.loginWithOtp(otpForm.value);
    ElMessage.success("验证码登录成功");
    await router.push(resolveDefaultDashboardPath(authStore.selectedClientId));
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    otpLoading.value = false;
  }
}

async function handlePasskeyLogin() {
  passkeyLoading.value = true;
  try {
    await authStore.loginWithPasskey(passkeyForm.value.username);
    ElMessage.success("Passkey 登录成功");
    await router.push(resolveDefaultDashboardPath(authStore.selectedClientId));
  } catch (error) {
    ElMessage.error(describeRequestError(error));
  } finally {
    passkeyLoading.value = false;
  }
}

onMounted(async () => {
  await loadClients();
});
</script>
