import { NextResponse } from "next/server";
import { decodeClaims } from "@/lib/claims";
import { gatewayUrl } from "@/lib/gateway";
import { apiBaseUrl, sessionCookie } from "@/lib/session";

interface VerifyEnvelope {
  success?: boolean;
  data?: { accessToken?: string; expiresInSeconds?: number };
  code?: string;
  message?: string;
}

/**
 * Verifies the OTP with auth-service and keeps the access token in an httpOnly
 * cookie; the browser only learns whether sign-in worked and which roles it has.
 */
export async function POST(request: Request) {
  const upstream = await fetch(gatewayUrl(apiBaseUrl(), "auth/otp/verify"), {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: await request.text(),
    cache: "no-store",
  });
  const payload = (await upstream.json().catch(() => ({}))) as VerifyEnvelope;
  const token = payload.data?.accessToken;
  if (!upstream.ok || !token) {
    return NextResponse.json(
      { success: false, code: payload.code ?? "OTP_REJECTED", message: payload.message ?? null },
      { status: upstream.ok ? 502 : upstream.status },
    );
  }
  const claims = decodeClaims(token);
  if (!claims || claims.roles.length === 0) {
    return NextResponse.json(
      { success: false, code: "NO_PLATFORM_ROLE", message: "This account has no back-office access." },
      { status: 403 },
    );
  }
  const response = NextResponse.json({ success: true, data: { roles: claims.roles } });
  response.cookies.set(sessionCookie(token, payload.data?.expiresInSeconds ?? 0));
  return response;
}
