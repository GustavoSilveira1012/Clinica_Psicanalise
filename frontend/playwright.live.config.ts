import { defineConfig, devices } from "@playwright/test";

// Explicitly targets ops/local-e2e.ps1 disposable stack; no request interception.
export default defineConfig({
  testDir: "./e2e/live",
  workers: 1,
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: "list",
  use: {
    ...devices["Desktop Chrome"],
    baseURL: "http://127.0.0.1:5174",
    channel: process.platform === "win32" ? "msedge" : undefined,
    timezoneId: "America/Sao_Paulo",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
