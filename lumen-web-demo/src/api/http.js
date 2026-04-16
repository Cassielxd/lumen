import axios from "axios";

export const http = axios.create({
  baseURL: "/admin",
  timeout: 20000
});

export function toFormBody(payload) {
  const form = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      form.append(key, String(value));
    }
  });
  return form.toString();
}

export function unwrapBusiness(payload) {
  if (payload?.code === 0) {
    return payload.data;
  }
  throw new Error(payload?.msg || payload?.error_description || payload?.error || "请求失败");
}

export function describeRequestError(error) {
  const payload = error?.response?.data;
  if (typeof payload === "string" && payload.trim()) {
    return payload;
  }
  if (payload?.msg) {
    return payload.msg;
  }
  if (payload?.error_description) {
    return payload.error_description;
  }
  if (payload?.error) {
    return payload.error;
  }
  return error?.message || "请求失败";
}
