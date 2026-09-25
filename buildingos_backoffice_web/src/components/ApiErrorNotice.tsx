import { feeMessage, isStaleConflict, type ApiFailure } from "@/lib/buildingApplications";

/**
 * Failed call: always shows the backend error code; a stale-state 409 offers a
 * refresh, fee preconditions get a plain-language explanation.
 */
export function ApiErrorNotice({ failure, onRefresh }: { failure: ApiFailure; onRefresh?: () => void }) {
  const stale = isStaleConflict(failure);
  const fee = feeMessage(failure);
  return (
    <div role="alert" className="rounded-card border border-error/30 bg-error-container px-4 py-3 text-sm text-on-surface">
      <p className="font-semibold">
        {fee ?? (stale ? "This application changed since you loaded it." : "The request failed.")}
      </p>
      {failure.message && !fee ? <p className="mt-0.5">{failure.message}</p> : null}
      <p className="mt-1 font-mono text-xs text-on-surface-variant">Error code: {failure.code}</p>
      {stale && onRefresh ? (
        <button
          type="button"
          onClick={onRefresh}
          className="mt-2 h-8 rounded-control border border-outline-variant bg-surface-lowest px-3 text-xs font-semibold hover:bg-surface-low"
        >
          Refresh application
        </button>
      ) : null}
    </div>
  );
}
