import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppProviders } from "../../app/providers";
import { AppRouter } from "../../app/router";

describe("LoginPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("moves a valid backend login to the MFA step", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/auth/refresh")) return new Response(null, { status: 401 });
      if (url.endsWith("/auth/csrf")) return Response.json({ token: "csrf-test-token" });
      if (url.endsWith("/auth/login") && init?.method === "POST") {
        return Response.json({ status: "MFA_REQUIRED", challenge: "challenge-test" });
      }
      return new Response(null, { status: 404 });
    }));

    const user = userEvent.setup();
    render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={["/login"]}><AppProviders><AppRouter /></AppProviders></MemoryRouter>);
    await user.type(screen.getByLabelText("E-mail profissional"), "clinica@example.com");
    await user.type(screen.getByLabelText("Senha"), "senha-segura");
    await user.click(screen.getByRole("button", { name: /continuar/i }));
    expect(await screen.findByText("Segundo fator")).toBeInTheDocument();
    expect(screen.getByLabelText("Código MFA")).toBeInTheDocument();
  });
});
