const EMPTY_CLIENT = {
  id: "",
  clientId: "",
  clientSecret: "",
  secret: "",
  label: "未选择客户端",
  audience: "客户端",
  description: "请先从后端公开客户端目录加载登录入口。",
  methods: [],
  requiresCaptcha: true,
  encryptPassword: true,
  heroFrom: "#334155",
  heroTo: "#111827",
  metrics: [
    { label: "来源", value: "动态加载", note: "可见客户端来自后端公开目录。" },
    { label: "认证能力", value: "配置驱动", note: "浏览器可用登录方式来自 authorized_grant_types。" },
    { label: "展示元数据", value: "待加载", note: "显示名称、受众和描述来自客户端附加信息。" }
  ],
  panels: [{ title: "动态客户端", body: "在平台运营端配置客户端后，这里会自动展示。" }]
};

const BASE_CLIENT_PROFILES = {
  test: {
    heroFrom: "#f97316",
    heroTo: "#0f172a",
    metrics: [
      { label: "登录门槛", value: "低", note: "适合做令牌、会话、Passkey 联调验证。" },
      { label: "关注点", value: "认证链路", note: "适合验证 grant_type、sid 和 refresh token 行为。" },
      { label: "定位", value: "调试沙盒", note: "用于开发和冒烟测试。" }
    ],
    panels: [
      { title: "联调控制台", body: "适合快速验证授权流、会话流和退出链路。" },
      { title: "会话检查", body: "可以直接验证会话列表、踢下线和登出失效。" },
      { title: "Passkey 验证", body: "适合做 Passkey 注册与登录演示。" }
    ]
  },
  app: {
    heroFrom: "#0f766e",
    heroTo: "#0f172a",
    metrics: [
      { label: "受众", value: "会员", note: "面向最终会员用户。" },
      { label: "认证策略", value: "多凭证", note: "支持密码、短信验证码和 Passkey 并行开启。" },
      { label: "体验定位", value: "自助服务", note: "适合账号中心、权益和会话管理页面。" }
    ],
    panels: [
      { title: "会员工作区", body: "适合演示终端用户登录后的内容承载。" },
      { title: "安全中心", body: "适合演示短信验证码、Passkey 和设备会话管理。" },
      { title: "扩展空间", body: "后续可以接入会员资料、订单和社区内容。" }
    ]
  },
  daemon: {
    heroFrom: "#2563eb",
    heroTo: "#111827",
    metrics: [
      { label: "受众", value: "社区运营", note: "面向社区运营人员。" },
      { label: "认证策略", value: "收敛", note: "通常比会员端更克制。" },
      { label: "体验定位", value: "执行台", note: "适合排班、审核和待办处理。" }
    ],
    panels: [
      { title: "运营看板", body: "适合值班、审核、反馈和通知处理。" },
      { title: "执行控制台", body: "适合承载社区运营任务和流程。" },
      { title: "账号边界", body: "即使是同一个自然人，在这里也是独立客户端账号。" }
    ]
  },
  lumen: {
    heroFrom: "#7c3aed",
    heroTo: "#111827",
    metrics: [
      { label: "受众", value: "平台运营", note: "面向平台运营和治理人员。" },
      { label: "认证策略", value: "治理型", note: "适合做客户端、登录方式和账号管理。" },
      { label: "体验定位", value: "平台控制台", note: "用于治理认证、会话与账号体系。" }
    ],
    panels: [
      { title: "控制平面", body: "展示平台级认证、会话与客户端治理能力。" },
      { title: "策略中心", body: "在这里管理客户端授权方式和展示元数据。" },
      { title: "扩展空间", body: "后续可继续扩展更细的权限和安全治理。" }
    ]
  }
};

export const METHOD_LABELS = {
  password: "密码登录",
  otp: "短信验证码",
  passkey: "Passkey",
  authorization_code: "授权码",
  refresh_token: "刷新令牌",
  client_credentials: "客户端模式",
  implicit: "隐式模式",
  mobile: "兼容手机号模式"
};

export const BROWSER_LOGIN_METHODS = ["password", "otp", "passkey"];

export function normalizeGrantTypes(grantTypes) {
  if (Array.isArray(grantTypes)) {
    return grantTypes
      .filter(Boolean)
      .map((item) => String(item).trim())
      .filter((item) => item && item !== "null" && item !== "undefined");
  }

  if (typeof grantTypes === "string") {
    return grantTypes
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item && item !== "null" && item !== "undefined");
  }

  return [];
}

export function getMethodLabel(method) {
  return METHOD_LABELS[method] || method;
}

function baseProfileFor(clientId) {
  return (
    BASE_CLIENT_PROFILES[clientId] || {
      heroFrom: "#334155",
      heroTo: "#111827",
      metrics: [
        { label: "来源", value: "动态加载", note: "这个客户端来自后端公开目录。" },
        { label: "认证能力", value: "可配置", note: "实际可用登录方式由 authorized_grant_types 决定。" },
        { label: "展示信息", value: "元数据", note: "显示名称、受众和描述来自 additional_information。" }
      ],
      panels: [{ title: "动态客户端", body: "这是后端公开目录动态暴露的客户端。" }]
    }
  );
}

function decorateClientOption(rawClient = {}) {
  const clientId = rawClient.clientId || rawClient.id || "";
  const base = baseProfileFor(clientId);
  const grantTypesProvided =
    Object.prototype.hasOwnProperty.call(rawClient, "authorizedGrantTypes") ||
    Object.prototype.hasOwnProperty.call(rawClient, "methods");

  const methods = grantTypesProvided
    ? normalizeGrantTypes(rawClient.authorizedGrantTypes ?? rawClient.methods)
    : [];

  return {
    ...EMPTY_CLIENT,
    ...base,
    ...rawClient,
    id: clientId,
    clientId,
    secret: rawClient.clientSecret ?? rawClient.secret ?? "",
    clientSecret: rawClient.clientSecret ?? rawClient.secret ?? "",
    label: rawClient.displayName || rawClient.label || clientId || EMPTY_CLIENT.label,
    audience: rawClient.audience || EMPTY_CLIENT.audience,
    description: rawClient.description || EMPTY_CLIENT.description,
    methods,
    requiresCaptcha:
      rawClient.requiresCaptcha === undefined ? EMPTY_CLIENT.requiresCaptcha : Boolean(rawClient.requiresCaptcha),
    encryptPassword:
      rawClient.encryptPassword === undefined ? EMPTY_CLIENT.encryptPassword : Boolean(rawClient.encryptPassword)
  };
}

export function buildClientCatalog(publicClients = []) {
  if (!Array.isArray(publicClients) || !publicClients.length) {
    return [];
  }

  const seen = new Set();

  return publicClients
    .filter((item) => {
      const clientId = item?.clientId || item?.id;
      if (!clientId || seen.has(clientId)) {
        return false;
      }
      seen.add(clientId);
      return true;
    })
    .map((item) => decorateClientOption(item));
}

export function getClientOption(clientId, catalog = []) {
  const normalizedCatalog = buildClientCatalog(catalog);
  return normalizedCatalog.find((item) => item.id === clientId) || normalizedCatalog[0] || EMPTY_CLIENT;
}
