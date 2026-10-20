"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { pageMeta } from "@/lib/buildingApplications";
import { listApplications } from "@/lib/buildingApplications";
import { listBuildings } from "@/lib/buildings";
import type { ApiResult } from "@/lib/api";
import type { PlatformRole } from "@/lib/claims";
import { listSessions } from "@/lib/onboardingSessions";
import { listPlans, listPlatformSubscriptions } from "@/lib/subscriptions";
import { listSupportSessions } from "@/lib/supportSessions";

type Count = { total: number } | { error: string };

interface Card {
  id: string;
  label: string;
  href: string;
  roles: PlatformRole[];
  load: () => Promise<ApiResult<unknown>>;
  /** Total from the same source as the linked list (list meta total, or the list length for unpaged plans). */
  total: (r: ApiResult<unknown> & { ok: true }) => number | null;
}

const ADMIN: PlatformRole[] = ["SUPER_ADMIN", "PLATFORM_ADMIN"];
const SUB: PlatformRole[] = ["SUPER_ADMIN", "PLATFORM_ADMIN", "SUBSCRIPTION_ADMIN"];
const SUPPORT: PlatformRole[] = ["SUPER_ADMIN", "PLATFORM_ADMIN", "SUPPORT_AGENT"];
const metaTotal = (r: { meta?: Record<string, unknown> }) => pageMeta(r.meta)?.total ?? null;

/** BRD §149.18 operational counts. Every number is the total of the list its card links to; nothing is derived. */
const CARDS: Card[] = [
  { id: "submitted", label: "Applications awaiting review", href: "/buildings/applications", roles: ADMIN, load: () => listApplications("SUBMITTED"), total: metaTotal },
  { id: "review", label: "Applications under review", href: "/buildings/under-review", roles: ADMIN, load: () => listApplications("UNDER_REVIEW"), total: metaTotal },
  { id: "onboarding", label: "Buildings onboarding", href: "/buildings/onboarding", roles: ADMIN, load: () => listBuildings("ONBOARDING", ""), total: metaTotal },
  { id: "active", label: "Active buildings", href: "/buildings/active", roles: ADMIN, load: () => listBuildings("ACTIVE", ""), total: metaTotal },
  { id: "suspended", label: "Suspended buildings", href: "/buildings/suspended", roles: ADMIN, load: () => listBuildings("SUSPENDED", ""), total: metaTotal },
  { id: "assigned", label: "Onboarding sessions awaiting start", href: "/support/assisted-onboarding", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "ONBOARDING_AGENT"], load: () => listSessions({ status: "ASSIGNED" }), total: metaTotal },
  { id: "support", label: "Active support sessions", href: "/support/active-sessions", roles: SUPPORT, load: () => listSupportSessions({ active: true }), total: metaTotal },
  { id: "subscriptions", label: "User subscriptions", href: "/subscriptions/user-subscriptions", roles: SUB, load: () => listPlatformSubscriptions(), total: metaTotal },
  { id: "plans", label: "Plans", href: "/subscriptions/plans", roles: SUB, load: () => listPlans(), total: (r) => (Array.isArray(r.data) ? r.data.length : null) },
];

/** BOC-09 Dashboard: only the cards the viewer's roles may open; a failed source shows its error code, never a number. */
export function DashboardScreen({ roles }: { roles: PlatformRole[] }) {
  const cards = CARDS.filter((c) => c.roles.some((r) => roles.includes(r)));
  const [counts, setCounts] = useState<Record<string, Count>>({});

  useEffect(() => {
    let cancelled = false;
    for (const card of cards) {
      card.load().then((r) => {
        if (cancelled) return;
        const total = r.ok ? card.total(r) : null;
        setCounts((cur) => ({
          ...cur,
          [card.id]: total !== null ? { total } : { error: r.ok ? "NO_TOTAL" : r.code },
        }));
      });
    }
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roles.join(",")]);

  return (
    <section className="space-y-6">
      <h1 className="text-[22px] font-semibold">Dashboard</h1>
      {cards.length === 0 ? (
        <p className="text-sm text-on-surface-variant">No operational counts are available for your role.</p>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {cards.map((c) => {
            const count = counts[c.id];
            return (
              <li key={c.id}>
                <Link
                  href={c.href}
                  className="block rounded-panel border border-outline-variant bg-surface-lowest p-5 shadow-tier1 hover:bg-surface-low"
                >
                  <p className="text-sm text-on-surface-variant">{c.label}</p>
                  <p className="mt-2 text-3xl font-semibold" aria-label={`${c.label} count`}>
                    {!count ? "…" : "total" in count ? count.total : "Not available"}
                  </p>
                  {count && "error" in count ? (
                    <p className="mt-1 font-mono text-xs text-on-surface-variant">Error code: {count.error}</p>
                  ) : null}
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
