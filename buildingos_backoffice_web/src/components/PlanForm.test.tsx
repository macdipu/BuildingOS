import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Plan } from "@/lib/subscriptions";
import { PlanForm } from "./PlanForm";

const plan: Plan = {
  id: "p-1",
  code: "PREMIUM",
  name: "Premium",
  status: "ACTIVE",
  billingCycles: ["MONTHLY"],
  selfService: true,
  entitlements: { "maintenance.enabled": true, max_units: 20, "future.key": 3 },
  createdAt: "2026-09-01T00:00:00Z",
  updatedAt: "2026-09-01T00:00:00Z",
};

function setup(fetchMock: ReturnType<typeof vi.fn>, existing?: Plan) {
  vi.stubGlobal("fetch", fetchMock);
  const onSaved = vi.fn();
  render(<PlanForm plan={existing} onSaved={onSaved} onCancel={vi.fn()} />);
  return { onSaved, user: userEvent.setup() };
}

const respond = (status: number, body: unknown) =>
  vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }));

afterEach(() => vi.unstubAllGlobals());

describe("PlanForm", () => {
  it("validates code, name, cycles and entitlement values before calling the API", async () => {
    const fetchMock = respond(201, { success: true, data: plan });
    const { user } = setup(fetchMock);

    await user.type(screen.getByLabelText("Plan code"), "bad code");
    await user.type(screen.getByLabelText(/Max units/), "-5");
    await user.type(screen.getByLabelText(/Support tier/), "x".repeat(65));
    await user.click(screen.getByRole("button", { name: "Create plan" }));

    expect(screen.getByText(/2-64 characters of A-Z/)).toBeInTheDocument();
    expect(screen.getByText("Name must be 1-200 characters.")).toBeInTheDocument();
    expect(screen.getByText("Offer at least one billing cycle.")).toBeInTheDocument();
    expect(screen.getByText(/non-negative whole number/)).toBeInTheDocument();
    expect(screen.getByText("Must be 1-64 characters.")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("creates a plan with structured §149.16 entitlements", async () => {
    const fetchMock = respond(201, { success: true, data: plan });
    const { onSaved, user } = setup(fetchMock);

    await user.type(screen.getByLabelText("Plan code"), "PREMIUM");
    await user.type(screen.getByLabelText("Name"), "Premium");
    await user.click(screen.getByLabelText("Yearly"));
    await user.click(screen.getByLabelText("Monthly"));
    await user.selectOptions(screen.getByLabelText(/Maintenance/), "true");
    await user.selectOptions(screen.getByLabelText(/Work orders/), "false");
    await user.type(screen.getByLabelText(/Max units/), "20");
    await user.click(screen.getByRole("button", { name: "Create plan" }));

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/gateway/platform/subscription-plans");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      code: "PREMIUM",
      name: "Premium",
      billingCycles: ["MONTHLY", "YEARLY"],
      selfService: false,
      entitlements: { "maintenance.enabled": true, "work_orders.enabled": false, max_units: 20 },
    });
    expect(onSaved).toHaveBeenCalledWith(plan);
  });

  it("shows an entitlement key rejected by the API inline with its error code", async () => {
    const fetchMock = respond(400, {
      success: false,
      code: "INVALID_REQUEST",
      message: "Unknown entitlement: future.key",
    });
    const { onSaved, user } = setup(fetchMock, plan);

    expect(screen.getByLabelText("Plan code")).toBeDisabled();
    expect(screen.getByText(/Kept unchanged.*future\.key/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Save plan" }));

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/gateway/platform/subscription-plans/p-1");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body)).toEqual({
      name: "Premium",
      billingCycles: ["MONTHLY"],
      selfService: true,
      entitlements: { "future.key": 3, "maintenance.enabled": true, max_units: 20 },
    });
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Unknown entitlement: future.key");
    expect(alert).toHaveTextContent("Error code: INVALID_REQUEST");
    expect(onSaved).not.toHaveBeenCalled();
  });
});
