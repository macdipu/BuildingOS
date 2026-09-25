import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { visibleNav } from "@/lib/nav";
import { Rail } from "./Rail";

vi.mock("next/navigation", () => ({ usePathname: () => "/system/audit-logs" }));

describe("Rail", () => {
  it("renders the visible §149.2 tree and marks the current page", () => {
    render(<Rail sections={visibleNav(["SUPER_ADMIN"])} />);
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Applications" })).toHaveAttribute("href", "/buildings/applications");
    expect(screen.getByRole("link", { name: "Audit Logs" })).toHaveAttribute("aria-current", "page");
  });

  it("hides sections the roles cannot see", () => {
    render(<Rail sections={visibleNav(["SUBSCRIPTION_ADMIN"])} />);
    expect(screen.queryByText("Buildings")).toBeNull();
    expect(screen.getByRole("link", { name: "Plans" })).toBeInTheDocument();
  });
});
