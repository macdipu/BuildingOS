"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { dangerButton, fieldClass, fieldError, panelClass, primaryButton, secondaryButton, successClass } from "@/components/formStyles";
import { formatDate, label } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import {
  canDecide,
  decideApproval,
  endSupportSession,
  getSupportSession,
  isHighRisk,
  requestElevated,
  scopeState,
  type ApiFailure,
  type ApiResult,
  type ScopeState,
  type SupportSessionDetail,
} from "@/lib/supportSessions";

const STATE_LABEL: Record<ScopeState, string> = {
  GRANTED: "Granted",
  PENDING: "Pending approval",
  APPROVED: "Approved",
  DENIED: "Denied",
  NOT_REQUESTED: "Not requested",
};

/**
 * Support session detail. A high-risk scope is never shown as granted until its request is
 * APPROVED; approve/deny appear only for SUPER_ADMIN who is not the owner (four-eyes).
 * The UI grants nothing itself, the backend decides every call.
 */
export function SupportSessionPanel({
  sessionId,
  userId,
  roles,
  onChanged,
  onClose,
}: {
  sessionId: string;
  userId: string;
  roles: PlatformRole[];
  onChanged: () => void;
  onClose: () => void;
}) {
  const [loaded, setLoaded] = useState<{ id: string; result: ApiResult<SupportSessionDetail> } | null>(null);
  const [version, setVersion] = useState(0);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const [requesting, setRequesting] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getSupportSession(sessionId).then((result) => {
      if (!cancelled) setLoaded({ id: sessionId, result });
    });
    return () => {
      cancelled = true;
    };
  }, [sessionId, version]);

  const current = loaded?.id === sessionId ? loaded.result : null;

  async function run(action: () => Promise<ApiResult<unknown>>, message: string) {
    setBusy(true);
    setFailure(null);
    setDone(null);
    try {
      const r = await action();
      if (r.ok) {
        setDone(message);
        setRequesting(null);
        setReason("");
        setVersion((v) => v + 1);
        onChanged();
      } else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  function submitRequest(e: React.FormEvent, scope: string) {
    e.preventDefault();
    if (!reason.trim()) {
      setReasonError("A reason is required.");
      return;
    }
    setReasonError(null);
    void run(() => requestElevated(sessionId, scope, reason.trim()), "Approval requested.");
  }

  return (
    <aside aria-label="Support session detail" className={`${panelClass} w-full space-y-4 lg:w-[26rem]`}>
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-base font-semibold">Support session</h2>
        <button type="button" onClick={onClose} className={secondaryButton} aria-label="Close session detail">
          Close
        </button>
      </div>
      {!current ? (
        <p className="text-sm text-on-surface-variant">Loading session…</p>
      ) : !current.ok ? (
        <ApiErrorNotice failure={current} context="plain" />
      ) : (
        (() => {
          const s = current.data;
          const active = s.status === "ACTIVE";
          const owner = s.platformUserId === userId;
          const approvals = s.elevatedApprovals;
          const pending = approvals.filter((a) => a.status === "PENDING");
          return (
            <div className="space-y-4 text-sm">
              <dl className="grid grid-cols-2 gap-x-4 gap-y-1">
                <dt className="text-on-surface-variant">Status</dt>
                <dd className="font-semibold">{label(s.status)}</dd>
                <dt className="text-on-surface-variant">Agent</dt>
                <dd className="break-all font-mono text-xs">{s.platformUserId}</dd>
                <dt className="text-on-surface-variant">Target user</dt>
                <dd className="break-all font-mono text-xs">{s.targetUserId ?? "—"}</dd>
                <dt className="text-on-surface-variant">Building</dt>
                <dd className="break-all font-mono text-xs">{s.buildingId ?? "—"}</dd>
                <dt className="text-on-surface-variant">Reason</dt>
                <dd>{s.reason}</dd>
                <dt className="text-on-surface-variant">Started</dt>
                <dd>{formatDate(s.startedAt)}</dd>
                <dt className="text-on-surface-variant">{active ? "Expires" : "Ended"}</dt>
                <dd>{formatDate(active ? s.expiresAt : s.endedAt)}</dd>
              </dl>

              <div className="border-t border-outline-variant pt-3">
                <h3 className="mb-1 font-semibold">Scopes</h3>
                <ul className="space-y-2">
                  {s.permissionScope.map((scope) => {
                    const state = scopeState(scope, approvals);
                    return (
                      <li key={scope} className="flex flex-wrap items-center justify-between gap-2">
                        <span>{label(scope.replace("SUPPORT_", ""))}</span>
                        <span className="flex items-center gap-2">
                          <span
                            className={`rounded-control px-2 py-0.5 text-xs font-semibold ${
                              state === "PENDING" || state === "DENIED" || state === "NOT_REQUESTED"
                                ? "bg-error-container"
                                : "bg-surface-container"
                            }`}
                          >
                            {STATE_LABEL[state]}
                          </span>
                          {active && owner && isHighRisk(scope) && (state === "DENIED" || state === "NOT_REQUESTED") ? (
                            <button type="button" className={secondaryButton} onClick={() => setRequesting(scope)}>
                              Request approval
                            </button>
                          ) : null}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </div>

              {requesting ? (
                <form onSubmit={(e) => submitRequest(e, requesting)} noValidate aria-label="Request elevated approval" className="space-y-2">
                  <label className="block font-semibold">
                    Reason for {label(requesting.replace("SUPPORT_", "")).toLowerCase()}
                    <textarea className={fieldClass} rows={2} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
                    {reasonError ? <span className={fieldError}>{reasonError}</span> : null}
                  </label>
                  <div className="flex gap-2">
                    <button type="submit" disabled={busy} className={primaryButton}>
                      Send request
                    </button>
                    <button type="button" className={secondaryButton} onClick={() => setRequesting(null)}>
                      Cancel
                    </button>
                  </div>
                </form>
              ) : null}

              {active && pending.length > 0 && canDecide(roles, userId, s) ? (
                <div className="space-y-2 border-t border-outline-variant pt-3">
                  <h3 className="font-semibold">Pending approvals</h3>
                  {pending.map((a) => (
                    <div key={a.id} className="flex items-center justify-between gap-2">
                      <span>{label(a.requestedScope.replace("SUPPORT_", ""))}</span>
                      <span className="flex gap-2">
                        <button type="button" disabled={busy} className={primaryButton} onClick={() => run(() => decideApproval(a.id, "approve"), "Approved.")}>
                          Approve
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          className={dangerButton}
                          onClick={() => {
                            if (!reason.trim()) {
                              setReasonError("A reason is required to deny.");
                              return;
                            }
                            setReasonError(null);
                            void run(() => decideApproval(a.id, "deny", reason.trim()), "Denied.");
                          }}
                        >
                          Deny
                        </button>
                      </span>
                    </div>
                  ))}
                  <label className="block font-semibold">
                    Decision reason (required to deny)
                    <textarea className={fieldClass} rows={2} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
                    {reasonError ? <span className={fieldError}>{reasonError}</span> : null}
                  </label>
                </div>
              ) : null}

              {done ? (
                <p role="status" className={successClass}>
                  {done}
                </p>
              ) : null}
              {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}

              {active && (owner || roles.includes("SUPER_ADMIN") || roles.includes("PLATFORM_ADMIN")) ? (
                <button type="button" disabled={busy} className={dangerButton} onClick={() => run(() => endSupportSession(s.id), "Session ended.")}>
                  End session
                </button>
              ) : null}
              {!active ? <p className="text-on-surface-variant">This session has ended and is read-only.</p> : null}
            </div>
          );
        })()
      )}
    </aside>
  );
}
