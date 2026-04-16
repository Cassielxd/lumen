<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><#if title??>${title}<#else>Lumen 统一身份认证</#if></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            50: '#eff6ff',
                            100: '#dbeafe',
                            500: '#2563eb',
                            600: '#1d4ed8',
                            700: '#1e40af'
                        }
                    },
                    boxShadow: {
                        panel: '0 24px 80px rgba(15, 23, 42, 0.18)'
                    }
                }
            }
        };
    </script>
    <#if extraHead??>${extraHead}</#if>
</head>
<body class="min-h-screen bg-slate-950 text-slate-900">
<div class="fixed inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,_rgba(37,99,235,0.28),_transparent_30%),radial-gradient(circle_at_bottom_right,_rgba(14,165,233,0.18),_transparent_30%),linear-gradient(180deg,_#020617_0%,_#0f172a_100%)]"></div>

<div class="relative min-h-screen">
    <header class="mx-auto flex w-full max-w-7xl items-center justify-between px-6 py-6 text-slate-200 lg:px-10">
        <div>
            <div class="text-xs uppercase tracking-[0.32em] text-sky-200/80">Lumen Cloud</div>
            <div class="mt-2 text-lg font-semibold">统一身份认证中心</div>
        </div>
        <a href="https://lumencloud.com"
           class="rounded-full border border-white/10 px-4 py-2 text-sm text-slate-200 transition hover:border-sky-300/50 hover:bg-white/5">
            官方站点
        </a>
    </header>

    <main class="mx-auto grid min-h-[calc(100vh-112px)] w-full max-w-7xl gap-8 px-6 pb-12 pt-4 lg:grid-cols-[1.05fr_0.95fr] lg:px-10">
        <section class="flex flex-col justify-center text-slate-100">
            <div class="inline-flex w-fit items-center rounded-full border border-sky-300/20 bg-sky-300/10 px-4 py-1 text-sm text-sky-100">
                Password / OTP / Passkey
            </div>
            <h1 class="mt-6 max-w-3xl text-4xl font-semibold leading-tight lg:text-5xl">
                一个认证入口，统一承接账号、会话和多种登录方式
            </h1>
            <p class="mt-6 max-w-2xl text-base leading-8 text-slate-300 lg:text-lg">
                当前页面同时保留浏览器表单登录和令牌演示面板，便于直接验证密码、短信验证码、Passkey
                以及会话接口的完整链路。
            </p>

            <div class="mt-8 grid gap-4 md:grid-cols-3">
                <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
                    <div class="text-sm font-medium text-slate-100">账号空间</div>
                    <div class="mt-2 text-sm leading-7 text-slate-300">
                        按 client 绑定认证账号，不再把多类主体硬编码进认证模型。
                    </div>
                </div>
                <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
                    <div class="text-sm font-medium text-slate-100">显式会话</div>
                    <div class="mt-2 text-sm leading-7 text-slate-300">
                        登录后写入 auth_session，可按设备查看、撤销和拦截 refresh token 续期。
                    </div>
                </div>
                <div class="rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
                    <div class="text-sm font-medium text-slate-100">Passkey</div>
                    <div class="mt-2 text-sm leading-7 text-slate-300">
                        支持注册、断言验证和签名计数更新，已接入统一令牌链路。
                    </div>
                </div>
            </div>
        </section>

        <section class="flex items-center">
            <div class="w-full rounded-[28px] border border-slate-200/80 bg-white p-6 shadow-panel lg:p-8">
                <#if content??>${content}</#if>
            </div>
        </section>
    </main>
</div>

<#if extraScript??>${extraScript}</#if>
</body>
</html>
