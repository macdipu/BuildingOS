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

/**
 * Decoded route segments -> upstream path, or null when unsafe. Segments arrive decoded once, so a
 * double-encoded `%252e%252e` shows up as `%2e%2e`: reject dot segments and any `%`, `?`, `#`, then
 * re-encode each segment so the upstream cannot normalize it into another route (review R-2).
 */
export function safeJoin(segments: string[]): string | null {
  if (segments.length === 0) return null;
  for (const s of segments) {
    if (s === "" || s === "." || s === ".." || /[%?#\\/]/.test(s)) return null;
  }
  const joined = segments.map(encodeURIComponent).join("/");
  return isAllowedPath(joined) ? joined : null;
}

export function gatewayUrl(baseUrl: string, path: string, search = ""): string {
  const base = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;
  return `${base}api/v1/${path}${search}`;
}

/** The URL actually sent upstream must still sit under an allowed `api/v1/` prefix after URL normalization. */
export function isAllowedUpstream(baseUrl: string, url: string): boolean {
  const base = new URL(gatewayUrl(baseUrl, "")).pathname;
  const pathname = new URL(url).pathname;
  return ALLOWED_PREFIXES.some((p) => pathname.startsWith(base + p));
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
