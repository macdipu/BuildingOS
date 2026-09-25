/**
 * Typed browser client for the building-service application review endpoints
 * (contracts/openapi/platform.yaml). Calls go through the same-origin
 * /api/gateway proxy, which attaches the session token server-side.
 */

import { call, type ApiFailure } from "./api";

/** BRD §149.4 review-queue statuses, in tab order (drafts are never listed). */
export const REVIEW_STATUSES = [
  "SUBMITTED",
  "UNDER_REVIEW",
  "MORE_INFORMATION_REQUIRED",
  "REJECTED",
  "APPROVED",
] as const;
export type ReviewStatus = (typeof REVIEW_STATUSES)[number];

export const STATUS_LABEL: Record<string, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under Review",
  MORE_INFORMATION_REQUIRED: "More Information Required",
  REJECTED: "Rejected",
  APPROVED: "Approved",
};

export interface BuildingApplication {
  id: string;
  applicationNumber: string;
  applicantUserId: string;
  status: string;
  source: string;
  buildingName: string | null;
  buildingType: string | null;
  address: string | null;
  area: string | null;
  district: string | null;
  postalCode: string | null;
  totalFloors: number | null;
  estimatedUnits: number | null;
  applicantRelationship: string | null;
  relationshipNote: string | null;
  contactName: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  managementType: string | null;
  latitude: number | null;
  longitude: number | null;
  missingFields: string[] | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
  rejectionReason: string | null;
  infoRequestMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Transition {
  fromStatus: string | null;
  toStatus: string;
  reason: string | null;
  occurredAt: string;
}

export interface ApplicationDocument {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface InternalNote {
  id: string;
  authorUserId: string;
  body: string;
  createdAt: string;
}

export interface DuplicateMatch {
  kind: string;
  id: string;
  reference: string;
  status: string;
  name: string | null;
  address: string | null;
  area: string | null;
  district: string | null;
  matchedOn: string[];
}

export interface Approval {
  application: BuildingApplication;
  buildingId: string;
  buildingStatus: string;
}

export interface PageMeta {
  page: number;
  size: number;
  total: number;
}

export type { ApiFailure, ApiResult } from "./api";

const platform = (id: string) => `platform/building-applications/${encodeURIComponent(id)}`;
const applicant = (id: string) => `building-applications/${encodeURIComponent(id)}`;

export function listApplications(status: ReviewStatus, page = 0) {
  const q = new URLSearchParams({ status, page: String(page) });
  return call<BuildingApplication[]>(`platform/building-applications?${q}`);
}

export const getApplication = (id: string) => call<BuildingApplication>(applicant(id));
export const getHistory = (id: string) => call<Transition[]>(`${applicant(id)}/history`);
export const getDocuments = (id: string) => call<ApplicationDocument[]>(`${applicant(id)}/documents`);
export const documentHref = (id: string, documentId: string) =>
  `/api/gateway/${applicant(id)}/documents/${encodeURIComponent(documentId)}`;

export const getNotes = (id: string) => call<InternalNote[]>(`${platform(id)}/notes`);
export const addNote = (id: string, body: string) =>
  call<InternalNote>(`${platform(id)}/notes`, { method: "POST", body: { body } });
export const getDuplicates = (id: string) => call<DuplicateMatch[]>(`${platform(id)}/duplicates`);

export const startReview = (id: string) =>
  call<BuildingApplication>(`${platform(id)}/start-review`, { method: "POST" });
export const requestInformation = (id: string, message: string) =>
  call<BuildingApplication>(`${platform(id)}/request-information`, { method: "POST", body: { message } });
export const rejectApplication = (id: string, reason: string) =>
  call<BuildingApplication>(`${platform(id)}/reject`, { method: "POST", body: { reason } });
export const approveApplication = (id: string, adminPhone: string, reason: string) =>
  call<Approval>(`${platform(id)}/approve`, { method: "POST", body: { adminPhone, reason } });

/** Approval preconditions the backend rejects with 409 that are not stale-state conflicts. */
const FEE_CODES: Record<string, string> = {
  CREATION_FEE_UNPAID:
    "Approval blocked: the building creation fee has not been recorded for this application. Record the fee, then approve again.",
  FEE_NOT_CONFIGURED: "Approval blocked: no building creation fee is configured on the platform.",
};

export function feeMessage(failure: ApiFailure): string | null {
  return FEE_CODES[failure.code] ?? null;
}

/** 409s other than fee preconditions mean the application changed since it was loaded. */
export function isStaleConflict(failure: ApiFailure): boolean {
  return failure.status === 409 && !(failure.code in FEE_CODES);
}

export function pageMeta(meta: Record<string, unknown> | undefined): PageMeta | null {
  if (!meta) return null;
  const { page, size, total } = meta;
  return typeof page === "number" && typeof size === "number" && typeof total === "number"
    ? { page, size, total }
    : null;
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}

export function label(value: string | null | undefined): string {
  if (!value) return "—";
  return STATUS_LABEL[value] ?? value.replaceAll("_", " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}
