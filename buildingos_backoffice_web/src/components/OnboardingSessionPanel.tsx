"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { dangerButton, fieldClass, panelClass, primaryButton, secondaryButton, successClass } from "@/components/formStyles";
import { formatDate, label } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import {
  ENDED_STATUSES,
  allowedActions,
  transition,
  type ApiFailure,
  type OnboardingSession,
  type SessionAction,
} from "@/lib/onboardingSessions";

const VERB: Record<SessionAction, string> = {
  "start-work": "Start work",
  "await-customer": "Await customer",
  complete: "Complete",
  cancel: "Cancel session",
};

/** Session detail and the transitions the viewer may attempt; ended sessions are read-only. */
export function OnboardingSessionPanel({
  session,
  userId,
  roles,
  onChanged,
  onClose,
}: {
  session: OnboardingSession;
  userId: string;
  roles: PlatformRole[];
  onChanged: () => void;
  onClose: () => void;
}) {
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const actions = allowedActions(session, userId, roles);
  const ended = ENDED_STATUSES.includes(session.status);

  async function run(action: SessionAction) {
    setBusy(true);
    setFailure(null);
    setDone(null);
    try {
      const r = await transition(session.id, action, reason.trim() || undefined);
      if (r.ok) {
        setDone(`${VERB[action]}: now ${label(r.data.status).toLowerCase()}.`);
        setReason("");
        onChanged();
      } else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <aside aria-label="Onboarding session detail" className={`${panelClass} w-full space-y-4 lg:w-96`}>
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-base font-semibold">Onboarding session</h2>
        <button type="button" onClick={onClose} className={secondaryButton} aria-label="Close session detail">
          Close
        </button>
      </div>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
        <dt className="text-on-surface-variant">Status</dt>
        <dd className="font-semibold">{label(session.status)}</dd>
        <dt className="text-on-surface-variant">Building</dt>
        <dd className="break-all font-mono text-xs">{session.buildingId}</dd>
        <dt className="text-on-surface-variant">Agent</dt>
        <dd className="break-all font-mono text-xs">{session.assignedAgentUserId}</dd>
        <dt className="text-on-surface-variant">Started</dt>
        <dd>{formatDate(session.startedAt)}</dd>
        <dt className="text-on-surface-variant">Expires</dt>
        <dd>{formatDate(session.expiresAt)}</dd>
        <dt className="text-on-surface-variant">Completed</dt>
        <dd>{formatDate(session.completedAt)}</dd>
        <dt className="text-on-surface-variant">Reason</dt>
        <dd>{session.reason}</dd>
        {session.notes ? (
          <>
            <dt className="text-on-surface-variant">Notes</dt>
            <dd>{session.notes}</dd>
          </>
        ) : null}
      </dl>
      <div className="border-t border-outline-variant pt-3 text-sm">
        <h3 className="mb-1 font-semibold">Access scope</h3>
        <ul className="flex flex-wrap gap-1">
          {session.accessScope.map((s) => (
            <li key={s} className="rounded-control bg-surface-container px-2 py-0.5 text-xs">
              {label(s.replace("ONBOARDING_", ""))}
            </li>
          ))}
        </ul>
      </div>

      {done ? (
        <p role="status" className={successClass}>
          {done}
        </p>
      ) : null}
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}

      {ended ? (
        <p className="text-sm text-on-surface-variant">This session has ended and is read-only.</p>
      ) : actions.length > 0 ? (
        <div className="space-y-3 border-t border-outline-variant pt-3">
          <label className="block text-sm font-semibold">
            Reason (optional)
            <textarea className={fieldClass} rows={2} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
          </label>
          <div className="flex flex-wrap gap-2">
            {actions.map((a) => (
              <button
                key={a}
                type="button"
                disabled={busy}
                onClick={() => run(a)}
                className={a === "cancel" ? dangerButton : a === "complete" ? primaryButton : secondaryButton}
              >
                {VERB[a]}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </aside>
  );
}
