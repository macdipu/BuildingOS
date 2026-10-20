"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { panelClass, secondaryButton } from "@/components/formStyles";
import { formatDate } from "@/lib/buildingApplications";
import { getSystemHealth, type ApiResult, type SystemHealth } from "@/lib/system";

const BADGE: Record<string, string> = {
  UP: "bg-success-container text-on-success-container",
  DEGRADED: "bg-error-container",
  DOWN: "bg-error-container",
  UNKNOWN: "bg-surface-container",
};

/** BOC-08 System Health: each service's readiness, latency and when it was checked. */
export function SystemHealthScreen({ title }: { title: string }) {
  const [version, setVersion] = useState(0);
  const [loaded, setLoaded] = useState<{ version: number; result: ApiResult<SystemHealth> } | null>(null);

  useEffect(() => {
    let cancelled = false;
    getSystemHealth().then((result) => {
      if (!cancelled) setLoaded({ version, result });
    });
    return () => {
      cancelled = true;
    };
  }, [version]);

  const current = loaded?.result ?? null;
  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-[22px] font-semibold">{title}</h1>
        <button type="button" className={secondaryButton} onClick={() => setVersion((v) => v + 1)}>
          Refresh
        </button>
      </div>
      {!current ? (
        <p className="text-sm text-on-surface-variant">Checking services…</p>
      ) : !current.ok ? (
        <ApiErrorNotice failure={current} context="plain" />
      ) : (
        <>
          <div className={`${panelClass} flex flex-wrap items-center justify-between gap-3`}>
            <p className="text-sm">
              Overall{" "}
              <span className={`ml-1 rounded-control px-2 py-0.5 text-xs font-semibold ${BADGE[current.data.overall]}`}>
                {current.data.overall}
              </span>
            </p>
            <p className="text-xs text-on-surface-variant">Last checked {formatDate(current.data.checkedAt)}</p>
          </div>
          <div className="rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Service</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">HTTP</th>
                  <th className="px-4 py-3 font-semibold">Latency</th>
                </tr>
              </thead>
              <tbody>
                {current.data.services.map((s) => (
                  <tr key={s.name} className="border-t border-outline-variant">
                    <td className="px-4 py-3 font-semibold">{s.name}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-control px-2 py-0.5 text-xs font-semibold ${BADGE[s.status]}`}>{s.status}</span>
                    </td>
                    <td className="px-4 py-3">{s.httpStatus ?? "—"}</td>
                    <td className="px-4 py-3">{s.latencyMs} ms</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}
