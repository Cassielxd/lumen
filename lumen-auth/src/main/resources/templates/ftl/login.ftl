<#assign title="Lumen 统一身份认证">

<#assign content>
    <div class="space-y-8">
        <section>
            <div class="flex items-start justify-between gap-4">
                <div>
                    <div class="text-sm font-medium uppercase tracking-[0.24em] text-brand-600">Sign In</div>
                    <h2 class="mt-2 text-3xl font-semibold text-slate-900">统一身份认证</h2>
                    <p class="mt-3 text-sm leading-7 text-slate-500">
                        上半部分是浏览器表单登录，下半部分是开发演示面板，可直接验证密码、OTP、Passkey 和会话接口。
                    </p>
                </div>
                <div class="rounded-2xl bg-slate-100 px-4 py-3 text-xs leading-6 text-slate-500">
                    演示 client：<span class="font-semibold text-slate-700">${tokenDemoClientId}</span>
                </div>
            </div>
        </section>

        <section class="rounded-3xl border border-slate-200 bg-slate-50 p-5">
            <div class="flex items-center justify-between gap-4">
                <div>
                    <h3 class="text-lg font-semibold text-slate-900">浏览器表单登录</h3>
                    <p class="mt-1 text-sm text-slate-500">用于授权码场景的标准登录页。</p>
                </div>
            </div>

            <form class="mt-5 space-y-4" action="${request.contextPath}/oauth2/form" method="post">
                <input type="hidden" name="client_id" value="${tokenDemoClientId}">
                <input type="hidden" name="grant_type" value="password">

                <div class="grid gap-4 md:grid-cols-2">
                    <label class="block">
                        <span class="mb-2 block text-sm font-medium text-slate-700">账号</span>
                        <input class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
                               type="text"
                               name="username"
                               placeholder="请输入账号"
                               required>
                    </label>

                    <label class="block">
                        <span class="mb-2 block text-sm font-medium text-slate-700">密码</span>
                        <input class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
                               type="password"
                               name="password"
                               placeholder="请输入密码"
                               required>
                    </label>
                </div>

                <#if error??>
                    <div class="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600">
                        ${error}
                    </div>
                </#if>

                <button type="submit"
                        class="inline-flex items-center justify-center rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-600">
                    进入浏览器登录流程
                </button>
            </form>
        </section>

        <section class="rounded-3xl border border-slate-200 p-5">
            <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <h3 class="text-lg font-semibold text-slate-900">令牌演示面板</h3>
                    <p class="mt-1 text-sm text-slate-500">
                        开发演示默认使用 seed client：<code class="rounded bg-slate-100 px-1 py-0.5 text-slate-700">${tokenDemoClientId}/test</code>。
                    </p>
                </div>
                <div class="text-xs leading-6 text-slate-500">
                    `test` client 已配置为免图形验证码；Passkey 仍需安全上下文（HTTPS 或 localhost）。
                </div>
            </div>

            <div class="mt-5 flex flex-wrap gap-2" id="demo-tabs">
                <button type="button" data-panel="password-panel" class="demo-tab rounded-full bg-slate-950 px-4 py-2 text-sm font-medium text-white">密码令牌</button>
                <button type="button" data-panel="otp-panel" class="demo-tab rounded-full bg-slate-100 px-4 py-2 text-sm font-medium text-slate-600">OTP 令牌</button>
                <button type="button" data-panel="passkey-panel" class="demo-tab rounded-full bg-slate-100 px-4 py-2 text-sm font-medium text-slate-600">Passkey 登录</button>
            </div>

            <div class="mt-5 space-y-5">
                <form id="password-panel" class="demo-panel space-y-4">
                    <div class="grid gap-4 md:grid-cols-2">
                        <label class="block">
                            <span class="mb-2 block text-sm font-medium text-slate-700">账号</span>
                            <input id="password-username" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100" value="admin">
                        </label>
                        <label class="block">
                            <span class="mb-2 block text-sm font-medium text-slate-700">密码</span>
                            <input id="password-value" type="password" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100" placeholder="请输入密码">
                        </label>
                    </div>

                    <button type="submit" class="inline-flex items-center justify-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-700">
                        获取密码令牌
                    </button>
                </form>

                <form id="otp-panel" class="demo-panel hidden space-y-4">
                    <div class="grid gap-4 md:grid-cols-2">
                        <label class="block">
                            <span class="mb-2 block text-sm font-medium text-slate-700">手机号</span>
                            <input id="otp-mobile" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100" value="17034642999">
                        </label>
                        <label class="block">
                            <span class="mb-2 block text-sm font-medium text-slate-700">短信验证码</span>
                            <input id="otp-code" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100" placeholder="请输入短信验证码">
                        </label>
                    </div>

                    <div class="flex flex-wrap gap-3">
                        <button type="button" id="otp-send-button" class="inline-flex items-center justify-center rounded-2xl border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-brand-500 hover:text-brand-600">
                            发送短信验证码
                        </button>
                        <button type="submit" class="inline-flex items-center justify-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-700">
                            获取 OTP 令牌
                        </button>
                    </div>
                </form>

                <form id="passkey-panel" class="demo-panel hidden space-y-4">
                    <label class="block">
                        <span class="mb-2 block text-sm font-medium text-slate-700">账号</span>
                        <input id="passkey-username" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100" value="admin">
                    </label>

                    <div class="flex flex-wrap gap-3">
                        <button type="submit" class="inline-flex items-center justify-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-700">
                            使用 Passkey 登录
                        </button>
                        <div class="rounded-2xl bg-slate-100 px-4 py-3 text-sm text-slate-500">
                            需要当前账号已完成 Passkey 注册。
                        </div>
                    </div>
                </form>
            </div>

            <div class="mt-6 rounded-3xl border border-slate-200 bg-slate-50 p-4">
                <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <div class="text-sm font-semibold text-slate-900">令牌与会话结果</div>
                        <div id="demo-token-summary" class="mt-1 text-xs leading-6 text-slate-500">尚未获取 access token。</div>
                    </div>
                    <div class="flex flex-wrap gap-2">
                        <button type="button" id="register-passkey-button" class="rounded-full border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:border-brand-500 hover:text-brand-600">注册当前账号 Passkey</button>
                        <button type="button" id="list-passkeys-button" class="rounded-full border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:border-brand-500 hover:text-brand-600">查看当前 Passkey</button>
                        <button type="button" id="list-sessions-button" class="rounded-full border border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:border-brand-500 hover:text-brand-600">查看当前会话</button>
                        <button type="button" id="logout-button" class="rounded-full border border-rose-200 px-3 py-2 text-sm text-rose-500 transition hover:bg-rose-50">退出当前令牌</button>
                    </div>
                </div>
                <pre id="demo-result" class="mt-4 max-h-[360px] overflow-auto rounded-2xl bg-slate-950 p-4 text-xs leading-6 text-slate-100">{
  "message": "准备就绪"
}</pre>
            </div>
        </section>
    </div>
