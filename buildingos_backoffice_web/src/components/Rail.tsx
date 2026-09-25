"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { hrefOf, type NavSection } from "@/lib/nav";

/** Persistent 260px left rail (DESIGN.md desktop layout) with the §149.2 tree. */
export function Rail({ sections }: { sections: NavSection[] }) {
  const pathname = usePathname();
  const isActive = (href: string) => (href === "/" ? pathname === "/" : pathname.startsWith(href));

  return (
    <nav aria-label="Back-office navigation" className="flex h-full w-[260px] shrink-0 flex-col bg-anchor text-white">
      <div className="flex items-center gap-3 px-5 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-card bg-primary-container font-bold">B</div>
        <div>
          <p className="font-display text-base font-bold leading-tight">BuildingOS</p>
          <p className="text-xs text-white/60">Back Office</p>
        </div>
      </div>
      <ul className="flex-1 space-y-4 overflow-y-auto px-3 pb-6">
        {sections.map((section) => {
          const sectionHref = hrefOf(section);
          return (
            <li key={section.slug || "dashboard"}>
              {section.items.length === 0 ? (
                <Link
                  href={sectionHref}
                  aria-current={isActive(sectionHref) ? "page" : undefined}
                  className={`block rounded-control px-3 py-2 text-sm font-semibold ${
                    isActive(sectionHref) ? "bg-primary-container" : "hover:bg-white/10"
                  }`}
                >
                  {section.label}
                </Link>
              ) : (
                <>
                  <p className="px-3 pb-1 text-[11px] font-semibold uppercase tracking-[0.04em] text-white/50">
                    {section.label}
                  </p>
                  <ul>
                    {section.items.map((item) => {
                      const href = hrefOf(section, item);
                      const active = isActive(href);
                      return (
                        <li key={item.slug}>
                          <Link
                            href={href}
                            aria-current={active ? "page" : undefined}
                            className={`block rounded-control px-3 py-1.5 text-sm ${
                              active ? "bg-primary-container font-semibold" : "text-white/85 hover:bg-white/10"
                            }`}
                          >
                            {item.label}
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </>
              )}
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
