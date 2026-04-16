import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";

import App from "./App.vue";
import { router } from "./router";
import { pinia } from "./stores";
import { useAuthStore } from "./stores/auth";
import "./style.css";

const app = createApp(App);

app.use(pinia);
useAuthStore(pinia).hydrate();
app.use(router);
app.use(ElementPlus);
app.mount("#app");

