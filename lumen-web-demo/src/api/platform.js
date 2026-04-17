import { http, unwrapBusiness } from "./http";

function bearerHeaders(token) {
  return {
    Authorization: `Bearer ${token}`
  };
}

export async function fetchClientPage(token, params = {}) {
  const response = await http.get("/client/page", {
    headers: bearerHeaders(token),
    params: {
      current: 1,
      size: 100,
      ...params
    }
  });
  return unwrapBusiness(response.data);
}

export async function fetchLogPage(token, params = {}) {
  const response = await http.get("/log/page", {
    headers: bearerHeaders(token),
    params: {
      current: 1,
      size: 10,
      ...params
    }
  });
  return unwrapBusiness(response.data);
}

export async function createClient(token, payload) {
  const response = await http.post("/client", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function updateClient(token, payload) {
  const response = await http.put("/client", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function removeClients(token, ids) {
  const response = await http.delete("/client", {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    },
    data: ids
  });
  return unwrapBusiness(response.data);
}

export async function syncClientCache(token) {
  const response = await http.put(
    "/client/sync",
    {},
    {
      headers: {
        ...bearerHeaders(token),
        "Content-Type": "application/json"
      }
    }
  );
  return unwrapBusiness(response.data);
}

export async function fetchLoginMethods(token) {
  const response = await http.get("/login-method/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function createLoginMethod(token, payload) {
  const response = await http.post("/login-method", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function updateLoginMethod(token, payload) {
  const response = await http.put("/login-method", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function removeLoginMethod(token, id) {
  const response = await http.delete(`/login-method/${encodeURIComponent(id)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchRoles(token) {
  const response = await http.get("/role/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchDepartments(token) {
  const response = await http.get("/dept/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchPosts(token) {
  const response = await http.get("/post/list", {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function createUser(token, payload) {
  const response = await http.post("/user", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function fetchUserPage(token, params = {}) {
  const response = await http.get("/user/page", {
    headers: bearerHeaders(token),
    params: {
      current: 1,
      size: 10,
      ...params
    }
  });
  return unwrapBusiness(response.data);
}

export async function fetchPlatformSessions(token, params = {}) {
  const response = await http.get("/auth-session/manage/list", {
    headers: bearerHeaders(token),
    params
  });
  return unwrapBusiness(response.data);
}

export async function revokePlatformSession(token, sid) {
  const response = await http.delete(`/auth-session/manage/${encodeURIComponent(sid)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchAccountCredentialGovernance(token, params = {}) {
  const response = await http.get("/auth-account/manage/list", {
    headers: bearerHeaders(token),
    params
  });
  return unwrapBusiness(response.data);
}

export async function resetAccountPassword(token, payload) {
  const response = await http.put("/auth-account/manage/password", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function updateAccountOtpStatus(token, payload) {
  const response = await http.put("/auth-account/manage/otp-status", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function clearAccountPasskeys(token, accountId) {
  const response = await http.delete(`/auth-account/manage/passkeys/${encodeURIComponent(accountId)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}

export async function fetchAccountIdentifiers(token, accountId) {
  const response = await http.get("/auth-account/manage/identifiers", {
    headers: bearerHeaders(token),
    params: { accountId }
  });
  return unwrapBusiness(response.data);
}

export async function saveAccountIdentifier(token, payload) {
  const response = await http.post("/auth-account/manage/identifier", payload, {
    headers: {
      ...bearerHeaders(token),
      "Content-Type": "application/json"
    }
  });
  return unwrapBusiness(response.data);
}

export async function removeAccountIdentifier(token, identifierId) {
  const response = await http.delete(`/auth-account/manage/identifier/${encodeURIComponent(identifierId)}`, {
    headers: bearerHeaders(token)
  });
  return unwrapBusiness(response.data);
}
