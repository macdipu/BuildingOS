"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { OnboardingSessionPanel } from "@/components/OnboardingSessionPanel";
import { StartOnboardingForm } from "@/components/StartOnboardingForm";
import { fieldClass, primaryButton } from "@/components/formStyles";
import { formatDate, label, pageMeta } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import {
  SESSION_STATUSES,
  canStartSession,
  listSessions,
  type ApiResult,
  type OnboardingSession,
} from "@/lib/onboardingSessions";

/** BOC-06 Support > Assisted Onboarding: filterable session list, start form (operators), detail with actions. */
export function OnboardingSessionsScreen({
  title,
  userId,
  roles,
  initialBuildingId = "",
}: {
  title: string;
  userId: string;
  roles: PlatformRole[];
  /** From "Assign onboarding agent" on a building or an approved application: opens the form prefilled. */
  initialBuildingId?: string;
}) {
  const [status, setStatus] = useState("");
  const [buildingId, setBuildingId] = useState("");
  const [agentUserId, setAgentUserId] = useState("");
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [starting, setStarting] = useState(initialBuildingId !== "" && canStartSession(roles));
  const [loaded, setLoaded] = useState<{ key: string; result: ApiResult<OnboardingSession[]> } | null>(null);
  const key = `${status}#${buildingId}#${agentUserId}#${page}#${version}`;

  useEffect(() => {
    let cancelled = false;
    listSessions({ status, buildingId, agentUserId, page }).then((result) => {
      if (!cancelled) setLoaded({ key, result });
    });
    return () => {
      cancelled = true;
    };
  }, [status, buildingId, agentUserId, page, key]);

  const current = loaded?.key === key ? loaded.result : null;
  const meta = current?.ok ? pageMeta(current.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;
  // Keep the last loaded session on screen while a refetch runs, so the panel and its result message survive.
  const last = loaded?.result;
  const selectedSession = last?.ok ? last.data.find((s) => s.id === selected) : undefined;

  function filter<T>(set: (v: T) => void) {
    return (v: T) => {
      set(v);
      setPage(0);
      setSelected(null);
    };
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-[22px] font-semibold">{title}</h1>
        {canStartSession(roles) ? (
          <button type="button" className={primaryButton} onClick={() => setStarting((s) => !s)}>
            Assign onboarding agent
          </button>
        ) : null}
      </div>

      {starting ? (
        <StartOnboardingForm
          initialBuildingId={initialBuildingId}
          onStarted={(s) => {
            setStarting(false);
            setSelected(s.id);
            setVersion((v) => v + 1);
          }}
          onCancel={() => setStarting(false)}
        />
      ) : null}

      <form aria-label="Filter sessions" className="flex flex-wrap gap-2" onSubmit={(e) => e.preventDefault()}>
        <label className="text-sm font-semibold">
          Status
          <select className={fieldClass} value={status} onChange={(e) => filter(setStatus)(e.target.value)}>
            <option value="">All</option>
            {SESSION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {label(s)}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-semibold">
          Building id
          <input className={fieldClass} value={buildingId} onChange={(e) => filter(setBuildingId)(e.target.value.trim())} />
        </label>
        <label className="text-sm font-semibold">
          Agent user id
          <input className={fieldClass} value={agentUserId} onChange={(e) => filter(setAgentUserId)(e.target.value.trim())} />
        </label>
      </form>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
          {!current ? (
            <p className="p-6 text-sm text-on-surface-variant">Loading sessions…</p>
          ) : !current.ok ? (
            <div className="p-4">
              <ApiErrorNotice failure={current} context="plain" />
            </div>
          ) : current.data.length === 0 ? (
            <p className="p-6 text-sm text-on-surface-variant">No onboarding sessions.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Building</th>
                  <th className="px-4 py-3 font-semibold">Agent</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Expires</th>
                </tr>
              </thead>
              <tbody>
                {current.data.map((s) => (
                  <tr
                    key={s.id}
                    aria-selected={s.id === selected}
                    className={`border-t border-outline-variant ${s.id === selected ? "bg-surface-container" : "hover:bg-surface-low"}`}
                  >
                    <td className="px-4 py-3">
                      <button type="button" onClick={() => setSelected(s.id)} className="font-mono text-xs font-semibold text-primary hover:underline">
                        {s.buildingId}
                      </button>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs">{s.assignedAgentUserId}</td>
                    <td className="px-4 py-3">{label(s.status)}</td>
                    <td className="px-4 py-3 text-on-surface-variant">{formatDate(s.expiresAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {meta && meta.total > meta.size ? (
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm">
              <span className="text-on-surface-variant">
                Page {meta.page + 1} of {lastPage + 1} · {meta.total} sessions
              </span>
              <div className="flex gap-2">
                <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50">
                  Previous
                </button>
                <button type="button" disabled={page >= lastPage} onClick={() => setPage((p) => p + 1)} className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50">
                  Next
                </button>
              </div>
            </div>
          ) : null}
        </div>

        {selectedSession ? (
          <OnboardingSessionPanel
            key={selectedSession.id}
            session={selectedSession}
            userId={userId}
            roles={roles}
            onChanged={() => setVersion((v) => v + 1)}
            onClose={() => setSelected(null)}
          />
        ) : null}
      </div>
    </section>
  );
}
