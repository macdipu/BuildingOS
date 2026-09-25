"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { ApplicationNotes } from "@/components/ApplicationNotes";
import { ReviewActions } from "@/components/ReviewActions";
import {
  documentHref,
  formatDate,
  getApplication,
  getDocuments,
  getDuplicates,
  getHistory,
  getNotes,
  label,
  type ApiResult,
  type ApplicationDocument,
  type BuildingApplication,
  type DuplicateMatch,
  type InternalNote,
  type Transition,
} from "@/lib/buildingApplications";

interface Loaded {
  key: string;
  application: ApiResult<BuildingApplication>;
  history: ApiResult<Transition[]>;
  documents: ApiResult<ApplicationDocument[]>;
  notes: ApiResult<InternalNote[]>;
  duplicates: ApiResult<DuplicateMatch[]>;
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="border-t border-outline-variant pt-4">
      <h3 className="mb-2 text-[11px] font-semibold uppercase tracking-[0.04em] text-on-surface-variant">{title}</h3>
      {children}
    </section>
  );
}

function Fields({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="grid grid-cols-[minmax(0,2fr)_minmax(0,3fr)] gap-x-3 gap-y-1.5 text-sm">
      {rows.map(([k, v]) => (
        <div key={k} className="contents">
          <dt className="text-on-surface-variant">{k}</dt>
          <dd className="break-words">{v ?? "—"}</dd>
        </div>
      ))}
    </dl>
  );
}

function Listed<T>({
  result,
  empty,
  render,
}: {
  result: ApiResult<T[]>;
  empty: string;
  render: (items: T[]) => React.ReactNode;
}) {
  if (!result.ok) return <ApiErrorNotice failure={result} />;
  if (result.data.length === 0) return <p className="text-sm text-on-surface-variant">{empty}</p>;
  return <>{render(result.data)}</>;
}

/** §149.5 side review panel: one application's details, history, notes, duplicates and actions. */
export function ApplicationReviewPanel({
  applicationId,
  onChanged,
  onClose,
}: {
  applicationId: string;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [version, setVersion] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${applicationId}#${version}`;

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      getApplication(applicationId),
      getHistory(applicationId),
      getDocuments(applicationId),
      getNotes(applicationId),
      getDuplicates(applicationId),
    ]).then(([application, history, documents, notes, duplicates]) => {
      if (!cancelled) setLoaded({ key, application, history, documents, notes, duplicates });
    });
    return () => {
      cancelled = true;
    };
  }, [applicationId, key]);

  const reload = () => setVersion((v) => v + 1);
  const current = loaded && loaded.key.startsWith(`${applicationId}#`) ? loaded : null;

  return (
    <aside
      aria-label="Application review"
      className="w-full shrink-0 space-y-4 rounded-panel border border-outline-variant bg-surface-lowest p-5 shadow-tier2 lg:w-[420px]"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs text-on-surface-variant">
            {current?.application.ok ? current.application.data.applicationNumber : "Application"}
          </p>
          <h2 className="text-lg font-semibold">
            {current?.application.ok ? (current.application.data.buildingName ?? "Unnamed building") : "Loading…"}
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="h-8 rounded-control border border-outline-variant px-2 text-xs font-semibold hover:bg-surface-low"
        >
          Close
        </button>
      </div>

      {!current ? (
        <p className="text-sm text-on-surface-variant">Loading application…</p>
      ) : !current.application.ok ? (
        <ApiErrorNotice failure={current.application} onRefresh={reload} />
      ) : (
        <ReviewBody
          loaded={current}
          application={current.application.data}
          onChanged={() => {
            reload();
            onChanged();
          }}
          onRefresh={reload}
        />
      )}
    </aside>
  );
}

