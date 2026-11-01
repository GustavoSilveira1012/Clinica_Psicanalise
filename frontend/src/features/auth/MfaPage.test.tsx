import { StrictMode } from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MfaPage } from "./MfaPage";
import { useAuth } from "./AuthContext";

vi.mock("./AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("qrcode", () => ({ default: { toDataURL: vi.fn().mockResolvedValue("data:image/png;base64,synthetic-qr") } }));

describe("MfaPage", () => {
  beforeEach(() => vi.mocked(useAuth).mockReset());

  it("continues to the protected destination if MFA succeeded before the challenge state cleared", async () => {
    vi.mocked(useAuth).mockReturnValue({
      session: { user: {}, sessionId: "synthetic-session", lastActivityAt: new Date(0).toISOString() },
      pendingMfaEmail: null,
      pendingMfaChallenge: null,
      pendingMfaEnrollment: false,
      isInitializing: false,
      login: vi.fn(),
      verifyMfa: vi.fn(),
      setupMfa: vi.fn(),
      confirmMfa: vi.fn(),
      selectOrganization: vi.fn(),
      logout: vi.fn(),
    } as unknown as ReturnType<typeof useAuth>);

    render(
      <MemoryRouter
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        initialEntries={[{ pathname: "/mfa", state: { from: "/dashboard" } }]}
      >
        <Routes>
          <Route path="/mfa" element={<MfaPage />} />
          <Route path="/dashboard" element={<h1>Destino autenticado</h1>} />
          <Route path="/login" element={<h1>Login</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "Destino autenticado" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Login" })).not.toBeInTheDocument();
  });

  it("requests enrollment once in StrictMode and renders the authenticator QR locally", async () => {
    const setupMfa = vi.fn().mockResolvedValue({
      secret: "SYNTHETIC-SECRET",
      otpauthUri: "otpauth://totp/PsicoGest:test?secret=SYNTHETIC-SECRET",
    });
    vi.mocked(useAuth).mockReturnValue({
      session: null,
      pendingMfaEmail: "synthetic@example.test",
      pendingMfaChallenge: "synthetic-challenge",
      pendingMfaEnrollment: true,
      isInitializing: false,
      login: vi.fn(), verifyMfa: vi.fn(), setupMfa, confirmMfa: vi.fn(),
      selectOrganization: vi.fn(), logout: vi.fn(),
    } as unknown as ReturnType<typeof useAuth>);

    render(
      <StrictMode>
        <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={["/mfa"]}>
          <Routes><Route path="/mfa" element={<MfaPage />} /><Route path="/login" element={<h1>Login</h1>} /></Routes>
        </MemoryRouter>
      </StrictMode>,
    );

    expect(await screen.findByRole("img", { name: "QR code para cadastrar o PsicoGest no aplicativo autenticador" })).toHaveAttribute(
      "src", "data:image/png;base64,synthetic-qr",
    );
    expect(setupMfa).toHaveBeenCalledTimes(1);
    expect(screen.getByText("SYNTHETIC-SECRET")).toBeInTheDocument();
  });
});
