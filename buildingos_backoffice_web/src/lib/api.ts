/**
 * Shared typed call helper for the same-origin /api/gateway proxy, which
 * attaches the session token server-side. `path` is the backend path after
 * `/api/v1/`. Failures always carry the backend error code.
 */

export type ApiFailure = { ok: false; status: number; code: string; message: string | null };
export type ApiResult<T> = { ok: true; data: T; meta?: Record<string, unknown> } | ApiFailure;

export async function call<T>(
  path: string,
  init?: { method: "POST" | "PUT" | "DELETE"; body?: unknown },
): Promise<ApiResult<T>> {
  let res: Response;
  try {
    res = await fetch(`/api/gateway/${path}`, {
      method: init?.method ?? "GET",
      headers: init?.body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: init?.body !== undefined ? JSON.stringify(init.body) : undefined,
      cache: "no-store",
    });
  } catch {
    return { ok: false, status: 0, code: "NETWORK_ERROR", message: null };
  }
  const json = (await res.json().catch(() => ({}))) as {
    success?: boolean;
    data?: T;
    meta?: Record<string, unknown>;
    code?: string;
    message?: string | null;
  };
  if (res.status === 401 && typeof window !== "undefined") {
    // Session expired mid-use: back to sign-in (F6-T6 AC, review R-6). A full load is intended here so
    // no client state from the expired session survives, and this helper has no router.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.assign("/login");
  }
  if (!res.ok || json.success === false) {
    return { ok: false, status: res.status, code: json.code ?? `HTTP_${res.status}`, message: json.message ?? null };
  }
  return { ok: true, data: json.data as T, meta: json.meta };
}
