"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { CreationFeePanel } from "@/components/CreationFeePanel";
import { FreeTierPanel } from "@/components/FreeTierPanel";
import { PlanForm } from "@/components/PlanForm";
import { formatDate, label } from "@/lib/buildingApplications";
import {
  ENTITLEMENTS,
  entitlementValue,
  listPlans,
  retirePlan,
  type ApiFailure,
  type ApiResult,
  type Plan,
} from "@/lib/subscriptions";
import { dangerButton, primaryButton, secondaryButton, successClass } from "./formStyles";

const KIND = new Map<string, (typeof ENTITLEMENTS)[number]["kind"]>(ENTITLEMENTS.map((e) => [e.key, e.kind]));
const LABEL = new Map<string, string>(ENTITLEMENTS.map((e) => [e.key, e.label]));

/** BOC-04 Subscriptions > Plans: plan cards (create / edit / retire), free tier and creation fee. */
export function PlansScreen({ title }: { title: string }) {
  const [version, setVersion] = useState(0);
  const [loaded, setLoaded] = useState<{ version: number; result: ApiResult<Plan[]> } | null>(null);
  const [editing, setEditing] = useState<string | "new" | null>(null);
  const [done, setDone] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listPlans().then((result) => {
      if (!cancelled) setLoaded({ version, result });
    });
    return () => {
      cancelled = true;
    };
  }, [version]);

  const reload = (message: string) => {
    setEditing(null);
    setDone(message);
    setVersion((v) => v + 1);
  };

  const current = loaded?.result ?? null;

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-[22px] font-semibold">{title}</h1>
        <button
          type="button"
          className={primaryButton}
          onClick={() => {
            setDone(null);
            setEditing("new");
          }}
        >
          Create plan
        </button>
      </div>

      {done ? (
        <p role="status" className={successClass}>
          {done}
        </p>
      ) : null}

      {editing === "new" ? (
        <PlanForm onSaved={(p) => reload(`Plan ${p.code} created.`)} onCancel={() => setEditing(null)} />
      ) : null}

      <div>
        <h2 className="mb-3 text-base font-semibold">Subscription plans</h2>
        {!current ? (
          <p className="text-sm text-on-surface-variant">Loading plans…</p>
        ) : !current.ok ? (
          <ApiErrorNotice failure={current} context="plain" />
        ) : current.data.length === 0 ? (
          <p className="text-sm text-on-surface-variant">No plans yet.</p>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
            {current.data.map((p) =>
              editing === p.id ? (
                <div key={p.id} className="lg:col-span-2 xl:col-span-3">
                  <PlanForm plan={p} onSaved={(s) => reload(`Plan ${s.code} saved.`)} onCancel={() => setEditing(null)} />
                </div>
              ) : (
                <PlanCard
                  key={p.id}
                  plan={p}
                  onEdit={() => {
                    setDone(null);
                    setEditing(p.id);
                  }}
                  onRetired={(r) => reload(`Plan ${r.code} retired.`)}
                />
              ),
            )}
          </div>
        )}
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <FreeTierPanel />
        <CreationFeePanel />
      </div>
    </section>
  );
}

function PlanCard({ plan, onEdit, onRetired }: { plan: Plan; onEdit: () => void; onRetired: (p: Plan) => void }) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const active = plan.status === "ACTIVE";

  async function retire() {
    setBusy(true);
    setFailure(null);
    try {
      const r = await retirePlan(plan.id);
      if (r.ok) onRetired(r.data);
      else setFailure(r);
    } finally {
      setBusy(false);
      setConfirming(false);
    }
  }

  const entries = Object.entries(plan.entitlements ?? {});

  return (
    <article aria-label={`Plan ${plan.code}`} className="flex flex-col rounded-panel border border-outline-variant bg-surface-lowest p-5 shadow-tier1">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-lg font-semibold">{plan.name}</h3>
          <p className="font-mono text-xs text-on-surface-variant">{plan.code}</p>
        </div>
        <span
          className={`rounded-control px-2 py-0.5 text-xs font-semibold ${
            active ? "bg-success-container text-on-success-container" : "bg-surface-high text-on-surface-variant"
          }`}
        >
          {label(plan.status)}
        </span>
      </div>
      <p className="mt-2 text-sm text-on-surface-variant">
        {plan.billingCycles.map((c) => label(c)).join(" · ") || "—"} · {plan.selfService ? "Self-service" : "Admin-assigned only"}
      </p>
      <dl className="mt-3 flex-1 space-y-1 border-t border-outline-variant pt-3 text-sm">
        {entries.length === 0 ? (
          <p className="text-on-surface-variant">No entitlements set.</p>
        ) : (
          entries.map(([k, v]) => (
            <div key={k} className="flex justify-between gap-3">
              <dt>{LABEL.get(k) ?? k}</dt>
              <dd className="font-semibold">{entitlementValue(KIND.get(k), v)}</dd>
            </div>
          ))
        )}
      </dl>
      <p className="mt-3 text-xs text-on-surface-variant">Updated {formatDate(plan.updatedAt)}</p>
      {active ? (
        <div className="mt-3 flex flex-wrap gap-2 border-t border-outline-variant pt-3">
          <button type="button" className={secondaryButton} disabled={busy} onClick={onEdit}>
            Edit
          </button>
          {confirming ? (
            <>
              <button type="button" className={dangerButton} disabled={busy} onClick={retire}>
                Confirm retire
              </button>
              <button type="button" className={secondaryButton} disabled={busy} onClick={() => setConfirming(false)}>
                Keep plan
              </button>
            </>
          ) : (
            <button type="button" className={dangerButton} disabled={busy} onClick={() => setConfirming(true)}>
              Retire
            </button>
          )}
        </div>
      ) : null}
      {confirming ? (
        <p className="mt-2 text-xs text-on-surface-variant">
          Retiring is one-way: the plan can no longer be granted; existing subscriptions keep it.
        </p>
      ) : null}
      {failure ? (
        <div className="mt-3">
          <ApiErrorNotice failure={failure} context="plain" />
        </div>
      ) : null}
    </article>
  );
}
