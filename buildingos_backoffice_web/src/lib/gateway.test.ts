import { describe, expect, it } from "vitest";
import { gatewayUrl, isAllowedPath, isAllowedUpstream, safeJoin, upstreamHeaders } from "./gateway";

describe("gateway proxy rules", () => {
  it("allows only back-office API groups", () => {
    expect(isAllowedPath("platform/building-applications")).toBe(true);
    expect(isAllowedPath("building-applications/123/history")).toBe(true);
    expect(isAllowedPath("me/buildings")).toBe(false);
    expect(isAllowedPath("auth/otp/start")).toBe(false);
    expect(isAllowedPath("platform/../me/buildings")).toBe(false);
    expect(isAllowedPath("/platform/x")).toBe(false);
  });

  it("builds /api/v1 URLs with or without a trailing slash on the base", () => {
    expect(gatewayUrl("http://gw:8080", "platform/x", "?a=1")).toBe("http://gw:8080/api/v1/platform/x?a=1");
    expect(gatewayUrl("http://gw:8080/", "platform/x")).toBe("http://gw:8080/api/v1/platform/x");
  });

  it("sends the bearer token and never forwards browser cookies", () => {
    const incoming = new Headers({ cookie: "bos_session=secret", "content-type": "application/json" });
    const out = upstreamHeaders("tkn", incoming);
    expect(out.get("authorization")).toBe("Bearer tkn");
    expect(out.get("cookie")).toBeNull();
    expect(out.get("content-type")).toBe("application/json");
  });

  it("safeJoin accepts the path shapes the app sends", () => {
    expect(safeJoin(["platform", "backoffice", "onboarding-sessions"])).toBe("platform/backoffice/onboarding-sessions");
    expect(safeJoin(["platform", "building-applications", "11111111-2222-3333-4444-555555555555", "approve"])).toBe(
      "platform/building-applications/11111111-2222-3333-4444-555555555555/approve",
    );
    expect(safeJoin(["building-applications", "a1", "history"])).toBe("building-applications/a1/history");
    // Decoded characters are re-encoded so they stay inside their segment.
    expect(safeJoin(["platform", "users", "u 1", "platform-roles", "SUPER_ADMIN"])).toBe(
      "platform/users/u%201/platform-roles/SUPER_ADMIN",
    );
  });

  it("safeJoin rejects traversal, encoded traversal and disallowed groups", () => {
    // /api/gateway/platform/%252e%252e/me arrives decoded once as %2e%2e.
    expect(safeJoin(["platform", "%2e%2e", "me"])).toBeNull();
    expect(safeJoin(["platform", "..", "me"])).toBeNull();
    expect(safeJoin(["platform", ".", "x"])).toBeNull();
    expect(safeJoin(["platform", "", "x"])).toBeNull();
    expect(safeJoin(["platform", "a%2fb"])).toBeNull();
    expect(safeJoin(["platform", "a/b"])).toBeNull();
    expect(safeJoin(["platform", "a\\b"])).toBeNull();
    expect(safeJoin(["platform", "x?y"])).toBeNull();
    expect(safeJoin(["platform", "x#y"])).toBeNull();
    expect(safeJoin(["me", "buildings"])).toBeNull();
    expect(safeJoin([])).toBeNull();
  });

  it("isAllowedUpstream checks the normalized upstream pathname", () => {
    expect(isAllowedUpstream("http://gw:8080", "http://gw:8080/api/v1/platform/x?a=1")).toBe(true);
    expect(isAllowedUpstream("http://gw:8080/", "http://gw:8080/api/v1/building-applications/1")).toBe(true);
    expect(isAllowedUpstream("http://gw:8080/base", "http://gw:8080/base/api/v1/platform/x")).toBe(true);
    expect(isAllowedUpstream("http://gw:8080", "http://gw:8080/api/v1/platform/../me")).toBe(false);
    expect(isAllowedUpstream("http://gw:8080", "http://gw:8080/api/v1/platform/%2e%2e/me")).toBe(false);
    expect(isAllowedUpstream("http://gw:8080", "http://gw:8080/api/v1/me")).toBe(false);
    expect(isAllowedUpstream("http://gw:8080/base", "http://gw:8080/api/v1/platform/x")).toBe(false);
  });
});
