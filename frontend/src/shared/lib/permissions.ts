import type { AuthUser, Permission, UserRole } from "../types/domain";

export const roleLabels: Record<UserRole, string> = {
  OWNER: "Administradora",
  CLINICAL: "Equipe clínica",
  ADMIN: "Administrativo",
  FINANCE: "Financeiro",
};

export function can(user: AuthUser | null, permission: Permission) {
  return Boolean(user?.permissions.includes(permission));
}

export const demoUser: AuthUser = {
  id: "usr-helena",
  name: "Helena Costa",
  email: "demo@psicogest.com",
  role: "OWNER",
  initials: "HC",
  tenant: { id: "clinic-1", name: "Clínica Horizonte", documentLabel: "CNPJ ••.•••.•••/0001-••", timezone: "America/Sao_Paulo", plan: "Growth" },
  permissions: ["dashboard:read", "clinical:read", "clinical:write", "patients:read", "patients:write", "finance:read", "finance:write", "fiscal:read", "packages:read", "notifications:read", "notifications:manage", "privacy:read", "settings:manage"],
};
