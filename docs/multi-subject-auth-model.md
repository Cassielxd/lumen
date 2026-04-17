# 多入口认证与会话模型

## 定位

当前项目采用的是以 `client` 为中心的 OAuth2 认证模型。

在这一版设计里：

- `client` 表示业务入口边界
- `account` 表示实际登录主体
- `identifier` 表示账号登录标识
- `credential` 表示认证凭证
- `grant_type` 表示令牌获取和登录方式
- `session` 表示显式会话

当前不单独引入 `subject_type` 这一层，而是由 `client` 承担入口隔离。

这和当前业务更契合：

- `app` 对应会员入口
- `daemon` 对应社区运营入口
- `lumen` 对应平台运营入口
- 后续可以继续扩展到 `1..N` 个入口

---

## 核心对象

## 1. Client

`client` 是登录入口，也是协议边界。

在当前代码里，`client` 由 `sys_oauth_client_details` 承载，核心字段包括：

- `client_id`
- `client_secret`
- `scope`
- `resource_ids`
- `authorized_grant_types`
- token 生命周期配置

关键点：

- `authorized_grant_types` 就是当前入口允许的登录方式来源
- 不再额外设计第二套“登录方式授权表”

## 2. Account

`auth_account` 表示某个入口下的认证账号。

它的职责是：

- 绑定用户与入口
- 作为认证后的实际账号主体
- 承接该入口下的凭证、标识、会话

同一个自然人可以在多个 `client` 下拥有多个 `auth_account`。

## 3. Identifier

`auth_account_identifier` 表示账号下的登录标识。

当前支持：

- `USERNAME`
- `PHONE`
- `EMAIL`

这一层的作用是把“账号”和“登录标识”拆开，便于后续扩展：

- 一个账号多个登录标识
- 标识启停
- 主标识与扩展标识并存

## 4. Credential

`auth_account_credential` 表示账号绑定的认证凭证。

当前支持：

- `PASSWORD`
- `OTP`
- `PASSKEY`

当前各凭证职责：

- `PASSWORD`：账号密码
- `OTP`：短信验证码登录能力
- `PASSKEY`：WebAuthn Passkey 凭证

## 5. Session

`auth_session` 表示显式会话。

它是会话治理的真相源，用于：

- 多设备在线
- 当前设备退出
- 其他设备退出
- 平台侧会话查询
- 平台侧会话撤销

当前设计不是只依赖 Redis 中的 token 缓存，而是把会话显式建模出来。

---

## 为什么选择这套模型

## 1. 与 OAuth2 协议边界一致

当前项目的核心边界是“入口边界”，而 OAuth2 天然提供了 `client`。

所以当前模型直接对齐：

- 入口边界 -> `client`
- 登录方式 -> `grant_type`
- 登录账号 -> `auth_account`
- 登录标识 -> `auth_account_identifier`
- 认证凭证 -> `auth_account_credential`
- 运行时会话 -> `auth_session`

这样协议边界和业务边界是一致的。

## 2. 支持 `1..N` 个入口

当前表面需求只有三类主体，但真实需求是支持 `1..N` 个业务入口。

这套模型不依赖固定三类主体，而是允许：

- 新增一个 `client`
- 配置它允许的 `grant_type`
- 配置它的展示元数据
- 配置它下面的账号

就可以自然扩展新入口。

## 3. 账号、标识、凭证已经拆层

如果把用户名、手机号、密码都直接堆到一张用户表里，后续扩展会很快变脏。

当前拆层后：

- `account` 解决“这个入口下是谁”
- `identifier` 解决“用什么标识登录”
- `credential` 解决“凭什么完成认证”

后续无论扩邮箱登录、外部标识还是更多凭证类型，都不需要推翻现有结构。

## 4. 会话治理独立成立

仅依赖 OAuth2 token 缓存无法很好表达治理动作，例如：

- 只踢某一台设备
- 查看某个账号有哪些会话
- 平台侧统一撤销会话

所以当前把 `auth_session` 单独建模，后续治理和审计会更稳定。

---

## 当前登录方式实现

