import { lazy, Suspense } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AppShell } from "../shared/components/AppShell";
import { RequireAuth, RequirePermission } from "../shared/components/ProtectedRoute";
import { LoginPage } from "../features/auth/LoginPage";
import { MfaPage } from "../features/auth/MfaPage";
import { Skeleton } from "../shared/components/ui";

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
const NotFoundPage = lazy(() => import("../features/not-found/NotFoundPage").then(({ NotFoundPage }) => ({ default: NotFoundPage })));

function PageLoading() {
  return <div className="space-y-5" aria-busy="true" aria-label="Carregando módulo"><Skeleton className="h-8 w-56" /><Skeleton className="h-4 w-96 max-w-full" /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Skeleton className="h-32" /><Skeleton className="h-32" /><Skeleton className="h-32" /><Skeleton className="h-32" /></div><Skeleton className="h-72" /></div>;
}

export function AppRouter() {
  return <Routes><Route path="/login" element={<LoginPage />} /><Route path="/mfa" element={<MfaPage />} /><Route element={<RequireAuth />}><Route element={<AppShell><Suspense fallback={<PageLoading />}><Outlet /></Suspense></AppShell>}><Route element={<RequirePermission permission="dashboard:read" />}><Route index element={<DashboardPage />} /></Route><Route element={<RequirePermission permission="clinical:read" />}><Route path="agenda" element={<AgendaPage />} /><Route path="clinical-records" element={<ClinicalRecordsPage />} /><Route path="patients/:patientId/clinical-record" element={<ClinicalRecordPage />} /></Route><Route element={<RequirePermission permission="patients:read" />}><Route path="patients" element={<PatientsPage />} /><Route path="patients/:patientId" element={<PatientDetailsPage />} /></Route><Route element={<RequirePermission permission="finance:read" />}><Route path="finance" element={<FinancePage />} /></Route><Route element={<RequirePermission permission="fiscal:read" />}><Route path="fiscal" element={<FiscalPage />} /></Route><Route element={<RequirePermission permission="packages:read" />}><Route path="packages" element={<PackagesPage />} /><Route path="subscriptions" element={<PackagesPage />} /></Route><Route element={<RequirePermission permission="notifications:read" />}><Route path="notifications" element={<NotificationsPage />} /></Route><Route element={<RequirePermission permission="privacy:read" />}><Route path="compliance" element={<CompliancePage />} /></Route><Route element={<RequirePermission permission="settings:manage" />}><Route path="settings" element={<SettingsPage />} /></Route><Route path="profile" element={<ProfilePage />} /><Route path="*" element={<NotFoundPage />} /></Route></Route><Route path="*" element={<Navigate to="/login" replace />} /></Routes>;
}
