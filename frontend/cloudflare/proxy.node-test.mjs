import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { createHandler } from "./worker.mjs";
import { buildEnvironment } from "./build.mjs";

const origin = "https://psicogest-synthetic-api.onrender.com";
const publicOrigin = "https://psicogest-synthetic-web.example.workers.dev";
const environment = { API_ORIGIN: origin, ASSETS: { fetch: () => new Response("<main>SPA</main>") } };
const request = (path, options) => new Request(`${publicOrigin}${path}`, options);

test("SPA navigation remains separate from backend requests", async () => {
  const handler = createHandler(() => assert.fail("Navigation must not contact the API"));
  assert.equal(await (await handler(request("/patients/9001"), environment)).text(), "<main>SPA</main>");
  assert.equal((await handler(request("/backend"), environment)).status, 404);
});

test("missing or unsafe API origins fail closed", async (t) => {
  const invalid = [undefined, "", "http://a.onrender.com", "https://a.onrender.com.evil.invalid", "https://localhost", "https://user:password@a.onrender.com", "https://a.onrender.com:8443", "https://a.onrender.com/path", "https://a.onrender.com?target=x", "https://a.onrender.com#fragment"];
  for (const [index, API_ORIGIN] of invalid.entries()) {
    await t.test(`invalid origin ${index}`, async () => {
      const handler = createHandler(() => assert.fail("Unsafe origin must not be fetched"));
      const response = await handler(request("/backend/auth/me"), { ...environment, API_ORIGIN });
      assert.equal(response.status, 503);
      assert.equal(response.headers.get("Cache-Control"), "no-store");
      assert.equal((await response.json()).code, "API_UNAVAILABLE");
    });
  }
});

test("proxy preserves POST body, authentication, CSRF and tenant context", async () => {
  const handler = createHandler(async (forwarded, options) => {
    assert.equal(forwarded.url, `${origin}/auth/refresh?test=1`);
    assert.equal(forwarded.method, "POST");
    assert.equal(await forwarded.text(), '{"synthetic":true}');
    assert.equal(forwarded.headers.get("Cookie"), "refresh=synthetic");
    assert.equal(forwarded.headers.get("Authorization"), "Bearer synthetic");
    assert.equal(forwarded.headers.get("X-CSRF-TOKEN"), "synthetic-csrf");
    assert.equal(forwarded.headers.get("X-Organization-Id"), "synthetic-org");
    assert.equal(forwarded.headers.get("Idempotency-Key"), "synthetic-key");
    assert.equal(forwarded.headers.get("Forwarded"), null);
    assert.equal(forwarded.headers.get("X-Real-IP"), null);
    assert.equal(forwarded.headers.get("X-Forwarded-Prefix"), null);
    assert.equal(forwarded.headers.get("X-Forwarded-Proto"), "https");
    assert.equal(forwarded.headers.get("X-Forwarded-Host"), new URL(publicOrigin).host);
    assert.equal(forwarded.headers.get("X-Forwarded-For"), "192.0.2.9");
    assert.equal(forwarded.redirect, "manual");
    assert.deepEqual(options.cf, { cacheTtl: 0, cacheEverything: false });
    return new Response('{"ok":true}');
  });
  const response = await handler(request("/backend/auth/refresh?test=1", {
    method: "POST", body: '{"synthetic":true}', headers: {
      Cookie: "refresh=synthetic", Authorization: "Bearer synthetic", "X-CSRF-TOKEN": "synthetic-csrf",
      "X-Organization-Id": "synthetic-org", "Idempotency-Key": "synthetic-key", "CF-Connecting-IP": "192.0.2.9",
      Forwarded: "host=evil.invalid", "X-Real-IP": "198.51.100.1", "X-Forwarded-For": "198.51.100.1",
      "X-Forwarded-Host": "evil.invalid", "X-Forwarded-Proto": "http", "X-Forwarded-Prefix": "/evil",
    },
  }), environment);
  assert.deepEqual(await response.json(), { ok: true });
});

test("multiple cookies and their security attributes survive the prefix rewrite", async () => {
  const headers = new Headers();
  headers.append("Set-Cookie", "refresh=synthetic; Path=/auth; Expires=Wed, 30 Sep 2026 10:00:00 GMT; Secure; HttpOnly; SameSite=Strict");
  headers.append("Set-Cookie", "psicogest_csrf=synthetic; Path=/; Secure; SameSite=Strict");
  const response = await createHandler(async () => new Response("{}", { headers }))(request("/backend/auth/login"), environment);
  const cookies = response.headers.getSetCookie();
  assert.equal(cookies.length, 2);
  assert.equal(cookies[0], "refresh=synthetic; Path=/backend/auth; Expires=Wed, 30 Sep 2026 10:00:00 GMT; Secure; HttpOnly; SameSite=Strict");
  assert.equal(cookies[1], "psicogest_csrf=synthetic; Path=/; Secure; SameSite=Strict");
});

