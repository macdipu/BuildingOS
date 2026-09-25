import { notFound, redirect } from "next/navigation";
import { ApplicationsScreen } from "@/components/ApplicationsScreen";
import { NotAvailable } from "@/components/NotAvailable";
import { PlansScreen } from "@/components/PlansScreen";
import { UserSubscriptionScreen } from "@/components/UserSubscriptionScreen";
import type { ReviewStatus } from "@/lib/buildingApplications";
import { findNavEntry, hrefOf } from "@/lib/nav";
import { currentSession } from "@/lib/session";

/** Implemented §149.2 screens keyed by `section/item`; the review queue opens on the item's tab. */
const APPLICATION_TABS: Record<string, ReviewStatus> = {
  "buildings/applications": "SUBMITTED",
  "buildings/under-review": "UNDER_REVIEW",
};

/** Placeholder notes for items that stay NotAvailable for a known reason. */
const NOTES: Record<string, string> = {
  "buildings/archived": "Archived buildings have no backend state yet (BRD §149.2, BOC-03).",
  "subscriptions/suspended-cancelled":
    "Needs the platform-wide subscription list, which lands with F6-T7b (D-35).",
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
  const screen = `${entry.section.slug}/${entry.item.slug}`;
  const tab = APPLICATION_TABS[screen];
  if (tab) return <ApplicationsScreen key={tab} title={title} initialStatus={tab} />;
  if (screen === "subscriptions/plans") return <PlansScreen title={title} />;
  if (screen === "subscriptions/user-subscriptions") return <UserSubscriptionScreen title={title} />;
  const note = NOTES[screen];
  return <NotAvailable title={title} note={note} />;
}