function ReviewBody({
  loaded,
  application: a,
  onChanged,
  onRefresh,
}: {
  loaded: Loaded;
  application: BuildingApplication;
  onChanged: () => void;
  onRefresh: () => void;
}) {
  const coords = a.latitude != null && a.longitude != null ? `${a.latitude}, ${a.longitude}` : null;
  return (
    <>
      <div className="flex flex-wrap items-center gap-2 text-xs">
        <span className="rounded-full bg-surface-high px-2.5 py-0.5 font-semibold">{label(a.status)}</span>
        <span className="text-on-surface-variant">Submitted {formatDate(a.submittedAt)}</span>
      </div>

      <ReviewActions key={a.id} application={a} onChanged={onChanged} onRefresh={onRefresh} />

      {a.infoRequestMessage ? (
        <p className="rounded-card bg-warning-container px-3 py-2 text-sm text-on-warning-container">
          Information requested: {a.infoRequestMessage}
        </p>
      ) : null}
      {a.rejectionReason ? (
        <p className="rounded-card bg-error-container px-3 py-2 text-sm">Rejection reason: {a.rejectionReason}</p>
      ) : null}

      <Section title="Applicant">
        <Fields
          rows={[
            ["Contact name", a.contactName],
            ["Contact phone", a.contactPhone],
            ["Contact email", a.contactEmail],
            ["Relationship", label(a.applicantRelationship)],
            ["Relationship note", a.relationshipNote],
            ["Source", label(a.source)],
          ]}
        />
      </Section>

      <Section title="Building">
        <Fields
          rows={[
            ["Name", a.buildingName],
            ["Type", label(a.buildingType)],
            ["Address", a.address],
            ["Area", a.area],
            ["District", a.district],
            ["Postal code", a.postalCode],
            ["Total floors", a.totalFloors],
            ["Estimated units", a.estimatedUnits],
            ["Management", label(a.managementType)],
            ["Coordinates", coords],
          ]}
        />
      </Section>

      <Section title="Verification documents">
        <Listed
          result={loaded.documents}
          empty="No documents uploaded."
          render={(docs) => (
            <ul className="space-y-1 text-sm">
              {docs.map((d) => (
                <li key={d.id}>
                  <a
                    href={documentHref(a.id, d.id)}
                    target="_blank"
                    rel="noreferrer"
                    className="font-semibold text-primary hover:underline"
                  >
                    {d.fileName}
                  </a>{" "}
                  <span className="text-xs text-on-surface-variant">
                    {d.contentType} · {Math.ceil(d.sizeBytes / 1024)} KB · {formatDate(d.uploadedAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        />
      </Section>

      <Section title="Possible duplicates">
        <Listed
          result={loaded.duplicates}
          empty="No duplicate signals."
          render={(items) => (
            <ul className="space-y-2 text-sm">
              {items.map((d) => (
                <li key={`${d.kind}-${d.id}`} className="rounded-card bg-warning-container px-3 py-2">
                  <p className="font-semibold text-on-warning-container">
                    {label(d.kind)} {d.reference} · {label(d.status)}
                  </p>
                  <p>{[d.name, d.address, d.area, d.district].filter(Boolean).join(", ") || "—"}</p>
                  <p className="text-xs text-on-surface-variant">Matched on: {d.matchedOn.map(label).join(", ")}</p>
                </li>
              ))}
            </ul>
          )}
        />
      </Section>

      <Section title="Review history">
        <Listed
          result={loaded.history}
          empty="No status changes yet."
          render={(items) => (
            <ol className="space-y-2 text-sm">
              {items.map((t, i) => (
                <li key={`${t.occurredAt}-${i}`}>
                  <p className="font-semibold">
                    {t.fromStatus ? `${label(t.fromStatus)} → ` : ""}
                    {label(t.toStatus)}
                  </p>
                  <p className="text-xs text-on-surface-variant">{formatDate(t.occurredAt)}</p>
                  {t.reason ? <p className="whitespace-pre-wrap">{t.reason}</p> : null}
                </li>
              ))}
            </ol>
          )}
        />
      </Section>

      <Section title="Internal notes">
        {loaded.notes.ok ? (
          <ApplicationNotes applicationId={a.id} notes={loaded.notes.data} onAdded={onRefresh} />
        ) : (
          <ApiErrorNotice failure={loaded.notes} />
        )}
      </Section>
    </>
  );
}
