import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { AuthSession } from "../../shared/types/domain";
import { demoUser } from "../../shared/lib/permissions";

interface AuthContextValue {
  session: AuthSession | null;
  pendingMfaEmail: string | null;
  login: (email: string, password: string) => Promise<void>;
  verifyMfa: (code: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const SESSION_KEY = "psicogest-demo-session";

function storedSession(): AuthSession | null {
  return sessionStorage.getItem(SESSION_KEY) === "active" ? { user: demoUser, sessionId: "sess-demo", lastActivityAt: new Date().toISOString() } : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => storedSession());
  const [pendingMfaEmail, setPendingMfaEmail] = useState<string | null>(null);

  const value = useMemo<AuthContextValue>(() => ({
    session,
    pendingMfaEmail,
    async login(email, password) {
      if (!email || !password) throw new Error("Informe suas credenciais.");
      setPendingMfaEmail(email);
    },
    async verifyMfa(code) {
      if (!/^\d{6}$/.test(code)) throw new Error("Código MFA inválido.");
      const next: AuthSession = { user: { ...demoUser, email: pendingMfaEmail || demoUser.email }, sessionId: `sess-${Date.now()}`, lastActivityAt: new Date().toISOString() };
      sessionStorage.setItem(SESSION_KEY, "active");
      setSession(next);
      setPendingMfaEmail(null);
    },
    logout() {
      sessionStorage.removeItem(SESSION_KEY);
      setSession(null);
      setPendingMfaEmail(null);
    },
  }), [pendingMfaEmail, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth precisa estar dentro de AuthProvider");
  return context;
}
