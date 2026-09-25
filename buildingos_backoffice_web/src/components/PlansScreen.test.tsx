import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PlansScreen } from "./PlansScreen";

const plan = (status: string) => ({
  id: "p-1",
  code: "PREMIUM",
  name: "Premium",
  status,
  billingCycles: ["MONTHLY"],
  selfService: true,
  entitlements: { max_units: 20 },
  createdAt: "2026-09-01T00:00:00Z",
  updatedAt: "2026-09-01T00:00:00Z",
});

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(retire: () => Response) {
  let retired = false;
  return vi.fn(async (url: string, init?: RequestInit) => {
    const path = new URL(url, "http://localhost").pathname;
    if (path === "/api/gateway/platform/subscription-plans/p-1/retire" && init?.method === "POST") {
      const r = retire();
      retired = r.ok;
      return r;
    }
    if (path === "/api/gateway/platform/subscription-plans") {
      return json(200, { success: true, data: [plan(retired ? "RETIRED" : "ACTIVE")] });
    }
    if (path === "/api/gateway/platform/free-tier") {
      return json(200, { success: true, data: { "maintenance.enabled": true } });
    }
    if (path === "/api/gateway/platform/fees/BUILDING_CREATION") {
      return json(409, {
        success: false,
        code: "FEE_NOT_CONFIGURED",
        message: "BUILDING_CREATION fee has not been configured",
      });
    }
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("PlansScreen", () => {
  it("retires a plan after confirmation and reloads it as retired", async () => {
    vi.stubGlobal("fetch", gateway(() => json(200, { success: true, data: plan("RETIRED") })));
    const user = userEvent.setup();
    render(<PlansScreen title="Subscriptions · Plans" />);

    const card = await screen.findByRole("article", { name: "Plan PREMIUM" });
    await user.click(within(card).getByRole("button", { name: "Retire" }));
    expect(within(card).getByText(/Retiring is one-way/)).toBeInTheDocument();
    await user.click(within(card).getByRole("button", { name: "Confirm retire" }));

    expect(await screen.findByText("Plan PREMIUM retired.")).toBeInTheDocument();
    const reloaded = await screen.findByRole("article", { name: "Plan PREMIUM" });
    expect(await within(reloaded).findByText("Retired")).toBeInTheDocument();
    expect(within(reloaded).queryByRole("button", { name: "Retire" })).toBeNull();
    expect(within(reloaded).queryByRole("button", { name: "Edit" })).toBeNull();
  });

  it("shows the backend error code when retire fails, without a stale-refresh prompt", async () => {
    vi.stubGlobal(
      "fetch",
      gateway(() => json(403, { success: false, code: "ACCESS_DENIED", message: "Access is denied" })),
    );
    const user = userEvent.setup();
    render(<PlansScreen title="Subscriptions · Plans" />);

    const card = await screen.findByRole("article", { name: "Plan PREMIUM" });
    await user.click(within(card).getByRole("button", { name: "Retire" }));
    await user.click(within(card).getByRole("button", { name: "Confirm retire" }));

    const alert = await within(card).findByRole("alert");
    expect(alert).toHaveTextContent("Error code: ACCESS_DENIED");
    expect(within(card).queryByRole("button", { name: /Refresh/ })).toBeNull();
  });

  it("surfaces an unconfigured creation fee with its code and offers to configure it", async () => {
    vi.stubGlobal("fetch", gateway(() => json(200, { success: true, data: plan("RETIRED") })));
    render(<PlansScreen title="Subscriptions · Plans" />);

    const fee = await screen.findByRole("region", { name: "Building creation fee" });
    expect(await within(fee).findByText("Error code: FEE_NOT_CONFIGURED")).toBeInTheDocument();
    expect(within(fee).queryByText(/Approval blocked/)).toBeNull();
    expect(within(fee).getByRole("button", { name: "Save fee" })).toBeInTheDocument();
  });
});
