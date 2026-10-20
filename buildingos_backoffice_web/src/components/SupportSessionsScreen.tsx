"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { StartSupportForm } from "@/components/StartSupportForm";
import { SupportSessionPanel } from "@/components/SupportSessionPanel";
import { primaryButton } from "@/components/formStyles";
import { formatDate, label, pageMeta } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import {
  canStartSupportSession,
  listPendingApprovals,
  listSupportSessions,
  type ApiResult,
  type ElevatedApproval,
  type SupportSession,
} from "@/lib/supportSessions";

/**
 * BOC-07: "active" = Active Support Sessions (start/end, pending approvals for SUPER_ADMIN);
 * "history" = Support History (every session, read-only rows that open the same detail).
 */
export function SupportSessionsScreen({
  title,
  mode,
  userId,
  roles,
}: {
  title: string;
  mode: "active" | "history";
  userId: string;
  roles: PlatformRole[];
}) {
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const [loaded, setLoaded] = useState<{ key: string; result: ApiResult<SupportSession[]> } | null>(null);
  const [approvals, setApprovals] = useState<ApiResult<ElevatedApproval[]> | null>(null);
  const key = `${mode}#${page}#${version}`;
  const superAdmin = roles.includes("SUPER_ADMIN");

  useEffect(() => {
    let cancelled = false;
    listSupportSessions({ active: mode === "active" ? true : undefined, page }).then((result) => {
      if (!cancelled) setLoaded({ key, result });
    });
    return () => {
      cancelled = true;
    };
  }, [mode, page, key]);

  useEffect(() => {
    if (mode !== "active" || !superAdmin) return;
    let cancelled = false;
    listPendingApprovals().then((r) => {
      if (!cancelled) setApprovals(r);
    });
    return () => {
      cancelled = true;
    };
  }, [mode, superAdmin, version]);

  const current = loaded?.key === key ? loaded.result : null;
  const meta = current?.ok ? pageMeta(current.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-[22px] font-semibold">{title}</h1>
        {mode === "active" && canStartSupportSession(roles) ? (
          <button type="button" className={primaryButton} onClick={() => setStarting((s) => !s)}>
            Start support session
          </button>
        ) : null}
      </div>

      {starting ? (
        <StartSupportForm
          onStarted={(s) => {
            setStarting(false);
            setSelected(s.id);
            setVersion((v) => v + 1);
          }}
          onCancel={() => setStarting(false)}
        />
      ) : null}

      {mode === "active" && superAdmin && approvals?.ok && approvals.data.length > 0 ? (
        <section aria-label="Pending elevated approvals" className="rounded-panel border border-error/30 bg-error-container p-4 text-sm">
          <h2 className="mb-2 font-semibold">Pending elevated approvals</h2>
          <ul className="space-y-1">
            {approvals.data.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-2">
                <span>{label(a.requestedScope.replace("SUPPORT_", ""))}</span>
                <button type="button" className="font-semibold text-primary hover:underline" onClick={() => setSelected(a.supportSessionId)}>
                  Review session
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
          {!current ? (
            <p className="p-6 text-sm text-on-surface-variant">Loading sessions…</p>
          ) : !current.ok ? (
            <div className="p-4">
              <ApiErrorNotice failure={current} context="plain" />
            </div>
          ) : current.data.length === 0 ? (
            <p className="p-6 text-sm text-on-surface-variant">
              {mode === "active" ? "No active support sessions." : "No support sessions yet."}
            </p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Session</th>
                  <th className="px-4 py-3 font-semibold">Agent</th>
                  <th className="px-4 py-3 font-semibold">Target</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">{mode === "active" ? "Expires" : "Started"}</th>
                </tr>
              </thead>
              <tbody>
                {current.data.map((s) => (
                  <tr key={s.id} aria-selected={s.id === selected} className={`border-t border-outline-variant ${s.id === selected ? "bg-surface-container" : "hover:bg-surface-low"}`}>
                    <td className="px-4 py-3">
                      <button type="button" onClick={() => setSelected(s.id)} className="font-mono text-xs font-semibold text-primary hover:underline">
                        {s.id.slice(0, 8)}
                      </button>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs">{s.platformUserId}</td>
                    <td className="px-4 py-3 font-mono text-xs">{s.targetUserId ?? s.buildingId ?? "—"}</td>
                    <td className="px-4 py-3">{label(s.status)}</td>
                    <td className="px-4 py-3 text-on-surface-variant">{formatDate(mode === "active" ? s.expiresAt : s.startedAt)}</td>
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

        {selected ? (
          <SupportSessionPanel
            key={selected}
            sessionId={selected}
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
