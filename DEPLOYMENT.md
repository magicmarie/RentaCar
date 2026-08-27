# Deploying RentaCar

Backend (Spring Boot + MySQL) on **Railway**, frontend (static Vite build) on
**Vercel**. Both have no-credit-card free tiers, which is what makes this pairing
workable for a one-off course deployment.

## 1. Backend + database on Railway

1. Sign up at [railway.com](https://railway.com) with GitHub (no card required for
   the free trial).
2. **New Project → Deploy from GitHub repo → `magicmarie/RentaCar`.** When Railway
   asks for a root directory / build context, point it at `backend/` — it will
   detect the `Dockerfile` there and build from it.
3. **New → Database → Add MySQL** in the same project. Railway provisions it and
   exposes connection details as reference variables
   (`${{MySQL.MYSQLHOST}}`, `${{MySQL.MYSQLPORT}}`, `${{MySQL.MYSQLDATABASE}}`,
   `${{MySQL.MYSQLUSER}}`, `${{MySQL.MYSQLPASSWORD}}`).
4. On the backend service, open **Variables** and set:

   | Variable | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` |
   | `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
   | `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
   | `JWT_SECRET` | a random 32+ character string (e.g. `openssl rand -base64 32`) — **not** the dev placeholder |
   | `CORS_ALLOWED_ORIGINS` | your Vercel frontend URL, once you have it (step 2) |
   | `MAIL_HOST` | `sandbox.smtp.mailtrap.io` (or a real provider) |
   | `MAIL_PORT` | `2525` |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | from your Mailtrap inbox's SMTP Settings tab |

   `application-prod.properties` has **no defaults** for `JWT_SECRET` or the
   `MAIL_*` vars — the app deliberately fails to boot without them, rather than
   silently running with a placeholder secret or a broken mailer.
5. Deploy. Railway gives the service a public URL like
   `https://rentacar-backend-production.up.railway.app`. Verify it's live:
   `curl https://<that-url>/api/auth/login` should return a 400/401 JSON body,
   not a connection error.

## 2. Frontend on Vercel

1. Sign up at [vercel.com](https://vercel.com) with GitHub (no card required).
2. **Add New → Project → import `magicmarie/RentaCar`.** Set **Root Directory**
   to `frontend/`. Framework preset: Vite (auto-detected).
3. Add an environment variable: `VITE_API_BASE_URL` =
   `https://<your-railway-backend-url>/api`.
4. Deploy. Vercel gives you a URL like `https://rentacar-<hash>.vercel.app`.
5. Go back to Railway and set `CORS_ALLOWED_ORIGINS` to that exact Vercel URL
   (no trailing slash), then redeploy the backend so the CORS config picks it up.

## 3. Verify end to end

Open the Vercel URL and log in with a seeded account
(`customer@rentacar.com` / `customer123`). If login hangs or errors in the
browser console with a CORS message, double-check step 2.5 above.

## Notes

- Railway's free trial is credit-based ($5, ~30 days) — plenty for a one-time
  graded demo, but the service will stop once the credit runs out. If the
  deployment needs to stay up past that, a paid Railway plan (or moving the DB
  to a separate always-free MySQL host) is the next step.
- The seeded accounts (`admin@rentacar.com` / `staff@rentacar.com` /
  `customer@rentacar.com`, passwords in the main README) work in the cloud
  exactly as they do locally — `DataSeeder` runs once against whatever database
  is empty on first boot.
