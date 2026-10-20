import { expect, type Page } from "@playwright/test";

/** Seeded first SUPER_ADMIN (BOS-002 D-02) and the local development OTP. */
export const SEED_PHONE = process.env.E2E_PHONE ?? "01306999005";
export const DEV_OTP = process.env.E2E_OTP ?? "000000";

/** Signs in through the real login form: phone, then the development code. */
export async function signIn(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Phone number").fill(SEED_PHONE);
  await page.getByRole("button", { name: "Send code" }).click();
  await page.getByLabel("Verification code").fill(DEV_OTP);
  await page.getByRole("button", { name: "Verify & sign in" }).click();
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
}
