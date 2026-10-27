# Production with Supabase

In development the API uses **MySQL in Docker** (`docker compose up -d`). In production it uses **PostgreSQL hosted on [Supabase](https://supabase.com)**: no server of our own to look after, encrypted connections and backups managed by Supabase (BGSI-328).

The code is the same for both databases: every query is JPQL, so Hibernate writes the right SQL for each one. Only the `prod` profile (`application-prod.yml`) changes, and it reads every value from environment variables.

## 1. Try the `prod` profile on your computer (PostgreSQL in Docker)

Before using Supabase, check that everything works with PostgreSQL:

```bash
docker compose --profile postgres up -d     # PostgreSQL 17 on port 5432 (MySQL keeps running on 3306)
```

Start the API with these variables (same `DB_USER` / `DB_PASSWORD` as in `.env`):

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://localhost:5432/bgsistemas \
DDL_AUTO=update \
CORS_ALLOWED_ORIGINS=http://localhost:5173 \
./mvnw spring-boot:run
```

`DDL_AUTO=update` creates the tables the first time. From then on leave it out: the default is `validate`, which only checks that the tables match the entities and never changes them.

## 2. Create the database on Supabase

1. Create a project on [supabase.com](https://supabase.com/dashboard) and save the **database password**.
2. Click **Connect** → tab **JDBC** → copy the **Session pooler** string (port `5432`).
   Do not use the Transaction pooler (port `6543`): it does not support the prepared statements of Hibernate.
3. Fill these variables on the server where the API runs (never in the code or in Git):

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
| `DB_USER` | `postgres.<project-ref>` |
| `DB_PASSWORD` | the database password |
| `DB_SCHEMA` | `app` |
| `DDL_AUTO` | `update` only the first time, then remove it (`validate`) |
| `CORS_ALLOWED_ORIGINS` | the URL of the website |
| `JWT_SECRET`, `TOTP_ENCRYPTION_KEY`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `MAIL_*` | as in `.env.example` |

`sslmode=require` encrypts the connection. `DB_SCHEMA=app` keeps our tables out of the `public` schema, which Supabase publishes through its REST API (Data API): only our API can read them.

## Notes

- Searches ignore upper/lower case in both databases. MySQL also ignores accents (`Cafeteria` finds `Cafetería`); PostgreSQL does not.
- The free plan of Supabase allows few connections, so the pool is small (`DB_POOL_SIZE`, 5 by default).
