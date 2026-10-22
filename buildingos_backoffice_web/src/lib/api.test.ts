// @vitest-environment node
import { afterEach, describe, expect, it, vi } from "vitest";
import { call } from "./api";

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status });

afterEach(() => vi.unstubAllGlobals());

describe("call", () => {
  it("sends the browser back to /login on a 401", async () => {
    const assign = vi.fn();
    vi.stubGlobal("window", { location: { assign } });
    vi.stubGlobal("fetch", vi.fn(async () => json(401, { success: false, code: "UNAUTHENTICATED" })));
    const r = await call("platform/x");
    expect(r).toEqual({ ok: false, status: 401, code: "UNAUTHENTICATED", message: null });
    expect(assign).toHaveBeenCalledWith("/login");
  });

  it("does not redirect on other failures or without a window", async () => {
    const assign = vi.fn();
    vi.stubGlobal("window", { location: { assign } });
    vi.stubGlobal("fetch", vi.fn(async () => json(403, { success: false, code: "FORBIDDEN" })));
    expect((await call("platform/x")).ok).toBe(false);
    expect(assign).not.toHaveBeenCalled();

    vi.unstubAllGlobals();
    vi.stubGlobal("fetch", vi.fn(async () => json(401, { success: false, code: "UNAUTHENTICATED" })));
    expect(await call("platform/x")).toMatchObject({ ok: false, status: 401 });
  });
});
