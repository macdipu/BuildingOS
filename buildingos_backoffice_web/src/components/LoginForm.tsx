"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

type Step = { kind: "phone" } | { kind: "code"; attemptId: string };

async function postJson(url: string, body: unknown) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const json = (await res.json().catch(() => ({}))) as {
    data?: { attemptId?: string };
    code?: string;
    message?: string | null;
  };
  return { ok: res.ok, status: res.status, json };
}

function messageFor(status: number, code?: string, message?: string | null): string {
  if (status === 429) return "Too many attempts. Please wait before requesting another code.";
  if (code === "NO_PLATFORM_ROLE") return "This account has no back-office access.";
  return message || "Sign-in failed. Check the number or code and try again.";
}

export function LoginForm() {
  const router = useRouter();
  const [step, setStep] = useState<Step>({ kind: "phone" });
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (step.kind === "phone") {
        const r = await postJson("/api/auth/otp/start", { phone: phone.trim() });
        const attemptId = r.json.data?.attemptId;
        if (!r.ok || !attemptId) setError(messageFor(r.status, r.json.code, r.json.message));
        else setStep({ kind: "code", attemptId });
      } else {
        const r = await postJson("/api/auth/otp/verify", {
          attemptId: step.attemptId,
          phone: phone.trim(),
          code: code.trim(),
        });
        if (!r.ok) setError(messageFor(r.status, r.json.code, r.json.message));
        else router.replace("/");
      }
    } finally {
      setBusy(false);
    }
  }

  const input =
    "h-10 w-full rounded-control border border-outline-variant bg-surface-lowest px-3 text-sm outline-none focus:border-primary-container focus:ring-3 focus:ring-primary-container/15";

  return (
    <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
      <label className="flex flex-col gap-1 text-sm font-semibold">
        Phone number
        <input
          className={input}
          inputMode="tel"
          autoComplete="tel"
          value={phone}
          disabled={step.kind === "code"}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="01XXXXXXXXX"
          required
        />
      </label>
      {step.kind === "code" && (
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Verification code
          <input
            className={input}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
          />
        </label>
      )}
      {error && (
        <p role="alert" className="rounded-control bg-error-container px-3 py-2 text-sm text-error">
          {error}
        </p>
      )}
      <button
        type="submit"
        disabled={busy}
        className="h-10 rounded-control bg-primary-container text-sm font-semibold text-on-primary hover:bg-primary-hover disabled:opacity-60"
      >
        {step.kind === "phone" ? "Send code" : "Verify & sign in"}
      </button>
      {step.kind === "code" && (
        <button
          type="button"
          className="text-sm font-semibold text-primary-container"
          onClick={() => {
            setStep({ kind: "phone" });
            setCode("");
          }}
        >
          Change number
        </button>
      )}
    </form>
  );
}
