/**
 * Typed browser client for the subscription-service platform endpoints
 * (contracts/openapi/platform.yaml). The contract types bodies as `{}`, so field
 * names follow the subscription-service request/response records.
 */

import { call } from "./api";

export type { ApiFailure, ApiResult } from "./api";

/** BRD §149.16 entitlement catalog (subscription-service `Feature`), in catalog order. */
export const ENTITLEMENTS = [
  { key: "maintenance.enabled", label: "Maintenance", kind: "FLAG" },
  { key: "rent_management.enabled", label: "Rent management", kind: "FLAG" },
  { key: "work_orders.enabled", label: "Work orders", kind: "FLAG" },
  { key: "reports.pdf_export", label: "Reports: PDF export", kind: "FLAG" },
  { key: "reports.excel_export", label: "Reports: Excel export", kind: "FLAG" },
  { key: "max_units", label: "Max units", kind: "LIMIT" },
  { key: "max_users", label: "Max users", kind: "LIMIT" },
  { key: "storage_limit_mb", label: "Storage limit (MB)", kind: "LIMIT" },
  { key: "support_tier", label: "Support tier", kind: "TEXT" },
] as const;
export type EntitlementKind = (typeof ENTITLEMENTS)[number]["kind"];
export const TEXT_MAX = 64;

/** Wire form: `{"max_units": 20, "maintenance.enabled": true}`; absent limit = unlimited. */
export type EntitlementMap = Record<string, unknown>;

/** Subscription-service `BillingCycle` labels (D-17; no billing is collected). */
export const BILLING_CYCLES = ["MONTHLY", "QUARTERLY", "YEARLY"] as const;
export type BillingCycle = (typeof BILLING_CYCLES)[number];

/** `PlanCode` format; immutable after create. */
export const PLAN_CODE_PATTERN = /^[A-Z][A-Z0-9_]{1,63}$/;
export const PLAN_NAME_MAX = 200;

export interface Plan {
  id: string;
  code: string;
  name: string;
  status: string;
  billingCycles: string[];
  selfService: boolean;
  entitlements: EntitlementMap;
  createdAt: string;
  updatedAt: string;
}

export interface PlanInput {
  code?: string;
  name: string;
  billingCycles: string[];
  selfService: boolean;
  entitlements: EntitlementMap;
}

export interface UserSubscription {
  id: string;
  userId: string;
  planId: string;
  status: string;
  billingCycle: string;
  grantedBy: string;
  startedAt: string;
  effectiveEntitlements: EntitlementMap;
}

/** Subscription-service `FeeCode` / `ReferenceType` / `FeeStatus` values (D-24). */
export const BUILDING_CREATION_FEE = "BUILDING_CREATION";
export const FEE_REFERENCE_TYPE = "BUILDING_APPLICATION";

export interface FeeSchedule {
  code: string;
  amount: number;
  currency: string;
  required: boolean;
  updatedAt: string;
}

export interface FeeScheduleInput {
  amount: number;
  currency: string;
  required: boolean;
}

export interface FeePayment {
  id: string;
  feeCode: string;
  referenceType: string;
  referenceId: string;
  amount: number;
  currency: string;
  method: string;
  externalReference: string | null;
  paidOn: string;
  recordedBy: string;
  recordedAt: string;
}

export interface PaymentInput {
  referenceType: string;
  referenceId: string;
  amount: number;
  currency: string;
  externalReference?: string;
  paidOn: string;
}

export interface FeeStatus {
  status: string;
  schedule: FeeSchedule;
  payments: FeePayment[];
}

const plans = "platform/subscription-plans";
const plan = (id: string) => `${plans}/${encodeURIComponent(id)}`;
const fee = (code: string) => `platform/fees/${encodeURIComponent(code)}`;

export const listPlans = () => call<Plan[]>(plans);
export const createPlan = (input: PlanInput) => call<Plan>(plans, { method: "POST", body: input });
export const updatePlan = (id: string, input: PlanInput) => call<Plan>(plan(id), { method: "PUT", body: input });
export const retirePlan = (id: string) => call<Plan>(`${plan(id)}/retire`, { method: "POST" });

export const getFreeTier = () => call<EntitlementMap>("platform/free-tier");
export const updateFreeTier = (entitlements: EntitlementMap) =>
  call<EntitlementMap>("platform/free-tier", { method: "PUT", body: entitlements });

const userSubscription = (userId: string) => `platform/users/${encodeURIComponent(userId)}/subscription`;
export const getUserSubscription = (userId: string) => call<UserSubscription>(userSubscription(userId));
export const assignPlan = (userId: string, planId: string, billingCycle: string) =>
  call<UserSubscription>(userSubscription(userId), { method: "POST", body: { planId, billingCycle } });

export const getFeeSchedule = (code: string) => call<FeeSchedule>(fee(code));
export const updateFeeSchedule = (code: string, input: FeeScheduleInput) =>
  call<FeeSchedule>(fee(code), { method: "PUT", body: input });
export const recordFeePayment = (code: string, input: PaymentInput) =>
  call<FeePayment>(`${fee(code)}/payments`, { method: "POST", body: input });
export function getFeeStatus(code: string, referenceType: string, referenceId: string) {
  const q = new URLSearchParams({ referenceType, referenceId });
  return call<FeeStatus>(`${fee(code)}/status?${q}`);
}

/** Display form of an entitlement value; absent limits are unlimited. */
export function entitlementValue(kind: EntitlementKind | undefined, value: unknown): string {
  if (value === undefined || value === null) return kind === "LIMIT" ? "Unlimited" : "—";
  if (typeof value === "boolean") return value ? "Enabled" : "Disabled";
  return String(value);
}

export const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
