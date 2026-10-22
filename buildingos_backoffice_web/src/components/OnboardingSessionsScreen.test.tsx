import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { OnboardingSessionsScreen } from "./OnboardingSessionsScreen";

const BUILDING = "11111111-2222-3333-4444-555555555555";
const AGENT = "aaaaaaaa-2222-3333-4444-555555555555";
const session = {
  id: "s1",
  buildingId: BUILDING,
  assignedAgentUserId: AGENT,
  requestedByUserId: null,
  status: "ASSIGNED",
  accessScope: ["ONBOARDING_VIEW_BUILDING"],
  reason: "Setup help",
  startedAt: "2026-10-01T10:00:00Z",
  expiresAt: "2026-10-20T10:00:00Z",
  completedAt: null,
  notes: null,
};
const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(onPost: (path: string, body: unknown) => Response) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const u = new URL(url, "http://localhost");
    if (init?.method === "POST") return onPost(u.pathname, init.body ? JSON.parse(String(init.body)) : null);
    if (u.pathname === "/api/gateway/platform/backoffice/onboarding-sessions") {
      return json(200, { success: true, data: [session], meta: { page: 0, size: 20, total: 1 } });
    }
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("OnboardingSessionsScreen", () => {
  it("hides the assign control from agents and shows only their transitions", async () => {
    vi.stubGlobal("fetch", gateway(() => json(200, {})));
    const user = userEvent.setup();
    render(<OnboardingSessionsScreen title="Support · Assisted Onboarding" userId={AGENT} roles={["ONBOARDING_AGENT"]} />);

    await user.click(await screen.findByRole("button", { name: BUILDING }));
    expect(screen.queryByRole("button", { name: "Assign onboarding agent" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start work" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Complete" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancel session" })).not.toBeInTheDocument();
  });

  it("validates the start form inline and surfaces backend 422", async () => {
    const fetchMock = gateway(() => json(422, { success: false, code: "ASSIGNEE_NOT_ONBOARDING_AGENT", message: "Not an agent" }));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<OnboardingSessionsScreen title="Support · Assisted Onboarding" userId="op" roles={["PLATFORM_ADMIN"]} />);

    await user.click(await screen.findByRole("button", { name: "Assign onboarding agent" }));
    await user.click(screen.getByRole("button", { name: "Start session" }));
    expect(screen.getByText("Enter a building id (UUID).")).toBeInTheDocument();
    expect(screen.getByText("Choose at least one scope.")).toBeInTheDocument();
    expect(screen.getByText("A reason is required.")).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);

    const form = screen.getByRole("form", { name: "Assign onboarding agent" });
    await user.type(form.querySelectorAll("input")[0], BUILDING);
    await user.type(form.querySelectorAll("input")[1], AGENT);
    await user.click(screen.getByRole("checkbox", { name: "View building" }));
    await user.type(screen.getAllByRole("textbox").find((e) => e.tagName === "TEXTAREA")!, "Help");
    const d = new Date(Date.now() + 5 * 86_400_000);
    const p = (n: number) => String(n).padStart(2, "0");
    const expires = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
    await user.type(form.querySelector('input[type="datetime-local"]')!, expires);
    await user.click(screen.getByRole("button", { name: "Start session" }));

    expect(await screen.findByText("Error code: ASSIGNEE_NOT_ONBOARDING_AGENT")).toBeInTheDocument();
  });

  it("lets an operator complete a session and shows the backend result", async () => {
    const fetchMock = gateway(() =>
      json(200, { success: true, data: { ...session, status: "COMPLETED", completedAt: "2026-10-02T10:00:00Z" } }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<OnboardingSessionsScreen title="Support · Assisted Onboarding" userId="op" roles={["SUPER_ADMIN"]} />);

    await user.click(await screen.findByRole("button", { name: BUILDING }));
    expect(screen.queryByRole("button", { name: "Start work" })).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Complete" }));
    expect(await screen.findByText("Complete: now completed.")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/gateway/platform/backoffice/onboarding-sessions/s1/complete",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("opens the start form prefilled when arriving with a building id, for operators only", async () => {
    vi.stubGlobal("fetch", gateway(() => json(200, {})));
    const { unmount } = render(
      <OnboardingSessionsScreen title="t" userId="op" roles={["PLATFORM_ADMIN"]} initialBuildingId={BUILDING} />,
    );
    const form = await screen.findByRole("form", { name: "Assign onboarding agent" });
    expect(form.querySelectorAll("input")[0]).toHaveValue(BUILDING);
    unmount();

    render(<OnboardingSessionsScreen title="t" userId={AGENT} roles={["ONBOARDING_AGENT"]} initialBuildingId={BUILDING} />);
    await screen.findByRole("button", { name: BUILDING });
    expect(screen.queryByRole("form", { name: "Assign onboarding agent" })).not.toBeInTheDocument();
  });

  it("preselects a valid initial status and lists with it; ignores an invalid one", async () => {
    const fetchMock = gateway(() => json(200, {}));
    vi.stubGlobal("fetch", fetchMock);
    const { unmount } = render(<OnboardingSessionsScreen title="t" userId="op" roles={["PLATFORM_ADMIN"]} initialStatus="ASSIGNED" />);
    await screen.findByRole("button", { name: BUILDING });
    expect(screen.getByRole("combobox", { name: "Status" })).toHaveValue("ASSIGNED");
    const listed = new URL(String(fetchMock.mock.calls[0][0]), "http://localhost");
    expect(listed.searchParams.get("status")).toBe("ASSIGNED");
    unmount();

    fetchMock.mockClear();
    render(<OnboardingSessionsScreen title="t" userId="op" roles={["PLATFORM_ADMIN"]} initialStatus="BOGUS" />);
    await screen.findByRole("button", { name: BUILDING });
    expect(screen.getByRole("combobox", { name: "Status" })).toHaveValue("");
    const all = new URL(String(fetchMock.mock.calls[0][0]), "http://localhost");
    expect(all.searchParams.has("status")).toBe(false);
  });
});
