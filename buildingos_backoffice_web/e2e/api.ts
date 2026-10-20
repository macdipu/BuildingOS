import { readFileSync } from "node:fs";
import { request, type APIRequestContext } from "@playwright/test";
import { STORAGE_STATE } from "../playwright.config";

/** Direct gateway URL for seeding data the console's same-origin proxy does not expose (applicant endpoints). */
const API = process.env.E2E_API_URL ?? "http://127.0.0.1:8080";

function sessionToken(): string {
  const state = JSON.parse(readFileSync(STORAGE_STATE, "utf8")) as { cookies: { name: string; value: string }[] };
  const cookie = state.cookies.find((c) => c.name === "bos_session");
  if (!cookie) throw new Error("no bos_session cookie in the saved sign-in state");
  return cookie.value;
}

export async function gateway(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: API, extraHTTPHeaders: { Authorization: `Bearer ${sessionToken()}` } });
}

async function ok<T>(res: { ok(): boolean; status(): number; text(): Promise<string>; json(): Promise<{ data: T }> }, what: string): Promise<T> {
  if (!res.ok()) throw new Error(`${what} failed: ${res.status()} ${await res.text()}`);
  return (await res.json()).data;
}

/** A unique Bangladesh mobile number (01 + 9 digits) so each run provisions a fresh initial admin. */
export function uniquePhone(): string {
  return `017${String(Math.floor(Math.random() * 1e8)).padStart(8, "0")}`;
}

/** Records the building creation fee schedule and returns nothing; idempotent PUT. */
export async function configureCreationFee(api: APIRequestContext) {
  await ok(await api.put("/api/v1/platform/fees/BUILDING_CREATION", { data: { amount: 500, currency: "BDT", required: true } }), "configure fee");
}

/** Creates and submits an application as the signed-in user and records its creation fee payment. */
export async function seedSubmittedApplication(api: APIRequestContext, name: string, contactPhone: string): Promise<string> {
  const created = await ok<{ id: string }>(
    await api.post("/api/v1/building-applications", {
      data: {
        buildingName: name,
        buildingType: "RESIDENTIAL",
        address: `Plot ${name}`,
        area: "Mirpur",
        district: "Dhaka",
        estimatedUnits: 24,
        applicantRelationship: "OWNER",
        contactName: "E2E Applicant",
        contactPhone,
      },
    }),
    "create application",
  );
  await ok(await api.post(`/api/v1/building-applications/${created.id}/submit`), "submit application");
  await ok(
    await api.post("/api/v1/platform/fees/BUILDING_CREATION/payments", {
      data: { referenceType: "BUILDING_APPLICATION", referenceId: created.id, amount: 500, currency: "BDT", paidOn: "2026-10-20" },
    }),
    "record fee payment",
  );
  return created.id;
}

export async function userIdByPhone(api: APIRequestContext, phone: string): Promise<string> {
  const users = await ok<{ id: string; phone: string }[]>(
    await api.get(`/api/v1/platform/backoffice/users?query=${encodeURIComponent(phone)}`),
    "find user",
  );
  const found = users.find((u) => u.phone === phone);
  if (!found) throw new Error(`no user with phone ${phone}`);
  return found.id;
}
