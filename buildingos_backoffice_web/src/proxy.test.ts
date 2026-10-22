// @vitest-environment node
import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { SESSION_COOKIE } from "@/lib/gateway";
import { proxy } from "./proxy";

const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString("base64url");
const token = (payload: object) => `${b64({ alg: "RS256" })}.${b64(payload)}.sig`;
const request = (cookie?: string) =>
  new NextRequest("http://localhost/dashboard", cookie ? { headers: { cookie: `${SESSION_COOKIE}=${cookie}` } } : undefined);
const cleared = (res: Response) => /bos_session=;/.test(res.headers.get("set-cookie") ?? "");

describe("proxy", () => {
  it("redirects a signed-in user without platform roles to /denied and clears the session", () => {
    const res = proxy(request(token({ sub: "u", exp: 4000000000 })));
    expect(res.headers.get("location")).toBe("http://localhost/denied");
    expect(cleared(res)).toBe(true);
  });

  it("redirects a missing or expired session to /login and clears it", () => {
    const res = proxy(request(token({ sub: "u", platform_roles: ["SUPER_ADMIN"], exp: 100 })));
    expect(res.headers.get("location")).toBe("http://localhost/login");
    expect(cleared(res)).toBe(true);
    expect(proxy(request()).headers.get("location")).toBe("http://localhost/login");
  });

  it("lets platform staff through without touching the cookie", () => {
    const res = proxy(request(token({ sub: "u", platform_roles: ["SUPER_ADMIN"], exp: 4000000000 })));
    expect(res.headers.get("location")).toBeNull();
    expect(res.headers.get("set-cookie")).toBeNull();
  });
});
