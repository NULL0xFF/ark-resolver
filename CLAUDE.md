# ark-resolver

Full-stack raid scheduling application — SvelteKit SPA frontend + Spring Boot API backend, deployed via Docker Compose.

## Project layout

```
ark-resolver/
├── frontend/     # SvelteKit 2 + Svelte 5, adapter-static, Tailwind/daisyUI, nginx serves on port 80
├── backend/      # Spring Boot 3.5.11, Gradle 9.3.1 Kotlin DSL, JDK 21, multi-module (server)
├── docker-compose.yml
├── .env.example
└── .github/workflows/
    ├── frontend.yml
    └── backend.yml
```

## Images

Published to GHCR via GitHub Actions:
- `ghcr.io/null0xff/ark-frontend:latest`
- `ghcr.io/null0xff/ark-backend:latest`

## Architecture

```
Internet → CloudFlare → Router:443 → NPM (SSL) → Host:80 → Docker frontend:80
                                                                ├─ /api/*  → backend:8080
                                                                └─ /*      → static files
```

- NPM (NGINX Proxy Manager) handles SSL termination
- Frontend container embeds nginx: serves static SPA + proxies /api to backend
- Backend is internal only (expose 8080, no host port mapping)
- MariaDB runs as an external service (not in Compose)

## Environment variables

All passed via `.env` file. See `.env.example`.

| Variable | Used by | Purpose |
|---|---|---|
| `EXTERNAL_URL` | backend | Publicly accessible base URL |
| `JWT_SECRET` | backend | HMAC-SHA256 signing key for JWT tokens |
| `DISCORD_CLIENT_ID` | backend | Discord OAuth2 app ID |
| `DISCORD_CLIENT_SECRET` | backend | Discord OAuth2 app secret |
| `DISCORD_REDIRECT_URI` | backend | Discord OAuth2 callback URI |
| `DB_HOST` | backend | External MariaDB host |
| `DB_PORT` | backend | MariaDB port (default 3306) |
| `DB_NAME` | backend | Database name |
| `DB_USER` | backend | Database user |
| `DB_PASSWORD` | backend | Database password |

## Database

MariaDB runs as an **external service** — not a container in Compose.

## Common commands

```bash
# Run locally
docker compose up -d

# Validate
curl http://localhost/api/health

# Build images locally
docker build -t ghcr.io/null0xff/ark-frontend:latest ./frontend
docker build -t ghcr.io/null0xff/ark-backend:latest ./backend
```
