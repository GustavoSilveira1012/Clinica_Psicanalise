export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export class ApiClient {
  constructor(private readonly baseUrl: string) {}

  async request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {}),
      },
    });

    if (!response.ok) {
      const message = await response.text().catch(() => "");
      throw new ApiError(message || "Não foi possível completar a operação.", response.status);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }
}

export const apiClient = new ApiClient(import.meta.env.VITE_API_URL ?? "/api/v1");
