"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { dangerButton, fieldClass, fieldError, panelClass, primaryButton, secondaryButton, successClass } from "@/components/formStyles";
import { formatDate, label } from "@/lib/buildingApplications";
import {
  ACTION_FOR_STATUS,
  getBuilding,
  lifecycle,
  type ApiFailure,
  type ApiResult,
  type Building,
  type BuildingStatus,
  type LifecycleAction,
} from "@/lib/buildings";

const VERB: Record<LifecycleAction, string> = {
  activate: "Activate",
  suspend: "Suspend",
  reactivate: "Reactivate",
};

const DONE: Record<LifecycleAction, string> = {
  activate: "Building activated.",
  suspend: "Building suspended.",
  reactivate: "Building reactivated.",
};

/** Building detail with the lifecycle action its status offers (activate / suspend / reactivate). */
export function BuildingPanel({
  buildingId,
  onChanged,
  onClose,
}: {
  buildingId: string;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [loaded, setLoaded] = useState<{ id: string; result: ApiResult<Building> } | null>(null);
  const [version, setVersion] = useState(0);
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getBuilding(buildingId).then((result) => {
      if (!cancelled) setLoaded({ id: buildingId, result });
    });
    return () => {
      cancelled = true;
    };
  }, [buildingId, version]);

  const current = loaded?.id === buildingId ? loaded.result : null;

  async function submit(e: React.FormEvent, action: LifecycleAction) {
    e.preventDefault();
    if (!reason.trim()) {
      setReasonError("A reason is required.");
      return;
    }
    setReasonError(null);
    setBusy(true);
    setFailure(null);
    try {
      const r = await lifecycle(buildingId, action, reason.trim());
      if (r.ok) {
        setDone(DONE[action]);
        setOpen(false);
        setReason("");
        setLoaded({ id: buildingId, result: r });
        onChanged();
      } else {
        setFailure(r);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <aside aria-label="Building detail" className={`${panelClass} w-full space-y-4 lg:w-96`}>
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-base font-semibold">{current?.ok ? current.data.name : "Building"}</h2>
        <button type="button" onClick={onClose} className={secondaryButton} aria-label="Close building detail">
          Close
        </button>
      </div>

      {!current ? (
        <p className="text-sm text-on-surface-variant">Loading building…</p>
      ) : !current.ok ? (
        <ApiErrorNotice failure={current} context="plain" />
      ) : (
        <Details
          building={current.data}
          open={open}
          setOpen={(v) => {
            setOpen(v);
            setFailure(null);
            setDone(null);
            setReason("");
            setReasonError(null);
          }}
          reason={reason}
          setReason={setReason}
          reasonError={reasonError}
          busy={busy}
          failure={failure}
          done={done}
          onSubmit={submit}
          onRefresh={() => setVersion((v) => v + 1)}
        />
      )}
    </aside>
  );
}

function Details({
  building: b,
  open,
  setOpen,
  reason,
  setReason,
  reasonError,
  busy,
  failure,
  done,
  onSubmit,
  onRefresh,
}: {
  building: Building;
  open: boolean;
  setOpen: (v: boolean) => void;
  reason: string;
  setReason: (v: string) => void;
  reasonError: string | null;
  busy: boolean;
  failure: ApiFailure | null;
  done: string | null;
  onSubmit: (e: React.FormEvent, action: LifecycleAction) => void;
  onRefresh: () => void;
}) {
  const action = ACTION_FOR_STATUS[b.status as BuildingStatus];
  return (
    <div className="space-y-4 text-sm">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-1">
        <dt className="text-on-surface-variant">Status</dt>
        <dd className="font-semibold">{label(b.status)}</dd>
        <dt className="text-on-surface-variant">Type</dt>
        <dd>{label(b.buildingType)}</dd>
        <dt className="text-on-surface-variant">Address</dt>
        <dd>{[b.address, b.area, b.district].filter(Boolean).join(", ")}</dd>
        <dt className="text-on-surface-variant">Contact</dt>
        <dd>{b.contactPhone}</dd>
        <dt className="text-on-surface-variant">Created</dt>
        <dd>{formatDate(b.createdAt)}</dd>
      </dl>

      <div className="border-t border-outline-variant pt-3">
        <h3 className="mb-1 font-semibold">Members</h3>
        {b.members.length === 0 ? (
          <p className="text-on-surface-variant">No members yet.</p>
        ) : (
          <ul className="space-y-1">
            {b.members.map((m) => (
              <li key={`${m.userId}-${m.role}`} className="flex justify-between gap-3">
                <span className="truncate font-mono text-xs">{m.userId}</span>
                <span>
                  {label(m.role)} · {label(m.status)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {done ? (
        <p role="status" className={successClass}>
          {done}
        </p>
      ) : null}
      {failure ? (
        <div className="space-y-2">
          <ApiErrorNotice failure={failure} context="plain" />
          {failure.status === 409 ? (
            <button type="button" onClick={onRefresh} className={secondaryButton}>
              Refresh building
            </button>
          ) : null}
        </div>
      ) : null}

      {action ? (
        open ? (
          <form onSubmit={(e) => onSubmit(e, action)} noValidate aria-label={`${VERB[action]} building`} className="space-y-3">
            <label className="block font-semibold">
              Reason
              <textarea className={fieldClass} rows={3} value={reason} onChange={(e) => setReason(e.target.value)} />
              {reasonError ? <span className={fieldError}>{reasonError}</span> : null}
            </label>
            <div className="flex gap-2">
              <button type="submit" disabled={busy} className={action === "suspend" ? dangerButton : primaryButton}>
                Confirm {VERB[action].toLowerCase()}
              </button>
              <button type="button" onClick={() => setOpen(false)} className={secondaryButton}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <button type="button" onClick={() => setOpen(true)} className={action === "suspend" ? dangerButton : primaryButton}>
            {VERB[action]}
          </button>
        )
      ) : null}
    </div>
  );
}
