"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { formatDate, label, pageMeta } from "@/lib/buildingApplications";
import {
  ENTITLEMENTS,
  UUID_PATTERN,
  assignPlan,
  entitlementValue,
  getUserSubscription,
  listPlans,
  listPlatformSubscriptions,
  type ApiFailure,
  type ApiResult,
  type Plan,
  type PlatformSubscription,
  type UserSubscription,
} from "@/lib/subscriptions";
import { fieldClass, fieldError, panelClass, primaryButton, secondaryButton, successClass } from "./formStyles";

const KIND = new Map<string, (typeof ENTITLEMENTS)[number]["kind"]>(ENTITLEMENTS.map((e) => [e.key, e.kind]));
const LABEL = new Map<string, string>(ENTITLEMENTS.map((e) => [e.key, e.label]));

/**
 * BOC-04 Subscriptions > User Subscriptions: look up one user's subscription by
 * user id and assign a plan, plus the platform-wide list (D-35).
 */
export function UserSubscriptionScreen({ title }: { title: string }) {
  const [plans, setPlans] = useState<ApiResult<Plan[]> | null>(null);
  const [userId, setUserId] = useState("");
  const [inputError, setInputError] = useState<string | null>(null);
  const [lookup, setLookup] = useState<{ userId: string; result: ApiResult<UserSubscription> } | null>(null);
  const [busy, setBusy] = useState(false);
  const [page, setPage] = useState(0);
  const [list, setList] = useState<{ page: number; result: ApiResult<PlatformSubscription[]> } | null>(null);

  useEffect(() => {
    let cancelled = false;
    listPlatformSubscriptions(page).then((result) => {
      if (!cancelled) setList({ page, result });
    });
    return () => {
      cancelled = true;
    };
  }, [page]);

  useEffect(() => {
    let cancelled = false;
    listPlans().then((r) => {
      if (!cancelled) setPlans(r);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function find(e: React.FormEvent) {
    e.preventDefault();
    await lookUp(userId.trim());
  }

  async function lookUp(id: string) {
    if (!UUID_PATTERN.test(id)) {
      setInputError("Enter a user id (UUID).");
      return;
    }
    setInputError(null);
    setBusy(true);
    try {
      setLookup({ userId: id, result: await getUserSubscription(id) });
    } finally {
      setBusy(false);
    }
  }

  const planList = plans?.ok ? plans.data : [];
  const planName = (id: string) => planList.find((p) => p.id === id)?.name ?? id;

  return (
    <section className="space-y-6">
      <h1 className="text-[22px] font-semibold">{title}</h1>

      <form onSubmit={find} noValidate aria-label="Find user subscription" className={`${panelClass} flex flex-wrap items-end gap-2`}>
        <label className="block min-w-64 flex-1 text-sm font-semibold">
          User id
          <input className={fieldClass} value={userId} onChange={(e) => setUserId(e.target.value)} />
          {inputError ? <span className={fieldError}>{inputError}</span> : null}
        </label>
        <button type="submit" className={secondaryButton} disabled={busy}>
          Look up
        </button>
      </form>

      <SubscriptionList
        state={list && list.page === page ? list.result : null}
        page={page}
        setPage={setPage}
        onPick={(id) => {
          setUserId(id);
          void lookUp(id);
        }}
      />

      {lookup ? (
        <div className="grid gap-6 xl:grid-cols-2">
          <section aria-label="Current subscription" className={panelClass}>
            <h2 className="mb-3 text-base font-semibold">Current subscription</h2>
            {lookup.result.ok ? (
              <SubscriptionDetails subscription={lookup.result.data} planName={planName} />
            ) : (
              <ApiErrorNotice failure={lookup.result} context="plain" />
            )}
          </section>
          <AssignPlan
            key={lookup.userId}
            userId={lookup.userId}
            plans={plans}
            onAssigned={(s) => setLookup({ userId: lookup.userId, result: { ok: true, data: s } })}
          />
        </div>
      ) : null}
    </section>
  );
}

function SubscriptionDetails({ subscription: s, planName }: { subscription: UserSubscription; planName: (id: string) => string }) {
  const entries = Object.entries(s.effectiveEntitlements ?? {});
  return (
    <div className="space-y-3 text-sm">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-1">
        <dt className="text-on-surface-variant">Plan</dt>
        <dd className="font-semibold">{planName(s.planId)}</dd>
        <dt className="text-on-surface-variant">Status</dt>
        <dd>{label(s.status)}</dd>
        <dt className="text-on-surface-variant">Billing cycle</dt>
        <dd>{label(s.billingCycle)}</dd>
        <dt className="text-on-surface-variant">Granted by</dt>
        <dd>{label(s.grantedBy)}</dd>
        <dt className="text-on-surface-variant">Started</dt>
        <dd>{formatDate(s.startedAt)}</dd>
      </dl>
      <div className="border-t border-outline-variant pt-3">
        <h3 className="mb-1 font-semibold">Effective entitlements</h3>
        {entries.length === 0 ? (
          <p className="text-on-surface-variant">None.</p>
        ) : (
          <dl className="space-y-1">
            {entries.map(([k, v]) => (
              <div key={k} className="flex justify-between gap-3">
                <dt>{LABEL.get(k) ?? k}</dt>
                <dd className="font-semibold">{entitlementValue(KIND.get(k), v)}</dd>
              </div>
            ))}
          </dl>
        )}
      </div>
    </div>
  );
}

function AssignPlan({
  userId,
  plans,
  onAssigned,
}: {
  userId: string;
  plans: ApiResult<Plan[]> | null;
  onAssigned: (s: UserSubscription) => void;
}) {
  const [planId, setPlanId] = useState("");
  const [cycle, setCycle] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);

  const active = plans?.ok ? plans.data.filter((p) => p.status === "ACTIVE") : [];
  const selected = active.find((p) => p.id === planId);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    setDone(null);
    const next: Record<string, string> = {};
    if (!selected) next.planId = "Choose a plan.";
    if (!cycle) next.cycle = "Choose a billing cycle.";
    setErrors(next);
    if (Object.keys(next).length || !selected) return;
    setBusy(true);
    try {
      const r = await assignPlan(userId, selected.id, cycle);
      if (r.ok) {
        setDone(`${selected.name} assigned.`);
        onAssigned(r.data);
      } else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-label="Assign plan" className={panelClass}>
      <h2 className="mb-3 text-base font-semibold">Assign plan</h2>
      {!plans ? (
        <p className="text-sm text-on-surface-variant">Loading plans…</p>
      ) : !plans.ok ? (
        <ApiErrorNotice failure={plans} context="plain" />
      ) : (
        <form onSubmit={submit} noValidate className="space-y-3">
          <label className="block text-sm font-semibold">
            Plan
            <select
              className={fieldClass}
              value={planId}
              onChange={(e) => {
                setPlanId(e.target.value);
                setCycle("");
              }}
            >
              <option value="">Select a plan</option>
              {active.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.code})
                </option>
              ))}
            </select>
            {errors.planId ? <span className={fieldError}>{errors.planId}</span> : null}
          </label>
          <label className="block text-sm font-semibold">
            Billing cycle
            <select className={fieldClass} value={cycle} onChange={(e) => setCycle(e.target.value)} disabled={!selected}>
              <option value="">Select a cycle</option>
              {(selected?.billingCycles ?? []).map((c) => (
                <option key={c} value={c}>
                  {label(c)}
                </option>
              ))}
            </select>
            {errors.cycle ? <span className={fieldError}>{errors.cycle}</span> : null}
          </label>
          {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
          {done ? (
            <p role="status" className={successClass}>
              {done}
            </p>
          ) : null}
          <button type="submit" className={primaryButton} disabled={busy}>
            Assign plan
          </button>
        </form>
      )}
    </section>
  );
}

