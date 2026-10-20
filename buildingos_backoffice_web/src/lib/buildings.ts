/**
 * Typed browser client for the building-service platform building endpoints
 * (contracts/openapi/platform.yaml): the status list (D-34) and lifecycle actions.
 */

import { call } from "./api";

export type { ApiFailure, ApiResult } from "./api";

export type BuildingStatus = "ONBOARDING" | "ACTIVE" | "SUSPENDED";

export interface BuildingSummary {
  id: string;
  applicationId: string;
  name: string;
  buildingType: string;
  area: string;
  district: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface BuildingMember {
  userId: string;
  role: string;
  status: string;
}

export interface Building extends BuildingSummary {
  address: string;
  postalCode: string | null;
  latitude: number | null;
  longitude: number | null;
  contactPhone: string;
  members: BuildingMember[];
}

export type LifecycleAction = "activate" | "suspend" | "reactivate";

/** The one lifecycle action each status offers (AP-09); the backend still authorizes it. */
export const ACTION_FOR_STATUS: Record<BuildingStatus, LifecycleAction> = {
  ONBOARDING: "activate",
  ACTIVE: "suspend",
  SUSPENDED: "reactivate",
};

const building = (id: string) => `platform/buildings/${encodeURIComponent(id)}`;

export function listBuildings(status: BuildingStatus, q: string, page = 0) {
  const params = new URLSearchParams({ status, page: String(page) });
  if (q.trim()) params.set("q", q.trim());
  return call<BuildingSummary[]>(`platform/buildings?${params}`);
}

export const getBuilding = (id: string) => call<Building>(building(id));

export const lifecycle = (id: string, action: LifecycleAction, reason: string) =>
  call<Building>(`${building(id)}/${action}`, { method: "POST", body: { reason } });
