import { notFound, redirect } from "next/navigation";
import { ApplicationsScreen } from "@/components/ApplicationsScreen";
import { NotAvailable } from "@/components/NotAvailable";
import type { ReviewStatus } from "@/lib/buildingApplications";
import { findNavEntry, hrefOf } from "@/lib/nav";
import { currentSession } from "@/lib/session";

/** Implemented §149.2 screens keyed by `section/item`; the review queue opens on the item's tab. */
const APPLICATION_TABS: Record<string, ReviewStatus> = {
  "buildings/applications": "SUBMITTED",
  "buildings/under-review": "UNDER_REVIEW",
};

/** Section item screens; items whose F6 task has not landed render the NotAvailable placeholder. */
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
  const title = `${entry.section.label} · ${entry.item.label}`;
  const tab = APPLICATION_TABS[`${entry.section.slug}/${entry.item.slug}`];
  if (tab) return <ApplicationsScreen key={tab} title={title} initialStatus={tab} />;
  const note =
    entry.section.slug === "buildings" && entry.item.slug === "archived"
      ? "Archived buildings have no backend state yet (BRD §149.2, BOC-03)."
      : undefined;
  return <NotAvailable title={title} note={note} />;
}
