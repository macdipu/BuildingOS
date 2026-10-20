import { test as setup } from "@playwright/test";
import { STORAGE_STATE } from "../playwright.config";
import { signIn } from "./session";

setup("sign in as the seeded SUPER_ADMIN", async ({ page }) => {
  await signIn(page);
  await page.context().storageState({ path: STORAGE_STATE });
});
