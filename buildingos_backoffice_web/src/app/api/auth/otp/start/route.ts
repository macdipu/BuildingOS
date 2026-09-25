import { apiBaseUrl } from "@/lib/session";
import { gatewayUrl } from "@/lib/gateway";

/** Relays OTP start to auth-service (no session needed yet). */
export async function POST(request: Request) {
  const body = await request.text();
  const upstream = await fetch(gatewayUrl(apiBaseUrl(), "auth/otp/start"), {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body,
    cache: "no-store",
  });
  return new Response(await upstream.text(), {
    status: upstream.status,
    headers: { "Content-Type": "application/json" },
  });
}
