import type { PlatformRole } from "./claims";

export interface NavItem {
  slug: string;
  label: string;
  /** Roles that see this item; undefined = inherit the section's roles. */
  roles?: PlatformRole[];
}

export interface NavSection {
  slug: string;
  label: string;
  roles: PlatformRole[];
  items: NavItem[];
}

const ALL: PlatformRole[] = [
  "SUPER_ADMIN",
  "PLATFORM_ADMIN",
  "ONBOARDING_AGENT",
  "SUPPORT_AGENT",
  "SUBSCRIPTION_ADMIN",
];

/**
 * BRD §149.2 navigation tree, labels and order verbatim. Role visibility follows
 * the BRD §4.1 platform role permissions and only hides menu entries; the
 * backend still authorizes every call. Subscriptions items name user
 * subscriptions (BOS-010 D-22, D-35); Trials / Past Due appear once those states exist.
 */
export const NAV: NavSection[] = [
  { slug: "", label: "Dashboard", roles: ALL, items: [] },
  {
    slug: "buildings",
    label: "Buildings",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN"],
    items: [
      { slug: "applications", label: "Applications" },
      { slug: "under-review", label: "Under Review" },
      { slug: "onboarding", label: "Onboarding" },
      { slug: "active", label: "Active Buildings" },
      { slug: "suspended", label: "Suspended Buildings" },
      { slug: "archived", label: "Archived Buildings" },
    ],
  },
  {
    slug: "users",
    label: "Users",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN"],
    items: [
      { slug: "platform-users", label: "Platform Users" },
      { slug: "building-admins", label: "Building Admins" },
      { slug: "onboarding-agents", label: "Onboarding Agents" },
      { slug: "support-agents", label: "Support Agents" },
    ],
  },
  {
    slug: "subscriptions",
    label: "Subscriptions",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SUBSCRIPTION_ADMIN"],
    items: [
      { slug: "plans", label: "Plans" },
      { slug: "user-subscriptions", label: "User Subscriptions" },
      { slug: "suspended-cancelled", label: "Suspended / Cancelled" },
    ],
  },
  {
    slug: "support",
    label: "Support",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "ONBOARDING_AGENT", "SUPPORT_AGENT"],
    items: [
      {
        slug: "assisted-onboarding",
        label: "Assisted Onboarding",
        roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "ONBOARDING_AGENT"],
      },
      {
        slug: "active-sessions",
        label: "Active Support Sessions",
        roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SUPPORT_AGENT"],
      },
      {
        slug: "history",
        label: "Support History",
        roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SUPPORT_AGENT"],
      },
    ],
  },
  {
    slug: "system",
    label: "System",
    roles: ["SUPER_ADMIN"],
    items: [
      { slug: "settings", label: "Platform Settings" },
      { slug: "entitlements", label: "Feature / Entitlement Configuration" },
      { slug: "audit-logs", label: "Audit Logs" },
      { slug: "health", label: "System Health" },
    ],
  },
];

const allowed = (roles: PlatformRole[], who: PlatformRole[]) => who.some((r) => roles.includes(r));

/** Nav filtered to what the signed-in roles may see; empty sections are dropped. */
export function visibleNav(roles: PlatformRole[]): NavSection[] {
  return NAV.filter((s) => allowed(roles, s.roles))
    .map((s) => ({ ...s, items: s.items.filter((i) => allowed(roles, i.roles ?? s.roles)) }))
    .filter((s) => s.slug === "" || s.items.length > 0);
}

/** Resolves /section/item to its nav entry if the roles may see it, else null. */
export function findNavEntry(
  roles: PlatformRole[],
  section: string,
  item?: string,
): { section: NavSection; item?: NavItem } | null {
  const s = visibleNav(roles).find((x) => x.slug === section);
  if (!s) return null;
  if (item === undefined) return { section: s };
  const i = s.items.find((x) => x.slug === item);
  return i ? { section: s, item: i } : null;
}

export function hrefOf(section: NavSection, item?: NavItem): string {
  if (section.slug === "") return "/";
  return item ? `/${section.slug}/${item.slug}` : `/${section.slug}`;
}
