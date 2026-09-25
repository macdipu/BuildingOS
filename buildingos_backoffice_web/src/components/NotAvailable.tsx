/** Explicit placeholder for sections whose task has not landed yet — never fabricated data. */
export function NotAvailable({ title, note }: { title: string; note?: string }) {
  return (
    <section>
      <h1 className="mb-6 text-[22px] font-semibold">{title}</h1>
      <div className="rounded-panel border border-outline-variant bg-surface-lowest p-8 shadow-tier1">
        <p className="font-semibold">Not available yet</p>
        <p className="mt-1 text-sm text-on-surface-variant">
          {note ?? "This section is planned and will appear in a later release."}
        </p>
      </div>
    </section>
  );
}
