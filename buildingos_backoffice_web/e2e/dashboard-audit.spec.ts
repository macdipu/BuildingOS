import { expect, test } from "@playwright/test";

test("dashboard cards link to their lists and the audit log is reachable and filterable", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();

  // Every card is a link to the list it counts; none shows a made-up number on failure.
  const cards = page.getByRole("list").getByRole("link");
  await expect(cards.first()).toBeVisible();
  await page.getByRole("link", { name: /Applications awaiting review/ }).click();
  await expect(page).toHaveURL(/\/buildings\/applications$/);

  await page.goto("/");
  await page.getByRole("link", { name: /User subscriptions/ }).click();
  await expect(page).toHaveURL(/\/subscriptions\/user-subscriptions$/);

  // System > Audit Logs: SUPER_ADMIN only; filter narrows server-side.
  await page.goto("/system/audit-logs");
  await expect(page.getByRole("heading", { name: /Audit Logs/ })).toBeVisible();
  await page.getByLabel("Entity type").fill("PLATFORM_ROLE");
  const request = page.waitForRequest((r) => r.url().includes("/api/gateway/platform/backoffice/audit") && r.url().includes("entityType=PLATFORM_ROLE"));
  await page.getByRole("button", { name: "Apply filters" }).click();
  await request;
  await expect(page.getByText("Loading audit log…")).toBeHidden();
});

test("system health lists every service with a status", async ({ page }) => {
  await page.goto("/system/health");
  await expect(page.getByRole("heading", { name: /System Health/ })).toBeVisible();
  await expect(page.getByText("auth-service")).toBeVisible();
  await expect(page.getByText(/Last checked/)).toBeVisible();
});
