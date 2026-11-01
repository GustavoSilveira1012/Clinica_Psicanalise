import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

export function buildEnvironment(source = process.env) {
  return {
    ...source,
    VITE_API_URL: "/backend",
    VITE_DEMO_MODE: "false",
    VITE_CLINICAL_ONLY_PILOT: "true",
    VITE_CLINICAL_DATA_ENABLED: "false",
    VITE_CLINICAL_DATA_RELEASE_APPROVED: "false",
  };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const root = fileURLToPath(new URL("../", import.meta.url));
  const env = buildEnvironment();
  for (const [script, args] of [
    ["node_modules/typescript/bin/tsc", ["-b"]],
    ["node_modules/vite/bin/vite.js", ["build"]],
  ]) {
    const result = spawnSync(process.execPath, [script, ...args], { cwd: root, env, stdio: "inherit" });
    if (result.error) throw result.error;
    if (result.status !== 0) process.exit(result.status ?? 1);
  }
}
