export function supportsPasskey() {
  return typeof window !== "undefined" && typeof window.PublicKeyCredential !== "undefined";
}

function isLoopbackIp(hostname) {
  return /^(127(?:\.\d{1,3}){3}|0\.0\.0\.0)$/i.test(hostname) || hostname === "::1" || hostname === "[::1]";
}

function isLocalhostHost(hostname) {
  return hostname === "localhost" || hostname.endsWith(".localhost");
}

export function getPasskeySupportError(
  locationLike = typeof window !== "undefined" ? window.location : undefined,
  passkeySupported = supportsPasskey(),
  secureContext = typeof window === "undefined" ? true : window.isSecureContext !== false
) {
  if (!passkeySupported) {
    return "Current browser does not support Passkey / WebAuthn";
  }

  if (!locationLike) {
    return "";
  }

  const hostname = String(locationLike.hostname || "").toLowerCase();
  const origin = String(locationLike.origin || "");
  const protocol = String(locationLike.protocol || "").toLowerCase();

  if (isLoopbackIp(hostname)) {
    return `Passkey requires localhost or HTTPS domain. Open this page with localhost instead of ${hostname}.`;
  }

  if (isLocalhostHost(hostname)) {
    return "";
  }

  if (!secureContext || protocol !== "https:") {
    return `Passkey requires HTTPS or localhost. Current origin: ${origin || "(unknown)"}`;
  }

  return "";
}

export function toUint8Array(base64UrlValue) {
  const padding = "=".repeat((4 - (base64UrlValue.length % 4 || 4)) % 4);
  const base64 = (base64UrlValue + padding).replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(base64);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

export function toBase64Url(buffer) {
  const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

export function normalizeAssertionOptions(options) {
  return {
    ...options,
    challenge: toUint8Array(options.challenge),
    allowCredentials: (options.allowCredentials || []).map((item) => ({
      ...item,
      id: toUint8Array(item.id)
    }))
  };
}

export function normalizeRegistrationOptions(options) {
  return {
    ...options,
    challenge: toUint8Array(options.challenge),
    user: {
      ...options.user,
      id: toUint8Array(options.user.id)
    },
    excludeCredentials: (options.excludeCredentials || []).map((item) => ({
      ...item,
      id: toUint8Array(item.id)
    }))
  };
}
