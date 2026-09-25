import "server-only";
import { cookies } from "next/headers";
import { decodeClaims, isExpired, type SessionClaims } from "./claims";
import { SESSION_COOKIE } from "./gateway";

export function apiBaseUrl(): string {
  const url = process.env.BACKOFFICE_API_BASE_URL;
  if (!url) throw new Error("BACKOFFICE_API_BASE_URL is not set");
  return url;
}

export async function sessionToken(): Promise<string | null> {
  return (await cookies()).get(SESSION_COOKIE)?.value ?? null;
}

/** Current, unexpired session claims, or null. */
export async function currentSession(): Promise<SessionClaims | null> {
  const claims = decodeClaims(await sessionToken());
  return claims && !isExpired(claims) ? claims : null;
}

export function sessionCookie(token: string, maxAgeSeconds: number) {
  return {
    name: SESSION_COOKIE,
    value: token,
    httpOnly: true,
    sameSite: "strict" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: Math.max(0, Math.floor(maxAgeSeconds)),
  };
}
