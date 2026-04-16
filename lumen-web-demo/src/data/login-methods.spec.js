import { describe, expect, it } from "vitest";

import { normalizeLoginMethodOptions } from "./login-methods";

describe("login method metadata", () => {
  it("normalizes backend dictionary payloads to stable option values", () => {
    expect(
      normalizeLoginMethodOptions([
        { value: "password", label: "密码模式" },
        { itemValue: "otp", label: "OTP" },
        { label: "invalid" }
      ])
    ).toEqual([
      { value: "password", label: "密码模式" },
      { itemValue: "otp", value: "otp", label: "OTP" }
    ]);
  });
});
