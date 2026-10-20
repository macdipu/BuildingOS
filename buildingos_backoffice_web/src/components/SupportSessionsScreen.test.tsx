import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SupportSessionsScreen } from "./SupportSessionsScreen";

const OWNER = "aaaaaaaa-2222-3333-4444-555555555555";
const OTHER = "bbbbbbbb-2222-3333-4444-555555555555";
const session = {
  id: "11111111-aaaa-bbbb-cccc-dddddddddddd",
  platformUserId: OWNER,
  targetUserId: null,
  buildingId: "cccccccc-2222-3333-4444-555555555555",
  status: "ACTIVE",
  reason: "Help with unit",
  permissionScope: ["SUPPORT_VIEW_MEMBERS", "SUPPORT_REVERSE_PAYMENT"],
  startedAt: "2026-10-20T10:00:00Z",
  expiresAt: "2026-10-20T18:00:00Z",
  endedAt: null,
};
const pending = {
  id: "ap1",
  supportSessionId: session.id,
  requestedScope: "SUPPORT_REVERSE_PAYMENT",
  status: "PENDING",
  decidedBy: null,
  requestedAt: "2026-10-20T10:05:00Z",
  decidedAt: null,
  decisionReason: null,
};
const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(onPost: (path: string, body: unknown) => Response = () => json(200, { success: true, data: {} })) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const u = new URL(url, "http://localhost");
    if (init?.method === "POST") return onPost(u.pathname, init.body ? JSON.parse(String(init.body)) : null);
    if (u.pathname === "/api/gateway/platform/backoffice/support-sessions") {
      return json(200, { success: true, data: [session], meta: { page: 0, size: 20, total: 1 } });
    }
    if (u.pathname === `/api/gateway/platform/backoffice/support-sessions/${session.id}`) {
      return json(200, { success: true, data: { ...session, elevatedApprovals: [pending] } });
    }
    if (u.pathname === "/api/gateway/platform/backoffice/elevated-approvals") {
      return json(200, { success: true, data: [pending], meta: { page: 0, size: 20, total: 1 } });
    }
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

/** datetime-local value in the runner's local time, `ms` from now. */
function localInput(ms: number) {
  const d = new Date(Date.now() + ms);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

afterEach(() => vi.unstubAllGlobals());

describe("SupportSessionsScreen", () => {
  it("shows a pending high-risk scope as pending, with no decision controls for the owning agent", async () => {
    vi.stubGlobal("fetch", gateway());
    const user = userEvent.setup();
    render(<SupportSessionsScreen title="Support · Active Support Sessions" mode="active" userId={OWNER} roles={["SUPPORT_AGENT"]} />);

    await user.click(await screen.findByRole("button", { name: "11111111" }));
    expect(await screen.findByText("Pending approval")).toBeInTheDocument();
    expect(screen.getByText("Granted")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Pending elevated approvals" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "End session" })).toBeInTheDocument();
  });

  it("hides approve/deny from a SUPER_ADMIN who owns the session (four-eyes)", async () => {
    vi.stubGlobal("fetch", gateway());
    const user = userEvent.setup();
    render(<SupportSessionsScreen title="t" mode="active" userId={OWNER} roles={["SUPER_ADMIN"]} />);

    await user.click(await screen.findByRole("button", { name: "11111111" }));
    await screen.findByText("Pending approval");
    expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
  });

  it("lets another SUPER_ADMIN approve, and requires a reason to deny", async () => {
    const fetchMock = gateway(() => json(200, { success: true, data: { ...pending, status: "APPROVED" } }));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<SupportSessionsScreen title="t" mode="active" userId={OTHER} roles={["SUPER_ADMIN"]} />);

    expect(await screen.findByRole("region", { name: "Pending elevated approvals" })).toBeInTheDocument();
    await user.click(await screen.findByRole("button", { name: "11111111" }));
    await user.click(await screen.findByRole("button", { name: "Deny" }));
    expect(screen.getAllByText("A reason is required to deny.").length).toBeGreaterThan(0);
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);

    await user.click(screen.getByRole("button", { name: "Approve" }));
    expect(await screen.findByText("Approved.")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/gateway/platform/backoffice/elevated-approvals/ap1/approve",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("validates the start form inline and shows the backend code", async () => {
    const fetchMock = gateway(() => json(422, { success: false, code: "BUILDING_NOT_FOUND", message: "No such building" }));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<SupportSessionsScreen title="t" mode="active" userId={OWNER} roles={["SUPPORT_AGENT"]} />);

    await user.click(await screen.findByRole("button", { name: "Start support session" }));
    await user.click(screen.getByRole("button", { name: "Start session" }));
    expect(screen.getByText("Name a target user, a building, or both.")).toBeInTheDocument();
    expect(screen.getByText("Choose at least one scope.")).toBeInTheDocument();
    expect(screen.getByText("A reason is required.")).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);

    const form = screen.getByRole("form", { name: "Start support session" });
    await user.type(form.querySelectorAll("input")[1], "cccccccc-2222-3333-4444-555555555555");
    await user.click(screen.getByRole("checkbox", { name: /View members/ }));
    await user.type(form.querySelector("textarea")!, "Help");
    const tooLate = localInput(12 * 3_600_000);
    await user.type(form.querySelector('input[type="datetime-local"]')!, tooLate);
    await user.click(screen.getByRole("button", { name: "Start session" }));
    expect(screen.getByText("Expiry can be at most 8 hours from now.")).toBeInTheDocument();
  });

  it("history mode lists every session without a start control", async () => {
    const fetchMock = gateway();
    vi.stubGlobal("fetch", fetchMock);
    render(<SupportSessionsScreen title="t" mode="history" userId={OWNER} roles={["SUPPORT_AGENT"]} />);
    await screen.findByRole("button", { name: "11111111" });
    expect(screen.queryByRole("button", { name: "Start support session" })).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/gateway/platform/backoffice/support-sessions?page=0", expect.anything());
  });
});
