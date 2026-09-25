import { NextResponse } from "next/server";
import { gatewayUrl, isAllowedPath, upstreamHeaders } from "@/lib/gateway";
import { apiBaseUrl, currentSession, sessionToken } from "@/lib/session";

type Ctx = { params: Promise<{ path: string[] }> };

/** Same-origin proxy: attaches the session token server-side and forwards to the API Gateway. */
async function forward(request: Request, ctx: Ctx) {
  const { path } = await ctx.params;
  const joined = path.join("/");
  if (!isAllowedPath(joined)) {
    return NextResponse.json({ success: false, code: "NOT_FOUND" }, { status: 404 });
  }
  const token = await sessionToken();
  if (!token || !(await currentSession())) {
    return NextResponse.json({ success: false, code: "UNAUTHENTICATED" }, { status: 401 });
  }
  const method = request.method;
  const upstream = await fetch(gatewayUrl(apiBaseUrl(), joined, new URL(request.url).search), {
    method,
    headers: upstreamHeaders(token, request.headers),
    body: method === "GET" || method === "HEAD" ? undefined : await request.text(),
    cache: "no-store",
  });
  return new Response(upstream.body, {
    status: upstream.status,
    headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" },
  });
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const DELETE = forward;
