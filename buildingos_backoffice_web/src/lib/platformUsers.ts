/**
 * Typed browser client for the back-office-service users proxy over auth-service
 * (contracts/openapi/platform.yaml, BOC-05). auth-service owns users and role changes.
 */

import { call } from "./api";
import { PLATFORM_ROLES, type PlatformRole } from "./claims";

export type { ApiFailure, ApiResult } from "./api";

export interface PlatformUser {
  id: string;
  phone: string;
  createdAt: string;
  platformRoles: PlatformRole[];
}

export const ROLE_LABEL: Record<PlatformRole, string> = {
  SUPER_ADMIN: "Super Admin",
  PLATFORM_ADMIN: "Platform Admin",
  ONBOARDING_AGENT: "Onboarding Agent",
  SUPPORT_AGENT: "Support Agent",
  SUBSCRIPTION_ADMIN: "Subscription Admin",
};

export { PLATFORM_ROLES };

/** Only SUPER_ADMIN assigns or revokes platform roles; auth-service re-checks and refuses self-changes. */
export const canManageRoles = (roles: PlatformRole[]) => roles.includes("SUPER_ADMIN");

const users = "platform/backoffice/users";
const user = (id: string) => `${users}/${encodeURIComponent(id)}`;

export function listUsers(f: { query?: string; role?: string; page?: number }) {
  const q = new URLSearchParams({ page: String(f.page ?? 0) });
  if (f.query?.trim()) q.set("query", f.query.trim());
  if (f.role) q.set("role", f.role);
  return call<PlatformUser[]>(`${users}?${q}`);
}

export const assignRole = (id: string, role: string) =>
  call<PlatformUser>(`${user(id)}/platform-roles`, { method: "POST", body: { role } });

export const revokeRole = (id: string, role: string) =>
  call<PlatformUser>(`${user(id)}/platform-roles/${encodeURIComponent(role)}`, { method: "DELETE" });
