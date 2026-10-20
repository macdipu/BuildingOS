/**
 * Typed browser client for the back-office-service assisted onboarding session
 * endpoints (contracts/openapi/platform.yaml, BOC-06, D-33).
 */

import { call } from "./api";
import type { PlatformRole } from "./claims";

export type { ApiFailure, ApiResult } from "./api";

export const SESSION_STATUSES = [
  "ASSIGNED",
  "IN_PROGRESS",
  "WAITING_FOR_CUSTOMER",
  "COMPLETED",
  "CANCELLED",
  "EXPIRED",
] as const;

/** Ended sessions are read-only (§149.11); EXPIRED is set by the system only. */
export const ENDED_STATUSES = ["COMPLETED", "CANCELLED", "EXPIRED"];

/** D-10 onboarding scopes the operator may grant, in contract order. */
export const ONBOARDING_SCOPES = [
  "ONBOARDING_VIEW_BUILDING",
  "ONBOARDING_EDIT_BUILDING_INFO",
  "ONBOARDING_MANAGE_FLOORS_UNITS",
  "ONBOARDING_MANAGE_COMMITTEE_STAFF",
  "ONBOARDING_CONFIGURE_MAINTENANCE",
  "ONBOARDING_CONFIGURE_RENT_MANAGEMENT",
  "ONBOARDING_CONFIGURE_PAYMENT_METHODS",
  "ONBOARDING_SEND_INVITATIONS",
  "ONBOARDING_ACTIVATE_BUILDING",
] as const;

/** Default maximum session length (buildingos.onboarding.max-duration, P30D); the server enforces it. */
export const MAX_DURATION_DAYS = 30;

export interface OnboardingSession {
  id: string;
  buildingId: string;
  assignedAgentUserId: string;
  requestedByUserId: string | null;
  status: string;
  accessScope: string[];
  reason: string;
  startedAt: string;
  expiresAt: string;
  completedAt: string | null;
  notes: string | null;
}

export interface StartOnboardingInput {
  buildingId: string;
  assignedAgentUserId: string;
  accessScope: string[];
  reason: string;
  notes?: string;
  expiresAt: string;
}

export type SessionAction = "start-work" | "await-customer" | "complete" | "cancel";

const isOperator = (roles: PlatformRole[]) => roles.includes("SUPER_ADMIN") || roles.includes("PLATFORM_ADMIN");

/**
 * Actions the viewer may attempt on a session (D-33). The UI only hides controls;
 * the backend still authorizes each call. start-work / await-customer are for the
 * assigned agent only, operators included in the 403.
 */
export function allowedActions(session: OnboardingSession, userId: string, roles: PlatformRole[]): SessionAction[] {
  if (ENDED_STATUSES.includes(session.status)) return [];
  const assigned = session.assignedAgentUserId === userId && roles.includes("ONBOARDING_AGENT");
  const actions: SessionAction[] = [];
  if (assigned && (session.status === "ASSIGNED" || session.status === "WAITING_FOR_CUSTOMER")) actions.push("start-work");
  if (assigned && session.status === "IN_PROGRESS") actions.push("await-customer");
  if (assigned || isOperator(roles)) actions.push("complete");
  if (isOperator(roles)) actions.push("cancel");
  return actions;
}

export const canStartSession = isOperator;

const sessions = "platform/backoffice/onboarding-sessions";

export interface SessionFilters {
  status?: string;
  buildingId?: string;
  agentUserId?: string;
  page?: number;
}

export function listSessions(f: SessionFilters) {
  const q = new URLSearchParams({ page: String(f.page ?? 0) });
  if (f.status) q.set("status", f.status);
  if (f.buildingId) q.set("buildingId", f.buildingId);
  if (f.agentUserId) q.set("agentUserId", f.agentUserId);
  return call<OnboardingSession[]>(`${sessions}?${q}`);
}

export const startSession = (input: StartOnboardingInput) =>
  call<OnboardingSession>(sessions, { method: "POST", body: input });

export const transition = (id: string, action: SessionAction, reason?: string) =>
  call<OnboardingSession>(`${sessions}/${encodeURIComponent(id)}/${action}`, {
    method: "POST",
    body: reason ? { reason } : {},
  });
