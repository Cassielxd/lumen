import { describe, expect, it } from "vitest";

import { encryptPassword, serializePassword } from "./password";

describe("password utils", () => {
  it("encryptPassword should match the backend AES/CFB/NoPadding output", () => {
    expect(encryptPassword("123456")).toBe("29f9bae2c988");
  });

  it("serializePassword should keep plaintext for ignore clients", () => {
    expect(serializePassword({ encryptPassword: false }, "123456")).toBe("123456");
    expect(serializePassword({ encryptPassword: true }, "123456")).toBe("29f9bae2c988");
  });
});

