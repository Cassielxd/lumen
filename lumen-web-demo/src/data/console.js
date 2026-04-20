export function isPlatformClient(clientId) {
  return clientId === "lumen";
}

const PLATFORM_GOVERNANCE_ITEMS = [
  { index: "/dashboard/platform/clients", label: "Client 管理" },
  { index: "/dashboard/platform/login-methods", label: "登录方式管理" },
  { index: "/dashboard/platform/accounts", label: "账号创建" },
  { index: "/dashboard/platform/credentials", label: "凭证治理", permission: "auth_account_manage" },
  { index: "/dashboard/platform/sessions", label: "会话治理", permission: "auth_session_manage" },
  { index: "/dashboard/platform/audit", label: "审计日志", permission: "sys_log_view" }
];

export function hasConsolePermission(permissions, permission) {
  if (!permission) {
    return true;
  }
  return Array.isArray(permissions) && permissions.includes(permission);
}

export function getAccessiblePlatformItems(permissions = []) {
  return PLATFORM_GOVERNANCE_ITEMS.filter((item) => hasConsolePermission(permissions, item.permission));
}

export function resolveDefaultDashboardPath(clientId, permissions = []) {
  if (!isPlatformClient(clientId)) {
    return "/dashboard/overview";
  }
  return getAccessiblePlatformItems(permissions)[0]?.index || "/dashboard/overview";
}

export function buildConsoleMenu(clientId, permissions = []) {
  const items = [
    {
      index: "/dashboard/overview",
      label: "工作台总览"
    }
  ];

  if (isPlatformClient(clientId)) {
    const children = getAccessiblePlatformItems(permissions);

    if (children.length) {
      items.push({
        index: "platform-governance",
        label: "平台治理",
        children
      });
    }
  }

  return items;
}
