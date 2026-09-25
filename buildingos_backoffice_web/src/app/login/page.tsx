import { LoginForm } from "@/components/LoginForm";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm rounded-panel border border-outline-variant bg-surface-lowest p-8 shadow-tier2">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-card bg-primary-container text-lg font-bold text-on-primary">
            B
          </div>
          <div>
            <p className="font-display text-lg font-bold">BuildingOS</p>
            <p className="text-xs text-on-surface-variant">Back Office</p>
          </div>
        </div>
        <h1 className="mb-1 text-2xl font-semibold">Sign in</h1>
        <p className="mb-6 text-sm text-on-surface-variant">Platform staff only. Use your registered phone number.</p>
        <LoginForm />
      </div>
    </main>
  );
}
