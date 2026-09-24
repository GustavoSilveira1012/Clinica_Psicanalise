import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClient } from "./api-client";

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
afterEach(() => vi.unstubAllGlobals());

describe("API session and tenant boundary", () => {
  it("sends bearer, tenant and httpOnly cookies without storing credentials", async () => {
    const fetch = vi.fn().mockResolvedValue(json({ ok: true }));
    vi.stubGlobal("fetch", fetch);
    const client = new ApiClient("https://api.example.test");
    client.setAccessToken("memory-only"); client.setOrganizationId("org-a");
    await client.request("/patients");
    const init = fetch.mock.calls[0][1] as RequestInit;
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer memory-only");
    expect(new Headers(init.headers).get("X-Organization-Id")).toBe("org-a");
    expect(init.credentials).toBe("include");
  });

  it("does not retry forbidden operations", async () => {
    const fetch = vi.fn().mockResolvedValue(json({ code: "FORBIDDEN" }, 403));
    vi.stubGlobal("fetch", fetch);
    await expect(new ApiClient("").request("/payments")).rejects.toMatchObject({ status: 403 });
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it("does not expose server exception details", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(json({ detail: "SQL exception with confidential data" }, 500)));
    await expect(new ApiClient("").request("/patients")).rejects.toMatchObject({
      message: "Serviço temporariamente indisponível. Tente novamente.", status: 500,
    });
  });

  it("refreshes once for concurrent unauthorized requests and retries with the new token", async () => {
    let refreshes = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path === "/auth/csrf") return json({ token: "csrf" });
      if (path === "/auth/refresh") {
        refreshes++;
        expect(new Headers(init?.headers).get("X-CSRF-TOKEN")).toBe("csrf");
        return json({ accessToken: "renewed" });
      }
      return new Headers(init?.headers).get("Authorization") === "Bearer renewed"
        ? json({ ok: true }) : json({}, 401);
    }));
    const client = new ApiClient(""); client.setAccessToken("expired");
    expect(await Promise.all([client.request("/patients"), client.request("/appointments")]))
      .toEqual([{ ok: true }, { ok: true }]);
    expect(refreshes).toBe(1);
  });

  it("discards an in-flight response after switching organizations", async () => {
    let resolve!: (response: Response) => void;
    vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>(done => { resolve = done; })));
    const client = new ApiClient(""); client.setOrganizationId("org-a");
    const request = client.request("/patients");
    client.setOrganizationId("org-b"); resolve(json([{ name: "Tenant A" }]));
    await expect(request).rejects.toMatchObject({ code: "SESSION_CONTEXT_CHANGED" });
  });

  it("cannot revive a session when logout races with refresh", async () => {
    let completeRefresh!: (response: Response) => void;
    let refreshStarted!: () => void;
    const started = new Promise<void>(done => { refreshStarted = done; });
    const fetch = vi.fn(async (path: string) => {
      if (path === "/auth/csrf") return json({ token: "csrf" });
      if (path === "/auth/refresh") {
        refreshStarted();
        return new Promise<Response>(done => { completeRefresh = done; });
      }
      return json({}, 401);
    });
    vi.stubGlobal("fetch", fetch);
    const client = new ApiClient(""); client.setAccessToken("expired");
    const pending = client.request("/patients");
    await started; client.clearSession(); completeRefresh(json({ accessToken: "must-not-survive" }));
    await expect(pending).rejects.toMatchObject({ code: "SESSION_CONTEXT_CHANGED" });
    fetch.mockResolvedValue(json({ ok: true }));
    await client.request("/patients");
    const init = (fetch.mock.calls.at(-1) as unknown as [string, RequestInit])[1];
    expect(new Headers(init.headers).has("Authorization")).toBe(false);
  });

  it("accepts empty 204 responses", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    await expect(new ApiClient("").request("/sessions/current", { method: "DELETE" })).resolves.toBeUndefined();
  });
});
