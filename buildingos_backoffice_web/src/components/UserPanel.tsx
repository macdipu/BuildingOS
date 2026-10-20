"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { dangerButton, fieldClass, fieldError, panelClass, primaryButton, secondaryButton, successClass } from "@/components/formStyles";
import { formatDate } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import {
  PLATFORM_ROLES,
  ROLE_LABEL,
  assignRole,
  canManageRoles,
  revokeRole,
  type ApiFailure,
  type ApiResult,
  type PlatformUser,
} from "@/lib/platformUsers";

/**
 * One user's platform roles. Assign/revoke are SUPER_ADMIN-only and hidden for your own account;
 * every change needs an explicit confirmation. auth-service re-checks all of it and its errors are shown with their code.
 */
export function UserPanel({
  user,
  viewerId,
  viewerRoles,
  onChanged,
  onClose,
}: {
  user: PlatformUser;
  viewerId: string;
  viewerRoles: PlatformRole[];
  onChanged: () => void;
  onClose: () => void;
}) {
  const [current, setCurrent] = useState(user);
  const [newRole, setNewRole] = useState("");
  const [pending, setPending] = useState<{ kind: "assign" | "revoke"; role: string } | null>(null);
  const [roleError, setRoleError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const manage = canManageRoles(viewerRoles) && viewerId !== current.id;
  const assignable = PLATFORM_ROLES.filter((r) => !current.platformRoles.includes(r));

  async function confirm() {
    if (!pending) return;
    setBusy(true);
    setFailure(null);
    setDone(null);
    try {
      const r: ApiResult<PlatformUser> =
        pending.kind === "assign" ? await assignRole(current.id, pending.role) : await revokeRole(current.id, pending.role);
      if (r.ok) {
        setCurrent(r.data);
        setDone(`${ROLE_LABEL[pending.role as PlatformRole]} ${pending.kind === "assign" ? "assigned" : "revoked"}.`);
        setPending(null);
        setNewRole("");
        onChanged();
      } else {
        setFailure(r);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <aside aria-label="User detail" className={`${panelClass} w-full space-y-4 lg:w-96`}>
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-base font-semibold">{current.phone}</h2>
        <button type="button" onClick={onClose} className={secondaryButton} aria-label="Close user detail">
          Close
        </button>
      </div>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
        <dt className="text-on-surface-variant">User id</dt>
        <dd className="break-all font-mono text-xs">{current.id}</dd>
        <dt className="text-on-surface-variant">Created</dt>
        <dd>{formatDate(current.createdAt)}</dd>
      </dl>

      <div className="border-t border-outline-variant pt-3 text-sm">
        <h3 className="mb-2 font-semibold">Platform roles</h3>
        {current.platformRoles.length === 0 ? (
          <p className="text-on-surface-variant">No platform roles.</p>
        ) : (
          <ul className="space-y-2">
            {current.platformRoles.map((r) => (
              <li key={r} className="flex items-center justify-between gap-2">
                <span>{ROLE_LABEL[r] ?? r}</span>
                {manage ? (
                  <button type="button" className={dangerButton} onClick={() => setPending({ kind: "revoke", role: r })}>
                    Revoke {ROLE_LABEL[r]}
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      {manage ? (
        <div className="space-y-2 border-t border-outline-variant pt-3 text-sm">
          <label className="block font-semibold">
            Assign role
            <select className={fieldClass} value={newRole} onChange={(e) => setNewRole(e.target.value)}>
              <option value="">Select a role</option>
              {assignable.map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABEL[r]}
                </option>
              ))}
            </select>
            {roleError ? <span className={fieldError}>{roleError}</span> : null}
          </label>
          <button
            type="button"
            className={secondaryButton}
            onClick={() => {
              if (!newRole) {
                setRoleError("Choose a role.");
                return;
              }
              setRoleError(null);
              setPending({ kind: "assign", role: newRole });
            }}
          >
            Assign role
          </button>
        </div>
      ) : canManageRoles(viewerRoles) ? (
        <p className="text-xs text-on-surface-variant">You cannot change your own platform roles.</p>
      ) : null}

      {pending ? (
        <div role="alertdialog" aria-label="Confirm role change" className="space-y-2 rounded-card border border-outline-variant bg-surface-low p-3 text-sm">
          <p>
            {pending.kind === "assign" ? "Assign" : "Revoke"} <strong>{ROLE_LABEL[pending.role as PlatformRole]}</strong>{" "}
            {pending.kind === "assign" ? "to" : "from"} {current.phone}? This is recorded in the audit log.
          </p>
          <div className="flex gap-2">
            <button type="button" disabled={busy} className={pending.kind === "revoke" ? dangerButton : primaryButton} onClick={confirm}>
              Confirm
            </button>
            <button type="button" className={secondaryButton} onClick={() => setPending(null)}>
              Cancel
            </button>
          </div>
        </div>
      ) : null}

      {done ? (
        <p role="status" className={successClass}>
          {done}
        </p>
      ) : null}
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
    </aside>
  );
}
