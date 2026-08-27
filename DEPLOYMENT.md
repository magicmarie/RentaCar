# Deploying RentaCar

**Live:**
- Frontend: https://frontend-three-pi-37.vercel.app
- Backend API: https://rentacar-backend-a2et.onrender.com

Backend (Spring Boot, Dockerized) on **Render**'s free tier; frontend (static Vite
build) on **Vercel**'s free tier. Both were set up with no credit card.

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
| `CORS_ALLOWED_ORIGINS` | the Vercel URL above | otherwise the browser blocks the frontend's API calls |

**Vercel (project `frontend`)** — deployed directly from the `frontend/` directory
(not via GitHub integration — the Vercel account here isn't linked to GitHub, so
deploys are pushed with `vercel --prod` rather than auto-deploying on push, which
means **redeploy manually after any frontend change** you want reflected live).
One environment variable:

| Variable | Value | Why |
|---|---|---|
| `VITE_API_BASE_URL` | `https://rentacar-backend-a2et.onrender.com/api` | baked in at build time — see [`frontend/src/api/client.ts`](frontend/src/api/client.ts) |

[`frontend/vercel.json`](frontend/vercel.json) rewrites all paths to `index.html`
— without it, a direct link to e.g. `/login` (or a page refresh anywhere but `/`)
404s, because this is a client-side-routed SPA and Vercel doesn't know that by
default.

## Before presenting

Render's free tier spins the container down after ~15 minutes idle, and cold
start takes 1–2 minutes. **Hit the live URL a few minutes before you go live**
so it's already warm — don't let the first load happen on screen during the
demo.

## Redeploying

**Backend:** push to `main`, or trigger manually:
```bash
curl -X POST https://api.render.com/v1/services/srv-da89apegekts73cjjv3g/deploys \
  -H "Authorization: Bearer $RENDER_API_KEY"
```

**Frontend:**
```bash
cd frontend
VERCEL_TOKEN=... npx vercel --prod --yes --token "$VERCEL_TOKEN"
```

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
