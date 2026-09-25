import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { UserSubscriptionScreen } from "./UserSubscriptionScreen";

const USER = "11111111-2222-3333-4444-555555555555";
const basePlan = { selfService: false, entitlements: {}, createdAt: "", updatedAt: "" };
const plans = [
  { ...basePlan, id: "p-1", code: "PREMIUM", name: "Premium", status: "ACTIVE", billingCycles: ["MONTHLY", "YEARLY"] },
  { ...basePlan, id: "p-0", code: "OLD", name: "Old", status: "RETIRED", billingCycles: ["MONTHLY"] },
];
const subscription = {
  id: "s-1",
  userId: USER,
  planId: "p-1",
  status: "ACTIVE",
  billingCycle: "YEARLY",
  grantedBy: "ADMIN",
  startedAt: "2026-09-25T10:00:00Z",
  effectiveEntitlements: { max_units: 20 },
};

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(grant: () => Response) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const path = new URL(url, "http://localhost").pathname;
    if (path === "/api/gateway/platform/subscription-plans") return json(200, { success: true, data: plans });
    if (path === `/api/gateway/platform/users/${USER}/subscription`) {
      if (init?.method === "POST") return grant();
      return json(404, { success: false, code: "SUBSCRIPTION_NOT_FOUND", message: "No active subscription" });
    }
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

async function lookUp(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("User id"), USER);
  await user.click(screen.getByRole("button", { name: "Look up" }));
  return screen.findByRole("region", { name: "Assign plan" });
}

afterEach(() => vi.unstubAllGlobals());

describe("UserSubscriptionScreen", () => {
  it("rejects a malformed user id without calling the API", async () => {
    const fetchMock = gateway(() => json(201, {}));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<UserSubscriptionScreen title="Subscriptions · User Subscriptions" />);

    await user.type(screen.getByLabelText("User id"), "abc");
    await user.click(screen.getByRole("button", { name: "Look up" }));

    expect(screen.getByText("Enter a user id (UUID).")).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([u]) => String(u).includes("/users/"))).toBe(false);
  });

  it("looks up a user without a subscription and assigns an active plan", async () => {
    const fetchMock = gateway(() => json(201, { success: true, data: subscription }));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<UserSubscriptionScreen title="Subscriptions · User Subscriptions" />);

    const assign = await lookUp(user);
    const current = screen.getByRole("region", { name: "Current subscription" });
    expect(await within(current).findByText("Error code: SUBSCRIPTION_NOT_FOUND")).toBeInTheDocument();

    const planSelect = within(assign).getByLabelText("Plan");
    expect(within(planSelect).queryByText(/Old/)).toBeNull();
    await user.selectOptions(planSelect, "p-1");
    await user.selectOptions(within(assign).getByLabelText("Billing cycle"), "YEARLY");
    await user.click(within(assign).getByRole("button", { name: "Assign plan" }));

    const post = fetchMock.mock.calls.find(([, init]) => init?.method === "POST")!;
    expect(post[0]).toBe(`/api/gateway/platform/users/${USER}/subscription`);
    expect(JSON.parse(String(post[1]!.body))).toEqual({ planId: "p-1", billingCycle: "YEARLY" });
    expect(await within(assign).findByRole("status")).toHaveTextContent("Premium assigned.");
    expect(within(current).getByText("Premium")).toBeInTheDocument();
    expect(within(current).getByText("Yearly")).toBeInTheDocument();
  });

  it("shows the backend error code when assignment is refused", async () => {
    vi.stubGlobal(
      "fetch",
      gateway(() =>
        json(409, { success: false, code: "ALREADY_SUBSCRIBED", message: "An active subscription already exists" }),
      ),
    );
    const user = userEvent.setup();
    render(<UserSubscriptionScreen title="Subscriptions · User Subscriptions" />);

    const assign = await lookUp(user);
    await user.selectOptions(within(assign).getByLabelText("Plan"), "p-1");
    await user.selectOptions(within(assign).getByLabelText("Billing cycle"), "MONTHLY");
    await user.click(within(assign).getByRole("button", { name: "Assign plan" }));

    const alert = await within(assign).findByRole("alert");
    expect(alert).toHaveTextContent("An active subscription already exists");
    expect(alert).toHaveTextContent("Error code: ALREADY_SUBSCRIBED");
    expect(within(assign).queryByRole("button", { name: /Refresh/ })).toBeNull();
  });
});
