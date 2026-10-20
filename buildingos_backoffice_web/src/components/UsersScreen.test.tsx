import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { UsersScreen } from "./UsersScreen";

const ME = "aaaaaaaa-2222-3333-4444-555555555555";
const OTHER = "bbbbbbbb-2222-3333-4444-555555555555";
const user = (id: string, phone: string, platformRoles: string[]) => ({ id, phone, createdAt: "2026-01-01T00:00:00Z", platformRoles });
const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(onWrite: (method: string, path: string, body: unknown) => Response = () => json(200, { success: true, data: {} })) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const u = new URL(url, "http://localhost");
    if (init?.method === "POST" || init?.method === "DELETE") {
      return onWrite(init.method, u.pathname, init.body ? JSON.parse(String(init.body)) : null);
    }
    if (u.pathname === "/api/gateway/platform/backoffice/users") {
      return json(200, {
        success: true,
        data: [user(ME, "01700000001", ["SUPER_ADMIN"]), user(OTHER, "01700000002", ["SUPPORT_AGENT"])],
        meta: { page: 0, size: 20, total: 2 },
      });
    }
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("UsersScreen", () => {
  it("lists users with roles, searches by phone and filters by role", async () => {
    const fetchMock = gateway();
    vi.stubGlobal("fetch", fetchMock);
    const user_ = userEvent.setup();
    render(<UsersScreen title="Users · Platform Users" userId={ME} roles={["PLATFORM_ADMIN"]} />);

    expect(await screen.findByRole("button", { name: "01700000002" })).toBeInTheDocument();
    await user_.type(screen.getByLabelText("Phone"), " 0170 ");
    await user_.selectOptions(screen.getByLabelText("Platform role"), "SUPPORT_AGENT");
    await user_.click(screen.getByRole("button", { name: "Search" }));
    await screen.findByRole("button", { name: "01700000002" });
    const last = String(fetchMock.mock.calls.at(-1)![0]);
    expect(last).toContain("query=0170");
    expect(last).toContain("role=SUPPORT_AGENT");
  });

  it("fixes the role on an agent list and has no role picker", async () => {
    const fetchMock = gateway();
    vi.stubGlobal("fetch", fetchMock);
    render(<UsersScreen title="Users · Support Agents" fixedRole="SUPPORT_AGENT" userId={ME} roles={["SUPER_ADMIN"]} />);
    await screen.findByRole("button", { name: "01700000002" });
    expect(screen.queryByLabelText("Platform role")).not.toBeInTheDocument();
    expect(String(fetchMock.mock.calls[0][0])).toContain("role=SUPPORT_AGENT");
  });

  it("shows no role controls to a PLATFORM_ADMIN", async () => {
    vi.stubGlobal("fetch", gateway());
    const user_ = userEvent.setup();
    render(<UsersScreen title="t" userId={ME} roles={["PLATFORM_ADMIN"]} />);
    await user_.click(await screen.findByRole("button", { name: "01700000002" }));
    const panel = await screen.findByRole("complementary", { name: "User detail" });
    expect(within(panel).getByText("Support Agent")).toBeInTheDocument();
    expect(within(panel).queryByRole("button", { name: /Revoke/ })).not.toBeInTheDocument();
    expect(within(panel).queryByLabelText(/^Assign role/)).not.toBeInTheDocument();
  });

  it("hides role changes on your own account, even as SUPER_ADMIN", async () => {
    vi.stubGlobal("fetch", gateway());
    const user_ = userEvent.setup();
    render(<UsersScreen title="t" userId={ME} roles={["SUPER_ADMIN"]} />);
    await user_.click(await screen.findByRole("button", { name: "01700000001" }));
    const panel = await screen.findByRole("complementary", { name: "User detail" });
    expect(within(panel).getByText("You cannot change your own platform roles.")).toBeInTheDocument();
    expect(within(panel).queryByRole("button", { name: /Revoke/ })).not.toBeInTheDocument();
  });

  it("asks for confirmation before assigning, then shows the updated roles", async () => {
    const fetchMock = gateway((method, path, body) => {
      expect(method).toBe("POST");
      expect(path).toBe(`/api/gateway/platform/backoffice/users/${OTHER}/platform-roles`);
      expect(body).toEqual({ role: "ONBOARDING_AGENT" });
      return json(200, { success: true, data: user(OTHER, "01700000002", ["ONBOARDING_AGENT", "SUPPORT_AGENT"]) });
    });
    vi.stubGlobal("fetch", fetchMock);
    const user_ = userEvent.setup();
    render(<UsersScreen title="t" userId={ME} roles={["SUPER_ADMIN"]} />);
    await user_.click(await screen.findByRole("button", { name: "01700000002" }));
    const panel = await screen.findByRole("complementary", { name: "User detail" });

    await user_.click(within(panel).getByRole("button", { name: "Assign role" }));
    expect(within(panel).getByText("Choose a role.")).toBeInTheDocument();
    await user_.selectOptions(within(panel).getByLabelText(/^Assign role/), "ONBOARDING_AGENT");
    await user_.click(within(panel).getByRole("button", { name: "Assign role" }));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);
    await user_.click(within(panel).getByRole("button", { name: "Confirm" }));
    expect(await within(panel).findByText("Onboarding Agent assigned.")).toBeInTheDocument();
  });

  it("revokes after confirmation and shows the backend code when auth-service refuses", async () => {
    let refuse = true;
    const fetchMock = gateway(() =>
      refuse
        ? json(403, { success: false, code: "ACCESS_DENIED", message: "Access is denied" })
        : json(200, { success: true, data: user(OTHER, "01700000002", []) }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const user_ = userEvent.setup();
    render(<UsersScreen title="t" userId={ME} roles={["SUPER_ADMIN"]} />);
    await user_.click(await screen.findByRole("button", { name: "01700000002" }));
    const panel = await screen.findByRole("complementary", { name: "User detail" });

    await user_.click(within(panel).getByRole("button", { name: "Revoke Support Agent" }));
    await user_.click(within(panel).getByRole("button", { name: "Confirm" }));
    expect(await within(panel).findByText("Error code: ACCESS_DENIED")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/gateway/platform/backoffice/users/${OTHER}/platform-roles/SUPPORT_AGENT`,
      expect.objectContaining({ method: "DELETE" }),
    );

    refuse = false;
    await user_.click(within(panel).getByRole("button", { name: "Confirm" }));
    expect(await within(panel).findByText("Support Agent revoked.")).toBeInTheDocument();
    expect(within(panel).getByText("No platform roles.")).toBeInTheDocument();
  });
});
