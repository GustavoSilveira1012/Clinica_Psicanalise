export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export class ApiClient {
  private accessToken: string | null = null;
  private csrfToken: string | null = null;
  private organizationId: string | null = null;
  private professionalId: number | null = null;
  private refreshPromise: Promise<string | null> | null = null;

  constructor(private readonly baseUrl: string) {}

  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  setOrganizationId(organizationId: string | null) {
    this.organizationId = organizationId;
  }

  setProfessionalId(professionalId: number | null) {
    this.professionalId = professionalId;
  }

  getProfessionalId() {
    return this.professionalId;
  }

  clearSession() {
    this.accessToken = null;
    this.csrfToken = null;
    this.organizationId = null;
    this.professionalId = null;
  }

  async request<T>(path: string, init?: RequestInit & { skipAuthRefresh?: boolean }): Promise<T> {
    const method = (init?.method ?? "GET").toUpperCase();
    const mutating = !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
    if (mutating && path.startsWith("/auth/") && path !== "/auth/csrf" && !this.csrfToken) {
      await this.ensureCsrf();
    }

    const headers = new Headers(init?.headers);
    if (!(init?.body instanceof FormData) && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    if (this.accessToken && !path.startsWith("/auth/refresh")) {
      headers.set("Authorization", `Bearer ${this.accessToken}`);
    }
    if (this.csrfToken && mutating && path.startsWith("/auth/")) {
      headers.set("X-CSRF-TOKEN", this.csrfToken);
    }
    if (this.organizationId) headers.set("X-Organization-Id", this.organizationId);

    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers,
      credentials: "include",
    });

    if (response.status === 401 && !init?.skipAuthRefresh && !path.startsWith("/auth/")) {
      const token = await this.refresh();
      if (token) return this.request<T>(path, { ...init, skipAuthRefresh: true });
    }

    if (!response.ok) {
      const payload = await response.json().catch(() => null) as { detail?: string; message?: string; code?: string } | null;
      const message = payload?.detail ?? payload?.message ?? "Não foi possível completar a operação.";
      throw new ApiError(message, response.status, payload?.code);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }

  private async ensureCsrf() {
    const response = await fetch(`${this.baseUrl}/auth/csrf`, { credentials: "include" });
    if (!response.ok) throw new ApiError("Não foi possível iniciar a sessão segura.", response.status);
    const payload = await response.json() as { token?: string };
    if (!payload.token) throw new ApiError("Token CSRF ausente na resposta do servidor.", 500);
    this.csrfToken = payload.token;
  }

  private async refresh(): Promise<string | null> {
    if (!this.refreshPromise) {
      this.refreshPromise = (async () => {
        try {
          const response = await this.request<{ accessToken: string }>("/auth/refresh", { method: "POST", skipAuthRefresh: true });
          this.accessToken = response.accessToken;
          return this.accessToken;
        } catch {
          this.accessToken = null;
          return null;
        } finally {
          this.refreshPromise = null;
        }
      })();
    }
    return this.refreshPromise;
  }
}

const configuredApiUrl = import.meta.env.VITE_API_URL ?? "";
export const apiClient = new ApiClient(configuredApiUrl.replace(/\/api\/v1\/?$/, ""));
