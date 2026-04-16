import { buildBasicAuth } from "../utils/client";
import { serializePassword } from "../utils/password";
import {
  getPasskeySupportError,
  normalizeAssertionOptions,
  normalizeRegistrationOptions,
  toBase64Url
} from "../utils/webauthn";
import { http, toFormBody, unwrapBusiness } from "./http";

function clientHeaders(client) {
  return {
    Authorization: buildBasicAuth(client.id, client.clientSecret || client.secret)
  };
}

function bearerHeaders(token) {
  return {
    Authorization: `Bearer ${token}`
  };
}

export function createCaptchaUrl(randomStr) {
  return `/admin/code/image?randomStr=${encodeURIComponent(randomStr)}&t=${Date.now()}`;
}

export async function fetchPublicClients() {
  const response = await http.get("/client/public/list");
  return unwrapBusiness(response.data);
}

export async function passwordLogin(client, form) {
  const response = await http.post(
    "/oauth2/token",
    toFormBody({
      grant_type: "password",
      client_id: client.id,
      username: form.username,
      password: serializePassword(client, form.password),
      code: form.code,
      randomStr: form.randomStr
    }),
    {
      headers: {
        ...clientHeaders(client),
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );
  return response.data;
}

export async function sendOtpCode(client, mobile) {
  const response = await http.get(`/mobile/${encodeURIComponent(mobile)}`, {
    headers: clientHeaders(client)
  });
  return unwrapBusiness(response.data);
}

export async function otpLogin(client, form) {
  const response = await http.post(
    "/oauth2/token",
    toFormBody({
      grant_type: "otp",
      client_id: client.id,
      mobile: form.mobile,
      code: form.code
    }),
    {
      headers: {
        ...clientHeaders(client),
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );
  return response.data;
}

export async function passkeyLogin(client, username) {
  const passkeySupportError = getPasskeySupportError();
  if (passkeySupportError) {
    throw new Error(passkeySupportError);
  }

  const optionsResponse = await http.post(
    "/passkey/assertion/options",
    { username },
    {
      headers: {
        ...clientHeaders(client),
        "Content-Type": "application/json"
      }
    }
  );
  const options = unwrapBusiness(optionsResponse.data);

  const credential = await navigator.credentials.get({
    publicKey: normalizeAssertionOptions(options)
  });

  const response = await http.post(
    "/oauth2/token",
    toFormBody({
      grant_type: "passkey",
      client_id: client.id,
      username,
      credentialId: credential.id,
      clientDataJSON: toBase64Url(credential.response.clientDataJSON),
      authenticatorData: toBase64Url(credential.response.authenticatorData),
      signature: toBase64Url(credential.response.signature)
    }),
    {
      headers: {
        ...clientHeaders(client),
        "Content-Type": "application/x-www-form-urlencoded"
      }
    }
  );
  return response.data;
}

export async function fetchSessions(token) {
  const response = await http.get("/auth-session/current/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function revokeSession(token, sid) {
  const response = await http.delete(`/auth-session/current/${encodeURIComponent(sid)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function revokeOtherSessions(token) {
  const response = await http.delete("/auth-session/current/others", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function logoutCurrentToken(token) {
  const response = await http.delete("/token/logout", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchPasskeys(token) {
  const response = await http.get("/passkey/current/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function registerCurrentPasskey(token) {
  const passkeySupportError = getPasskeySupportError();
  if (passkeySupportError) {
    throw new Error(passkeySupportError);
  }

  const optionsResponse = await http.post(
    "/passkey/current/register/options",
    {},
    {
      headers: {
        ...bearerHeaders(token),
        "Content-Type": "application/json"
      }
    }
  );
  const options = unwrapBusiness(optionsResponse.data);

  const credential = await navigator.credentials.create({
    publicKey: normalizeRegistrationOptions(options)
  });

  const payload = {
    clientDataJSON: toBase64Url(credential.response.clientDataJSON),
    attestationObject: toBase64Url(credential.response.attestationObject),
    transports:
      typeof credential.response.getTransports === "function"
        ? credential.response.getTransports()
        : []
  };

  const response = await http.post("/passkey/current/register", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function deleteCurrentPasskey(token, credentialKey) {
  const response = await http.delete(`/passkey/current/${encodeURIComponent(credentialKey)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}
