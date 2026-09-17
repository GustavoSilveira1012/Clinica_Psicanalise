import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthSession, AuthUser, OrganizationOption, Permission, Tenant, UserRole } from "../../shared/types/domain";
import { apiClient, ApiError } from "../../shared/lib/api-client";
import { demoUser } from "../../shared/lib/permissions";

interface AuthContextValue {
  session: AuthSession | null;
  isInitializing: boolean;
  pendingMfaEmail: string | null;
  pendingMfaChallenge: string | null;
  pendingMfaEnrollment: boolean;
  login: (email: string, password: string) => Promise<void>;
  verifyMfa: (code: string) => Promise<void>;
  setupMfa: () => Promise<{ secret: string; otpauthUri: string }>;
  confirmMfa: (code: string) => Promise<void>;
  selectOrganization: (organizationId: string) => void;
  logout: () => Promise<void>;
}

interface AuthProfile {
  userId: number;
  name: string;
  email: string;
  role: string;
  psychoanalystId?: number;
  sessionId?: string;
  organizations: Array<{ id: string; name: string; slug: string; type: string; status: string; timezone: string; role: string }>;
}

interface LoginResponse {
  status: "AUTHENTICATED" | "MFA_REQUIRED" | "MFA_ENROLLMENT_REQUIRED";
  authentication?: { accessToken: string; userId: number; role: string };
  challenge?: string;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const SESSION_KEY = "psicogest-demo-session";
const DEMO_MODE = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE === "true";
const DEMO_EMAIL = "demo@psicogest.com";
const DEMO_PASSWORD = "demo123";
const allPermissions: Permission[] = ["dashboard:read", "clinical:read", "clinical:write", "patients:read", "patients:write", "finance:read", "finance:write", "fiscal:read", "packages:read", "notifications:read", "notifications:manage", "privacy:read", "settings:manage", "billing:read"];

function permissionsFor(role: UserRole): Permission[] {
  if (role === "OWNER" || role === "ADMIN") return allPermissions;
  if (role === "FINANCE") return ["dashboard:read", "finance:read", "finance:write", "fiscal:read", "packages:read", "notifications:read"];
  return ["dashboard:read", "clinical:read", "clinical:write", "patients:read", "patients:write", "packages:read", "notifications:read", "privacy:read"];
}

function frontendRole(role: string): UserRole {
  if (["ADMIN", "CLINIC_ADMIN", "SYSTEM_ADMIN"].includes(role)) return role === "ADMIN" ? "ADMIN" : "OWNER";
  if (role === "PSYCHOANALYST") return "CLINICAL";
  return "CLINICAL";
}

function userFromProfile(profile: AuthProfile): AuthUser {
  const role = frontendRole(profile.role);
  const name = profile.name || profile.email;
  const organization = profile.organizations[0];
  const tenant: Tenant = organization
    ? { id: organization.id, name: organization.name, documentLabel: "Documento protegido", timezone: organization.timezone, plan: "Growth" }
    : { id: "unselected", name: "Organização não selecionada", documentLabel: "Documento protegido", timezone: "America/Sao_Paulo", plan: "Growth" };
  const organizations: OrganizationOption[] = profile.organizations.map((item) => ({
    id: item.id,
    name: item.name,
    slug: item.slug,
    type: item.type as OrganizationOption["type"],
    status: item.status as OrganizationOption["status"],
    timezone: item.timezone,
    role: item.role as OrganizationOption["role"],
  }));
  return { id: String(profile.userId), name, email: profile.email, role, initials: name.split(/\s+/).map((part) => part[0]).join("").slice(0, 2).toUpperCase(), tenant, organizations, permissions: permissionsFor(role) };
}

async function profileSession(): Promise<AuthSession> {
  const profile = await apiClient.request<AuthProfile>("/auth/me");
  const user = userFromProfile(profile);
  apiClient.setOrganizationId(user.tenant.id === "unselected" ? null : user.tenant.id);
  apiClient.setProfessionalId(profile.psychoanalystId ?? null);
  return { user, sessionId: profile.sessionId ?? `session-${profile.userId}`, lastActivityAt: new Date().toISOString() };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(DEMO_MODE && sessionStorage.getItem(SESSION_KEY) === "active" ? { user: demoUser, sessionId: "sess-demo", lastActivityAt: new Date().toISOString() } : null);
  const [isInitializing, setIsInitializing] = useState(!DEMO_MODE);
  const [pendingMfaEmail, setPendingMfaEmail] = useState<string | null>(null);
  const [pendingMfaChallenge, setPendingMfaChallenge] = useState<string | null>(null);
  const [pendingMfaEnrollment, setPendingMfaEnrollment] = useState(false);

  useEffect(() => {
    if (DEMO_MODE) return;
    let active = true;
    void (async () => {
      try {
        const response = await apiClient.request<{ accessToken: string }>("/auth/refresh", { method: "POST" });
        apiClient.setAccessToken(response.accessToken);
        const restored = await profileSession();
        if (active) setSession(restored);
      } catch {
        apiClient.clearSession();
      } finally {
        if (active) setIsInitializing(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const finishAuthentication = async (accessToken: string) => {
    apiClient.setAccessToken(accessToken);
    setSession(await profileSession());
    setPendingMfaEmail(null); setPendingMfaChallenge(null); setPendingMfaEnrollment(false);
  };

  const value = useMemo<AuthContextValue>(() => ({
    session, isInitializing, pendingMfaEmail, pendingMfaChallenge, pendingMfaEnrollment,
    async login(email, password) {
      if (DEMO_MODE) {
        if (email.trim().toLowerCase() !== DEMO_EMAIL || password !== DEMO_PASSWORD) throw new Error("Use as credenciais de demonstração exibidas nesta tela.");
        setPendingMfaEmail(email); setPendingMfaChallenge("demo"); setPendingMfaEnrollment(false); return;
      }
      const response = await apiClient.request<LoginResponse>("/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
      if (response.status === "AUTHENTICATED" && response.authentication) { await finishAuthentication(response.authentication.accessToken); return; }
      if (!response.challenge) throw new ApiError("O servidor não retornou o desafio de autenticação.", 502);
      setPendingMfaEmail(email); setPendingMfaChallenge(response.challenge); setPendingMfaEnrollment(response.status === "MFA_ENROLLMENT_REQUIRED");
    },
    async verifyMfa(code) {
      if (DEMO_MODE) {
        if (!/^\d{6}$/.test(code)) throw new Error("Código MFA inválido.");
        sessionStorage.setItem(SESSION_KEY, "active"); setSession({ user: { ...demoUser, email: pendingMfaEmail || demoUser.email }, sessionId: `sess-${Date.now()}`, lastActivityAt: new Date().toISOString() });
        setPendingMfaEmail(null); setPendingMfaChallenge(null); return;
      }
      if (!pendingMfaChallenge) throw new Error("Desafio MFA expirado. Faça login novamente.");
      const response = await apiClient.request<{ accessToken: string }>("/auth/mfa/totp/verify", { method: "POST", body: JSON.stringify({ challenge: pendingMfaChallenge, code }) });
      await finishAuthentication(response.accessToken);
    },
    async setupMfa() {
      if (!pendingMfaChallenge) throw new Error("Desafio MFA expirado. Faça login novamente.");
      return apiClient.request<{ secret: string; otpauthUri: string }>("/auth/mfa/totp/setup", { method: "POST", body: JSON.stringify({ challenge: pendingMfaChallenge }) });
    },
    async confirmMfa(code) {
      if (!pendingMfaChallenge) throw new Error("Desafio MFA expirado. Faça login novamente.");
      const response = await apiClient.request<{ authentication: { accessToken: string } }>("/auth/mfa/totp/confirm", { method: "POST", body: JSON.stringify({ challenge: pendingMfaChallenge, code }) });
      await finishAuthentication(response.authentication.accessToken);
    },
    selectOrganization(organizationId) {
      setSession((current) => {
        const organization = current?.user.organizations?.find((item) => item.id === organizationId);
        if (!current || !organization || organization.status !== "ACTIVE") return current;
        apiClient.setOrganizationId(organization.id);
        return { ...current, user: { ...current.user, tenant: { ...current.user.tenant, id: organization.id, name: organization.name, timezone: organization.timezone ?? current.user.tenant.timezone } } };
      });
    },
    async logout() {
      try { if (!DEMO_MODE) await apiClient.request<void>("/auth/logout", { method: "POST" }); } finally {
        sessionStorage.removeItem(SESSION_KEY); apiClient.clearSession(); setSession(null); setPendingMfaEmail(null); setPendingMfaChallenge(null); setPendingMfaEnrollment(false);
      }
    },
  }), [isInitializing, pendingMfaChallenge, pendingMfaEmail, pendingMfaEnrollment, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth precisa estar dentro de AuthProvider");
  return context;
}
