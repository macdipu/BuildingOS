/** Shared control classes (DESIGN.md tokens via globals.css) for console forms. */
const button = "h-9 rounded-control px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60";
export const primaryButton = `${button} bg-primary-container text-on-primary hover:bg-primary-hover`;
export const secondaryButton = `${button} border border-outline-variant bg-surface-lowest hover:bg-surface-low`;
export const dangerButton = `${button} border border-error/40 bg-surface-lowest text-error hover:bg-error-container`;
export const fieldClass =
  "mt-1 w-full rounded-control border border-outline-variant bg-surface-lowest px-3 py-2 text-sm focus:border-primary focus:outline-none";
export const panelClass = "rounded-panel border border-outline-variant bg-surface-lowest p-5 shadow-tier1";
export const fieldError = "mt-1 block text-xs font-normal text-error";
export const successClass = "rounded-card bg-success-container px-4 py-3 text-sm text-on-success-container";
