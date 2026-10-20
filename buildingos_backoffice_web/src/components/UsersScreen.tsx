"use client";

import { useEffect, useState } from "react";
import { ApiErrorNotice } from "@/components/ApiErrorNotice";
import { UserPanel } from "@/components/UserPanel";
import { fieldClass, secondaryButton } from "@/components/formStyles";
import { formatDate, pageMeta } from "@/lib/buildingApplications";
import type { PlatformRole } from "@/lib/claims";
import { ROLE_LABEL, PLATFORM_ROLES, listUsers, type ApiResult, type PlatformUser } from "@/lib/platformUsers";

/**
 * BOC-05 Users: a user list (phone search; role filter, or fixed to one role for the agent lists)
 * with a side panel to assign and revoke platform roles (SUPER_ADMIN only).
 */
export function UsersScreen({
  title,
  fixedRole,
  userId,
  roles,
}: {
  title: string;
  fixedRole?: PlatformRole;
  userId: string;
  roles: PlatformRole[];
}) {
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [loaded, setLoaded] = useState<{ key: string; result: ApiResult<PlatformUser[]> } | null>(null);
  const role = fixedRole ?? roleFilter;
  const key = `${query}#${role}#${page}#${version}`;

  useEffect(() => {
    let cancelled = false;
    listUsers({ query, role, page }).then((result) => {
      if (!cancelled) setLoaded({ key, result });
    });
    return () => {
      cancelled = true;
    };
  }, [query, role, page, key]);

  const current = loaded?.key === key ? loaded.result : null;
  const meta = current?.ok ? pageMeta(current.meta) : null;
  const lastPage = meta ? Math.max(0, Math.ceil(meta.total / meta.size) - 1) : 0;
  // Keep the last loaded row on screen while a refetch runs, so the panel and its result message survive.
  const last = loaded?.result;
  const selectedUser = last?.ok ? last.data.find((u) => u.id === selected) : undefined;

  function reset() {
    setPage(0);
    setSelected(null);
  }

  return (
    <section className="space-y-4">
      <h1 className="text-[22px] font-semibold">{title}</h1>

      <form
        role="search"
        aria-label="Search users"
        className="flex flex-wrap items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          setQuery(draft);
          reset();
        }}
      >
        <label className="text-sm font-semibold">
          Phone
          <input className={fieldClass} value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Search by phone" />
        </label>
        {!fixedRole ? (
          <label className="text-sm font-semibold">
            Platform role
            <select
              className={fieldClass}
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                reset();
              }}
            >
              <option value="">All users</option>
              {PLATFORM_ROLES.map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABEL[r]}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        <button type="submit" className={secondaryButton}>
          Search
        </button>
      </form>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-panel border border-outline-variant bg-surface-lowest shadow-tier1">
          {!current ? (
            <p className="p-6 text-sm text-on-surface-variant">Loading users…</p>
          ) : !current.ok ? (
            <div className="p-4">
              <ApiErrorNotice failure={current} context="plain" />
            </div>
          ) : current.data.length === 0 ? (
            <p className="p-6 text-sm text-on-surface-variant">No users match.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-surface-low text-[11px] uppercase tracking-[0.04em] text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3 font-semibold">Phone</th>
                  <th className="px-4 py-3 font-semibold">Platform roles</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                </tr>
              </thead>
              <tbody>
                {current.data.map((u) => (
                  <tr key={u.id} aria-selected={u.id === selected} className={`border-t border-outline-variant ${u.id === selected ? "bg-surface-container" : "hover:bg-surface-low"}`}>
                    <td className="px-4 py-3">
                      <button type="button" onClick={() => setSelected(u.id)} className="font-semibold text-primary hover:underline">
                        {u.phone}
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      {u.platformRoles.length === 0 ? (
                        <span className="text-on-surface-variant">None</span>
                      ) : (
                        <span className="flex flex-wrap gap-1">
                          {u.platformRoles.map((r) => (
                            <span key={r} className="rounded-control bg-surface-container px-2 py-0.5 text-xs font-semibold">
                              {ROLE_LABEL[r] ?? r}
                            </span>
                          ))}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-on-surface-variant">{formatDate(u.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {meta && meta.total > meta.size ? (
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm">
              <span className="text-on-surface-variant">
                Page {meta.page + 1} of {lastPage + 1} · {meta.total} users
              </span>
              <div className="flex gap-2">
                <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50">
                  Previous
                </button>
                <button type="button" disabled={page >= lastPage} onClick={() => setPage((p) => p + 1)} className="h-8 rounded-control border border-outline-variant px-3 text-xs font-semibold disabled:opacity-50">
                  Next
                </button>
              </div>
            </div>
          ) : null}
        </div>

        {selectedUser ? (
          <UserPanel
            key={selectedUser.id}
            user={selectedUser}
            viewerId={userId}
            viewerRoles={roles}
            onChanged={() => setVersion((v) => v + 1)}
            onClose={() => setSelected(null)}
          />
        ) : null}
      </div>
    </section>
  );
}
