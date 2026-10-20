import { expect, test } from "@playwright/test";
import { configureCreationFee, gateway, seedSubmittedApplication, uniquePhone, userIdByPhone } from "./api";

/** One building carried through the whole back-office path: review → approve → agent role → onboarding session. */
test.describe.serial("application review to onboarding session", () => {
  const phone = uniquePhone();
  const name = `E2E Tower ${Date.now()}`;

  test("review and approve an application with the recorded creation fee", async ({ page }) => {
    const api = await gateway();
    await configureCreationFee(api);
    await seedSubmittedApplication(api, name, phone);

    await page.goto("/buildings/applications");
    const row = page.getByRole("row", { name: new RegExp(name) });
    await row.getByRole("button").click();
    const panel = page.getByRole("complementary").or(page.getByRole("region", { name: "Review actions" }));
    await page.getByRole("button", { name: "Start Review" }).click();
    await expect(page.getByRole("status")).toContainText("Under Review");

    await page.goto("/buildings/under-review");
    await page.getByRole("row", { name: new RegExp(name) }).getByRole("button").click();
    await page.getByRole("button", { name: "Approve" }).click();
    await expect(page.getByLabel(/Initial building admin phone/)).toHaveValue(phone);
    await page.getByLabel("Reason").fill("Documents verified");
    await page.getByRole("button", { name: "Confirm approval" }).click();
    await expect(page.getByRole("status")).toContainText("created in Onboarding");
    await expect(page.getByRole("link", { name: "Assign onboarding agent" })).toBeVisible();
    void panel;
  });

  test("grant the initial admin the onboarding agent role", async ({ page }) => {
    await page.goto("/users/platform-users");
    await page.getByLabel("Phone").fill(phone);
    await page.getByRole("button", { name: "Search" }).click();
    await page.getByRole("button", { name: phone }).click();
    const panel = page.getByRole("complementary", { name: "User detail" });
    await panel.getByLabel(/^Assign role/).selectOption("ONBOARDING_AGENT");
    await panel.getByRole("button", { name: "Assign role" }).click();
    await panel.getByRole("button", { name: "Confirm" }).click();
    await expect(panel.getByText("Onboarding Agent assigned.")).toBeVisible();
  });

  test("assign the agent to the new building and complete the session", async ({ page }) => {
    const api = await gateway();
    const agentId = await userIdByPhone(api, phone);

    await page.goto("/buildings/onboarding");
    await page.getByRole("button", { name }).click();
    await page.getByRole("link", { name: "Assign onboarding agent" }).click();

    const form = page.getByRole("form", { name: "Assign onboarding agent" });
    await expect(form).toBeVisible();
    await form.locator("input").nth(1).fill(agentId);
    await form.getByRole("checkbox", { name: "View building" }).check();
    await form.locator("textarea").first().fill("E2E assisted onboarding");
    const at = new Date(Date.now() + 2 * 86_400_000);
    const p = (n: number) => String(n).padStart(2, "0");
    await form.locator('input[type="datetime-local"]').fill(`${at.getFullYear()}-${p(at.getMonth() + 1)}-${p(at.getDate())}T${p(at.getHours())}:${p(at.getMinutes())}`);
    await form.getByRole("button", { name: "Start session" }).click();

    const detail = page.getByRole("complementary", { name: "Onboarding session detail" });
    await expect(detail.getByText("Assigned")).toBeVisible();
    await detail.getByRole("button", { name: "Complete" }).click();
    await expect(detail.getByRole("status")).toContainText("completed");
    await expect(detail.getByText("This session has ended and is read-only.")).toBeVisible();
  });

  test("the audit log shows the role grant and the session transitions", async ({ page }) => {
    await page.goto("/system/audit-logs");
    await expect(page.getByText("ASSISTED_ONBOARDING_SESSION").first()).toBeVisible();
    await expect(page.getByText("PLATFORM_ROLE").first()).toBeVisible();
  });
});
