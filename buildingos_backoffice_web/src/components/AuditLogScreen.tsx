"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { fieldClass, fieldError, panelClass, secondaryButton } from "@/components/formStyles";
import { formatDate } from "@/lib/buildingApplications";
import { UUID_PATTERN } from "@/lib/subscriptions";
import {
  AUDIT_LIMITS,
  AUDIT_SOURCES,
  listAudit,
  toInstant,
  type ApiFailure,
  type AuditEvent,
  type AuditFilters,
  type AuditLogPage,
} from "@/lib/system";

interface Draft {
  since: string;
  until: string;
  source: string;
  entityType: string;
  actorUserId: string;
  limit: string;
}

function cursor(page: AuditLogPage) {
  return page.nextUntil ? { until: page.nextUntil, untilId: page.nextUntilId ?? undefined } : null;
}

const EMPTY: Draft = { since: "", until: "", source: "", entityType: "", actorUserId: "", limit: "50" };

/**
 * BOC-08 Audit Logs (read-only): filters narrow the merged log server-side. Older pages are
 * fetched with the API's (nextUntil, nextUntilId) keyset cursor. Sources that did not answer are named, never hidden.
 */
export function AuditLogScreen({ title, initialEntityType = "" }: { title: string; initialEntityType?: string }) {
  const [draft, setDraft] = useState<Draft>({ ...EMPTY, entityType: initialEntityType });
  const [applied, setApplied] = useState<AuditFilters>(() => (initialEntityType ? { entityType: initialEntityType, limit: 50 } : { limit: 50 }));
  const [actorError, setActorError] = useState<string | null>(null);
  const [events, setEvents] = useState<AuditEvent[] | null>(null);
  const [unavailable, setUnavailable] = useState<string[]>([]);
  const [next, setNext] = useState<{ until: string; untilId: string | undefined } | null>(null);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [loading, setLoading] = useState(false);
  const [appliedKey, setAppliedKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    listAudit(applied).then((r) => {
      if (cancelled) return;
      setLoading(false);
      if (r.ok) {
        setEvents(r.data.items);
        setUnavailable(r.data.unavailableSources);
        setNext(cursor(r.data));
        setFailure(null);
      } else {
        setFailure(r);
        setEvents(null);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [applied, appliedKey]);

  function apply(e: React.FormEvent) {
    e.preventDefault();
    const actor = draft.actorUserId.trim();
    if (actor && !UUID_PATTERN.test(actor)) {
      setActorError("Enter a user id (UUID).");
      return;
    }
    setActorError(null);
    setLoading(true);
    setEvents(null);
    setApplied({
      since: toInstant(draft.since),
      until: toInstant(draft.until),
      source: draft.source || undefined,
      entityType: draft.entityType.trim() || undefined,
      actorUserId: actor || undefined,
      limit: Number(draft.limit),
    });
    setAppliedKey((k) => k + 1);
  }

  async function older() {
    if (!next) return;
    setLoading(true);
    const r = await listAudit({ ...applied, ...next });
    setLoading(false);
    if (r.ok) {
      setEvents((cur) => [...(cur ?? []), ...r.data.items]);
      setUnavailable((cur) => [...new Set([...cur, ...r.data.unavailableSources])]);
      setNext(cursor(r.data));
    } else setFailure(r);
  }

  const set = (k: keyof Draft) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setDraft((d) => ({ ...d, [k]: e.target.value }));

  return (
    <section className="space-y-4">
      <h1 className="text-[22px] font-semibold">{title}</h1>

      <form onSubmit={apply} noValidate aria-label="Filter audit log" className={`${panelClass} grid gap-3 md:grid-cols-3`}>
        <label className="text-sm font-semibold">
          From
          <input type="datetime-local" className={fieldClass} value={draft.since} onChange={set("since")} />
        </label>
        <label className="text-sm font-semibold">
          To
          <input type="datetime-local" className={fieldClass} value={draft.until} onChange={set("until")} />
        </label>
        <label className="text-sm font-semibold">
          Module
          <select className={fieldClass} value={draft.source} onChange={set("source")}>
            <option value="">All modules</option>
            {AUDIT_SOURCES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-semibold">
          Entity type
          <input className={fieldClass} value={draft.entityType} onChange={set("entityType")} placeholder="e.g. SUPPORT_SESSION" />
        </label>
        <label className="text-sm font-semibold">
          User id
          <input className={fieldClass} value={draft.actorUserId} onChange={set("actorUserId")} />
          {actorError ? <span className={fieldError}>{actorError}</span> : null}
        </label>
        <label className="text-sm font-semibold">
          Rows
          <select className={fieldClass} value={draft.limit} onChange={set("limit")}>
            {AUDIT_LIMITS.map((l) => (
              <option key={l} value={l}>
                {l}
              </option>
            ))}
          </select>
        </label>
        <div className="md:col-span-3">
          <button type="submit" className={secondaryButton}>
            Apply filters
          </button>
        </div>
      </form>

      {unavailable.length > 0 ? (
        <p role="status" className="rounded-card border border-error/30 bg-error-container px-4 py-3 text-sm">
          Not included: {unavailable.join(", ")} did not respond. Results may be incomplete.
        </p>
      ) : null}
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}

      <div className="rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
        {!events ? (
          failure ? null : <p className="p-6 text-sm text-on-surface-variant">Loading audit log…</p>
        ) : events.length === 0 ? (
          <p className="p-6 text-sm text-on-surface-variant">No audit events match these filters.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Time</th>
                  <th className="px-4 py-3 font-semibold">Actor</th>
                  <th className="px-4 py-3 font-semibold">Role</th>
                  <th className="px-4 py-3 font-semibold">Action</th>
                  <th className="px-4 py-3 font-semibold">Entity</th>
                  <th className="px-4 py-3 font-semibold">Module</th>
                  <th className="px-4 py-3 font-semibold">Reason</th>
                </tr>
              </thead>
              <tbody>
                {events.map((ev) => (
                  <tr key={`${ev.source}-${ev.id}`} className="border-t border-outline-variant align-top">
                    <td className="whitespace-nowrap px-4 py-3 text-on-surface-variant">{formatDate(ev.occurredAt)}</td>
                    <td className="px-4 py-3 font-mono text-xs">{ev.actorUserId ?? "System"}</td>
                    <td className="px-4 py-3">{ev.role ?? "—"}</td>
                    <td className="px-4 py-3">{ev.action}</td>
                    <td className="px-4 py-3">
                      {ev.entityType}
                      <span className="block break-all font-mono text-xs text-on-surface-variant">{ev.entityId}</span>
                    </td>
                    <td className="px-4 py-3">{ev.source}</td>
                    <td className="px-4 py-3">{ev.reason ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {next ? (
          <div className="border-t border-outline-variant px-4 py-3">
            <button type="button" disabled={loading} className={secondaryButton} onClick={older}>
              Load older events
            </button>
          </div>
        ) : null}
      </div>
      <p className="text-xs text-on-surface-variant">
        Before/after values are not recorded in the audit sources; each row shows the transition (action), actor and reason.
      </p>
    </section>
  );
}
