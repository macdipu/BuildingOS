import { feeMessage, isStaleConflict, type ApiFailure } from "@/lib/buildingApplications";

/**
 * Failed call: always shows the backend error code. For building applications
 * (default), a stale-state 409 offers a refresh and fee preconditions get a
 * plain-language explanation. `context="plain"` is for endpoints without a
 * version check, where a 409 is a business-rule conflict, not stale state.
 */
export function ApiErrorNotice({
  failure,
  onRefresh,
  context = "application",
}: {
  failure: ApiFailure;
  onRefresh?: () => void;
  context?: "application" | "plain";
}) {
  const application = context === "application";
  const stale = application && isStaleConflict(failure);
  const fee = application ? feeMessage(failure) : null;
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
