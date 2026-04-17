import { createRouter, createWebHistory } from "vue-router";

import { isPlatformClient, resolveDefaultDashboardPath } from "../data/console";
import { pinia } from "../stores";
import { useAuthStore } from "../stores/auth";
import ConsoleLayout from "../views/layouts/ConsoleLayout.vue";
import LoginView from "../views/LoginView.vue";
import OverviewView from "../views/OverviewView.vue";
import PlatformAccountCreateView from "../views/platform/PlatformAccountCreateView.vue";
import PlatformClientManagementView from "../views/platform/PlatformClientManagementView.vue";
import PlatformCredentialManagementView from "../views/platform/PlatformCredentialManagementView.vue";
import PlatformAuditManagementView from "../views/platform/PlatformAuditManagementView.vue";
import PlatformLoginMethodManagementView from "../views/platform/PlatformLoginMethodManagementView.vue";
import PlatformSessionManagementView from "../views/platform/PlatformSessionManagementView.vue";

const routes = [
  {
    path: "/",
    redirect: "/login"
  },
  {
    path: "/login",
    name: "login",
    component: LoginView
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: ConsoleLayout,
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: "overview",
        name: "dashboard-overview",
        component: OverviewView,
        meta: {
          requiresAuth: true
        }
      },
      {
        path: "platform/clients",
        name: "platform-clients",
        component: PlatformClientManagementView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      },
      {
        path: "platform/login-methods",
        name: "platform-login-methods",
        component: PlatformLoginMethodManagementView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      },
      {
        path: "platform/accounts",
        name: "platform-accounts",
        component: PlatformAccountCreateView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      },
      {
        path: "platform/credentials",
        name: "platform-credentials",
        component: PlatformCredentialManagementView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      },
      {
        path: "platform/sessions",
        name: "platform-sessions",
        component: PlatformSessionManagementView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      },
      {
        path: "platform/audit",
        name: "platform-audit",
        component: PlatformAuditManagementView,
        meta: {
          requiresAuth: true,
          requiresPlatform: true
        }
      }
    ]
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia);
  authStore.hydrate();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login" };
  }

  if (to.name === "login" && authStore.isAuthenticated) {
    return resolveDefaultDashboardPath(authStore.selectedClientId);
  }

  if (authStore.isAuthenticated && to.path === "/dashboard") {
    return resolveDefaultDashboardPath(authStore.selectedClientId);
  }

  if (to.meta.requiresPlatform && !isPlatformClient(authStore.currentClient.id)) {
    return "/dashboard/overview";
  }

  return true;
});
