import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AppShell } from "../shared/components/AppShell";
import { RequireAuth, RequirePermission } from "../shared/components/ProtectedRoute";
import { LoginPage } from "../features/auth/LoginPage";
import { MfaPage } from "../features/auth/MfaPage";
import { DemoRequestPage } from "../features/public/DemoRequestPage";
import { LandingPage } from "../features/public/LandingPage";
import { InvitePage } from "../features/saas/InvitePage";
import { OnboardingPage } from "../features/saas/OnboardingPage";
import { useAuth } from "../features/auth/AuthContext";
import { Skeleton } from "../shared/components/ui";
import { clinicalDataEnabled, clinicalDataRoutes, clinicalOnlyPilot, pilotUnavailableRoutes } from "../shared/lib/pilot-scope";

const DashboardPage = lazy(() => import("../features/dashboard/DashboardPage").then(({ DashboardPage }) => ({ default: DashboardPage })));
const AgendaPage = lazy(() => import("../features/agenda/AgendaPage").then(({ AgendaPage }) => ({ default: AgendaPage })));
const PatientsPage = lazy(() => import("../features/patients/PatientsPage").then(({ PatientsPage }) => ({ default: PatientsPage })));
const PatientDetailsPage = lazy(() => import("../features/patients/PatientDetailsPage").then(({ PatientDetailsPage }) => ({ default: PatientDetailsPage })));
const ClinicalRecordPage = lazy(() => import("../features/clinical/ClinicalRecordPage").then(({ ClinicalRecordPage }) => ({ default: ClinicalRecordPage })));
const ClinicalRecordsPage = lazy(() => import("../features/clinical/ClinicalRecordsPage").then(({ ClinicalRecordsPage }) => ({ default: ClinicalRecordsPage })));
const FinancePage = lazy(() => import("../features/finance/FinancePage").then(({ FinancePage }) => ({ default: FinancePage })));
const FiscalPage = lazy(() => import("../features/fiscal/FiscalPage").then(({ FiscalPage }) => ({ default: FiscalPage })));
const PackagesPage = lazy(() => import("../features/packages/PackagesPage").then(({ PackagesPage }) => ({ default: PackagesPage })));
const NotificationsPage = lazy(() => import("../features/notifications/NotificationsPage").then(({ NotificationsPage }) => ({ default: NotificationsPage })));
const CompliancePage = lazy(() => import("../features/compliance/CompliancePage").then(({ CompliancePage }) => ({ default: CompliancePage })));
const SettingsPage = lazy(() => import("../features/settings/SettingsPage").then(({ SettingsPage }) => ({ default: SettingsPage })));
const ProfilePage = lazy(() => import("../features/profile/ProfilePage").then(({ ProfilePage }) => ({ default: ProfilePage })));
const BillingPage = lazy(() => import("../features/saas/BillingPage").then(({ BillingPage }) => ({ default: BillingPage })));
const NotFoundPage = lazy(() => import("../features/not-found/NotFoundPage").then(({ NotFoundPage }) => ({ default: NotFoundPage })));

function PublicHome() {
  const { session } = useAuth();
  return session ? <Navigate to="/dashboard" replace /> : <LandingPage />;
}

function PageLoading() {
  return <div className="space-y-5" aria-busy="true" aria-label="Carregando módulo"><Skeleton className="h-8 w-56" /><Skeleton className="h-4 w-96 max-w-full" /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Skeleton className="h-32" /><Skeleton className="h-32" /><Skeleton className="h-32" /><Skeleton className="h-32" /></div><Skeleton className="h-72" /></div>;
}

function PilotModuleGate({ path, title }: { path: string; title: string }) {
  if (clinicalOnlyPilot && pilotUnavailableRoutes.has(path)) {
    return <section className="rounded-2xl border border-amber-200 bg-amber-50 p-6 dark:border-amber-900 dark:bg-amber-950/30" role="status">
      <p className="text-xs font-bold uppercase tracking-[0.16em] text-amber-800 dark:text-amber-300">Escopo do piloto</p>
      <h1 className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">{title} indisponível</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">Este módulo está desativado neste piloto clínico-operacional. Nenhuma cobrança, nota fiscal ou notificação automática será executada.</p>
    </section>;
  }
  return null;
}

function PilotModule({ path, title, children }: { path: string; title: string; children: ReactNode }) {
  return clinicalOnlyPilot && pilotUnavailableRoutes.has(path) ? <PilotModuleGate path={path} title={title} /> : children;
}

