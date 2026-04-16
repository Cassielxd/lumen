import { getClientOption } from "../data/clients";

export function buildBasicAuth(clientId, clientSecret) {
  const raw = `${clientId}:${clientSecret}`;
  if (typeof btoa === "function") {
    return `Basic ${btoa(raw)}`;
  }
  return `Basic ${Buffer.from(raw, "utf-8").toString("base64")}`;
}

export function resolveClient(clientId, catalog = []) {
  return getClientOption(clientId, catalog);
}
