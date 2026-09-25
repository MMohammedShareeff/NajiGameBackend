# Deploying Naji on one small server (free or nearly free)

This folder runs the whole game on one machine: the app, Postgres, Redis and Caddy (a web server that also gets a free HTTPS certificate). The frontend is served by Caddy from the sibling `NajiGameFrontend` folder, so everything is on one address and needs no CORS setup.

Expected layout on the server:

```
/opt/naji/
  NajiGameBackend/    <- this repo (this deploy folder is inside it)
  NajiGameFrontend/   <- the frontend repo
```

## Before you start

1. **Rotate any key that was ever stored in a file inside the project** (the Gmail app password, the OpenAI key, anything in `application-dev.yaml` or a local `.env`). Create new ones for the server. The server only reads secrets from `deploy/.env`, which is git-ignored.
2. Get free AI keys: Groq, Google AI Studio (Gemini) and OpenRouter. Optional: an OpenAI key with a low monthly spending limit set in its dashboard, used only as the last fallback.
3. Get free SMTP credentials for sending codes, for example Brevo (verify one sender email address there). Without a domain, some mail may land in spam.

## 1. Create the server

- Oracle Cloud "Always Free" ARM VM (Ubuntu 22.04) is the free option. Hetzner CX22 (about 4 euro per month) is the easy paid fallback.
- Open ports 22, 80 and 443 in the provider's network rules. On Oracle Ubuntu images also allow them in the machine firewall:
  ```
  sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
  sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
  sudo netfilter-persistent save
  ```
- Install Docker:
  ```
  curl -fsSL https://get.docker.com | sh
  sudo usermod -aG docker $USER
  ```
  Log out and back in.

## 2. Free address without buying a domain

Create a name at duckdns.org (for example `naji-game.duckdns.org`) and point it at the server's public IP. Caddy then gets a real HTTPS certificate for it automatically.

## 3. Get the code

```
sudo mkdir -p /opt/naji && sudo chown $USER /opt/naji && cd /opt/naji
git clone <backend repo url> NajiGameBackend
git clone <frontend repo url> NajiGameFrontend
```

## 4. Configure

```
cd NajiGameBackend/deploy
cp .env.example .env
```

Edit `.env`:

- `SITE_ADDRESS` = your DuckDNS name, `PUBLIC_ORIGIN` = `https://` plus the same name.
- `DB_PASSWORD` and `REDIS_PASSWORD`: `openssl rand -hex 24`
- `JWT_SECRET`: `openssl rand -base64 64` (must be base64)
- AI keys, `AI_PROVIDER_ORDER` and the mail settings.
- `GAME_DAILY_LIMIT`: how many games one host can start per day (protects your AI allowance).

## 5. Start

```
docker compose -f docker-compose.prod.yaml up -d --build
docker compose -f docker-compose.prod.yaml logs -f app
```

The first start builds the app (a few minutes) and creates the database tables and 100 starter scenarios automatically. Open `https://<your name>.duckdns.org`.

## 6. Check it works

1. Register with a real email and enter the code you receive.
2. Play a game as a guest from a phone on mobile data and one on Wi-Fi.
3. `https://<your name>.duckdns.org/actuator/health` shows `UP`.

## 7. Keep it healthy

- **Backups:** `chmod +x backup.sh`, then add to `crontab -e`: `0 4 * * * /opt/naji/NajiGameBackend/deploy/backup.sh`. Copies are kept for 7 days in `deploy/backups`. Copy them off the server sometimes.
- **Uptime alert:** add `https://<your name>.duckdns.org/actuator/health` to a free monitor such as UptimeRobot.
- **Update:** `git pull` in both repos, then `docker compose -f docker-compose.prod.yaml up -d --build`. A restart ends any game in progress, so do it at a quiet time.
- **Logs:** `docker compose -f docker-compose.prod.yaml logs --tail 200 app`.

## What costs money, and how it is kept low

- A whole game now uses about 5 AI calls (one per round), and scenarios come from the built-in scenario bank, so free AI tiers go a long way. Each host can start `GAME_DAILY_LIMIT` games a day.
- If free AI limits run out, players see a clear "AI is busy" message and the game stops. Add a cheap paid key last in `AI_PROVIDER_ORDER` and set a hard monthly limit in that provider's dashboard.
- Free tiers and free VM offers change. Recheck their current limits before relying on them.

## Limits to know about

- One app instance only: game timers live in memory, so a restart or crash ends running games.
- There is no automatic failover on a single server.
- Free servers can be reclaimed if they sit idle for a long time. The uptime monitor and the app's own scheduled jobs keep some activity.
