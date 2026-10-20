"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { fieldClass, fieldError, panelClass, primaryButton, secondaryButton } from "@/components/formStyles";
import { label } from "@/lib/buildingApplications";
import { UUID_PATTERN } from "@/lib/subscriptions";
import {
  MAX_DURATION_DAYS,
  ONBOARDING_SCOPES,
  startSession,
  type ApiFailure,
  type OnboardingSession,
} from "@/lib/onboardingSessions";

/** Operator form: assign an ONBOARDING_AGENT to a building with D-10 scopes, reason and expiry (D-33). */
export function StartOnboardingForm({
  initialBuildingId = "",
  onStarted,
  onCancel,
}: {
  initialBuildingId?: string;
  onStarted: (s: OnboardingSession) => void;
  onCancel: () => void;
}) {
  const [buildingId, setBuildingId] = useState(initialBuildingId);
  const [agentId, setAgentId] = useState("");
  const [scopes, setScopes] = useState<string[]>([]);
  const [reason, setReason] = useState("");
  const [notes, setNotes] = useState("");
  const [expires, setExpires] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);

  function toggle(scope: string) {
    setScopes((cur) => (cur.includes(scope) ? cur.filter((s) => s !== scope) : [...cur, scope]));
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    const next: Record<string, string> = {};
    if (!UUID_PATTERN.test(buildingId.trim())) next.buildingId = "Enter a building id (UUID).";
    if (!UUID_PATTERN.test(agentId.trim())) next.agentId = "Enter the agent's user id (UUID).";
    if (scopes.length === 0) next.scopes = "Choose at least one scope.";
    if (!reason.trim()) next.reason = "A reason is required.";
    const at = new Date(expires);
    if (!expires || Number.isNaN(at.getTime())) next.expires = "Choose an expiry.";
    else if (at.getTime() <= Date.now()) next.expires = "Expiry must be in the future.";
    else if (at.getTime() > Date.now() + MAX_DURATION_DAYS * 86_400_000) {
      next.expires = `Expiry can be at most ${MAX_DURATION_DAYS} days from now.`;
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const r = await startSession({
        buildingId: buildingId.trim(),
        assignedAgentUserId: agentId.trim(),
        accessScope: scopes,
        reason: reason.trim(),
        notes: notes.trim() || undefined,
        expiresAt: at.toISOString(),
      });
      if (r.ok) onStarted(r.data);
      else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate aria-label="Assign onboarding agent" className={`${panelClass} space-y-3`}>
      <h2 className="text-base font-semibold">Assign onboarding agent</h2>
      <div className="grid gap-3 md:grid-cols-2">
        <label className="block text-sm font-semibold">
          Building id
          <input className={fieldClass} value={buildingId} onChange={(e) => setBuildingId(e.target.value)} />
          {errors.buildingId ? <span className={fieldError}>{errors.buildingId}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Agent user id
          <input className={fieldClass} value={agentId} onChange={(e) => setAgentId(e.target.value)} />
          {errors.agentId ? <span className={fieldError}>{errors.agentId}</span> : null}
        </label>
      </div>
      <fieldset>
        <legend className="text-sm font-semibold">Access scope</legend>
        <div className="mt-1 grid gap-1 md:grid-cols-2">
          {ONBOARDING_SCOPES.map((s) => (
            <label key={s} className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={scopes.includes(s)} onChange={() => toggle(s)} />
              {label(s.replace("ONBOARDING_", ""))}
            </label>
          ))}
        </div>
        {errors.scopes ? <span className={fieldError}>{errors.scopes}</span> : null}
      </fieldset>
      <label className="block text-sm font-semibold">
        Reason
        <textarea className={fieldClass} rows={2} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
        {errors.reason ? <span className={fieldError}>{errors.reason}</span> : null}
      </label>
      <label className="block text-sm font-semibold">
        Notes (optional)
        <textarea className={fieldClass} rows={2} maxLength={2000} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </label>
      <label className="block text-sm font-semibold">
        Expires
        <input type="datetime-local" className={fieldClass} value={expires} onChange={(e) => setExpires(e.target.value)} />
        {errors.expires ? <span className={fieldError}>{errors.expires}</span> : null}
      </label>
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
      <div className="flex gap-2">
        <button type="submit" disabled={busy} className={primaryButton}>
          Start session
        </button>
        <button type="button" onClick={onCancel} className={secondaryButton}>
          Cancel
        </button>
      </div>
    </form>
  );
}
