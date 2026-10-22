import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuditLogScreen } from "./AuditLogScreen";
import { DashboardScreen } from "./DashboardScreen";
import { SystemHealthScreen } from "./SystemHealthScreen";

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });
const ev = (id: string, over = {}) => ({
  id,
  source: "building-service",
  occurredAt: "2026-10-20T10:00:00Z",
  actorUserId: "aaaaaaaa-2222-3333-4444-555555555555",
  action: "NONE→ACTIVE",
  entityType: "BUILDING",
  entityId: "b1",
  buildingId: null,
  reason: "ok",
  ...over,
});

afterEach(() => vi.unstubAllGlobals());

describe("SystemHealthScreen", () => {
  it("shows each service status with the last-checked time", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        json(200, {
          success: true,
          data: {
            checkedAt: "2026-10-20T10:00:00Z",
            overall: "DEGRADED",
            services: [
              { name: "auth-service", status: "UP", httpStatus: 200, latencyMs: 12 },
              { name: "building-service", status: "UNKNOWN", latencyMs: 3000 },
            ],
          },
        }),
      ),
    );
    render(<SystemHealthScreen title="System · System Health" />);
    expect(await screen.findByText("auth-service")).toBeInTheDocument();
    expect(screen.getByText("DEGRADED")).toBeInTheDocument();
    expect(screen.getByText("UNKNOWN")).toBeInTheDocument();
    expect(screen.getByText(/Last checked/)).toBeInTheDocument();
  });
});

describe("AuditLogScreen", () => {
  it("sends filters to the server, names unavailable sources, and pages older events", async () => {
    const fetchMock = vi.fn(async (url: string) => {
      const u = new URL(url, "http://localhost");
      const until = u.searchParams.get("until");
      if (until === "2026-10-20T09:00:00Z" && u.searchParams.get("untilId") === "e1") {
        return json(200, { success: true, data: { items: [ev("e2")], nextUntil: null, nextUntilId: null, unavailableSources: [] } });
      }
      return json(200, {
        success: true,
        data: {
          items: [ev("e1", { actorUserId: null, role: "SUPER_ADMIN" })],
          nextUntil: "2026-10-20T09:00:00Z",
          nextUntilId: "e1",
          unavailableSources: ["auth-service"],
        },
      });
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<AuditLogScreen title="System · Audit Logs" />);

    expect(await screen.findByText("System")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("auth-service did not respond");

    await user.type(screen.getByLabelText("Entity type"), "BUILDING");
    await user.selectOptions(screen.getByLabelText("Module"), "building-service");
    await user.click(screen.getByRole("button", { name: "Apply filters" }));
    await screen.findByText("System");
    const last = String(fetchMock.mock.calls.at(-1)![0]);
    expect(last).toContain("entityType=BUILDING");
    expect(last).toContain("source=building-service");

    await user.click(screen.getByRole("button", { name: "Load older events" }));
    expect(await screen.findAllByText("NONE→ACTIVE")).toHaveLength(2);
    const older = new URL(String(fetchMock.mock.calls.at(-1)![0]), "http://localhost");
    expect(older.searchParams.get("until")).toBe("2026-10-20T09:00:00Z");
    expect(older.searchParams.get("untilId")).toBe("e1");
    expect(older.searchParams.get("source")).toBe("building-service");
    expect(screen.queryByRole("button", { name: "Load older events" })).not.toBeInTheDocument();
  });

  it("rejects a malformed user id without calling the API again", async () => {
    const fetchMock = vi.fn(async () => json(200, { success: true, data: { items: [], nextUntil: null, nextUntilId: null, unavailableSources: [] } }));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<AuditLogScreen title="t" />);
    await screen.findByText("No audit events match these filters.");
    const before = fetchMock.mock.calls.length;
    await user.type(screen.getByLabelText("User id"), "nope");
    await user.click(screen.getByRole("button", { name: "Apply filters" }));
    expect(screen.getByText("Enter a user id (UUID).")).toBeInTheDocument();
    expect(fetchMock.mock.calls.length).toBe(before);
  });
});

describe("DashboardScreen", () => {
  it("shows list totals as links, and never fabricates a failed count", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) => {
        const u = new URL(url, "http://localhost");
        if (u.pathname === "/api/gateway/platform/building-applications") {
          return json(200, { success: true, data: [], meta: { page: 0, size: 20, total: u.searchParams.get("status") === "SUBMITTED" ? 7 : 2 } });
        }
        if (u.pathname === "/api/gateway/platform/buildings") {
          return u.searchParams.get("status") === "ACTIVE"
            ? json(503, { success: false, code: "DEPENDENCY_UNAVAILABLE" })
            : json(200, { success: true, data: [], meta: { page: 0, size: 20, total: 3 } });
        }
        return json(200, { success: true, data: [], meta: { page: 0, size: 20, total: 0 } });
      }),
    );
    render(<DashboardScreen roles={["PLATFORM_ADMIN"]} />);

    const awaiting = await screen.findByRole("link", { name: /Applications awaiting review/ });
    expect(awaiting).toHaveAttribute("href", "/buildings/applications");
    expect(await within(awaiting).findByText("7")).toBeInTheDocument();
    const active = screen.getByRole("link", { name: /Active buildings/ });
    expect(await within(active).findByText("Not available")).toBeInTheDocument();
    expect(within(active).getByText("Error code: DEPENDENCY_UNAVAILABLE")).toBeInTheDocument();
  });

  it("shows a role only its own cards", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => json(200, { success: true, data: [], meta: { page: 0, size: 20, total: 1 } })));
    render(<DashboardScreen roles={["SUBSCRIPTION_ADMIN"]} />);
    expect(await screen.findByRole("link", { name: /User subscriptions/ })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /Applications/ })).not.toBeInTheDocument();
  });
});
