"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { ApplicationReviewPanel } from "@/components/ApplicationReviewPanel";
import {
  REVIEW_STATUSES,
  STATUS_LABEL,
  formatDate,
  label,
  listApplications,
  pageMeta,
  type ApiResult,
  type BuildingApplication,
  type ReviewStatus,
} from "@/lib/buildingApplications";

interface Loaded {
  key: string;
  result: ApiResult<BuildingApplication[]>;
}

/** BOC-03 review queue: §149.4 status tabs, application table and side review panel. */
export function ApplicationsScreen({ title, initialStatus }: { title: string; initialStatus: ReviewStatus }) {
  const [status, setStatus] = useState<ReviewStatus>(initialStatus);
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${status}#${page}#${version}`;

  useEffect(() => {
    let cancelled = false;
    listApplications(status, page).then((result) => {
      if (!cancelled) setLoaded({ key, result });
    });
    return () => {
      cancelled = true;
    };
  }, [status, page, key]);

  const current = loaded && loaded.key.startsWith(`${status}#${page}#`) ? loaded.result : null;
  const meta = current?.ok ? pageMeta(current.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;

  function selectTab(next: ReviewStatus) {
    setStatus(next);
    setPage(0);
    setSelected(null);
  }

  return (
    <section>
      <h1 className="mb-6 text-[22px] font-semibold">{title}</h1>

      <div role="tablist" aria-label="Application status" className="mb-4 flex flex-wrap gap-1 border-b border-outline-variant">
        {REVIEW_STATUSES.map((s) => (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={s === status}
            onClick={() => selectTab(s)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-semibold ${
              s === status
                ? "border-primary text-primary"
                : "border-transparent text-on-surface-variant hover:text-on-surface"
            }`}
          >
            {STATUS_LABEL[s]}
          </button>
        ))}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
          {!current ? (
            <p className="p-6 text-sm text-on-surface-variant">Loading applications…</p>
          ) : !current.ok ? (
            <div className="p-4">
              <ApiErrorNotice failure={current} />
            </div>
          ) : current.data.length === 0 ? (
            <p className="p-6 text-sm text-on-surface-variant">No {label(status).toLowerCase()} applications.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Application</th>
                  <th className="px-4 py-3 font-semibold">Building</th>
                  <th className="px-4 py-3 font-semibold">Area / District</th>
                  <th className="px-4 py-3 font-semibold">Applicant</th>
                  <th className="px-4 py-3 font-semibold">Submitted</th>
                </tr>
              </thead>
              <tbody>
                {current.data.map((a) => (
                  <tr
                    key={a.id}
                    aria-selected={a.id === selected}
                    className={`border-t border-outline-variant ${a.id === selected ? "bg-surface-container" : "hover:bg-surface-low"}`}
                  >
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => setSelected(a.id)}
                        className="font-semibold text-primary hover:underline"
                      >
                        {a.applicationNumber}
                      </button>
                    </td>
                    <td className="px-4 py-3">{a.buildingName ?? "—"}</td>
                    <td className="px-4 py-3">{[a.area, a.district].filter(Boolean).join(", ") || "—"}</td>
                    <td className="px-4 py-3">{a.contactName ?? "—"}</td>
                    <td className="px-4 py-3 text-on-surface-variant">{formatDate(a.submittedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {meta && meta.total > meta.size ? (
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm">
              <span className="text-on-surface-variant">
                Page {meta.page + 1} of {lastPage + 1} · {meta.total} applications
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  type="button"
                  disabled={page >= lastPage}
                  onClick={() => setPage((p) => p + 1)}
                  className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          ) : null}
        </div>

        {selected ? (
          <ApplicationReviewPanel
            applicationId={selected}
            onChanged={() => setVersion((v) => v + 1)}
            onClose={() => setSelected(null)}
          />
        ) : null}
      </div>
    </section>
  );
}