当前项目的登录方式全部沿着 OAuth2 扩展 grant 实现。

已经支持：

- `password`
- `otp`
- `passkey`

对应原则：

- 客户端是否允许使用某种登录方式，只看 `authorized_grant_types`
- 新增登录方式时，只扩 grant，不造第二套授权模型

也就是说：

- 登录方式授权来源：`sys_oauth_client_details.authorized_grant_types`
- 登录方式展示来源：平台侧登录方式字典和公开 `client` 元数据

---

## 当前运行时链路

## 1. 密码登录

- 前端根据 `client` 决定是否验证码、是否前端加密
- `grant_type=password`
- 后端按 `client + identifier` 解析 `auth_account`
- 再按账号读取 `PASSWORD` 凭证校验
- 登录成功后写入 `auth_session`

## 2. OTP 登录

- 前端请求短信验证码
- 演示模式下接口直接返回验证码并回填输入框
- `grant_type=otp`
- 后端按 `client + phone` 解析账号
- 再检查账号下 `OTP` 凭证状态
- 登录成功后写入 `auth_session`

## 3. Passkey 登录

- 前端先拉取断言选项
- 浏览器完成 WebAuthn 断言
- `grant_type=passkey`
- 后端按 `client + username + credentialId` 校验当前账号下的 Passkey
- 登录成功后写入 `auth_session`

---

## 当前平台治理能力

平台运营端已经具备以下治理入口：

- `Client` 管理
- 登录方式管理
- 账号创建
- 凭证与标识治理
- 会话治理
- 审计日志查询

当前已支持的具体操作：

- 管理 `client` 的授权登录方式
- 管理 `client` 的展示元数据
- 管理登录方式字典
- 新建账号并绑定多个 `client`
- 重置账号密码
- 启用或停用 OTP
- 清空账号 Passkey
- 查看和维护账号标识
- 查询并撤销会话
- 查询平台治理操作和系统日志

---

## 当前设计边界

这一版已经收口的边界：

- 登录入口边界：由 `client` 承担
- 登录方式边界：由 `authorized_grant_types` 承担
- 账号边界：由 `auth_account` 承担
- 标识边界：由 `auth_account_identifier` 承担
- 凭证边界：由 `auth_account_credential` 承担
- 会话边界：由 `auth_session` 承担

当前仍保留的兼容点：

- `sys_user.password` 仍保留兼容影子
- `auth_account.loginName / phone` 仍保留兼容字段

但当前密码治理已经收口到账号维度：

- 平台重置密码：只影响当前账号
- 当前用户改密：只影响当前登录账号
- 旧的 `/user` 更新接口只有显式传 `clientIds` 才会同步目标入口的密码凭证

---

## 后续扩展方向

如果继续往下做，最值得继续扩展的是：

## 1. 认证域继续去兼容化

- 让 `sys_user.password` 不再承担兼容影子
- 让 `auth_account.loginName / phone` 进一步弱化为缓存字段
- 进一步把认证职责彻底收回 `account / identifier / credential`

## 2. 审计继续做深

当前已有平台审计页，但还可以继续细化：

- 登录成功与失败
- 凭证变更
- Passkey 注册与删除
- 会话撤销
- Client 配置变更

## 3. 浏览器级自动化回归

当前单测和构建链已经覆盖，但真实浏览器链路仍建议补：

- 密码登录
- OTP 登录
- Passkey 注册与登录
- 平台治理页操作

## 4. 前端工程化优化

当前前端功能已经可用，但构建仍有 chunk 过大提示，后续可以继续做：

- 路由拆包
- 平台页按模块拆分
- 共享依赖优化

---

## 结论

当前项目已经不是概念设计，而是一套已经落到代码、数据库、平台治理页面和演示前端里的多入口认证系统。

这套模型的核心价值在于：

- 支持 `1..N` 个入口
- 支持多维凭证
- 支持多设备会话
- 支持平台治理
- 与 OAuth2 协议边界一致
- 后续扩展成本可控

如果继续演进，重点不再是“再加一个登录方式”，而是继续把认证域边界收得更干净，把治理和审计做得更完整。
