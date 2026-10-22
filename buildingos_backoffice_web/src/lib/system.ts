/**
 * Typed browser client for the back-office-service system endpoints
 * (contracts/openapi/platform.yaml, BOC-08): readiness aggregation and the merged audit log.
 */

import { call } from "./api";

export type { ApiFailure, ApiResult } from "./api";

export interface ServiceReadiness {
  name: string;
  status: "UP" | "DOWN" | "UNKNOWN";
  httpStatus?: number;
  latencyMs: number;
}

export interface SystemHealth {
  checkedAt: string;
  overall: "UP" | "DEGRADED" | "DOWN";
  services: ServiceReadiness[];
}

export const getSystemHealth = () => call<SystemHealth>("platform/backoffice/system-health");

export const AUDIT_SOURCES = ["auth-service", "building-service", "subscription-service", "back-office-service"] as const;
export const AUDIT_LIMITS = [25, 50, 100, 200] as const;

export interface AuditEvent {
  id: string;
  source: string;
  occurredAt: string;
  actorUserId: string | null;
  action: string;
  entityType: string;
  entityId: string;
  buildingId: string | null;
  reason: string | null;
  role?: string;
}

export interface AuditLogPage {
  items: AuditEvent[];
  /** Keyset cursor (last item's occurredAt and id) when the page is full, else both null. */
  nextUntil: string | null;
  nextUntilId: string | null;
  unavailableSources: string[];
}

export interface AuditFilters {
  since?: string;
  until?: string;
  /** With until, the keyset tie-breaker: rows strictly before (until, untilId). */
  untilId?: string;
  source?: string;
  entityType?: string;
  actorUserId?: string;
  limit?: number;
}

export function listAudit(f: AuditFilters) {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(f)) if (v !== undefined && v !== "") q.set(k, String(v));
  return call<AuditLogPage>(`platform/backoffice/audit${q.size ? `?${q}` : ""}`);
}

/** A datetime-local value as the ISO instant the API expects, or undefined when empty/invalid. */
export function toInstant(local: string): string | undefined {
  if (!local) return undefined;
  const d = new Date(local);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}
