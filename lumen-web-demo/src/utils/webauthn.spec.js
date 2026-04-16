import { describe, expect, it } from "vitest";

import { getPasskeySupportError } from "./webauthn";

describe("getPasskeySupportError", () => {
  it("allows localhost over http for local development", () => {
    expect(
      getPasskeySupportError(
        {
          hostname: "localhost",
          origin: "http://localhost:5173",
          protocol: "http:"
        },
        true,
        true
      )
    ).toBe("");
  });

  it("rejects loopback ip addresses with a clear message", () => {
    expect(
      getPasskeySupportError(
        {
          hostname: "127.0.0.1",
          origin: "http://127.0.0.1:5173",
          protocol: "http:"
        },
        true,
        true
      )
    ).toContain("localhost instead of 127.0.0.1");
  });

  it("rejects insecure non-localhost origins", () => {
    expect(
      getPasskeySupportError(
        {
          hostname: "demo.example.com",
          origin: "http://demo.example.com",
          protocol: "http:"
        },
        true,
        false
      )
    ).toContain("requires HTTPS or localhost");
  });
});
