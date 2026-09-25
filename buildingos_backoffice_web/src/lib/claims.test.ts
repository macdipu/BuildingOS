import { describe, expect, it } from "vitest";
import { decodeClaims, isExpired } from "./claims";

const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString("base64url");
const token = (payload: object) => `${b64({ alg: "RS256" })}.${b64(payload)}.sig`;

describe("decodeClaims", () => {
  it("reads subject, phone, known platform roles and expiry", () => {
    const c = decodeClaims(
      token({ sub: "u1", phone: "01306999005", platform_roles: ["SUPER_ADMIN", "HACKER"], exp: 2000000000 }),
    );
    expect(c).toEqual({ userId: "u1", phone: "01306999005", roles: ["SUPER_ADMIN"], expiresAt: 2000000000 });
  });

  it("rejects malformed tokens and missing required claims", () => {
    expect(decodeClaims(undefined)).toBeNull();
    expect(decodeClaims("a.b")).toBeNull();
    expect(decodeClaims("a.!!!.c")).toBeNull();
    expect(decodeClaims(token({ phone: "x", exp: 1 }))).toBeNull();
  });

  it("treats a token without platform_roles as having no roles", () => {
    expect(decodeClaims(token({ sub: "u", exp: 2000000000 }))?.roles).toEqual([]);
  });

  it("isExpired compares exp to now", () => {
    const c = decodeClaims(token({ sub: "u", exp: 100 }));
    expect(c && isExpired(c, 99)).toBe(false);
    expect(c && isExpired(c, 100)).toBe(true);
  });
});