function ClinicalDataModule({ path, title, children }: { path: string; title: string; children: ReactNode }) {
  if (!clinicalDataEnabled && clinicalDataRoutes.has(path)) {
    return <section className="rounded-2xl border border-amber-200 bg-amber-50 p-6 dark:border-amber-900 dark:bg-amber-950/30" role="status">
      <p className="text-xs font-bold uppercase tracking-[0.16em] text-amber-800 dark:text-amber-300">Ambiente protegido</p>
      <h1 className="mt-2 font-display text-2xl font-extrabold text-ink dark:text-white">{title} indisponível</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">O acesso a dados clínicos e financeiros está desabilitado neste ambiente. Não conecte bases com dados reais; habilite o acesso somente após a validação formal dos controles de produção.</p>
    </section>;
  }
  return children;
}

function AppFrame() {
  return <AppShell><Suspense fallback={<PageLoading />}><Outlet /></Suspense></AppShell>;
}

const workspaceRoutes = <>
    <Route index element={<Navigate to="dashboard" replace />} />
    <Route element={<RequirePermission permission="dashboard:read" />}><Route path="dashboard" element={<ClinicalDataModule path="/dashboard" title="Dashboard"><DashboardPage /></ClinicalDataModule>} /></Route>
    <Route element={<RequirePermission permission="clinical:read" />}><Route path="agenda" element={<ClinicalDataModule path="/agenda" title="Agenda"><AgendaPage /></ClinicalDataModule>} /><Route path="clinical-records" element={<ClinicalDataModule path="/clinical-records" title="Prontuários"><ClinicalRecordsPage /></ClinicalDataModule>} /><Route path="patients/:patientId/clinical-record" element={<ClinicalDataModule path="/clinical-records" title="Prontuário"><ClinicalRecordPage /></ClinicalDataModule>} /></Route>
    <Route element={<RequirePermission permission="patients:read" />}><Route path="patients" element={<ClinicalDataModule path="/patients" title="Pacientes"><PatientsPage /></ClinicalDataModule>} /><Route path="patients/:patientId" element={<ClinicalDataModule path="/patients" title="Paciente"><PatientDetailsPage /></ClinicalDataModule>} /></Route>
    <Route element={<RequirePermission permission="finance:read" />}><Route path="finance" element={<ClinicalDataModule path="/finance" title="Financeiro"><FinancePage /></ClinicalDataModule>} /></Route>
    <Route element={<RequirePermission permission="fiscal:read" />}><Route path="fiscal" element={<PilotModule path="/fiscal" title="Fiscal / NFS-e"><FiscalPage /></PilotModule>} /></Route>
    <Route element={<RequirePermission permission="packages:read" />}><Route path="packages" element={<PilotModule path="/packages" title="Pacotes"><PackagesPage /></PilotModule>} /><Route path="subscriptions" element={<PilotModule path="/subscriptions" title="Assinaturas"><PackagesPage /></PilotModule>} /></Route>
    <Route element={<RequirePermission permission="notifications:read" />}><Route path="notifications" element={<PilotModule path="/notifications" title="Notificações"><NotificationsPage /></PilotModule>} /></Route>
    <Route element={<RequirePermission permission="privacy:read" />}><Route path="compliance" element={<ClinicalDataModule path="/compliance" title="LGPD & compliance"><CompliancePage /></ClinicalDataModule>} /></Route>
    <Route element={<RequirePermission permission="settings:manage" />}><Route path="settings" element={<SettingsPage />} /></Route>
    <Route element={<RequirePermission permission="billing:read" />}><Route path="billing" element={<BillingPage />} /></Route>
    <Route path="onboarding" element={<OnboardingPage />} />
    <Route path="profile" element={<ProfilePage />} />
    <Route path="*" element={<NotFoundPage />} />
  </>;

export function AppRouter() {
  return <Routes>
    <Route path="/" element={<PublicHome />} />
    <Route path="/demo" element={<DemoRequestPage />} />
    <Route path="/login" element={<LoginPage />} />
    <Route path="/mfa" element={<MfaPage />} />
    <Route path="/invite/:token" element={<InvitePage />} />
    <Route element={<RequireAuth />}>
      <Route element={<AppFrame />}>{workspaceRoutes}</Route>
      <Route path="app/:organizationSlug" element={<AppFrame />}>{workspaceRoutes}</Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>;
}
