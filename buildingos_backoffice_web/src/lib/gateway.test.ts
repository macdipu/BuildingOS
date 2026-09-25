import { describe, expect, it } from "vitest";
import { gatewayUrl, isAllowedPath, upstreamHeaders } from "./gateway";

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
});
