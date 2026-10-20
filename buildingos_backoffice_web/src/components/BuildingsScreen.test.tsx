import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BuildingsScreen } from "./BuildingsScreen";

const summary = {
  id: "b1",
  applicationId: "a1",
  name: "Lake View",
  buildingType: "RESIDENTIAL",
  area: "Gulshan",
  district: "Dhaka",
  status: "ONBOARDING",
  createdAt: "2026-09-20T10:00:00Z",
  updatedAt: "2026-09-21T10:00:00Z",
};
const detail = {
  ...summary,
  address: "Road 1",
  postalCode: null,
  latitude: null,
  longitude: null,
  contactPhone: "01711111111",
  members: [{ userId: "u-1", role: "BUILDING_ADMIN", status: "ACTIVE" }],
};

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

function gateway(activate: () => Response) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    const u = new URL(url, "http://localhost");
    if (u.pathname === "/api/gateway/platform/buildings") {
      return json(200, { success: true, data: [summary], meta: { page: 0, size: 20, total: 1 } });
    }
    if (u.pathname === "/api/gateway/platform/buildings/b1") return json(200, { success: true, data: detail });
    if (u.pathname === "/api/gateway/platform/buildings/b1/activate" && init?.method === "POST") return activate();
    return json(404, { success: false, code: "NOT_FOUND" });
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("BuildingsScreen", () => {
  it("lists buildings for the status and searches by name", async () => {
    const fetchMock = gateway(() => json(200, {}));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<BuildingsScreen title="Buildings · Onboarding" status="ONBOARDING" />);

    expect(await screen.findByRole("button", { name: "Lake View" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/gateway/platform/buildings?status=ONBOARDING&page=0", expect.anything());

    await user.type(screen.getByLabelText("Building name"), " Lake ");
    await user.click(screen.getByRole("button", { name: "Search" }));
    await screen.findByRole("button", { name: "Lake View" });
    expect(fetchMock).toHaveBeenCalledWith("/api/gateway/platform/buildings?status=ONBOARDING&page=0&q=Lake", expect.anything());
  });

  it("requires a reason, activates, and shows the backend code on failure", async () => {
    let fail = true;
    const fetchMock = gateway(() =>
      fail
        ? json(409, { success: false, code: "NO_BUILDING_ADMIN", message: "No active admin" })
        : json(200, { success: true, data: { ...detail, status: "ACTIVE" } }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<BuildingsScreen title="Buildings · Onboarding" status="ONBOARDING" />);

    await user.click(await screen.findByRole("button", { name: "Lake View" }));
    expect(await screen.findByRole("link", { name: "Assign onboarding agent" })).toHaveAttribute(
      "href",
      "/support/assisted-onboarding?buildingId=b1",
    );
    await user.click(await screen.findByRole("button", { name: "Activate" }));
    await user.click(screen.getByRole("button", { name: "Confirm activate" }));
    expect(screen.getByText("A reason is required.")).toBeInTheDocument();

    await user.type(screen.getByLabelText(/^Reason/), "Ready");
    await user.click(screen.getByRole("button", { name: "Confirm activate" }));
    expect(await screen.findByText("Error code: NO_BUILDING_ADMIN")).toBeInTheDocument();
    expect(JSON.parse(String(fetchMock.mock.calls.at(-1)![1]!.body))).toEqual({ reason: "Ready" });

    fail = false;
    await user.click(screen.getByRole("button", { name: "Confirm activate" }));
    expect(await screen.findByText("Building activated.")).toBeInTheDocument();
  });
});
