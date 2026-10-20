"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { fieldClass, fieldError, panelClass, primaryButton, secondaryButton } from "@/components/formStyles";
import { label } from "@/lib/buildingApplications";
import { UUID_PATTERN } from "@/lib/subscriptions";
import {
  MAX_DURATION_HOURS,
  SUPPORT_SCOPES,
  isHighRisk,
  startSupportSession,
  type ApiFailure,
  type SupportSession,
} from "@/lib/supportSessions";

/** Open a support session for yourself (D-36). Scopes are only requested here; the server grants them. */
export function StartSupportForm({ onStarted, onCancel }: { onStarted: (s: SupportSession) => void; onCancel: () => void }) {
  const [targetUserId, setTarget] = useState("");
  const [buildingId, setBuilding] = useState("");
  const [scopes, setScopes] = useState<string[]>([]);
  const [reason, setReason] = useState("");
  const [expires, setExpires] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);

  const toggle = (scope: string) =>
    setScopes((cur) => (cur.includes(scope) ? cur.filter((s) => s !== scope) : [...cur, scope]));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    const next: Record<string, string> = {};
    const t = targetUserId.trim();
    const b = buildingId.trim();
    if (!t && !b) next.target = "Name a target user, a building, or both.";
    if (t && !UUID_PATTERN.test(t)) next.targetUserId = "Enter a user id (UUID).";
    if (b && !UUID_PATTERN.test(b)) next.buildingId = "Enter a building id (UUID).";
    if (scopes.length === 0) next.scopes = "Choose at least one scope.";
    if (!reason.trim()) next.reason = "A reason is required.";
    const at = new Date(expires);
    if (!expires || Number.isNaN(at.getTime())) next.expires = "Choose an expiry.";
    else if (at.getTime() <= Date.now()) next.expires = "Expiry must be in the future.";
    else if (at.getTime() > Date.now() + MAX_DURATION_HOURS * 3_600_000) {
      next.expires = `Expiry can be at most ${MAX_DURATION_HOURS} hours from now.`;
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const r = await startSupportSession({
        targetUserId: t || undefined,
        buildingId: b || undefined,
        permissionScope: scopes,
        reason: reason.trim(),
        expiresAt: at.toISOString(),
      });
      if (r.ok) onStarted(r.data);
      else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate aria-label="Start support session" className={`${panelClass} space-y-3`}>
      <h2 className="text-base font-semibold">Start support session</h2>
      <div className="grid gap-3 md:grid-cols-2">
        <label className="block text-sm font-semibold">
          Target user id
          <input className={fieldClass} value={targetUserId} onChange={(e) => setTarget(e.target.value)} />
          {errors.targetUserId ? <span className={fieldError}>{errors.targetUserId}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Building id
          <input className={fieldClass} value={buildingId} onChange={(e) => setBuilding(e.target.value)} />
          {errors.buildingId ? <span className={fieldError}>{errors.buildingId}</span> : null}
        </label>
      </div>
      {errors.target ? <span className={fieldError}>{errors.target}</span> : null}
      <fieldset>
        <legend className="text-sm font-semibold">Scopes</legend>
        <div className="mt-1 grid gap-1 md:grid-cols-2">
          {SUPPORT_SCOPES.map((s) => (
            <label key={s} className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={scopes.includes(s)} onChange={() => toggle(s)} />
              {label(s.replace("SUPPORT_", ""))}
              {isHighRisk(s) ? (
                <span className="rounded-control bg-error-container px-1.5 py-0.5 text-[11px] font-semibold">Needs approval</span>
              ) : null}
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