function SubscriptionList({
  state,
  page,
  setPage,
  onPick,
}: {
  state: ApiResult<PlatformSubscription[]> | null;
  page: number;
  setPage: (fn: (p: number) => number) => void;
  onPick: (userId: string) => void;
}) {
  const meta = state?.ok ? pageMeta(state.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;
  return (
    <section aria-label="User subscriptions" className="rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
      {!state ? (
        <p className="p-6 text-sm text-on-surface-variant">Loading subscriptions…</p>
      ) : !state.ok ? (
        <div className="p-4">
          <ApiErrorNotice failure={state} context="plain" />
        </div>
      ) : state.data.length === 0 ? (
        <p className="p-6 text-sm text-on-surface-variant">No user subscriptions yet.</p>
      ) : (
        <table className="w-full text-left text-sm">
          <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
            <tr>
              <th className="px-4 py-3 font-semibold">User</th>
              <th className="px-4 py-3 font-semibold">Plan</th>
              <th className="px-4 py-3 font-semibold">Status</th>
              <th className="px-4 py-3 font-semibold">Cycle</th>
              <th className="px-4 py-3 font-semibold">Started</th>
            </tr>
          </thead>
          <tbody>
            {state.data.map((s) => (
              <tr key={s.id} className="border-t border-outline-variant hover:bg-surface-low">
                <td className="px-4 py-3">
                  <button type="button" onClick={() => onPick(s.userId)} className="font-mono text-xs font-semibold text-primary hover:underline">
                    {s.userId}
                  </button>
                </td>
                <td className="px-4 py-3">{s.planName}</td>
                <td className="px-4 py-3">{label(s.status)}</td>
                <td className="px-4 py-3">{label(s.billingCycle)}</td>
                <td className="px-4 py-3 text-on-surface-variant">{formatDate(s.startedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {meta && meta.total > meta.size ? (
        <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm">
          <span className="text-on-surface-variant">
            Page {meta.page + 1} of {lastPage + 1} · {meta.total} subscriptions
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
    </section>
  );
}
