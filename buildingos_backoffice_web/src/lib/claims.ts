export const PLATFORM_ROLES = [
  "SUPER_ADMIN",
  "PLATFORM_ADMIN",
  "ONBOARDING_AGENT",
  "SUPPORT_AGENT",
  "SUBSCRIPTION_ADMIN",
] as const;

export type PlatformRole = (typeof PLATFORM_ROLES)[number];

export interface SessionClaims {
  userId: string;
  phone: string;
  roles: PlatformRole[];
  expiresAt: number;
}

function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  return new TextDecoder().decode(Uint8Array.from(atob(padded), (c) => c.charCodeAt(0)));
}

/**
 * Reads the auth-service JWT payload for UI decisions only (route guard, nav).
 * The signature is NOT verified here: every API call is still verified by the
 * backend, which remains the authorization boundary (TECH-SPEC, BOC-01).
 */
export function decodeClaims(token: string | undefined | null): SessionClaims | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(base64UrlDecode(parts[1])) as Record<string, unknown>;
    if (typeof payload.sub !== "string" || typeof payload.exp !== "number") return null;
    const raw = Array.isArray(payload.platform_roles) ? payload.platform_roles : [];
    const roles = raw.filter((r): r is PlatformRole =>
      typeof r === "string" && (PLATFORM_ROLES as readonly string[]).includes(r),
    );
    return {
      userId: payload.sub,
      phone: typeof payload.phone === "string" ? payload.phone : "",
      roles,
      expiresAt: payload.exp,
    };
  } catch {
    return null;
  }
}

export function isExpired(claims: SessionClaims, nowSeconds = Date.now() / 1000): boolean {
  return claims.expiresAt <= nowSeconds;
}
