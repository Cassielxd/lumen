<#assign title="授权确认">

<#assign content>
    <div class="space-y-6">
        <div>
            <div class="text-sm font-medium uppercase tracking-[0.24em] text-brand-600">Authorize</div>
            <h2 class="mt-2 text-3xl font-semibold text-slate-900">应用授权确认</h2>
            <p class="mt-3 text-sm leading-7 text-slate-500">
                当前客户端正在申请访问你的账号数据，请确认授权范围是否符合预期。
            </p>
        </div>

        <div class="rounded-3xl border border-slate-200 bg-slate-50 p-5">
            <div class="text-sm text-slate-500">当前登录用户</div>
            <div class="mt-2 text-lg font-semibold text-slate-900">
                <#if principalName == "anonymousUser">
                    未登录用户
                <#else>
                    ${principalName}
                </#if>
            </div>
            <div class="mt-4 text-sm text-slate-500">客户端</div>
            <div class="mt-1 font-mono text-sm text-slate-800">${clientId}</div>
        </div>

        <form id="confirmationForm" name="confirmationForm" action="${request.contextPath}/oauth2/authorize" method="post" class="space-y-5">
            <input type="hidden" name="client_id" value="${clientId}">
            <input type="hidden" name="state" value="${state}">

            <div>
                <div class="text-sm font-medium text-slate-700">授权范围</div>
                <div class="mt-3 space-y-3 rounded-3xl border border-slate-200 bg-white p-4">
                    <#list scopeList as scope>
                        <label class="flex items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-700">
                            <input type="checkbox" checked="checked" name="scope" value="${scope}" class="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500">
                            <span>${scope}</span>
                        </label>
                    </#list>
                </div>
            </div>

            <div class="text-sm leading-7 text-slate-500">
                点击“确认授权”即表示你同意当前客户端按以上 scope 访问你的账号资源。
            </div>

            <button type="submit" class="inline-flex items-center justify-center rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-600">
                确认授权
            </button>
        </form>
    </div>
</#assign>

<#include "layout/base.ftl">
