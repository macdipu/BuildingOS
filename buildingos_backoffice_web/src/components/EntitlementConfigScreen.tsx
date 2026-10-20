import Link from "next/link";
import { FreeTierPanel } from "@/components/FreeTierPanel";

/** BOC-08 Feature / Entitlement Configuration: the free-tier defaults, with plan entitlements on Plans. */
export function EntitlementConfigScreen({ title }: { title: string }) {
  return (
    <section className="space-y-6">
      <h1 className="text-[22px] font-semibold">{title}</h1>
      <FreeTierPanel />
      <p className="text-sm text-on-surface-variant">
        Per-plan entitlements are edited on{" "}
        <Link href="/subscriptions/plans" className="font-semibold text-primary hover:underline">
          Subscriptions · Plans
        </Link>
        .
      </p>
    </section>
  );
}