test("logout cookie deletion uses the same rewritten cookie path", async () => {
  const response = await createHandler(async () => new Response(null, { status: 204, headers: {
    "Set-Cookie": "refresh=; Path=/auth; Max-Age=0; Secure; HttpOnly; SameSite=Strict",
  } }))(request("/backend/auth/logout", { method: "POST" }), environment);
  assert.equal(response.status, 204);
  assert.equal(response.headers.getSetCookie()[0], "refresh=; Path=/backend/auth; Max-Age=0; Secure; HttpOnly; SameSite=Strict");
});

test("Cloudflare getAll preserves cookies without splitting Expires commas", async () => {
  const headers = new Headers({ "Content-Type": "application/json" });
  headers.getAll = (name) => {
    assert.equal(name, "Set-Cookie");
    return ["refresh=x; Path=/auth; Expires=Wed, 30 Sep 2026 10:00:00 GMT; Secure", "csrf=y; Path=/"];
  };
  const response = await createHandler(async () => ({ headers, status: 200, statusText: "OK", body: null }))(request("/backend/auth/me"), environment);
  assert.equal(response.headers.getSetCookie().length, 2);
});

test("API errors preserve status/body and disable every CDN cache", async () => {
  const handler = createHandler(async () => new Response('{"code":"IDEMPOTENCY_CONFLICT"}', { status: 409, headers: { "Cache-Control": "public, max-age=60" } }));
  const response = await handler(request("/backend/api/v1/payments"), environment);
  assert.equal(response.status, 409);
  assert.equal(await response.text(), '{"code":"IDEMPOTENCY_CONFLICT"}');
  for (const header of ["Cache-Control", "CDN-Cache-Control", "Cloudflare-CDN-Cache-Control"]) assert.equal(response.headers.get(header), "no-store");
});

test("client double slashes and query targets cannot change the upstream host", async () => {
  const handler = createHandler(async (forwarded) => {
    assert.equal(new URL(forwarded.url).origin, origin);
    assert.equal(new URL(forwarded.url).pathname, "//evil.invalid/auth/me");
    assert.equal(new URL(forwarded.url).searchParams.get("url"), "https://evil.invalid");
    return new Response("{}");
  });
  assert.equal((await handler(request("/backend//evil.invalid/auth/me?url=https://evil.invalid"), environment)).status, 200);
});

test("redirects never forward credentials to a second host", async () => {
  let calls = 0;
  const response = await createHandler(async (forwarded) => {
    calls++;
    assert.equal(forwarded.redirect, "manual");
    return new Response(null, { status: 302, headers: { Location: "https://evil.invalid" } });
  })(request("/backend/auth/me", { headers: { Authorization: "Bearer synthetic" } }), environment);
  assert.equal(response.status, 502);
  assert.equal(response.headers.get("Location"), null);
  assert.equal(calls, 1);
});

test("network failure does not disclose upstream details", async () => {
  const response = await createHandler(async () => { throw new Error("synthetic-secret-detail"); })(request("/backend/auth/me"), environment);
  assert.equal(response.status, 502);
  assert.ok(!(await response.text()).includes("synthetic-secret-detail"));
});

test("cloud build overrides local demo, backend URL and clinical release flags", () => {
  const source = { VITE_API_URL: "http://127.0.0.1:18080", VITE_DEMO_MODE: "true", VITE_CLINICAL_DATA_ENABLED: "true", VITE_CLINICAL_DATA_RELEASE_APPROVED: "true", SYNTHETIC_TEST: "preserved" };
  const result = buildEnvironment(source);
  assert.equal(result.VITE_API_URL, "/backend");
  assert.equal(result.VITE_DEMO_MODE, "false");
  assert.equal(result.VITE_CLINICAL_ONLY_PILOT, "true");
  assert.equal(result.VITE_CLINICAL_DATA_ENABLED, "false");
  assert.equal(result.VITE_CLINICAL_DATA_RELEASE_APPROVED, "false");
  assert.equal(result.SYNTHETIC_TEST, "preserved");
  assert.equal(source.VITE_DEMO_MODE, "true");
});

test("manifest uses static assets and proxy paths with no paid resource bindings", async () => {
  const manifest = JSON.parse(await readFile(new URL("../wrangler.json", import.meta.url), "utf8"));
  assert.deepEqual(manifest.assets.run_worker_first, ["/backend", "/backend/*"]);
  assert.equal(manifest.assets.not_found_handling, "single-page-application");
  assert.equal(manifest.vars.API_ORIGIN, "https://psicogest-synthetic-api.onrender.com");
  assert.equal(manifest.observability.enabled, false);
  for (const key of ["containers", "r2_buckets", "durable_objects", "d1_databases", "queues", "workflows"]) assert.equal(manifest[key], undefined);
});
