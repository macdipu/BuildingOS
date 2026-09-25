export default function DeniedPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <div className="max-w-sm rounded-panel border border-outline-variant bg-surface-lowest p-8 text-center shadow-tier1">
        <h1 className="mb-2 text-xl font-semibold">Access denied</h1>
        <p className="mb-6 text-sm text-on-surface-variant">
          This account has no BuildingOS platform role. Ask a Super Admin to grant back-office access.
        </p>
        <form action="/api/auth/logout" method="post">
          <button className="h-10 rounded-control border border-outline-variant px-4 text-sm font-semibold">
            Sign out
          </button>
        </form>
      </div>
    </main>
  );
}
