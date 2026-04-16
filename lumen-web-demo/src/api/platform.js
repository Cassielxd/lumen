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
