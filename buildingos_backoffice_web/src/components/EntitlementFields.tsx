"use client";

import { ENTITLEMENTS, TEXT_MAX, type EntitlementMap } from "@/lib/subscriptions";
import { fieldClass, fieldError } from "./formStyles";

/** Editable form of an entitlement map: flags "" | "true" | "false", limits and text as typed. */
export interface EntitlementDraft {
  values: Record<string, string>;
  /** Keys the API returned that this console does not know; sent back unchanged. */
  extra: EntitlementMap;
}

const KNOWN = new Set<string>(ENTITLEMENTS.map((e) => e.key));

export function toDraft(map: EntitlementMap | null | undefined): EntitlementDraft {
  const values: Record<string, string> = {};
  const extra: EntitlementMap = {};
  for (const e of ENTITLEMENTS) {
    const v = map?.[e.key];
    values[e.key] = v === undefined || v === null ? "" : String(v);
  }
  for (const [k, v] of Object.entries(map ?? {})) if (!KNOWN.has(k)) extra[k] = v;
  return { values, extra };
}

/** Client-side checks mirroring the subscription-service `Entitlements` rules; the API still decides. */
export function fromDraft(draft: EntitlementDraft): { map: EntitlementMap; errors: Record<string, string> } {
  const map: EntitlementMap = { ...draft.extra };
  const errors: Record<string, string> = {};
  for (const e of ENTITLEMENTS) {
    const raw = (draft.values[e.key] ?? "").trim();
    if (raw === "") continue;
    if (e.kind === "FLAG") map[e.key] = raw === "true";
    else if (e.kind === "LIMIT") {
      if (!/^\d+$/.test(raw)) errors[e.key] = "Must be a non-negative whole number (leave empty for unlimited).";
      else map[e.key] = Number(raw);
    } else if (raw.length > TEXT_MAX) errors[e.key] = `Must be 1-${TEXT_MAX} characters.`;
    else map[e.key] = raw;
  }
  return { map, errors };
}

/** §149.16 entitlement keys as structured fields (not marketing bullets). */
export function EntitlementFields({
  draft,
  errors,
  onChange,
}: {
  draft: EntitlementDraft;
  errors: Record<string, string>;
  onChange: (next: EntitlementDraft) => void;
}) {
  const set = (key: string, value: string) => onChange({ ...draft, values: { ...draft.values, [key]: value } });
  const extraKeys = Object.keys(draft.extra);
  return (
    <fieldset className="grid gap-3 sm:grid-cols-2">
      <legend className="mb-2 text-sm font-semibold">Entitlements</legend>
      {ENTITLEMENTS.map((e) => {
        const id = `ent-${e.key}`;
        const value = draft.values[e.key] ?? "";
        return (
          <label key={e.key} htmlFor={id} className="block text-sm font-semibold">
            {e.label} <span className="font-mono text-xs font-normal text-on-surface-variant">{e.key}</span>
            {e.kind === "FLAG" ? (
              <select id={id} className={fieldClass} value={value} onChange={(ev) => set(e.key, ev.target.value)}>
                <option value="">Not set</option>
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
            ) : (
              <input
                id={id}
                className={fieldClass}
                value={value}
                inputMode={e.kind === "LIMIT" ? "numeric" : undefined}
                placeholder={e.kind === "LIMIT" ? "Unlimited" : "Not set"}
                aria-invalid={errors[e.key] ? true : undefined}
                onChange={(ev) => set(e.key, ev.target.value)}
              />
            )}
            {errors[e.key] ? <span className={fieldError}>{errors[e.key]}</span> : null}
          </label>
        );
      })}
      {extraKeys.length ? (
        <p className="text-xs text-on-surface-variant sm:col-span-2">
          Kept unchanged (not editable here): {extraKeys.join(", ")}
        </p>
      ) : null}
    </fieldset>
  );
}
