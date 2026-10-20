import { defineConfig, devices } from "@playwright/test";

/**
 * Browser flows against the running local stack (`make local-up`; web on :3000). The stack is not started
 * here: these tests exercise real services and the seeded SUPER_ADMIN (D-02, dev OTP 000000).
 * Starting an OTP has a 60s per-phone cooldown, so the suite signs in once (auth.setup.ts) and reuses the session.
 */
export const STORAGE_STATE = "e2e/.auth/super-admin.json";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/ },
    {
      name: "chromium",
      testMatch: /.*\.spec\.ts/,
      dependencies: ["setup"],
      use: { ...devices["Desktop Chrome"], storageState: STORAGE_STATE },
    },
  ],
});
