import { defineStore } from "pinia";

import {
  deleteCurrentPasskey,
  fetchCurrentUserInfo,
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
    hydrated: false,
    selectedClientId: "",
    clientCatalog: [],
    accessToken: "",
    refreshToken: "",
    tokenType: "Bearer",
    expiresIn: 0,
    sid: "",
    username: "",
    permissions: [],
    profileLoaded: false,
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
    },
    hasPermission(state) {
      return (permission) => !permission || state.permissions.includes(permission);
    }
  },
  actions: {
    hydrate() {
      if (this.hydrated) {
        return;
      }
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        this.clientCatalog = [];
        this.hydrated = true;
        return;
      }
      try {
        Object.assign(this, initialState(), JSON.parse(raw));
      } catch (error) {
        localStorage.removeItem(STORAGE_KEY);
      }
      this.clientCatalog = buildClientCatalog(this.clientCatalog);
      this.permissions = Array.isArray(this.permissions) ? this.permissions : [];
      this.profileLoaded = false;
      this.hydrated = true;
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
          permissions: this.permissions,
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
      this.permissions = [];
      this.profileLoaded = false;
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
      await this.ensureCurrentUserProfile();
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
      await this.ensureCurrentUserProfile();
      return tokenPayload;
    },
    async loginWithPasskey(username) {
      const tokenPayload = await passkeyLogin(this.currentClient, username);
      this.setSession(tokenPayload, {
        username,
        grantType: "passkey"
      });
      await this.ensureCurrentUserProfile();
      return tokenPayload;
    },
    async ensureCurrentUserProfile() {
      if (!this.accessToken) {
        return null;
      }
      if (this.profileLoaded) {
        return {
          username: this.username,
          permissions: this.permissions
        };
      }
      const profile = await fetchCurrentUserInfo(this.accessToken);
      this.username = profile?.username || this.username;
      this.permissions = Array.isArray(profile?.permissions) ? profile.permissions : [];
      this.profileLoaded = true;
      this.persist();
      return profile;
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
