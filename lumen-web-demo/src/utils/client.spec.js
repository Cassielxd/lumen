import { describe, expect, it } from "vitest";

import { buildBasicAuth, resolveClient } from "./client";

describe("client utils", () => {
  it("buildBasicAuth should encode client credentials", () => {
    expect(buildBasicAuth("test", "test")).toBe("Basic dGVzdDp0ZXN0");
  });

  it("resolveClient should use the provided public client catalog", () => {
    const catalog = [
      { clientId: "app", displayName: "会员入口" },
      { clientId: "lumen", displayName: "平台运营" }
    ];

    expect(resolveClient("missing", catalog).id).toBe("app");
    expect(resolveClient("lumen", catalog).label).toBe("平台运营");
  });
});
