# Naji: Backend

The API behind **Naji**, an online multiplayer survival game. A room of up to 5 players gets a survival scenario each round, everyone writes how they would survive, and an AI judge scores every answer. A game is 5 rounds and ends with a leaderboard. Everything runs live over WebSockets.

The web client is in the sibling repo `NajiGameFrontend`.

## Features

- Accounts with email verification codes, password reset, profile changes confirmed by email, and guest play with an optional nickname
- Rooms with a host, kick, invites (by username or from a friends list) and a friends list
- Timed rounds (default 90 seconds), early finish when everyone has answered
- AI judging: score 0 to 10, a short funny comment, survived when the score is above 5
- Themed scenario arc (real world, comedy, fantasy and sci-fi, surreal, epic finale)
- English and Arabic game content: scenario bank in both languages, AI comments in the room language
- Per-player dashboard (games, wins, streak, best and average score)
- Live updates: players, rounds, submissions, results, leaderboard, invites

## Tech stack

Spring Boot 3.3 (Java 17), PostgreSQL with Flyway migrations, Redis (verification codes, invites, rate limits, daily game counters), STOMP over SockJS WebSockets, JWT authentication. The AI provider chain (Groq, Gemini, OpenRouter, OpenAI) is tried in the order set by `AI_PROVIDER_ORDER`.

## Run it locally

You need Docker. From `src/main/resources`:

```
docker compose -f docker-compose.yaml up -d --build --force-recreate app
```

The API is then on http://localhost:8080. Put your keys in `src/main/resources/.env` (git-ignored); the names are listed in `deploy/.env.example`. At least one AI key is required to play.

To read verification emails without a real mailbox, use the Mailpit override and open http://localhost:8025:

```
docker compose -f docker-compose.yaml -f docker-compose.mail.yaml up -d --build app mailpit
```

Then start the web client from `NajiGameFrontend` (see its README).

## Main endpoints

| Area | Endpoints |
| --- | --- |
| Player | `POST /player/register`, `/login`, `/guest`; `GET /player/me`; `PUT /player/update`, `/reset-password` |
| Verification | `POST /verification/verify-email`, `/verify-update` |
| Room | `POST /room/create`, `/add-player`, `/leave`; `GET /room/get-players`, `/room-id`, `/admin`; `DELETE /room/kick-player/{name}` |
| Game | `POST /game/start?passCode&lang=en\|ar`, `/game/stop`, `/game/language`; `GET /game/state` |
| Answers | `POST /Submission/create?text=` |
| Social | `/invite/*`, `/friends/*` |
| Stats | `GET /dashboard/get-by-id/{playerId}` |

Live topics (SockJS endpoint `/game-webSocket?token=<jwt>`): `/topic/room/{id}/updates|round|submissions|results|leaderboard|final_leaderboard|players` and `/user/queue/invites`.

## Security notes

Everything except register, login, guest, reset-password and email verification needs a JWT. Login is locked for 10 minutes after 10 failures, sensitive endpoints are rate limited per IP, and CORS is restricted in production with `CORS_ALLOWED_ORIGINS`. Secrets only live in `.env` files that are git-ignored; never commit them.

## Deploy

`deploy/README.md` explains how to run the whole stack (app, PostgreSQL, Redis and a Caddy web server with automatic HTTPS) on one small server, including backups and updates.

## Project notes

Built as an internship graduation project. Games are kept in memory per room, so run a single app instance; a restart ends games in progress.
