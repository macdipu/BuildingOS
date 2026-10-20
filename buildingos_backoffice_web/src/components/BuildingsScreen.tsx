"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { BuildingPanel } from "@/components/BuildingPanel";
import { fieldClass, secondaryButton } from "@/components/formStyles";
import { formatDate, label, pageMeta } from "@/lib/buildingApplications";
import { listBuildings, type ApiResult, type BuildingStatus, type BuildingSummary } from "@/lib/buildings";

interface Loaded {
  key: string;
  result: ApiResult<BuildingSummary[]>;
}

/** BOC-03 Onboarding / Active / Suspended building list (D-34) with a side detail panel. */
export function BuildingsScreen({ title, status }: { title: string; status: BuildingStatus }) {
  const [draft, setDraft] = useState("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${q}#${page}#${version}`;

  useEffect(() => {
    let cancelled = false;
    listBuildings(status, q, page).then((result) => {
      if (!cancelled) setLoaded({ key, result });
    });
    return () => {
      cancelled = true;
    };
  }, [status, q, page, key]);

  const current = loaded && loaded.key.startsWith(`${q}#${page}#`) ? loaded.result : null;
  const meta = current?.ok ? pageMeta(current.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;

  function search(e: React.FormEvent) {
    e.preventDefault();
    setQ(draft);
    setPage(0);
    setSelected(null);
  }

  return (
    <section>
      <h1 className="mb-6 text-[22px] font-semibold">{title}</h1>

      <form onSubmit={search} role="search" aria-label="Search buildings" className="mb-4 flex max-w-md gap-2">
        <input
          aria-label="Building name"
          placeholder="Search by building name"
          className={`${fieldClass} mt-0`}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
        />
        <button type="submit" className={secondaryButton}>
          Search
        </button>
      </form>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
          {!current ? (
            <p className="p-6 text-sm text-on-surface-variant">Loading buildings…</p>
          ) : !current.ok ? (
            <div className="p-4">
              <ApiErrorNotice failure={current} context="plain" />
            </div>
          ) : current.data.length === 0 ? (
            <p className="p-6 text-sm text-on-surface-variant">No {label(status).toLowerCase()} buildings.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Building</th>
                  <th className="px-4 py-3 font-semibold">Type</th>
                  <th className="px-4 py-3 font-semibold">Area / District</th>
                  <th className="px-4 py-3 font-semibold">Updated</th>
                </tr>
              </thead>
              <tbody>
                {current.data.map((b) => (
                  <tr
                    key={b.id}
                    aria-selected={b.id === selected}
                    className={`border-t border-outline-variant ${b.id === selected ? "bg-surface-container" : "hover:bg-surface-low"}`}
                  >
                    <td className="px-4 py-3">
                      <button type="button" onClick={() => setSelected(b.id)} className="font-semibold text-primary hover:underline">
                        {b.name}
                      </button>
                    </td>
                    <td className="px-4 py-3">{label(b.buildingType)}</td>
                    <td className="px-4 py-3">{[b.area, b.district].filter(Boolean).join(", ") || "—"}</td>
                    <td className="px-4 py-3 text-on-surface-variant">{formatDate(b.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {meta && meta.total > meta.size ? (
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm">
              <span className="text-on-surface-variant">
                Page {meta.page + 1} of {lastPage + 1} · {meta.total} buildings
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
          <BuildingPanel buildingId={selected} onChanged={() => setVersion((v) => v + 1)} onClose={() => setSelected(null)} />
        ) : null}
      </div>
    </section>
  );
}
