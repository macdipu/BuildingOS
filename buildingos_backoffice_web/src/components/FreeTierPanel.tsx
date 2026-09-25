"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { EntitlementFields, fromDraft, toDraft, type EntitlementDraft } from "@/components/EntitlementFields";
import { getFreeTier, updateFreeTier, type ApiFailure } from "@/lib/subscriptions";
import { panelClass, primaryButton, successClass } from "./formStyles";

/** Free-tier (default) entitlements every user gets; PUT replaces the whole map. */
export function FreeTierPanel() {
  const [draft, setDraft] = useState<EntitlementDraft | null>(null);
  const [loadFailure, setLoadFailure] = useState<ApiFailure | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getFreeTier().then((r) => {
      if (cancelled) return;
      if (r.ok) setDraft(toDraft(r.data));
      else setLoadFailure(r);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!draft) return;
    setFailure(null);
    setDone(false);
    const { map, errors: next } = fromDraft(draft);
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const r = await updateFreeTier(map);
      if (r.ok) {
        setDraft(toDraft(r.data));
        setDone(true);
      } else setFailure(r);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-label="Free tier" className={panelClass}>
      <h2 className="text-base font-semibold">Free tier</h2>
      <p className="mb-3 text-sm text-on-surface-variant">Default entitlements every user gets without a plan.</p>
      {loadFailure ? (
        <ApiErrorNotice failure={loadFailure} context="plain" />
      ) : !draft ? (
        <p className="text-sm text-on-surface-variant">Loading free tier…</p>
      ) : (
        <form onSubmit={save} noValidate className="space-y-3">
          <EntitlementFields draft={draft} errors={errors} onChange={setDraft} />
          {failure ? <ApiErrorNotice failure={failure} context="plain" /> : null}
          {done ? (
            <p role="status" className={successClass}>
              Free tier saved.
            </p>
          ) : null}
          <button type="submit" className={primaryButton} disabled={busy}>
            Save free tier
          </button>
        </form>
      )}
    </section>
  );
}
