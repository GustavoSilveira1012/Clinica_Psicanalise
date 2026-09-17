import type { SaasBillingSummary } from "../types/domain";
import { apiClient } from "./api-client";

export interface OnboardingSummary {
  organizationId: string;
  currentStep: "PROFILE" | "CLINIC" | "TEAM" | "NOTIFICATIONS" | "FIRST_APPOINTMENT" | "COMPLETE";
  status: "IN_PROGRESS" | "COMPLETED";
  completedSteps: string[];
  completedAt?: string;
}

export interface OrganizationMember {
  membershipId: string;
  organizationId: string;
  userId: number;
  userName: string;
  role: "OWNER" | "ADMIN" | "BILLING" | "MEMBER";
  status: string;
}

export interface OrganizationInvite {
  inviteId: string;
  maskedEmail: string;
  oneTimeToken: string;
  expiresAt: string;
}

export async function getSaasBilling(organizationId: string) {
  return apiClient.request<SaasBillingSummary>(`/organizations/${organizationId}/billing`);
}

export async function getOnboarding(organizationId: string) {
  return apiClient.request<OnboardingSummary>(`/organizations/${organizationId}/onboarding`);
}

export async function updateOnboarding(organizationId: string, step: OnboardingSummary["currentStep"]) {
  return apiClient.request<OnboardingSummary>(`/organizations/${organizationId}/onboarding`, {
    method: "PATCH",
    body: JSON.stringify({ step }),
  });
}

export async function acceptOrganizationInvite(token: string) {
  return apiClient.request<OrganizationMember>(`/organization-invites/${encodeURIComponent(token)}/accept`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export async function getOrganizationMembers(organizationId: string) {
  return apiClient.request<OrganizationMember[]>(`/organizations/${organizationId}/members`);
}

export async function createOrganizationInvite(
  organizationId: string,
  email: string,
  role: OrganizationMember["role"]
) {
  return apiClient.request<OrganizationInvite>(`/organizations/${organizationId}/invites`, {
    method: "POST",
    body: JSON.stringify({ email, role }),
  });
}
