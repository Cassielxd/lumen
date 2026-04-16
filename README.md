# LUMEN

## 项目介绍

LUMEN 当前是一个围绕认证、授权、账号治理、会话治理构建的单体系统。

当前交付重点：

- 多入口登录
- 多凭证认证
- 显式会话管理
- 平台治理与前端演示

技术栈：

- 后端：Spring Boot + Spring Authorization Server
- 前端：Vue 3 + Element Plus

---

## 需求理解

表面需求是三类主体独立登录：

- 会员
- 社区运营
- 平台运营

但当前项目对需求的理解不止是“三个固定主体”，而是：

- 当前先有 3 个主要业务入口
- 实际模型必须支持 `1..N` 个入口
- 每个入口可以配置不同登录方式
- 每个入口下可以有独立账号空间和独立会话

所以这里真正的核心不是“只有三类人”，而是：

> 系统要支持按入口隔离的多主体认证与会话管理，并且入口数量可扩展。

当前入口映射：

- `app`：会员入口
- `daemon`：社区运营入口
- `lumen`：平台运营入口
- `test`：联调与演示入口

也就是说，当前是 3 个业务入口 + 1 个测试入口，但模型本身支持继续扩展更多入口。

---

## 为什么选择 OAuth2

当前项目选择 OAuth2，不是为了“跟风标准”，而是因为它和这个项目的契合度高，而且扩展性足够好。

### 契合度

OAuth2 天然就有 `client` 这个边界，而当前项目的核心需求正好就是“按入口隔离”：

- 不同入口独立登录
- 不同入口开放不同登录方式
- 不同入口下有独立账号空间
- 不同入口下登录后进入不同工作台

这和 OAuth2 里的 `client` 模型是天然对齐的。

在当前项目里：

- `client` 表示登录入口
- `authorized_grant_types` 表示当前入口允许哪些登录方式
- token 表示访问凭据
- 显式 `session` 表示运行态会话

所以 OAuth2 在这里不是“额外增加的一层”，而是刚好适合作为认证主链的协议基础。

### 扩展性

OAuth2 的好处是：协议边界稳定，但认证方式可以继续扩展。

当前已经基于扩展 grant 支持：

- `password`
- `otp`
- `passkey`

后续如果继续扩展，也可以沿着同一条主链增加：

- 新 grant
- 新 converter
- 新 provider
- 新 credential 类型

不用重新设计一套新的登录协议。

这意味着：

- 扩入口，不推翻现有模型
- 扩登录方式，不推翻现有模型
- 扩会话治理，不推翻现有模型

### 为什么不用额外再造一套授权模型

因为 OAuth2 自己已经有 client 和 grant_type 这两个关键点。

如果再额外设计一套“client-登录方式关系模型”，会带来：

- 双重配置
- 配置不一致
- 维护复杂度上升

所以当前直接复用：

- `sys_oauth_client_details.authorized_grant_types`

由它作为“当前入口允许哪些登录方式”的唯一来源。

---

## 架构思路

当前方案以 `client` 为登录入口边界，以 `account + credential + session` 为认证主链。

核心对象：

- `client`
  - 表示登录入口
  - 控制当前入口开放哪些登录方式
- `auth_account`
  - 表示某个入口下的认证账号
- `auth_account_credential`
  - 表示账号绑定的认证凭证
  - 当前支持 `PASSWORD / OTP / PASSKEY`
- `auth_session`
  - 表示显式会话
  - 用于多设备在线、会话查看、会话撤销

登录方式不额外造一套授权模型，而是直接复用 OAuth2 的：

- `sys_oauth_client_details.authorized_grant_types`

当前已支持：

- `password`
- `otp`
- `passkey`

这样设计的好处：

- 入口扩展简单，天然支持 `1..N`
- 登录方式扩展简单，直接扩 grant
- 同一个人可以在不同入口下拥有独立账号空间
- 多设备会话可以显式治理，不只靠 token

---

## 当前模块

```text
lumen
├─ lumen-boot        # 单体启动入口
├─ lumen-auth        # OAuth2 授权服务、扩展 grant、Passkey 登录
├─ lumen-upms        # Client、账号、登录方式、会话、Passkey 管理
├─ lumen-common      # 安全与基础组件
├─ lumen-web-demo    # Vue 3 + Element Plus 演示前端
├─ db                # SQL 脚本
└─ docs              # 设计文档与截图
```

---

## 主流程截图

### 动态登录页

登录页根据后端公开的 client 目录动态展示入口、登录方式、验证码策略和密码加密策略。

![动态登录页](docs/screenshots/01-login-page.png)

### 会员入口

![会员入口](docs/screenshots/02-member-overview.png)

### 社区运营入口

![社区运营入口](docs/screenshots/03-community-overview.png)

### 平台运营入口

![平台运营入口](docs/screenshots/04-platform-overview.png)

### 平台 Client 管理

![平台 Client 管理](docs/screenshots/05-platform-client-management.png)

---

## 当前能力

- 多入口独立登录
- 多设备会话管理
- `password / otp / passkey`
- 动态 client 登录页
- 平台侧 Client 管理
- 平台侧登录方式管理
- 平台侧账号创建
- Passkey 注册、登录、管理

---

## 演示地址

- 后端：`http://127.0.0.1:9999/admin`
- 前端：`http://localhost:5173`

演示账号：

- 用户名：`admin`
- 密码：`123456`
- OTP 手机号：`17034642999`
