import { notFound, redirect } from "next/navigation";
import { NotAvailable } from "@/components/NotAvailable";
import { findNavEntry, hrefOf } from "@/lib/nav";
import { currentSession } from "@/lib/session";

/** Placeholder for every §149.2 section item until its F6 task implements it. */
export default async function SectionPage({ params }: PageProps<"/[section]/[[...item]]">) {
  const { section, item } = await params;
  const session = await currentSession();
  if (!session) redirect("/login");
  const entry = findNavEntry(session.roles, section, item?.[0]);
  if (!entry || (item && item.length > 1)) notFound();
  if (!entry.item) {
    const first = entry.section.items[0];
    if (first) redirect(hrefOf(entry.section, first));
    notFound();
  }
  const note =
    entry.section.slug === "buildings" && entry.item.slug === "archived"
      ? "Archived buildings have no backend state yet (BRD §149.2, BOC-03)."
      : undefined;
  return <NotAvailable title={`${entry.section.label} · ${entry.item.label}`} note={note} />;
}
