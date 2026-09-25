/**
 * Server-side forwarding rules for /api/gateway/*. Only back-office API groups
 * are reachable, so the browser cannot use the session cookie to call arbitrary
 * gateway routes (e.g. customer /me endpoints).
 */
export const ALLOWED_PREFIXES = ["platform/", "building-applications/"] as const;

export const SESSION_COOKIE = "bos_session";

export function isAllowedPath(path: string): boolean {
  if (path.includes("..") || path.includes("//") || path.startsWith("/")) return false;
  return ALLOWED_PREFIXES.some((p) => path.startsWith(p));
}

export function gatewayUrl(baseUrl: string, path: string, search = ""): string {
  const base = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;
  return `${base}api/v1/${path}${search}`;
}

/** Headers sent upstream: never forward the browser's cookies or host. */
export function upstreamHeaders(token: string, incoming: Headers): Headers {
  const out = new Headers();
  out.set("Authorization", `Bearer ${token}`);
  const contentType = incoming.get("content-type");
  if (contentType) out.set("Content-Type", contentType);
  out.set("Accept", "application/json");
  const correlation = incoming.get("x-correlation-id");
  if (correlation) out.set("X-Correlation-Id", correlation);
  return out;
}
