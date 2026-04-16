import { defineStore } from "pinia";

import {
  deleteCurrentPasskey,
  fetchPasskeys,
  fetchPublicClients,
  fetchSessions,
  logoutCurrentToken,
  otpLogin,
  passkeyLogin,
  passwordLogin,
  registerCurrentPasskey,
  revokeOtherSessions,
  revokeSession,
  sendOtpCode
} from "../api/auth";
import { buildClientCatalog, getClientOption } from "../data/clients";

const STORAGE_KEY = "lumen-web-demo-auth";

function initialState() {
  return {
    selectedClientId: "",
    clientCatalog: [],
    accessToken: "",
    refreshToken: "",
    tokenType: "Bearer",
    expiresIn: 0,
    sid: "",
    username: "",
    grantType: "",
    loggedInAt: ""
  };
}

export const useAuthStore = defineStore("auth", {
  state: () => initialState(),
  getters: {
    currentClient(state) {
      return getClientOption(state.selectedClientId, state.clientCatalog);
    },
    availableClients(state) {
      return buildClientCatalog(state.clientCatalog);
    },
    isAuthenticated(state) {
      return Boolean(state.accessToken);
    }
  },
  actions: {
    hydrate() {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        this.clientCatalog = [];
        return;
      }
      try {
        Object.assign(this, initialState(), JSON.parse(raw));
      } catch (error) {
        localStorage.removeItem(STORAGE_KEY);
      }
      this.clientCatalog = buildClientCatalog(this.clientCatalog);
    },
    persist() {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          selectedClientId: this.selectedClientId,
          clientCatalog: this.clientCatalog,
          accessToken: this.accessToken,
          refreshToken: this.refreshToken,
          tokenType: this.tokenType,
          expiresIn: this.expiresIn,
          sid: this.sid,
          username: this.username,
          grantType: this.grantType,
          loggedInAt: this.loggedInAt
        })
      );
    },
    setClientCatalog(clientCatalog) {
      this.clientCatalog = buildClientCatalog(clientCatalog);
      if (!this.clientCatalog.some((item) => item.id === this.selectedClientId)) {
        this.selectedClientId = this.clientCatalog[0]?.id || "";
      }
      this.persist();
    },
    setSelectedClient(clientId) {
      this.selectedClientId = clientId;
      this.persist();
    },
    setSession(tokenPayload, meta) {
      this.accessToken = tokenPayload.access_token || "";
      this.refreshToken = tokenPayload.refresh_token || "";
      this.tokenType = tokenPayload.token_type || "Bearer";
      this.expiresIn = tokenPayload.expires_in || 0;
      this.sid = tokenPayload.sid || "";
      this.username = meta.username;
      this.grantType = meta.grantType;
      this.loggedInAt = new Date().toISOString();
      this.persist();
    },
    clearSession() {
      const clientId = this.selectedClientId;
      const clientCatalog = this.clientCatalog;
      Object.assign(this, initialState(), { selectedClientId: clientId, clientCatalog });
      localStorage.removeItem(STORAGE_KEY);
      this.persist();
    },
    async loadClientCatalog() {
      const clients = await fetchPublicClients();
      this.setClientCatalog(clients);
      return this.clientCatalog;
    },
    async loginWithPassword(form) {
      const tokenPayload = await passwordLogin(this.currentClient, form);
      this.setSession(tokenPayload, {
        username: form.username,
        grantType: "password"
      });
      return tokenPayload;
    },
    async sendOtpCode(mobile) {
      return sendOtpCode(this.currentClient, mobile);
    },
    async loginWithOtp(form) {
      const tokenPayload = await otpLogin(this.currentClient, form);
      this.setSession(tokenPayload, {
        username: form.mobile,
        grantType: "otp"
      });
      return tokenPayload;
    },
    async loginWithPasskey(username) {
      const tokenPayload = await passkeyLogin(this.currentClient, username);
      this.setSession(tokenPayload, {
        username,
        grantType: "passkey"
      });
      return tokenPayload;
    },
    async loadSessions() {
      return fetchSessions(this.accessToken);
    },
    async revokeSessionBySid(sid) {
      return revokeSession(this.accessToken, sid);
    },
    async revokeOtherCurrentSessions() {
      return revokeOtherSessions(this.accessToken);
    },
    async loadPasskeys() {
      return fetchPasskeys(this.accessToken);
    },
    async registerPasskey() {
      return registerCurrentPasskey(this.accessToken);
    },
    async deletePasskey(credentialKey) {
      return deleteCurrentPasskey(this.accessToken, credentialKey);
    },
    async logout() {
      if (this.accessToken) {
        try {
          await logoutCurrentToken(this.accessToken);
        } catch (error) {
          // Ignore server-side logout failures and clear local token anyway.
        }
      }
      this.clearSession();
    }
  }
});
