import { describe, expect, it } from "vitest";

import { buildConsoleMenu, isPlatformClient, resolveDefaultDashboardPath } from "./console";

describe("console helpers", () => {
  it("returns governance routes for platform client", () => {
    const menu = buildConsoleMenu("lumen");
    expect(menu).toHaveLength(2);
    expect(menu[1].children.map((item) => item.index)).toEqual([
      "/dashboard/platform/clients",
      "/dashboard/platform/login-methods",
      "/dashboard/platform/accounts",
      "/dashboard/platform/credentials",
      "/dashboard/platform/sessions",
      "/dashboard/platform/audit"
    ]);
  });

  it("hides governance routes for non-platform clients", () => {
    expect(buildConsoleMenu("app")).toEqual([{ index: "/dashboard/overview", label: "工作台总览" }]);
  });

  it("resolves dashboard defaults by client", () => {
    expect(resolveDefaultDashboardPath("lumen")).toBe("/dashboard/platform/clients");
    expect(resolveDefaultDashboardPath("daemon")).toBe("/dashboard/overview");
    expect(isPlatformClient("lumen")).toBe(true);
    expect(isPlatformClient("test")).toBe(false);
  });
});
