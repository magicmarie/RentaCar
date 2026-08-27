# Deploying RentaCar

**Live:**
- Frontend: https://rentacar-frontend.onrender.com
- Backend API: https://rentacar-backend-a2et.onrender.com

Both on **Render**'s free tier, no credit card. Backend is a Docker-based Web
Service; frontend is a Static Site (a separate Render product — just a CDN
serving the Vite build, no running container, so it doesn't sleep the way the
backend does).

## Why H2 instead of MySQL here

The original plan was Railway (backend + a MySQL plugin in one place), documented
below in case that becomes available again. Railway's free trial turned out to be
expired on the account used for this deployment, and moving to a separate free
MySQL host would have meant a fourth account/signup. Given the time available,
the backend runs on Render with the same file-backed H2 database used in local
dev instead of MySQL — the app code and `prod` (MySQL) profile are unchanged and
still there; this deployment just runs under the `dev` Spring profile.

**What this trades away:** Render's free tier has no persistent disk, so the H2
file resets whenever the container restarts (redeploys, or Render recycling an
idle instance). Each restart re-seeds fresh data via `DataSeeder`, so the app
still works, but nothing you create in one running instance survives to the
next. Fine for a live demo in one sitting; not a substitute for a real database
for anything longer-lived.

## What's actually configured

**Render (backend service `rentacar-backend`)** — built from
[`backend/Dockerfile`](backend/Dockerfile), root directory `backend/`, deployed
straight from the public GitHub repo (no GitHub App connection needed since the
repo is public). Environment variables:

| Variable | Value | Why |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | H2, not MySQL — see above |
| `JWT_SECRET` | a random 64-char string, generated for this deployment | overrides the dev placeholder; `prod` profile would refuse to boot without one, `dev` doesn't require it but this deployment sets a real one anyway |
| `SPRING_H2_CONSOLE_ENABLED` | `false` | `dev` profile enables `/h2-console` by default (fine on localhost, not fine on a public URL) |
| `CORS_ALLOWED_ORIGINS` | `https://rentacar-frontend.onrender.com` | otherwise the browser blocks the frontend's API calls |

**Render (static site `rentacar-frontend`)** — built from the `frontend/`
directory, `npm install && npm run build`, publishes `dist/`. One environment
variable (build-time, baked into the JS bundle — see
[`frontend/src/api/client.ts`](frontend/src/api/client.ts)):

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://rentacar-backend-a2et.onrender.com/api` |

A rewrite rule (`/* → /index.html`, added via `POST /v1/services/{id}/routes`)
sends every path to the SPA — without it, a direct link to e.g. `/login` (or a
refresh anywhere but `/`) 404s, since Render's static-site product otherwise
serves only files that literally exist and this is a client-side-routed SPA.

## Before presenting

The backend (Web Service, not the frontend Static Site) spins its container
down after ~15 minutes idle, and cold start takes 1–2 minutes. **Hit the live
frontend URL a few minutes before you go live** so the backend it calls is
already warm — don't let that delay happen on screen during the demo.

## Redeploying

Both services were created via Render's API rather than through the
dashboard's "connect GitHub" flow, so **no webhook is wired up on either one**
— pushing to `main` does *not* auto-redeploy them, even though the dashboard
shows `autoDeploy: yes`. Trigger a deploy manually after any change:
```bash
# backend
curl -X POST https://api.render.com/v1/services/srv-da89apegekts73cjjv3g/deploys \
  -H "Authorization: Bearer $RENDER_API_KEY"
# frontend
curl -X POST https://api.render.com/v1/services/srv-da8aj47avr4c73eu9mmg/deploys \
  -H "Authorization: Bearer $RENDER_API_KEY"
```
(Or fix this properly once per service: open it in the Render dashboard →
Settings → connect the GitHub repo through their UI, which sets up the webhook
the API can't create on its own for an unlinked account.)

## Original plan: Railway (backend + MySQL in one place)

Kept here in case the free trial issue is resolved later, or for a real
production deployment where paying for Railway (or another MySQL host) makes
sense — this path exercises the actual `prod` profile end to end (MySQL,
required `JWT_SECRET`/`MAIL_*`, no fallbacks).

1. Sign up at [railway.com](https://railway.com) with GitHub, **New Project →
   Deploy from GitHub repo → `magicmarie/RentaCar`**, root directory `backend/`
   (it will use `backend/Dockerfile`).
2. **New → Database → Add MySQL** in the same project.
3. On the backend service, set variables:

   | Variable | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` |
   | `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
   | `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
   | `JWT_SECRET` | a random 32+ character string |
   | `CORS_ALLOWED_ORIGINS` | your frontend URL |
   | `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | a Mailtrap sandbox inbox's SMTP settings, or a real provider |

   `application-prod.properties` has no defaults for `JWT_SECRET` or the
   `MAIL_*` vars — it fails to boot without them, rather than silently running
   with a placeholder secret or a broken mailer.
4. Point `VITE_API_BASE_URL` (frontend) at the resulting Railway backend URL,
   and `CORS_ALLOWED_ORIGINS` (backend) at the frontend's URL.
