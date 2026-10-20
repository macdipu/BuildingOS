import { describe, expect, it } from "vitest";
import { allowedActions, type OnboardingSession } from "./onboardingSessions";

const base: OnboardingSession = {
  id: "s1",
  buildingId: "b1",
  assignedAgentUserId: "agent",
  requestedByUserId: null,
  status: "ASSIGNED",
  accessScope: ["ONBOARDING_VIEW_BUILDING"],
  reason: "r",
  startedAt: "",
  expiresAt: "",
  completedAt: null,
  notes: null,
};

describe("allowedActions", () => {
  it("gives the assigned agent work transitions and complete, never cancel", () => {
    expect(allowedActions(base, "agent", ["ONBOARDING_AGENT"])).toEqual(["start-work", "complete"]);
    expect(allowedActions({ ...base, status: "IN_PROGRESS" }, "agent", ["ONBOARDING_AGENT"])).toEqual([
      "await-customer",
      "complete",
    ]);
    expect(allowedActions({ ...base, status: "WAITING_FOR_CUSTOMER" }, "agent", ["ONBOARDING_AGENT"])).toEqual([
      "start-work",
      "complete",
    ]);
  });

  it("gives operators complete and cancel only (not agent transitions)", () => {
    expect(allowedActions(base, "op", ["PLATFORM_ADMIN"])).toEqual(["complete", "cancel"]);
    expect(allowedActions(base, "op", ["SUPER_ADMIN"])).toEqual(["complete", "cancel"]);
  });

  it("gives another agent nothing and ended sessions are read-only", () => {
    expect(allowedActions(base, "other", ["ONBOARDING_AGENT"])).toEqual([]);
    for (const status of ["COMPLETED", "CANCELLED", "EXPIRED"]) {
      expect(allowedActions({ ...base, status }, "op", ["SUPER_ADMIN"])).toEqual([]);
    }
  });
});
