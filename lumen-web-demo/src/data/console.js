export function isPlatformClient(clientId) {
  return clientId === "lumen";
}

export function resolveDefaultDashboardPath(clientId) {
  return isPlatformClient(clientId) ? "/dashboard/platform/clients" : "/dashboard/overview";
}

export function buildConsoleMenu(clientId) {
  const items = [
    {
      index: "/dashboard/overview",
      label: "工作台总览"
    }
  ];

  if (isPlatformClient(clientId)) {
    items.push({
      index: "platform-governance",
      label: "平台治理",
      children: [
        { index: "/dashboard/platform/clients", label: "Client 管理" },
        { index: "/dashboard/platform/login-methods", label: "登录方式管理" },
        { index: "/dashboard/platform/accounts", label: "账号创建" }
      ]
    });
  }

  return items;
}
