# BuildingOS back-office web

Next.js console for BuildingOS platform staff (BRD §149.1–149.2, BOS-010 F6, decision D-09).

- Sign-in: phone OTP via auth-service (`/api/auth/otp/*` route handlers). The access token lives
  only in an httpOnly `bos_session` cookie; browser code never sees it.
- API calls: same-origin `/api/gateway/<path>` proxy adds the bearer token server-side and
  forwards to the API Gateway. Only `platform/**` and `building-applications/**` are reachable.
- Route guard: `src/proxy.ts` (Next 16 "proxy", formerly middleware) sends signed-out users to
  `/login` and users without a platform role to `/denied`. Backend services remain the
  authorization boundary; nav filtering (`src/lib/nav.ts`) is UI convenience only.
- Design: Stitch tokens (`agentic/data/project-context/ui/stitch/design-system/DESIGN.md`) in
  `src/app/globals.css`; bundled OFL fonts in `src/fonts`.

## Run

```bash
cp .env.example .env.local        # BACKOFFICE_API_BASE_URL=http://localhost:8080/
npm ci
npm run dev                       # http://localhost:3000
npm run lint && npm run typecheck && npm test && npm run build
```

Local stack: `docker compose -f infra/local/compose.yaml up -d --build backoffice-web`
(http://127.0.0.1:3000). Development OTP is `000000` (BOS-010 D-01); seed super admin
phone per D-02.

This project uses Next.js 16 — see `AGENTS.md` before changing framework code.
