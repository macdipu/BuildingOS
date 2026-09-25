"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import {
  approveApplication,
  label,
  rejectApplication,
  requestInformation,
  startReview,
  type ApiFailure,
  type BuildingApplication,
} from "@/lib/buildingApplications";

type Form = "request-information" | "reject" | "approve" | null;

const button =
  "h-9 rounded-control px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60";
const primary = `${button} bg-primary-container text-on-primary hover:bg-primary-hover`;
const secondary = `${button} border border-outline-variant bg-surface-lowest hover:bg-surface-low`;
const danger = `${button} border border-error/40 bg-surface-lowest text-error hover:bg-error-container`;
const field =
  "mt-1 w-full rounded-control border border-outline-variant bg-surface-lowest px-3 py-2 text-sm focus:border-primary focus:outline-none";

/**
 * §149.5 review actions for the application's current state. Transitions follow
 * the contract: SUBMITTED → start review; UNDER_REVIEW → request information,
 * reject or approve. The backend remains the authority on every transition.
 */
export function ReviewActions({
  application,
  onChanged,
  onRefresh,
}: {
  application: BuildingApplication;
  onChanged: () => void;
  onRefresh: () => void;
}) {
  const [form, setForm] = useState<Form>(null);
  const [text, setText] = useState("");
  const [adminPhone, setAdminPhone] = useState(application.contactPhone ?? "");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState<string | null>(null);

  const id = application.id;

  function open(next: Form) {
    setForm(next);
    setText("");
    setAdminPhone(application.contactPhone ?? "");
    setFailure(null);
    setDone(null);
  }

  async function run(action: () => Promise<{ ok: true; message: string } | ApiFailure>) {
    setBusy(true);
    setFailure(null);
    setDone(null);
    try {
      const r = await action();
      if (r.ok) {
        setDone(r.message);
        setForm(null);
        onChanged();
      } else {
        setFailure(r);
      }
    } finally {
      setBusy(false);
    }
  }

  const moved = (next: string) => `Application moved to ${label(next)}.`;

  const doStartReview = () =>
    run(async () => {
      const r = await startReview(id);
      return r.ok ? { ok: true, message: moved(r.data.status) } : r;
    });

  const submitForm = (e: React.FormEvent) => {
    e.preventDefault();
    const value = text.trim();
    if (form === "request-information") {
      run(async () => {
        const r = await requestInformation(id, value);
        return r.ok ? { ok: true, message: moved(r.data.status) } : r;
      });
    } else if (form === "reject") {
      run(async () => {
        const r = await rejectApplication(id, value);
        return r.ok ? { ok: true, message: moved(r.data.status) } : r;
      });
    } else if (form === "approve") {
      run(async () => {
        const r = await approveApplication(id, adminPhone.trim(), value);
        return r.ok
          ? {
              ok: true,
              message: `Application approved. Building ${r.data.buildingId} created in ${label(r.data.buildingStatus)}.`,
            }
          : r;
      });
    }
  };

  const status = application.status;
  const hasActions = status === "SUBMITTED" || status === "UNDER_REVIEW";

  return (
    <section aria-label="Review actions" className="space-y-3">
      {hasActions ? (
        <div className="flex flex-wrap gap-2">
          {status === "SUBMITTED" ? (
            <button type="button" className={primary} disabled={busy} onClick={doStartReview}>
              Start Review
            </button>
          ) : (
            <>
              <button type="button" className={primary} disabled={busy} onClick={() => open("approve")}>
                Approve
              </button>
              <button type="button" className={secondary} disabled={busy} onClick={() => open("request-information")}>
                Request Information
              </button>
              <button type="button" className={danger} disabled={busy} onClick={() => open("reject")}>
                Reject
              </button>
            </>
          )}
        </div>
      ) : (
        <p className="text-sm text-on-surface-variant">No review actions for {label(status)} applications.</p>
      )}

      {form ? (
        <form onSubmit={submitForm} className="space-y-3 rounded-card border border-outline-variant bg-surface-low p-4">
          {form === "approve" ? (
            <label className="block text-sm font-semibold">
              Initial building admin phone
              <input
                className={field}
                value={adminPhone}
                onChange={(e) => setAdminPhone(e.target.value)}
                required
                inputMode="tel"
              />
              <span className="mt-1 block text-xs font-normal text-on-surface-variant">
                Pre-filled with the applicant contact phone; change it to appoint someone else.
              </span>
            </label>
          ) : null}
          <label className="block text-sm font-semibold">
            {form === "request-information" ? "Message to applicant" : "Reason"}
            <textarea
              className={field}
              rows={3}
              maxLength={1000}
              value={text}
              onChange={(e) => setText(e.target.value)}
              required
            />
          </label>
          <div className="flex gap-2">
            <button type="submit" className={form === "reject" ? danger : primary} disabled={busy}>
              {form === "approve" ? "Confirm approval" : form === "reject" ? "Confirm rejection" : "Send request"}
            </button>
            <button type="button" className={secondary} disabled={busy} onClick={() => setForm(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {failure ? <ApiErrorNotice failure={failure} onRefresh={onRefresh} /> : null}
      {done ? (
        <p role="status" className="rounded-card bg-success-container px-4 py-3 text-sm text-on-success-container">
          {done}
        </p>
      ) : null}
    </section>
  );
}
