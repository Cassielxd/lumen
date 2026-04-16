import { describe, expect, it } from "vitest";

import { buildClientCatalog, getClientOption, getMethodLabel, normalizeGrantTypes } from "./clients";

describe("client metadata helpers", () => {
  it("normalizes array and comma separated grant types", () => {
    expect(normalizeGrantTypes(["password", "otp"])).toEqual(["password", "otp"]);
    expect(normalizeGrantTypes("password, otp,passkey")).toEqual(["password", "otp", "passkey"]);
    expect(normalizeGrantTypes(["password", "null", "undefined", "otp"])).toEqual(["password", "otp"]);
    expect(normalizeGrantTypes(null)).toEqual([]);
  });

  it("returns display label or raw method name", () => {
    expect(getMethodLabel("password")).toBe("密码登录");
    expect(getMethodLabel("custom_grant")).toBe("custom_grant");
  });

  it("builds login clients from backend public catalog without fixed filtering", () => {
    const catalog = buildClientCatalog([
      {
        clientId: "app",
        clientSecret: "app",
        authorizedGrantTypes: ["password", "otp"],
        requiresCaptcha: true,
        displayName: "Member App",
        audience: "Member",
        description: "Member login portal"
      },
      {
        clientId: "partner",
        clientSecret: "partner",
        authorizedGrantTypes: ["password"],
        displayName: "Partner Console",
        audience: "Partner",
        description: "Partner login portal"
      }
    ]);

    expect(catalog.map((item) => item.id)).toEqual(["app", "partner"]);
    expect(catalog.find((item) => item.id === "app").methods).toEqual(["password", "otp"]);
    expect(catalog.find((item) => item.id === "partner").label).toBe("Partner Console");
    expect(catalog.find((item) => item.id === "partner").audience).toBe("Partner");
    expect(catalog.find((item) => item.id === "partner").description).toBe("Partner login portal");
  });

  it("returns a safe empty client when public catalog is empty", () => {
    const client = getClientOption("", []);

    expect(client.id).toBe("");
    expect(client.methods).toEqual([]);
    expect(client.clientSecret).toBe("");
    expect(client.label).toBe("未选择客户端");
  });
});
