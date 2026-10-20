import { describe, expect, it } from "vitest";
import { canDecide, scopeState, type ElevatedApproval, type SupportSession } from "./supportSessions";

const approval = (over: Partial<ElevatedApproval>): ElevatedApproval => ({
  id: "a",
  supportSessionId: "s",
  requestedScope: "SUPPORT_REVERSE_PAYMENT",
  status: "PENDING",
  decidedBy: null,
  requestedAt: "2026-10-01T10:00:00Z",
  decidedAt: null,
  decisionReason: null,
  ...over,
});

const session = { platformUserId: "owner" } as SupportSession;

describe("scopeState", () => {
  it("grants ordinary scopes and never shows a high-risk scope as granted before approval", () => {
    expect(scopeState("SUPPORT_VIEW_MEMBERS", [])).toBe("GRANTED");
    expect(scopeState("SUPPORT_REVERSE_PAYMENT", [])).toBe("NOT_REQUESTED");
    expect(scopeState("SUPPORT_REVERSE_PAYMENT", [approval({})])).toBe("PENDING");
    expect(scopeState("SUPPORT_REVERSE_PAYMENT", [approval({ status: "APPROVED" })])).toBe("APPROVED");
  });

  it("follows the latest request, so a denied scope re-requested is pending again", () => {
    const denied = approval({ id: "1", status: "DENIED", requestedAt: "2026-10-01T10:00:00Z" });
    const again = approval({ id: "2", status: "PENDING", requestedAt: "2026-10-01T11:00:00Z" });
    expect(scopeState("SUPPORT_REVERSE_PAYMENT", [denied, again])).toBe("PENDING");
    expect(scopeState("SUPPORT_REVERSE_PAYMENT", [denied])).toBe("DENIED");
  });
});

describe("canDecide (four-eyes)", () => {
  it("allows only a SUPER_ADMIN who is not the owner", () => {
    expect(canDecide(["SUPER_ADMIN"], "other", session)).toBe(true);
    expect(canDecide(["SUPER_ADMIN"], "owner", session)).toBe(false);
    expect(canDecide(["PLATFORM_ADMIN"], "other", session)).toBe(false);
    expect(canDecide(["SUPPORT_AGENT"], "other", session)).toBe(false);
  });
});
