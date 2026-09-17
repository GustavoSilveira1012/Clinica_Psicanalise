import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const projectRoot = (globalThis as unknown as { process: { cwd: () => string } }).process.cwd();
const sourcePath = (fileName: string) => `${projectRoot}/src/shared/lib/${fileName}`.replaceAll("\\", "/");

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  resolve: {
    alias: {
      "@psicogest/demo-api": sourcePath(mode === "production" ? "production-demo-api.ts" : "mock-api.ts"),
    },
  },
  server: {
    port: 5173,
  },
}));