</#assign>

<#assign extraScript>
    <script>
        const contextPath = '${request.contextPath?js_string}';
        const tokenDemoClientId = '${tokenDemoClientId?js_string}';
        const tokenDemoBasicAuth = '${tokenDemoBasicAuth?js_string}';
        let currentAccessToken = null;

        const tabButtons = document.querySelectorAll('.demo-tab');
        const panels = document.querySelectorAll('.demo-panel');
        const demoResult = document.getElementById('demo-result');
        const demoTokenSummary = document.getElementById('demo-token-summary');
        const passwordPanel = document.getElementById('password-panel');
        const otpPanel = document.getElementById('otp-panel');
        const passkeyPanel = document.getElementById('passkey-panel');
        const otpSendButton = document.getElementById('otp-send-button');
        const registerPasskeyButton = document.getElementById('register-passkey-button');
        const listPasskeysButton = document.getElementById('list-passkeys-button');
        const listSessionsButton = document.getElementById('list-sessions-button');
        const logoutButton = document.getElementById('logout-button');

        function setActivePanel(panelId) {
            panels.forEach((panel) => panel.classList.toggle('hidden', panel.id !== panelId));
            tabButtons.forEach((button) => {
                const active = button.dataset.panel === panelId;
                button.classList.toggle('bg-slate-950', active);
                button.classList.toggle('text-white', active);
                button.classList.toggle('bg-slate-100', !active);
                button.classList.toggle('text-slate-600', !active);
            });
        }

        function updateTokenSummary(tokenPayload) {
            if (tokenPayload && tokenPayload.access_token) {
                currentAccessToken = tokenPayload.access_token;
                demoTokenSummary.textContent = '已获取 access token，grant_type='
                    + (tokenPayload.grant_type || 'unknown')
                    + '，sid='
                    + (tokenPayload.sid || 'N/A');
            } else {
                currentAccessToken = null;
                demoTokenSummary.textContent = '尚未获取 access token。';
            }
        }

        function showResult(title, payload) {
            demoResult.textContent = JSON.stringify({ title, payload }, null, 2);
        }

        async function parseResponse(response) {
            const text = await response.text();
            if (!text) {
                return {};
            }
            try {
                return JSON.parse(text);
            } catch (error) {
                return { raw: text };
            }
        }

        function toUrlEncoded(params) {
            const urlParams = new URLSearchParams();
            Object.entries(params).forEach(([key, value]) => {
                if (value !== undefined && value !== null && value !== '') {
                    urlParams.append(key, value);
                }
            });
            return urlParams.toString();
        }

        function toUint8Array(base64url) {
            const padding = '='.repeat((4 - (base64url.length % 4 || 4)) % 4);
            const base64 = (base64url + padding).replace(/-/g, '+').replace(/_/g, '/');
            const raw = atob(base64);
            return Uint8Array.from(raw, (char) => char.charCodeAt(0));
        }

        function toBase64Url(buffer) {
            const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
            let binary = '';
            bytes.forEach((byte) => {
                binary += String.fromCharCode(byte);
            });
            return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
        }

        function normalizeAssertionOptions(options) {
            return {
                ...options,
                challenge: toUint8Array(options.challenge),
                allowCredentials: (options.allowCredentials || []).map((credential) => ({
                    ...credential,
                    id: toUint8Array(credential.id)
                }))
            };
        }

        function normalizeRegistrationOptions(options) {
            return {
                ...options,
                challenge: toUint8Array(options.challenge),
                user: {
                    ...options.user,
                    id: toUint8Array(options.user.id)
                },
                excludeCredentials: (options.excludeCredentials || []).map((credential) => ({
                    ...credential,
                    id: toUint8Array(credential.id)
                }))
            };
        }

        async function requestToken(params) {
            const response = await fetch(contextPath + '/oauth2/token', {
                method: 'POST',
                headers: {
                    'Authorization': tokenDemoBasicAuth,
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: toUrlEncoded(params)
            });
            const payload = await parseResponse(response);
            if (response.ok && payload.access_token) {
                payload.grant_type = params.grant_type;
                updateTokenSummary(payload);
            } else if (!response.ok) {
                updateTokenSummary(null);
            }
            showResult('oauth2/token (' + params.grant_type + ')', payload);
            return { response, payload };
        }

        async function callBearerJson(url, options = {}) {
            if (!currentAccessToken) {
                throw new Error('请先通过任意登录方式获取 access token');
            }
            const response = await fetch(url, {
                ...options,
                headers: {
                    'Authorization': 'Bearer ' + currentAccessToken,
                    'Content-Type': 'application/json',
                    ...(options.headers || {})
                }
            });
            const payload = await parseResponse(response);
            showResult(url, payload);
            return { response, payload };
        }

        async function sendOtpCode() {
            const mobile = document.getElementById('otp-mobile').value.trim();
            if (!mobile) {
                showResult('send otp', { message: '手机号不能为空' });
                return;
            }
            const response = await fetch(contextPath + '/mobile/' + encodeURIComponent(mobile), {
                method: 'GET',
                headers: {
                    'Authorization': tokenDemoBasicAuth
                }
            });
            const payload = await parseResponse(response);
            if (payload && payload.code === 0 && payload.data && payload.data.code) {
                document.getElementById('otp-code').value = payload.data.code;
            }
            showResult('send otp', payload);
        }

        function getPasskeyEnvironmentError() {
            if (!window.PublicKeyCredential) {
                return 'Current browser does not support Passkey / WebAuthn';
            }
            const hostname = window.location.hostname.toLowerCase();
            const origin = window.location.origin;
            if (hostname === '127.0.0.1' || hostname === '0.0.0.0' || hostname === '::1' || hostname === '[::1]') {
                return 'Passkey requires localhost or HTTPS domain. Open this page with localhost instead of ' + hostname + '.';
            }
            if (hostname === 'localhost' || hostname.endsWith('.localhost')) {
                return '';
            }
            if (!window.isSecureContext || window.location.protocol !== 'https:') {
                return 'Passkey requires HTTPS or localhost. Current origin: ' + origin;
            }
            return '';
        }

        async function loginWithPasskey(username) {
            const passkeyEnvironmentError = getPasskeyEnvironmentError();
            if (passkeyEnvironmentError) {
                showResult('passkey', { message: passkeyEnvironmentError });
                return;
            }
            if (!window.PublicKeyCredential) {
                showResult('passkey', { message: '当前浏览器不支持 Passkey / WebAuthn' });
                return;
            }
            const optionsResponse = await fetch(contextPath + '/passkey/assertion/options', {
                method: 'POST',
                headers: {
                    'Authorization': tokenDemoBasicAuth,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username })
            });
            const optionsPayload = await parseResponse(optionsResponse);
            if (!optionsResponse.ok || optionsPayload.code !== 0) {
                showResult('passkey assertion options', optionsPayload);
                return;
            }

            const credential = await navigator.credentials.get({
                publicKey: normalizeAssertionOptions(optionsPayload.data)
            });

            await requestToken({
                grant_type: 'passkey',
                username,
                credentialId: credential.id,
                clientDataJSON: toBase64Url(credential.response.clientDataJSON),
                authenticatorData: toBase64Url(credential.response.authenticatorData),
                signature: toBase64Url(credential.response.signature)
            });
        }

        async function registerCurrentPasskey() {
            const passkeyEnvironmentError = getPasskeyEnvironmentError();
            if (passkeyEnvironmentError) {
                showResult('passkey register', { message: passkeyEnvironmentError });
                return;
            }
            if (!window.PublicKeyCredential) {
                showResult('passkey register', { message: '当前浏览器不支持 Passkey / WebAuthn' });
                return;
            }
            const optionsResult = await callBearerJson(contextPath + '/passkey/current/register/options', {
                method: 'POST'
            });
            if (optionsResult.response.status >= 400 || optionsResult.payload.code !== 0) {
                return;
            }

            const credential = await navigator.credentials.create({
                publicKey: normalizeRegistrationOptions(optionsResult.payload.data)
            });
            const finishPayload = {
                clientDataJSON: toBase64Url(credential.response.clientDataJSON),
                attestationObject: toBase64Url(credential.response.attestationObject),
                transports: typeof credential.response.getTransports === 'function' ? credential.response.getTransports() : []
            };
            await callBearerJson(contextPath + '/passkey/current/register', {
                method: 'POST',
                body: JSON.stringify(finishPayload)
            });
        }

        tabButtons.forEach((button) => {
            button.addEventListener('click', () => setActivePanel(button.dataset.panel));
        });

        passwordPanel.addEventListener('submit', async (event) => {
            event.preventDefault();
            await requestToken({
                grant_type: 'password',
                client_id: tokenDemoClientId,
                username: document.getElementById('password-username').value.trim(),
                password: document.getElementById('password-value').value
            });
        });

        otpSendButton.addEventListener('click', sendOtpCode);

        otpPanel.addEventListener('submit', async (event) => {
            event.preventDefault();
            await requestToken({
                grant_type: 'otp',
                mobile: document.getElementById('otp-mobile').value.trim(),
                code: document.getElementById('otp-code').value.trim()
            });
        });

        passkeyPanel.addEventListener('submit', async (event) => {
            event.preventDefault();
            await loginWithPasskey(document.getElementById('passkey-username').value.trim());
        });

        registerPasskeyButton.addEventListener('click', async () => {
            try {
                await registerCurrentPasskey();
            } catch (error) {
                showResult('register passkey', { message: error.message });
            }
        });

        listPasskeysButton.addEventListener('click', async () => {
            try {
                await callBearerJson(contextPath + '/passkey/current/list', { method: 'GET' });
            } catch (error) {
                showResult('current passkeys', { message: error.message });
            }
        });

        listSessionsButton.addEventListener('click', async () => {
            try {
                await callBearerJson(contextPath + '/auth-session/current/list', { method: 'GET' });
            } catch (error) {
                showResult('current sessions', { message: error.message });
            }
        });

        logoutButton.addEventListener('click', async () => {
            if (!currentAccessToken) {
                showResult('logout', { message: '当前没有可退出的 access token' });
                return;
            }
            const response = await fetch(contextPath + '/token/logout', {
                method: 'DELETE',
                headers: {
                    'Authorization': 'Bearer ' + currentAccessToken
                }
            });
            const payload = await parseResponse(response);
            currentAccessToken = null;
            updateTokenSummary(null);
            showResult('logout', payload);
        });

        setActivePanel('password-panel');
    </script>
</#assign>

<#include "layout/base.ftl">
