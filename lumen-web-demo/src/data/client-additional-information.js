function safeParseJson(raw) {
  if (!raw || typeof raw !== "string") {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch (error) {
    return {};
  }
}

function resolveBooleanFlag(metadata, preferredKey, legacyKey, fallback) {
  if (metadata[preferredKey] !== undefined) {
    const value = metadata[preferredKey];
    if (typeof value === "boolean") {
      return value;
    }
    return String(value).toLowerCase() === "true" || String(value) === "1";
  }
  if (metadata[legacyKey] !== undefined) {
    const value = metadata[legacyKey];
    if (typeof value === "boolean") {
      return value;
    }
    return String(value).toLowerCase() === "true" || String(value) === "1";
  }
  return fallback;
}

export function parseClientAdditionalInformation(raw) {
  const metadata = safeParseJson(raw);

  return {
    metadata,
    displayName: metadata.display_name || metadata.displayName || metadata.label || "",
    audience: metadata.audience || metadata.clientAudience || "",
    description: metadata.description || metadata.clientDescription || "",
    requiresCaptcha: resolveBooleanFlag(metadata, "requiresCaptcha", "captcha_flag", true),
    encryptPassword: resolveBooleanFlag(metadata, "encryptPassword", "enc_flag", true)
  };
}

export function stringifyClientAdditionalInformation(raw, values) {
  const metadata = safeParseJson(raw);

  if (values.displayName) {
    metadata.display_name = values.displayName;
  } else {
    delete metadata.display_name;
  }

  if (values.audience) {
    metadata.audience = values.audience;
  } else {
    delete metadata.audience;
  }

  if (values.description) {
    metadata.description = values.description;
  } else {
    delete metadata.description;
  }

  metadata.captcha_flag = values.requiresCaptcha ? "1" : "0";
  metadata.enc_flag = values.encryptPassword ? "1" : "0";

  delete metadata.displayName;
  delete metadata.clientAudience;
  delete metadata.clientDescription;
  delete metadata.requiresCaptcha;
  delete metadata.encryptPassword;

  return JSON.stringify(metadata);
}
