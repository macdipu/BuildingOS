/**
 * Typed browser client for the back-office-service support session and elevated
 * approval endpoints (contracts/openapi/platform.yaml, BOC-07, D-36).
 */

import { call } from "./api";
import type { PlatformRole } from "./claims";

export type { ApiFailure, ApiResult } from "./api";

/** D-10 support scopes in contract order; high-risk ones need a four-eyes approval (D-36). */
export const SUPPORT_SCOPES = [
  "SUPPORT_VIEW_MEMBERS",
  "SUPPORT_VIEW_UNITS",
  "SUPPORT_VIEW_OWNERSHIP_HISTORY",
  "SUPPORT_EDIT_UNIT",
  "SUPPORT_MANAGE_MEMBERSHIP_INVITE",
  "SUPPORT_REVOKE_MEMBERSHIP",
  "SUPPORT_VIEW_SUBSCRIPTION",
  "SUPPORT_VIEW_PAYMENTS",
  "SUPPORT_REVERSE_PAYMENT",
  "SUPPORT_TRANSFER_OWNERSHIP",
  "SUPPORT_REMOVE_BUILDING_ADMIN",
  "SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED",
] as const;

export const HIGH_RISK_SCOPES: readonly string[] = [
  "SUPPORT_REVERSE_PAYMENT",
  "SUPPORT_TRANSFER_OWNERSHIP",
  "SUPPORT_REMOVE_BUILDING_ADMIN",
  "SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED",
];
export const isHighRisk = (scope: string) => HIGH_RISK_SCOPES.includes(scope);

/** Default maximum session length (buildingos.support.max-duration, PT8H); the server enforces it. */
export const MAX_DURATION_HOURS = 8;

export interface SupportSession {
  id: string;
  platformUserId: string;
  targetUserId: string | null;
  buildingId: string | null;
  status: string;
  reason: string;
  permissionScope: string[];
  startedAt: string;
  expiresAt: string;
  endedAt: string | null;
}

export interface ElevatedApproval {
  id: string;
  supportSessionId: string;
  requestedScope: string;
  status: "PENDING" | "APPROVED" | "DENIED";
  decidedBy: string | null;
  requestedAt: string;
  decidedAt: string | null;
  decisionReason: string | null;
}

export interface SupportSessionDetail extends SupportSession {
  elevatedApprovals: ElevatedApproval[];
}

export interface StartSupportInput {
  targetUserId?: string;
  buildingId?: string;
  permissionScope: string[];
  reason: string;
  expiresAt: string;
}

export type ScopeState = "GRANTED" | "PENDING" | "APPROVED" | "DENIED" | "NOT_REQUESTED";

/** What a scope means on a session now: ordinary scopes are granted, high-risk ones follow their latest request. */
export function scopeState(scope: string, approvals: ElevatedApproval[]): ScopeState {
  if (!isHighRisk(scope)) return "GRANTED";
  const latest = approvals
    .filter((a) => a.requestedScope === scope)
    .sort((a, b) => b.requestedAt.localeCompare(a.requestedAt))[0];
  return latest ? latest.status : "NOT_REQUESTED";
}

export const canStartSupportSession = (roles: PlatformRole[]) =>
  roles.some((r) => r === "SUPER_ADMIN" || r === "PLATFORM_ADMIN" || r === "SUPPORT_AGENT");

/** SUPER_ADMIN other than the session owner may decide a request (four-eyes); the backend still enforces it. */
export const canDecide = (roles: PlatformRole[], userId: string, session: SupportSession) =>
  roles.includes("SUPER_ADMIN") && session.platformUserId !== userId;

const base = "platform/backoffice";
const session = (id: string) => `${base}/support-sessions/${encodeURIComponent(id)}`;

export function listSupportSessions(f: { active?: boolean; page?: number }) {
  const q = new URLSearchParams({ page: String(f.page ?? 0) });
  if (f.active !== undefined) q.set("active", String(f.active));
  return call<SupportSession[]>(`${base}/support-sessions?${q}`);
}

export const getSupportSession = (id: string) => call<SupportSessionDetail>(session(id));
export const startSupportSession = (input: StartSupportInput) =>
  call<SupportSession>(`${base}/support-sessions`, { method: "POST", body: input });
export const endSupportSession = (id: string) => call<SupportSession>(`${session(id)}/end`, { method: "POST", body: {} });
export const requestElevated = (id: string, scope: string, reason: string) =>
  call<ElevatedApproval>(`${session(id)}/elevated-approvals`, { method: "POST", body: { scope, reason } });

export const listPendingApprovals = (page = 0) =>
  call<ElevatedApproval[]>(`${base}/elevated-approvals?status=PENDING&page=${page}`);
export const decideApproval = (approvalId: string, decision: "approve" | "deny", reason?: string) =>
  call<ElevatedApproval>(`${base}/elevated-approvals/${encodeURIComponent(approvalId)}/${decision}`, {
    method: "POST",
    body: reason ? { reason } : {},
  });
