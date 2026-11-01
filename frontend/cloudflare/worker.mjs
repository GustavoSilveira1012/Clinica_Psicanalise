const API_PREFIX = "/backend";

function unavailable(status = 503) {
  return new Response(JSON.stringify({ message: "Serviço temporariamente indisponível.", code: "API_UNAVAILABLE" }), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
    },
  });
}

function apiOrigin(value) {
  const url = new URL(value);
  if (url.protocol !== "https:" || !url.hostname.endsWith(".onrender.com") ||
      url.username || url.password || url.port || url.pathname !== "/" || url.search || url.hash) {
    throw new Error("Invalid API origin");
  }
  return url;
}

function cookiesFrom(headers) {
  // Workers preserves separate cookies with getAll; Node uses getSetCookie.
  if (typeof headers.getAll === "function") return headers.getAll("Set-Cookie");
  return headers.getSetCookie();
}

function cookieForPublicPath(cookie) {
  return cookie.replace(/(;\s*Path=)(\/auth(?:\/[^;]*)?)(?=;|$)/i, `$1${API_PREFIX}$2`);
}

export function createHandler(fetchApi = fetch) {
  return async function handle(request, env) {
    const publicUrl = new URL(request.url);
    if (!publicUrl.pathname.startsWith(`${API_PREFIX}/`)) {
      if (publicUrl.pathname === API_PREFIX) return unavailable(404);
      return env.ASSETS.fetch(request);
    }

    let upstream;
    try {
      upstream = apiOrigin(env.API_ORIGIN);
    } catch {
      return unavailable();
    }
    // Assign pathname instead of resolving a client path: //host cannot change origin.
    upstream.pathname = publicUrl.pathname.slice(API_PREFIX.length);
    upstream.search = publicUrl.search;
    const headers = new Headers(request.headers);
    for (const name of [...headers.keys()]) {
      if (name === "forwarded" || name.startsWith("x-forwarded-") || name === "x-real-ip") headers.delete(name);
    }
    headers.delete("host");
    headers.set("X-Forwarded-Proto", "https");
    headers.set("X-Forwarded-Host", publicUrl.host);
    const clientIp = request.headers.get("CF-Connecting-IP");
    if (clientIp) headers.set("X-Forwarded-For", clientIp);

    try {
      const response = await fetchApi(new Request(upstream, {
        method: request.method,
        headers,
        body: request.method === "GET" || request.method === "HEAD" ? undefined : request.body,
        redirect: "manual",
        duplex: "half",
      }), { cf: { cacheTtl: 0, cacheEverything: false } });
      // API requests must never follow a redirect to another destination with credentials.
      if (response.status >= 300 && response.status < 400 && response.status !== 304) return unavailable(502);
      const responseHeaders = new Headers(response.headers);
      responseHeaders.delete("Set-Cookie");
      for (const cookie of cookiesFrom(response.headers)) responseHeaders.append("Set-Cookie", cookieForPublicPath(cookie));
      responseHeaders.set("Cache-Control", "no-store");
      responseHeaders.set("CDN-Cache-Control", "no-store");
      responseHeaders.set("Cloudflare-CDN-Cache-Control", "no-store");
      responseHeaders.set("X-Content-Type-Options", "nosniff");
      responseHeaders.set("Referrer-Policy", "no-referrer");
      return new Response(response.body, { status: response.status, statusText: response.statusText, headers: responseHeaders });
    } catch {
      // No bodies, credentials, URLs or upstream exception details in logs/responses.
      return unavailable(502);
    }
  };
}

export default { fetch: createHandler() };
