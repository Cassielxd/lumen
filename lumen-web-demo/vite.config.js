import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    host: "localhost",
    port: 5173,
    proxy: {
      "/admin": {
        target: "http://localhost:9999",
        changeOrigin: false
      }
    }
  },
  preview: {
    host: "localhost",
    port: 4173
  }
});
