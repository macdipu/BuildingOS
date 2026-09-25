import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { BuildingApplication } from "@/lib/buildingApplications";
import { ReviewActions } from "./ReviewActions";

const base: BuildingApplication = {
  id: "app-1",
  applicationNumber: "BA-0001",
  applicantUserId: "user-1",
  status: "UNDER_REVIEW",
  source: "SELF_SERVICE",
  buildingName: "Lake View",
  buildingType: "RESIDENTIAL",
  address: "Road 1",
  area: "Gulshan",
  district: "Dhaka",
  postalCode: "1212",
  totalFloors: 10,
  estimatedUnits: 40,
  applicantRelationship: "OWNER",
  relationshipNote: null,
  contactName: "Rahim",
  contactPhone: "01711111111",
  contactEmail: null,
  managementType: "SELF_MANAGED",
  latitude: null,
  longitude: null,
  missingFields: [],
  submittedAt: "2026-09-20T10:00:00Z",
  reviewedAt: null,
  reviewedBy: null,
  rejectionReason: null,
  infoRequestMessage: null,
  createdAt: "2026-09-19T10:00:00Z",
  updatedAt: "2026-09-20T10:00:00Z",
};

function respond(status: number, body: unknown) {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

function setup(application: BuildingApplication, fetchMock: ReturnType<typeof vi.fn>) {
  vi.stubGlobal("fetch", fetchMock);
  const onChanged = vi.fn();
  const onRefresh = vi.fn();
  render(<ReviewActions application={application} onChanged={onChanged} onRefresh={onRefresh} />);
  return { onChanged, onRefresh, user: userEvent.setup() };
}

afterEach(() => vi.unstubAllGlobals());

describe("ReviewActions", () => {
  it("starts review on a submitted application", async () => {
    const fetchMock = respond(200, { success: true, data: { ...base, status: "UNDER_REVIEW" }, traceId: "t" });
    const { onChanged, user } = setup({ ...base, status: "SUBMITTED" }, fetchMock);

    await user.click(screen.getByRole("button", { name: "Start Review" }));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/gateway/platform/building-applications/app-1/start-review",
      expect.objectContaining({ method: "POST" }),
    );
    expect(await screen.findByRole("status")).toHaveTextContent("Application moved to Under Review.");
    expect(onChanged).toHaveBeenCalledOnce();
  });

  it("approves with the applicant phone pre-filled as initial admin (D-12)", async () => {
    const fetchMock = respond(200, {
      success: true,
      data: { application: { ...base, status: "APPROVED" }, buildingId: "b-9", buildingStatus: "ONBOARDING" },
      traceId: "t",
    });
    const { onChanged, user } = setup(base, fetchMock);

    await user.click(screen.getByRole("button", { name: "Approve" }));
    expect(screen.getByLabelText(/Initial building admin phone/)).toHaveValue("01711111111");
    await user.type(screen.getByLabelText("Reason"), "Documents verified");
    await user.click(screen.getByRole("button", { name: "Confirm approval" }));

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/gateway/platform/building-applications/app-1/approve");
    expect(JSON.parse(init.body)).toEqual({ adminPhone: "01711111111", reason: "Documents verified" });
    expect(await screen.findByRole("status")).toHaveTextContent("Building b-9 created in Onboarding");
    expect(onChanged).toHaveBeenCalledOnce();
  });

  it("shows the backend error code when a mutation fails", async () => {
    const fetchMock = respond(400, { success: false, code: "INVALID_REQUEST", message: "reason too long", traceId: "t" });
    const { onChanged, user } = setup(base, fetchMock);

    await user.click(screen.getByRole("button", { name: "Reject" }));
    await user.type(screen.getByLabelText("Reason"), "Not eligible");
    await user.click(screen.getByRole("button", { name: "Confirm rejection" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Error code: INVALID_REQUEST");
    expect(alert).toHaveTextContent("reason too long");
    expect(screen.queryByRole("button", { name: "Refresh application" })).toBeNull();
    expect(onChanged).not.toHaveBeenCalled();
  });

  it("offers a refresh when the application changed underneath (409)", async () => {
    const fetchMock = respond(409, { success: false, code: "INVALID_TRANSITION", message: "stale", traceId: "t" });
    const { onRefresh, user } = setup(base, fetchMock);

    await user.click(screen.getByRole("button", { name: "Request Information" }));
    await user.type(screen.getByLabelText("Message to applicant"), "Upload deed");
    await user.click(screen.getByRole("button", { name: "Send request" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("changed since you loaded it");
    expect(screen.getByRole("alert")).toHaveTextContent("Error code: INVALID_TRANSITION");
    await user.click(screen.getByRole("button", { name: "Refresh application" }));
    expect(onRefresh).toHaveBeenCalledOnce();
  });

  it("explains an approval blocked by an unrecorded creation fee", async () => {
    const fetchMock = respond(409, { success: false, code: "CREATION_FEE_UNPAID", message: "fee", traceId: "t" });
    const { user } = setup(base, fetchMock);

    await user.click(screen.getByRole("button", { name: "Approve" }));
    await user.type(screen.getByLabelText("Reason"), "ok");
    await user.click(screen.getByRole("button", { name: "Confirm approval" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("creation fee has not been recorded");
    expect(alert).toHaveTextContent("Error code: CREATION_FEE_UNPAID");
    expect(screen.queryByRole("button", { name: "Refresh application" })).toBeNull();
  });
});
