import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://127.0.0.1:4173",
    trace: "retain-on-failure",
    channel: process.env.PLAYWRIGHT_CHANNEL || (process.platform === "win32" ? "msedge" : undefined),
    ...devices["Desktop Chrome"],
  },
  webServer: {
    command: "node node_modules/vite/bin/vite.js build --configLoader runner && node node_modules/vite/bin/vite.js preview --configLoader runner --host 127.0.0.1 --port 4173 --strictPort",
    url: "http://127.0.0.1:4173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      ...process.env,
      VITE_CLINICAL_ONLY_PILOT: "true",
      VITE_CLINICAL_DATA_ENABLED: "false",
      VITE_CLINICAL_DATA_RELEASE_APPROVED: "false",
      VITE_DEMO_MODE: "false",
      VITE_API_URL: "",
    },
  },
});
