# Deploying RentaCar

**Live:**
- Frontend: https://rentacar-frontend.onrender.com
- Backend API: https://rentacar-backend-a2et.onrender.com

Both on **Render**'s free tier: the backend is a Docker-based Web Service, the
frontend is a Static Site (a CDN serving the Vite build — no running
container, so it doesn't sleep the way the backend does).

## Database

The backend runs the `dev` Spring profile, using the same file-backed H2
database as local development — not MySQL. Render's free tier has no
persistent disk, so the H2 file resets whenever the container restarts;
`DataSeeder` re-seeds fresh data every time. Fine for a live demo; anything
created in one running instance doesn't survive to the next restart.

## Configuration

**Backend (`rentacar-backend`)** — built from
[`backend/Dockerfile`](backend/Dockerfile), root directory `backend/`.
Environment variables:

| Variable | Value | Why |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | H2, not MySQL |
| `JWT_SECRET` | a random 64-char string | overrides the source's dev placeholder |
| `SPRING_H2_CONSOLE_ENABLED` | `false` | the `dev` profile enables `/h2-console` by default — not appropriate on a public URL |
| `CORS_ALLOWED_ORIGINS` | `https://rentacar-frontend.onrender.com` | otherwise the browser blocks the frontend's API calls |

**Frontend (`rentacar-frontend`)** — built from `frontend/`,
`npm install && npm run build`, publishes `dist/`. One environment variable
(build-time, baked into the JS bundle — see
[`frontend/src/api/client.ts`](frontend/src/api/client.ts)):

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://rentacar-backend-a2et.onrender.com/api` |

A rewrite rule (`/* → /index.html`) sends every path to the SPA — without it,
a direct link to e.g. `/login` (or a refresh anywhere but `/`) 404s, since
Render's static-site product otherwise only serves files that literally exist.

## Before presenting

The backend spins its container down after ~15 minutes idle; cold start takes
1–2 minutes. **Open the live frontend URL a few minutes before you go live**
so the backend it calls is already warm.

## Redeploying

Neither service auto-deploys on push (they were created via Render's API, not
the dashboard's GitHub-connect flow, so no webhook is wired up). Trigger a
deploy manually after any change:

```bash
# backend
curl -X POST https://api.render.com/v1/services/srv-da89apegekts73cjjv3g/deploys \
  -H "Authorization: Bearer $RENDER_API_KEY"
# frontend
curl -X POST https://api.render.com/v1/services/srv-da8aj47avr4c73eu9mmg/deploys \
  -H "Authorization: Bearer $RENDER_API_KEY"
```
