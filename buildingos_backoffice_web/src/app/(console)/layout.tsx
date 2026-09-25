import { redirect } from "next/navigation";
import { Rail } from "@/components/Rail";
import { visibleNav } from "@/lib/nav";
import { currentSession } from "@/lib/session";

const ROLE_LABEL: Record<string, string> = {
  SUPER_ADMIN: "Super Admin",
  PLATFORM_ADMIN: "Platform Admin",
  ONBOARDING_AGENT: "Onboarding Agent",
  SUPPORT_AGENT: "Support Agent",
  SUBSCRIPTION_ADMIN: "Subscription Admin",
};

export default async function ConsoleLayout({ children }: LayoutProps<"/">) {
  const session = await currentSession();
  if (!session) redirect("/login");
  if (session.roles.length === 0) redirect("/denied");

  return (
    <div className="flex h-screen">
      <Rail sections={visibleNav(session.roles)} />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 shrink-0 items-center justify-end gap-4 border-b border-outline-variant bg-surface-lowest px-8">
          <div className="flex flex-wrap items-center gap-2">
            {session.roles.map((role) => (
              <span
                key={role}
                className="rounded-full bg-anchor px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-[0.04em] text-white"
              >
                {ROLE_LABEL[role] ?? role}
              </span>
            ))}
          </div>
          <span className="text-sm text-on-surface-variant">{session.phone}</span>
          <form action="/api/auth/logout" method="post">
            <button className="h-9 rounded-control border border-outline-variant px-3 text-sm font-semibold hover:bg-surface-low">
              Sign out
            </button>
          </form>
        </header>
        <main className="flex-1 overflow-y-auto px-8 py-8">{children}</main>
      </div>
    </div>
  );
}
