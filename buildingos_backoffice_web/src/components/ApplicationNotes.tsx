"use client";

import { useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { addNote, formatDate, type ApiFailure, type InternalNote } from "@/lib/buildingApplications";

/** Internal review notes (never shown to the applicant) with an add-note form. */
export function ApplicationNotes({
  applicationId,
  notes,
  onAdded,
}: {
  applicationId: string;
  notes: InternalNote[];
  onAdded: () => void;
}) {
  const [body, setBody] = useState("");
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<ApiFailure | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setFailure(null);
    try {
      const r = await addNote(applicationId, body.trim());
      if (r.ok) {
        setBody("");
        onAdded();
      } else {
        setFailure(r);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      {notes.length === 0 ? (
        <p className="text-sm text-on-surface-variant">No internal notes yet.</p>
      ) : (
        <ul className="space-y-2">
          {notes.map((n) => (
            <li key={n.id} className="rounded-card bg-surface-low px-3 py-2 text-sm">
              <p className="whitespace-pre-wrap">{n.body}</p>
              <p className="mt-1 text-xs text-on-surface-variant">
                {formatDate(n.createdAt)} · {n.authorUserId}
              </p>
            </li>
          ))}
        </ul>
      )}
      <form onSubmit={submit} className="space-y-2">
        <label className="block text-sm font-semibold">
          Add internal note
          <textarea
            className="mt-1 w-full rounded-control border border-outline-variant bg-surface-lowest px-3 py-2 text-sm focus:border-primary focus:outline-none"
            rows={2}
            maxLength={2000}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
          />
        </label>
        <button
          type="submit"
          disabled={busy || body.trim() === ""}
          className="h-8 rounded-control border border-outline-variant bg-surface-lowest px-3 text-xs font-semibold hover:bg-surface-low disabled:opacity-60"
        >
          Add note
        </button>
      </form>
      {failure ? <ApiErrorNotice failure={failure} /> : null}
    </div>
  );
}
