import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApplicationsScreen } from "./ApplicationsScreen";

const row = (id: string, number: string, status: string) => ({
  id,
  applicationNumber: number,
  status,
  buildingName: `Building ${number}`,
  area: "Gulshan",
  district: "Dhaka",
  contactName: "Rahim",
  contactPhone: "01711111111",
  submittedAt: "2026-09-20T10:00:00Z",
});

function ok(data: unknown, meta?: unknown) {
  return new Response(JSON.stringify({ success: true, data, meta, traceId: "t" }), { status: 200 });
}

function gateway() {
  return vi.fn(async (url: string) => {
    const u = new URL(url, "http://localhost");
    if (u.pathname === "/api/gateway/platform/building-applications") {
      const status = u.searchParams.get("status")!;
      const items = status === "SUBMITTED" ? [row("a1", "BA-0001", status)] : status === "UNDER_REVIEW" ? [row("a2", "BA-0002", status)] : [];
      return ok(items, { page: 0, size: 20, total: items.length });
    }
    if (u.pathname === "/api/gateway/building-applications/a2") return ok(row("a2", "BA-0002", "UNDER_REVIEW"));
    if (u.pathname === "/api/gateway/platform/building-applications/a2/duplicates") {
      return ok([{ kind: "BUILDING", id: "b1", reference: "BLD-7", status: "ACTIVE", name: "Lake View", address: null, area: null, district: null, matchedOn: ["NAME"] }]);
    }
    if (u.pathname === "/api/gateway/building-applications/a2/history") {
      return ok([{ fromStatus: "SUBMITTED", toStatus: "UNDER_REVIEW", reason: null, occurredAt: "2026-09-21T10:00:00Z" }]);
    }
    return ok([]);
  });
}

afterEach(() => vi.unstubAllGlobals());

describe("ApplicationsScreen", () => {
  it("lists the initial tab and switches status tabs", async () => {
    const fetchMock = gateway();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<ApplicationsScreen title="Buildings · Applications" initialStatus="SUBMITTED" />);

    expect(screen.getAllByRole("tab").map((t) => t.textContent)).toEqual([
      "Submitted",
      "Under Review",
      "More Information Required",
      "Rejected",
      "Approved",
    ]);
    expect(screen.getByRole("tab", { name: "Submitted" })).toHaveAttribute("aria-selected", "true");
    expect(await screen.findByRole("button", { name: "BA-0001" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/gateway/platform/building-applications?status=SUBMITTED&page=0",
      expect.anything(),
    );

    await user.click(screen.getByRole("tab", { name: "Rejected" }));
    expect(await screen.findByText("No rejected applications.")).toBeInTheDocument();
  });

  it("opens the review panel with history and duplicates for the Under Review item", async () => {
    vi.stubGlobal("fetch", gateway());
    const user = userEvent.setup();
    render(<ApplicationsScreen title="Buildings · Under Review" initialStatus="UNDER_REVIEW" />);

    expect(screen.getByRole("tab", { name: "Under Review" })).toHaveAttribute("aria-selected", "true");
    await user.click(await screen.findByRole("button", { name: "BA-0002" }));

    const panel = await screen.findByRole("complementary", { name: "Application review" });
    expect(await within(panel).findByText("BLD-7", { exact: false })).toBeInTheDocument();
    expect(within(panel).getByText("Matched on: Name")).toBeInTheDocument();
    expect(within(panel).getByText("Submitted → Under Review")).toBeInTheDocument();
    expect(within(panel).getByRole("button", { name: "Approve" })).toBeInTheDocument();
    expect(within(panel).getByText("No internal notes yet.")).toBeInTheDocument();
  });
});
