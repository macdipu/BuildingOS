import { describe, expect, it } from "vitest";
import { NAV, findNavEntry, visibleNav } from "./nav";

const labels = (roles: Parameters<typeof visibleNav>[0]) => visibleNav(roles).map((s) => s.label);

describe("§149.2 navigation", () => {
  it("keeps the BRD section order and Buildings/Users/Support/System item labels", () => {
    expect(NAV.map((s) => s.label)).toEqual(["Dashboard", "Buildings", "Users", "Subscriptions", "Support", "System"]);
    expect(NAV[1].items.map((i) => i.label)).toEqual([
      "Applications",
      "Under Review",
      "Onboarding",
      "Active Buildings",
      "Suspended Buildings",
      "Archived Buildings",
    ]);
    expect(NAV[5].items.map((i) => i.label)).toEqual([
      "Platform Settings",
      "Feature / Entitlement Configuration",
      "Audit Logs",
      "System Health",
    ]);
  });

  it("SUPER_ADMIN sees every section", () => {
    expect(labels(["SUPER_ADMIN"])).toHaveLength(6);
  });

  it("role-scoped staff see only their sections", () => {
    expect(labels(["SUBSCRIPTION_ADMIN"])).toEqual(["Dashboard", "Subscriptions"]);
    expect(labels(["PLATFORM_ADMIN"])).toEqual(["Dashboard", "Buildings", "Users", "Subscriptions", "Support"]);
    const onboarding = visibleNav(["ONBOARDING_AGENT"]);
    expect(onboarding.map((s) => s.label)).toEqual(["Dashboard", "Support"]);
    expect(onboarding[1].items.map((i) => i.slug)).toEqual(["assisted-onboarding"]);
    expect(visibleNav(["SUPPORT_AGENT"])[1].items.map((i) => i.slug)).toEqual(["active-sessions", "history"]);
  });

  it("no platform role sees nothing", () => {
    expect(visibleNav([])).toEqual([]);
  });

  it("findNavEntry refuses items outside the caller's roles", () => {
    expect(findNavEntry(["SUPER_ADMIN"], "system", "audit-logs")?.item?.label).toBe("Audit Logs");
    expect(findNavEntry(["PLATFORM_ADMIN"], "system", "audit-logs")).toBeNull();
    expect(findNavEntry(["SUPPORT_AGENT"], "support", "assisted-onboarding")).toBeNull();
    expect(findNavEntry(["SUPER_ADMIN"], "buildings", "nope")).toBeNull();
  });
});
