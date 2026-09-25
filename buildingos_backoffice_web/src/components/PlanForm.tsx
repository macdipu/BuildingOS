"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { EntitlementFields, fromDraft, toDraft, type EntitlementDraft } from "@/components/EntitlementFields";
import {
  BILLING_CYCLES,
  PLAN_CODE_PATTERN,
  PLAN_NAME_MAX,
  createPlan,
  updatePlan,
  type ApiFailure,
  type Plan,
} from "@/lib/subscriptions";
import { fieldClass, fieldError, primaryButton, secondaryButton } from "./formStyles";

/**
 * Create (no `plan`) or edit a plan. Code is set on create only (immutable);
 * edits apply immediately to every subscriber (D-20).
 */
export function PlanForm({
  plan,
  onSaved,
  onCancel,
}: {
  plan?: Plan;
  onSaved: (saved: Plan) => void;
  onCancel: () => void;
}) {
  const [code, setCode] = useState(plan?.code ?? "");
  const [name, setName] = useState(plan?.name ?? "");
  const [cycles, setCycles] = useState<string[]>(plan?.billingCycles ?? []);
  const [selfService, setSelfService] = useState(plan?.selfService ?? false);
  const [draft, setDraft] = useState<EntitlementDraft>(() => toDraft(plan?.entitlements));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);

  const toggleCycle = (c: string) => setCycles((cs) => (cs.includes(c) ? cs.filter((x) => x !== c) : [...cs, c]));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    const { map, errors: next } = fromDraft(draft);
    if (!plan && !PLAN_CODE_PATTERN.test(code.trim())) {
      next.code = "2-64 characters of A-Z, 0-9 or _, starting with a letter.";
    }
    if (!name.trim() || name.trim().length > PLAN_NAME_MAX) next.name = `Name must be 1-${PLAN_NAME_MAX} characters.`;
    if (cycles.length === 0) next.billingCycles = "Offer at least one billing cycle.";
    setErrors(next);
    if (Object.keys(next).length) return;

    const input = {
      name: name.trim(),
      billingCycles: BILLING_CYCLES.filter((c) => cycles.includes(c)),
      selfService,
      entitlements: map,
    };
    setBusy(true);
    try {
      const r = plan ? await updatePlan(plan.id, input) : await createPlan({ code: code.trim(), ...input });
      if (r.ok) onSaved(r.data);
      else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={submit}
      noValidate
      aria-label={plan ? `Edit plan ${plan.code}` : "Create plan"}
      className="space-y-4 rounded-card border border-outline-variant bg-surface-low p-4"
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-sm font-semibold">
          Plan code
          <input
            className={fieldClass}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            disabled={!!plan}
            aria-invalid={errors.code ? true : undefined}
          />
          {errors.code ? <span className={fieldError}>{errors.code}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Name
          <input
            className={fieldClass}
            value={name}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={errors.name ? true : undefined}
          />
          {errors.name ? <span className={fieldError}>{errors.name}</span> : null}
        </label>
      </div>

      <fieldset>
        <legend className="text-sm font-semibold">Billing cycles</legend>
        <div className="mt-1 flex flex-wrap gap-4 text-sm">
          {BILLING_CYCLES.map((c) => (
            <label key={c} className="flex items-center gap-2">
              <input type="checkbox" checked={cycles.includes(c)} onChange={() => toggleCycle(c)} />
              {c.charAt(0) + c.slice(1).toLowerCase()}
            </label>
          ))}
        </div>
        {errors.billingCycles ? <span className={fieldError}>{errors.billingCycles}</span> : null}
      </fieldset>

      <label className="flex items-center gap-2 text-sm font-semibold">
        <input type="checkbox" checked={selfService} onChange={(e) => setSelfService(e.target.checked)} />
        Self-service (users may pick this plan themselves)
      </label>

      <EntitlementFields draft={draft} errors={errors} onChange={setDraft} />

      {plan ? (
        <p className="text-xs text-on-surface-variant">Changes apply immediately to every subscriber of this plan.</p>
      ) : null}

      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}

      <div className="flex gap-2">
        <button type="submit" className={primaryButton} disabled={busy}>
          {plan ? "Save plan" : "Create plan"}
        </button>
        <button type="button" className={secondaryButton} disabled={busy} onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
