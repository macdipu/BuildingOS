import { notFound, redirect } from "next/navigation";
import { BuildingsScreen } from "@/components/BuildingsScreen";
import { AuditLogScreen } from "@/components/AuditLogScreen";
import { ApplicationsScreen } from "@/components/ApplicationsScreen";
import { EntitlementConfigScreen } from "@/components/EntitlementConfigScreen";
import { NotAvailable } from "@/components/NotAvailable";
import { OnboardingSessionsScreen } from "@/components/OnboardingSessionsScreen";
import { PlansScreen } from "@/components/PlansScreen";
import { SupportSessionsScreen } from "@/components/SupportSessionsScreen";
import { SystemHealthScreen } from "@/components/SystemHealthScreen";
import { UsersScreen } from "@/components/UsersScreen";
import { UserSubscriptionScreen } from "@/components/UserSubscriptionScreen";
import type { PlatformRole } from "@/lib/claims";
import type { BuildingStatus } from "@/lib/buildings";
import type { ReviewStatus } from "@/lib/buildingApplications";
import { findNavEntry, hrefOf } from "@/lib/nav";
import { currentSession } from "@/lib/session";

/** Implemented §149.2 screens keyed by `section/item`; the review queue opens on the item's tab. */
const APPLICATION_TABS: Record<string, ReviewStatus> = {
  "buildings/applications": "SUBMITTED",
  "buildings/under-review": "UNDER_REVIEW",
};

const USER_LISTS: Record<string, PlatformRole | "ALL"> = {
  "users/platform-users": "ALL",
  "users/onboarding-agents": "ONBOARDING_AGENT",
  "users/support-agents": "SUPPORT_AGENT",
};

const BUILDING_LISTS: Record<string, BuildingStatus> = {
  "buildings/onboarding": "ONBOARDING",
  "buildings/active": "ACTIVE",
  "buildings/suspended": "SUSPENDED",
};

/** Placeholder notes for items that stay NotAvailable for a known reason. */
const NOTES: Record<string, string> = {
  "users/building-admins":
    "Building admins are building memberships owned by building-service; no platform API lists them yet.",
  "buildings/archived": "Archived buildings have no backend state yet (BRD §149.2, BOC-03).",
  "system/settings": "No platform settings are stored or editable yet; there are no existing configuration surfaces to list.",
  "subscriptions/suspended-cancelled":
    "Subscriptions have only an active state today; suspended and cancelled states do not exist yet (D-35).",
};

/** Section item screens; items whose F6 task has not landed render the NotAvailable placeholder. */
export default async function SectionPage({ params, searchParams }: PageProps<"/[section]/[[...item]]">) {
  const { section, item } = await params;
  const query = await searchParams;
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
  const buildingStatus = BUILDING_LISTS[screen];
  if (buildingStatus) return <BuildingsScreen key={buildingStatus} title={title} status={buildingStatus} />;
  if (screen === "support/assisted-onboarding") {
    const buildingId = typeof query.buildingId === "string" ? query.buildingId : "";
    return <OnboardingSessionsScreen title={title} userId={session.userId} roles={session.roles} initialBuildingId={buildingId} />;
  }
  if (screen === "support/active-sessions" || screen === "support/history") {
    const mode = screen === "support/history" ? "history" : "active";
    return <SupportSessionsScreen title={title} mode={mode} userId={session.userId} roles={session.roles} />;
  }
  if (screen === "system/audit-logs") {
    const entityType = typeof query.entityType === "string" ? query.entityType : "";
    return <AuditLogScreen title={title} initialEntityType={entityType} />;
  }
  if (screen === "system/health") return <SystemHealthScreen title={title} />;
  if (screen === "system/entitlements") return <EntitlementConfigScreen title={title} />;
  const userList = USER_LISTS[screen];
  if (userList) {
    return (
      <UsersScreen
        key={screen}
        title={title}
        fixedRole={userList === "ALL" ? undefined : userList}
        userId={session.userId}
        roles={session.roles}
      />
    );
  }
  if (screen === "subscriptions/plans") return <PlansScreen title={title} />;
  if (screen === "subscriptions/user-subscriptions") return <UserSubscriptionScreen title={title} />;
  const note = NOTES[screen];
  return <NotAvailable title={title} note={note} />;
}
