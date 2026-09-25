import { NextResponse, type NextRequest } from "next/server";
import { decodeClaims, isExpired } from "@/lib/claims";
import { SESSION_COOKIE } from "@/lib/gateway";

/** Route guard: signed-in platform staff only. Backend still authorizes every call. */
export function proxy(request: NextRequest) {
  const claims = decodeClaims(request.cookies.get(SESSION_COOKIE)?.value);
  if (!claims || isExpired(claims)) {
    const login = new URL("/login", request.url);
    const response = NextResponse.redirect(login);
    response.cookies.delete(SESSION_COOKIE);
    return response;
  }
  if (claims.roles.length === 0) {
    return NextResponse.redirect(new URL("/denied", request.url));
  }
  return NextResponse.next();
}

export const config = {
  // API routes answer 401 JSON themselves instead of an HTML redirect.
  matcher: ["/((?!login|denied|api/|_next/static|_next/image|favicon.ico).*)"],
};
