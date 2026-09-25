"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { formatDate, label } from "@/lib/buildingApplications";
import {
  BUILDING_CREATION_FEE,
  FEE_REFERENCE_TYPE,
  UUID_PATTERN,
  getFeeSchedule,
  getFeeStatus,
  recordFeePayment,
  updateFeeSchedule,
  type ApiFailure,
  type FeeSchedule,
  type FeeStatus,
} from "@/lib/subscriptions";
import { fieldClass, fieldError, panelClass, primaryButton, secondaryButton, successClass } from "./formStyles";

const AMOUNT = /^\d{1,10}(\.\d{1,2})?$/;
const CURRENCY = /^[A-Z]{3}$/;
const EXTERNAL_REFERENCE_MAX = 128;

/**
 * Building-creation fee (D-24): schedule, per-application settlement status and
 * manual (offline) payment records. An unconfigured fee fails closed.
 */
export function CreationFeePanel() {
  const [schedule, setSchedule] = useState<FeeSchedule | null>(null);
  const [scheduleFailure, setScheduleFailure] = useState<ApiFailure | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getFeeSchedule(BUILDING_CREATION_FEE).then((r) => {
      if (cancelled) return;
      if (r.ok) setSchedule(r.data);
      else setScheduleFailure(r);
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section aria-label="Building creation fee" className={`${panelClass} space-y-5`}>
      <div>
        <h2 className="text-base font-semibold">Building creation fee</h2>
        <p className="text-sm text-on-surface-variant">
          One-time fee checked before a building application is approved. Payments are collected offline and recorded here.
        </p>
      </div>
      {loading ? (
        <p className="text-sm text-on-surface-variant">Loading fee…</p>
      ) : (
        <ScheduleForm
          schedule={schedule}
          loadFailure={scheduleFailure}
          onSaved={(s) => {
            setSchedule(s);
            setScheduleFailure(null);
          }}
        />
      )}
      <FeeStatusLookup defaultCurrency={schedule?.currency ?? ""} />
    </section>
  );
}

function ScheduleForm({
  schedule,
  loadFailure,
  onSaved,
}: {
  schedule: FeeSchedule | null;
  loadFailure: ApiFailure | null;
  onSaved: (s: FeeSchedule) => void;
}) {
  const [amount, setAmount] = useState(schedule ? String(schedule.amount) : "");
  const [currency, setCurrency] = useState(schedule?.currency ?? "");
  const [required, setRequired] = useState(schedule?.required ?? true);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState(false);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    setDone(false);
    const next: Record<string, string> = {};
    if (!AMOUNT.test(amount.trim())) next.amount = "Non-negative amount with at most 2 decimals.";
    if (!CURRENCY.test(currency.trim())) next.currency = "3-letter ISO currency code.";
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const r = await updateFeeSchedule(BUILDING_CREATION_FEE, {
        amount: Number(amount.trim()),
        currency: currency.trim(),
        required,
      });
      if (r.ok) {
        onSaved(r.data);
        setDone(true);
      } else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={save} noValidate aria-label="Fee schedule" className="space-y-3">
      {loadFailure ? <ApiErrorNotice failure={loadFailure} context="plain" /> : null}
      {schedule ? (
        <p className="text-xs text-on-surface-variant">Last updated {formatDate(schedule.updatedAt)}</p>
      ) : null}
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-sm font-semibold">
          Amount
          <input className={fieldClass} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
          {errors.amount ? <span className={fieldError}>{errors.amount}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Currency
          <input className={fieldClass} value={currency} maxLength={3} onChange={(e) => setCurrency(e.target.value.toUpperCase())} />
          {errors.currency ? <span className={fieldError}>{errors.currency}</span> : null}
        </label>
      </div>
      <label className="flex items-center gap-2 text-sm font-semibold">
        <input type="checkbox" checked={required} onChange={(e) => setRequired(e.target.checked)} />
        Fee required before approval
      </label>
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
      {done ? (
        <p role="status" className={successClass}>
          Fee schedule saved.
        </p>
      ) : null}
      <button type="submit" className={primaryButton} disabled={busy}>
        Save fee
      </button>
    </form>
  );
}

function FeeStatusLookup({ defaultCurrency }: { defaultCurrency: string }) {
  const [applicationId, setApplicationId] = useState("");
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [status, setStatus] = useState<{ id: string; data: FeeStatus } | null>(null);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [busy, setBusy] = useState(false);
  const [recording, setRecording] = useState(false);

  async function load(id: string) {
    setBusy(true);
    setFailure(null);
    try {
      const r = await getFeeStatus(BUILDING_CREATION_FEE, FEE_REFERENCE_TYPE, id);
      if (r.ok) setStatus({ id, data: r.data });
      else {
        setStatus(null);
        setFailure(r);
      }
    } finally {
      setBusy(false);
    }
  }

  function lookup(e: React.FormEvent) {
    e.preventDefault();
    const id = applicationId.trim();
    if (!UUID_PATTERN.test(id)) {
      setLookupError("Enter a building application id (UUID).");
      return;
    }
    setLookupError(null);
    setRecording(false);
    load(id);
  }

  return (
    <div className="space-y-3 border-t border-outline-variant pt-4">
      <form onSubmit={lookup} noValidate aria-label="Fee status lookup" className="flex flex-wrap items-end gap-2">
        <label className="block min-w-64 flex-1 text-sm font-semibold">
          Building application id
          <input className={fieldClass} value={applicationId} onChange={(e) => setApplicationId(e.target.value)} />
          {lookupError ? <span className={fieldError}>{lookupError}</span> : null}
        </label>
        <button type="submit" className={secondaryButton} disabled={busy}>
          Check fee status
        </button>
      </form>
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
      {status ? (
        <div className="space-y-3">
          <p className="text-sm">
            Status: <span className="font-semibold">{label(status.data.status)}</span> · Fee{" "}
            {status.data.schedule.amount} {status.data.schedule.currency}
          </p>
          {status.data.payments.length === 0 ? (
            <p className="text-sm text-on-surface-variant">No payments recorded.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-3 py-2 font-semibold">Paid on</th>
                  <th className="px-3 py-2 font-semibold">Amount</th>
                  <th className="px-3 py-2 font-semibold">Method</th>
                  <th className="px-3 py-2 font-semibold">External reference</th>
                  <th className="px-3 py-2 font-semibold">Recorded</th>
                </tr>
              </thead>
              <tbody>
                {status.data.payments.map((p) => (
                  <tr key={p.id} className="border-t border-outline-variant">
                    <td className="px-3 py-2">{p.paidOn}</td>
                    <td className="px-3 py-2">
                      {p.amount} {p.currency}
                    </td>
                    <td className="px-3 py-2">{label(p.method)}</td>
                    <td className="px-3 py-2">{p.externalReference ?? "—"}</td>
                    <td className="px-3 py-2 text-on-surface-variant">{formatDate(p.recordedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {recording ? (
            <PaymentForm
              referenceId={status.id}
              defaultCurrency={status.data.schedule.currency || defaultCurrency}
              onRecorded={() => {
                setRecording(false);
                load(status.id);
              }}
              onCancel={() => setRecording(false)}
            />
          ) : (
            <button type="button" className={primaryButton} onClick={() => setRecording(true)}>
              Record offline payment
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
}

function PaymentForm({
  referenceId,
  defaultCurrency,
  onRecorded,
  onCancel,
}: {
  referenceId: string;
  defaultCurrency: string;
  onRecorded: () => void;
  onCancel: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState(defaultCurrency);
  const [externalReference, setExternalReference] = useState("");
  const [paidOn, setPaidOn] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setFailure(null);
    const next: Record<string, string> = {};
    if (!AMOUNT.test(amount.trim()) || Number(amount) <= 0) next.amount = "Positive amount with at most 2 decimals.";
    if (!CURRENCY.test(currency.trim())) next.currency = "3-letter ISO currency code.";
    if (!/^\d{4}-\d{2}-\d{2}$/.test(paidOn)) next.paidOn = "Payment date is required.";
    if (externalReference.trim().length > EXTERNAL_REFERENCE_MAX) {
      next.externalReference = `At most ${EXTERNAL_REFERENCE_MAX} characters.`;
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const r = await recordFeePayment(BUILDING_CREATION_FEE, {
        referenceType: FEE_REFERENCE_TYPE,
        referenceId,
        amount: Number(amount.trim()),
        currency: currency.trim(),
        ...(externalReference.trim() ? { externalReference: externalReference.trim() } : {}),
        paidOn,
      });
      if (r.ok) onRecorded();
      else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate aria-label="Record payment" className="space-y-3 rounded-card border border-outline-variant bg-surface-low p-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-sm font-semibold">
          Amount paid
          <input className={fieldClass} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
          {errors.amount ? <span className={fieldError}>{errors.amount}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Payment currency
          <input className={fieldClass} value={currency} maxLength={3} onChange={(e) => setCurrency(e.target.value.toUpperCase())} />
          {errors.currency ? <span className={fieldError}>{errors.currency}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          Paid on
          <input type="date" className={fieldClass} value={paidOn} onChange={(e) => setPaidOn(e.target.value)} />
          {errors.paidOn ? <span className={fieldError}>{errors.paidOn}</span> : null}
        </label>
        <label className="block text-sm font-semibold">
          External reference (optional)
          <input className={fieldClass} value={externalReference} onChange={(e) => setExternalReference(e.target.value)} />
          {errors.externalReference ? <span className={fieldError}>{errors.externalReference}</span> : null}
        </label>
      </div>
      {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
      <div className="flex gap-2">
        <button type="submit" className={primaryButton} disabled={busy}>
          Record payment
        </button>
        <button type="button" className={secondaryButton} disabled={busy} onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
