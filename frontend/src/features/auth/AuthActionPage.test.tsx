import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthActionPage } from "./AuthActionPage";

describe("AuthActionPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("requests password recovery with a generic account-safe confirmation", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/auth/csrf")) return Response.json({ token: "csrf-synthetic" });
      if (url.endsWith("/auth/password/reset-request") && init?.method === "POST") {
        return Response.json({ message: "Se o endereço estiver cadastrado, você receberá as instruções por e-mail." }, { status: 202 });
      }
      return new Response(null, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    const user = userEvent.setup();
    render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={["/reset-password"]}>
      <Routes><Route path="/reset-password" element={<AuthActionPage />} /></Routes>
    </MemoryRouter>);
    await user.type(screen.getByLabelText("E-mail da conta"), "synthetic@example.invalid");
    await user.click(screen.getByRole("button", { name: "Enviar link de recuperação" }));

    expect(await screen.findByText("Se o endereço estiver cadastrado, você receberá as instruções por e-mail.")).toBeInTheDocument();
    const request = fetchMock.mock.calls.find(([url]) => String(url).endsWith("/auth/password/reset-request"));
    expect(request?.[1]?.body).toBe(JSON.stringify({ email: "synthetic@example.invalid" }));
  });

  it("submits only the one-time token and new password to reset", async () => {
    const token = "a".repeat(43);
    const replaceState = vi.spyOn(window.history, "replaceState");
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/auth/csrf")) return Response.json({ token: "csrf-synthetic" });
      if (url.endsWith("/auth/password/reset") && init?.method === "POST") return new Response(null, { status: 204 });
      return new Response(null, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    const user = userEvent.setup();
    render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={[`/reset-password#token=${token}`]}>
      <Routes><Route path="/reset-password" element={<AuthActionPage />} /></Routes>
    </MemoryRouter>);
    await user.type(screen.getByLabelText(/Nova senha/), "synthetic secure pass 2026");
    await user.type(screen.getByLabelText("Confirme a nova senha"), "synthetic secure pass 2026");
    await user.click(screen.getByRole("button", { name: "Salvar nova senha" }));

    expect(await screen.findByText(/Senha atualizada/)).toBeInTheDocument();
    expect(replaceState).toHaveBeenCalledWith(null, "", "/reset-password");
    const request = fetchMock.mock.calls.find(([url]) => String(url).endsWith("/auth/password/reset"));
    expect(request?.[1]?.body).toBe(JSON.stringify({ token, newPassword: "synthetic secure pass 2026" }));
  });

  it("automatically consumes an email-verification token from the URL fragment", async () => {
    const token = "b".repeat(43);
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/auth/email/verify")) return new Response(null, { status: 204 });
      return new Response(null, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={[`/verify-email#token=${token}`]}>
      <Routes><Route path="/verify-email" element={<AuthActionPage />} /></Routes>
    </MemoryRouter>);

    expect(await screen.findByText("E-mail confirmado. Você já pode entrar na sua conta.")).toBeInTheDocument();
    const request = fetchMock.mock.calls.find(([url]) => String(url).endsWith("/auth/email/verify"));
    expect(request?.[1]?.body).toBe(JSON.stringify({ token }));
  });
});
