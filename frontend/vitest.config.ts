import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

const projectRoot = (globalThis as unknown as { process: { cwd: () => string } }).process.cwd();
const sourcePath = (fileName: string) => `${projectRoot}/src/shared/lib/${fileName}`.replaceAll("\\", "/");

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@psicogest/demo-api": sourcePath("mock-api.ts"),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    globals: true,
  },
});
