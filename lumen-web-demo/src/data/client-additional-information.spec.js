import { describe, expect, it } from "vitest";

import {
  parseClientAdditionalInformation,
  stringifyClientAdditionalInformation
} from "./client-additional-information";

describe("client additional information helpers", () => {
  it("parses display metadata and login flags", () => {
    const parsed = parseClientAdditionalInformation(
      JSON.stringify({
        display_name: "Member App",
        audience: "Member",
        description: "Member login portal",
        captcha_flag: "0",
        enc_flag: "1"
      })
    );

    expect(parsed.displayName).toBe("Member App");
    expect(parsed.audience).toBe("Member");
    expect(parsed.description).toBe("Member login portal");
    expect(parsed.requiresCaptcha).toBe(false);
    expect(parsed.encryptPassword).toBe(true);
  });

  it("merges edited values back into json and preserves unknown fields", () => {
    const raw = JSON.stringify({
      online_quantity: "1",
      captcha_flag: "1"
    });

    const serialized = stringifyClientAdditionalInformation(raw, {
      displayName: "Platform Console",
      audience: "Platform Staff",
      description: "Platform operator login portal",
      requiresCaptcha: false,
      encryptPassword: true
    });

    expect(JSON.parse(serialized)).toEqual({
      online_quantity: "1",
      captcha_flag: "0",
      enc_flag: "1",
      display_name: "Platform Console",
      audience: "Platform Staff",
      description: "Platform operator login portal"
    });
  });
});
