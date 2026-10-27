# SkillSwap

**Learn. Teach. Swap. Grow.**

A peer-to-peer skill exchange platform for students. Instead of paying for courses,
students trade skills: you teach Python, your peer teaches React, and you meet in a
built-in video room.

Built with **Spring Boot 3.4 (Java 21)** + **React 19 / Vite 8** + **MySQL 8**.

---

## Features

- **Skill swaps** — list TEACH and LEARN skills, find peers in the marketplace, propose a swap, accept/decline/complete with a 5-state lifecycle.
- **Shared learning** — book a free session with someone who knows the skill you're studying.
- **Video rooms** — an unguessable Jitsi room is generated on acceptance, with shared session notes.
- **Reviews & reputation** — rate completed sessions; accepted Q&A answers earn helpful points.
- **Q&A** — public questions, answers, accepted answers, close/reopen.
- **Messaging** — 1:1 conversations with unread badges and read receipts.
- **Notifications** — in-app bell plus optional SMTP email (dispatched after commit).
- **Moderation** — report content, admin queue, hide/restore, suspend accounts, append-only audit trail.
- **Profiles** — avatars, ratings, teaching/learning skills, Q&A stats, contributor leaderboard.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 8, React Router 7, Axios, plain CSS |
| Backend | Spring Boot 3.4, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Spring Mail |
| Auth | JWT (HS256, 24 h) + BCrypt |
| Database | MySQL 8 (H2 for tests) |
| Build | Maven Wrapper, npm |

## Run locally

**Prerequisites:** Java 21, Node 18+, MySQL 8 on `:3306`.

```powershell
# Backend
cd backend
# copy .env.example to .env and fill in DB_PASS
.\mvnw.cmd spring-boot:run          # http://localhost:9090

# Frontend
cd frontend
npm install
npm run dev                         # http://localhost:5173
```

Health check: <http://localhost:9090/api/health>

### Demo accounts

| Username | Password | Teaches | Wants to learn |
|---|---|---|---|
| `skillswap_demo_21` (Alex Demo) | `DemoLearn2026!` | Python | React |
| `skillswap_demo_22` (Blair Demo) | `DemoLearn2026!` | React | Python |

All seeded profiles use `skillswap_demo_01` … `skillswap_demo_22` with the same password.

## Tests

```powershell
cd backend
.\mvnw.cmd test          # 74 integration tests

cd ..
powershell -ExecutionPolicy Bypass -File .\e2e-smoke.ps1
```

## Documentation

- **[PROJECT_GUIDE.md](PROJECT_GUIDE.md)** — tech stack rationale, architecture, request flow, swap state machine, data model, security, demo script and viva Q&A.
- **[SkillSwap-Project-Guide.pdf](SkillSwap-Project-Guide.pdf)** — the same guide, print-ready.
- **[ROADMAP.md](ROADMAP.md)** — engineering history and production roadmap.

## Project layout

```
backend/    Spring Boot API (14 feature modules)
frontend/   React SPA (15 pages, 14 API services)
e2e-*.ps1   Live API end-to-end scripts
ui-smoke.mjs  Browser smoke tests over Chrome DevTools Protocol
```

## Notes

- `backend/.env` is gitignored — never commit database or SMTP credentials.
- Uploaded avatars are stored on the local filesystem (`backend/uploads/`), which is
  not suitable for ephemeral production hosts without object storage.
