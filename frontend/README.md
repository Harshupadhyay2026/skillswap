# SkillSwap

A student-powered learning community. Swap skills one-for-one, or request a shared session to
study the same skill with a peer. Q&A is also available for free community help.

- **Backend** — Spring Boot 3.4.3, Java 21, Spring Security + JWT, MySQL, JPA
- **Frontend** — React 19, Vite 8, React Router, Axios

---

## Features

| Feature | Status |
|---|---|
| Register / login with JWT | ✅ |
| Skill catalog + search | ✅ |
| Add skills you can teach / want to learn | ✅ |
| Marketplace of students by skill | ✅ |
| **Propose a swap** | ✅ |
| **Accept / decline / cancel / complete a swap** | ✅ |
| **Auto-generated video room on acceptance** — Jitsi embedded in-app | ✅ |
| Mutual swap matching (teach X, want Y) | ✅ |
| **Free shared learning sessions for the same skill** | ✅ |
| **Shared session notes + next steps** | ✅ |
| **Learning-progress dashboard** | ✅ |
| **Public Q&A (problems + answers)** | ✅ |
| **Reviews & ratings** | ✅ |
| **Public profiles + avatar upload** | ✅ |
| **Top-contributor leaderboard** | ✅ |
| **1:1 messaging** | ✅ |
| **In-app notifications + SMTP email** | ✅ |
| **Reporting + moderator queue with audit trail** | ✅ |

See [`ROADMAP.md`](./ROADMAP.md) for the full plan.

---

## Prerequisites

- Java 21+
- Node 18+
- MySQL 8 running on `localhost:3306`

---

## Backend setup

```powershell
cd backend

# Set the database password (never commit this)
$env:DB_PASS = "your_mysql_password"

# Optional overrides
$env:DB_USER = "root"
$env:DB_NAME = "skillswap_db"
$env:JWT_SECRET = "a-random-string-at-least-64-characters-long"

.\mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:9090**. Hibernate creates the schema automatically
(`ddl-auto: update`) and `DataInitializer` seeds 32 skills and 3 roles. On local startup,
`DemoDataInitializer` adds 20 clearly marked demo profiles, 20 Q&A questions and 40 answers.
The seed is repeat-safe and does not overwrite existing accounts.

Demo sign-in (same password for all demo accounts):

- Username: `skillswap_demo_01` through `skillswap_demo_20`
- Password: `DemoLearn2026!`

Set `DEMO_DATA_ENABLED=false` for production. Demo accounts are for local walkthroughs only.

You can also copy `backend/.env.example` to `backend/.env` and fill in the values there;
the app imports that file when it starts. Real environment variables take precedence.

Check it:

```powershell
Invoke-RestMethod http://localhost:9090/api/health
```

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `skillswap_db` | Database name (auto-created) |
| `DB_USER` | `root` | MySQL user |
| `DB_PASS` | *(empty)* | MySQL password — **required** |
| `JWT_SECRET` | dev default | HS256 signing key, ≥ 64 chars |
| `JWT_EXPIRATION_MS` | `86400000` | Access-token lifetime (24 h) |
| `UPLOAD_DIR` | `uploads` | Where avatars are written |
| `MAIL_ENABLED` | `true` | Mirror notifications to email (in-app always works) |
| `MAIL_HOST` | *(empty)* | SMTP host, e.g. `smtp.gmail.com` |
| `MAIL_PORT` | `587` | SMTP port (587 = STARTTLS, 465 = SSL) |
| `MAIL_USERNAME` | *(empty)* | SMTP login / Gmail address |
| `MAIL_PASSWORD` | *(empty)* | SMTP password or app password |
| `MAIL_FROM` | *(SMTP username)* | Verified sender address |
| `MAIL_STARTTLS` | `true` | STARTTLS for port 587 |
| `MAIL_SSL_ENABLE` | `false` | SSL for port 465 |
| `APP_FRONTEND_URL` | `http://localhost:5173` | Origin used for links inside emails |
| `ADMIN_USERNAME` | *(empty)* | Promote this existing account, or create it with the two variables below |
| `ADMIN_EMAIL` | *(empty)* | Email for a newly created moderator account |
| `ADMIN_PASSWORD` | *(empty)* | Password for a newly created moderator account |
See `backend/.env.example`.

---

## Frontend setup

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

To point at a different API host, create `frontend/.env.local`:

```
VITE_API_BASE_URL=http://localhost:9090/api
```

### Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Vite dev server with HMR |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serve the production build |
| `npm run lint` | Oxlint |

---

## API reference

All responses share the envelope `{ success, message, data, timestamp }`.

### Auth — public

| Method | Path | Body |
|---|---|---|
| POST | `/api/auth/register` | `{ name, username, email, password, college, branch, year, city, bio, mode }` |
| POST | `/api/auth/login` | `{ usernameOrEmail, password }` |

Both return `{ accessToken, tokenType, user }`.

### Users

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/users/me` | ✅ | Current profile |
| PUT | `/api/users/me` | ✅ | Update profile |
| POST | `/api/users/me/avatar` | ✅ | Multipart `file`, ≤ 2 MB, PNG/JPEG/GIF/WebP |
| DELETE | `/api/users/me/avatar` | ✅ | Remove the photo |
| GET | `/api/files/**` | — | Public uploaded avatars |
| GET | `/api/users/{id}/profile` | — | Public profile, **no email** |

### Skills & marketplace

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/skills?query=&category=` | — | Catalog |
| GET | `/api/skills/categories` | — | Distinct categories |
| GET | `/api/marketplace?query=&skillId=` | — | Teachers |
| GET | `/api/marketplace/matches?offered=&wanted=` | ✅ | Mutual swap matches |
| GET | `/api/users/skills` | ✅ | My skills |
| POST | `/api/users/skills` | ✅ | `{ skillId, skillType, proficiency }` |
| DELETE | `/api/users/skills/{id}` | ✅ | Remove |

Marketplace rows also include `teacherBio`, `teacherCity`, `teacherHelpfulPoints`,
`teacherLearningMode` and `teacherJoinedAt`, so Explore can describe the person without a
second profile request.

### Swaps

| Method | Path | Auth | Who |
|---|---|---|---|
| POST | `/api/swaps` | ✅ | Propose |
| GET | `/api/swaps/incoming` | ✅ | Received |
| GET | `/api/swaps/outgoing` | ✅ | Sent |
| GET | `/api/swaps/all` | ✅ | Both |
| GET | `/api/swaps/{id}` | ✅ | Participants only — room page |
| PUT | `/api/swaps/{id}/accept` | ✅ | Receiver only — returns a Jitsi link |
| PUT | `/api/swaps/{id}/decline` | ✅ | Receiver only |
| PUT | `/api/swaps/{id}/complete` | ✅ | Either participant |
| PUT | `/api/swaps/{id}/cancel` | ✅ | Sender only |

A proposal is rejected unless the sender actually teaches the offered skill and the receiver
actually teaches the requested skill. Duplicate active proposals for the same pair are blocked.

### Shared learning sessions — always free

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/learning` | ✅ | `{ teacherId, skillId, message, scheduledAt? }` |
| GET | `/api/learning/incoming` | ✅ | Requests for your skills |
| GET | `/api/learning/outgoing` | ✅ | Your requests |
| GET | `/api/learning/all` | ✅ | Both |
| GET | `/api/learning/{id}` | ✅ | Participants only — room page |
| PUT | `/api/learning/{id}/accept` | ✅ | Teacher only |
| PUT | `/api/learning/{id}/decline` | ✅ | Teacher only |
| PUT | `/api/learning/{id}/complete` | ✅ | Either participant |
| PUT | `/api/learning/{id}/cancel` | ✅ | Learner only |



Both session kinds share one notes API. Notes unlock when a request is **accepted** and remain
available after it is **completed**; only the two participants can read them, and only the author
can edit or delete a note.

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/sessions/{swap\|learning}/{id}/notes` | ✅ | Participants only; chronological |
| POST | `/api/sessions/{swap\|learning}/{id}/notes` | ✅ | `{ title?, content, nextStep? }` |
| PUT | `/api/sessions/{swap\|learning}/{id}/notes/{noteId}` | ✅ | Author only |
| DELETE | `/api/sessions/{swap\|learning}/{id}/notes/{noteId}` | ✅ | Author only |
| GET | `/api/progress/me` | ✅ | Dashboard learning summary |

`GET /api/progress/me` is derived from completed sessions — it stores no separate progress
state. It returns `learningSessions`, `teachingSessions`, `notesWritten`, `skillsInProgress`,
`lastActivityAt`, and one row per learned skill with `completedSessions`, `targetSessions` (5),
`progressPercent`, `stage`, `teachers`, `lastSessionAt`, and the latest session type/id for the
**Notes** link. Skills a user marked as `LEARN` appear at zero sessions.

### Reviews

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/reviews/pending` | ✅ | Completed interactions to rate |
| POST | `/api/reviews` | ✅ | One review per participant per interaction |
| GET | `/api/reviews/user/{id}` | — | Public reviews |

### Q&A — free help

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/problems?query=&category=&status=` | — | Browse questions |
| GET | `/api/problems/{id}` | — | Question + answers |
| GET | `/api/problems/mine` | ✅ | Your questions |
| POST | `/api/problems` | ✅ | Ask |
| POST | `/api/problems/{id}/answers` | ✅ | Answer |
| PUT | `/api/problems/{id}/answers/{answerId}/accept` | ✅ | Author only; awards 10 reputation |

Hidden questions disappear from public feeds. Their author (and moderators) can still open the
detail page and sees a “hidden by a moderator” banner.

### Reporting & moderation

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/reports` | ✅ | `{ targetType: PROBLEM\|ANSWER\|USER, targetId, reason, details? }` |
| GET | `/api/reports/mine` | ✅ | Your report history |
| GET | `/api/admin/reports?status=&targetType=&page=&size=` | 🛡️ | Moderation queue |
| GET | `/api/admin/reports/{id}` | 🛡️ | Full context + decision history |
| PUT | `/api/admin/reports/{id}/claim` | 🛡️ | `OPEN → IN_REVIEW` |
| PUT | `/api/admin/reports/{id}/resolve` | 🛡️ | `{ action: DISMISS\|HIDE_CONTENT\|SUSPEND_USER, note }` |
| PUT | `/api/admin/problems/{id}/restore` | 🛡️ | Make a hidden question public again |
| PUT | `/api/admin/answers/{id}/restore` | 🛡️ | Make a hidden answer public again |
| PUT | `/api/admin/users/{id}/reactivate` | 🛡️ | Clear a suspension |
| GET | `/api/admin/stats` | 🛡️ | Queue and suspension counts |

**Rules:** self-reports and duplicate open reports are rejected; reports are limited to 10 per
reporter per 24 hours. Hiding an accepted answer removes its 10 reputation points and reopens the
question. Suspending an account blocks login **and** invalidates existing JWTs on their next
request. Every decision writes a `moderation_actions` row, and content/account actions are reversible.

To create the first moderator, start the backend once with:

```powershell
$env:ADMIN_USERNAME = "moderator"
$env:ADMIN_EMAIL = "moderator@example.com"
$env:ADMIN_PASSWORD = "choose-a-strong-password"
```

If `ADMIN_USERNAME` already exists, it is promoted instead of creating a duplicate.

### Community

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/community/top-contributors?limit=` | — | Ranked by accepted answers |

### Messaging

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/conversations` | ✅ | Inbox, latest activity first |
| POST | `/api/conversations` | ✅ | Get-or-create `{ userId }` |
| GET | `/api/conversations/unread-count` | ✅ | Navbar badge |
| GET | `/api/conversations/{id}/messages?before=&size=` | ✅ | Participants only |
| POST | `/api/conversations/{id}/messages` | ✅ | `{ body }` |
| PUT | `/api/conversations/{id}/read` | ✅ | Marks incoming messages read |

Non-participants get a **404** so thread existence is not leaked.

### Notifications

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/notifications?page=&size=` | ✅ | Newest first |
| GET | `/api/notifications/unread-count` | ✅ | Bell badge |
| GET | `/api/notifications/email-status` | ✅ | SMTP readiness + masked recipient |
| POST | `/api/notifications/test-email` | ✅ | Sends a real test message synchronously |
| PUT | `/api/notifications/{id}/read` | ✅ | Owner only |
| PUT | `/api/notifications/read-all` | ✅ | Owner only |

Every in-app notification is also emailed to the recipient's registered address when
SMTP is configured. Delivery runs on a bounded async pool, and failures are logged
without rolling back the triggering action. Email is enabled by default (`MAIL_ENABLED=true`);
set `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` and `MAIL_FROM` in `backend/.env`
(or the process environment) to deliver. The Notifications page has a **Send test email**
button that reports SMTP errors immediately.

For Gmail, enable 2-Step Verification and create an [App Password](https://myaccount.google.com/apppasswords);
use that 16-character password, not your normal account password. Port 587 uses STARTTLS.
For port 465 set `MAIL_STARTTLS=false`, `MAIL_STARTTLS_REQUIRED=false`, `MAIL_SSL_ENABLE=true`.

---

## Project structure

```
backend/src/main/java/com/skillswap/
├── common/          ApiResponse + PageResponse envelopes
├── config/          Security, CORS, static file serving, seed data, async
├── exception/       Custom exceptions + global handler
├── security/        JWT provider, filter, UserDetails
└── modules/
    ├── auth/        register, login
    ├── user/        profile, public profile, avatar storage
    ├── skill/       catalog, user skills, marketplace
    ├── swap/        proposal state machine
    ├── learning/    free shared learning sessions
    ├── review/      ratings + aggregates
    ├── problem/     Q&A, answers, reputation
    ├── chat/        conversations + messages
    ├── notification/ in-app + SMTP email
    ├── moderation/  reports, admin queue, audit trail
    └── community/   contributor leaderboard

frontend/src/
├── context/         AuthContext, ToastContext
├── components/
│   ├── layout/      Navbar, NotificationBell
│   ├── ui/          Modal, Avatar
│   ├── swap/        SwapCard, StatusBadge, RequestModal
│   ├── learning/    LearningCard
│   ├── session/     SessionNotesPanel
│   ├── problem/     AskQuestionModal
│   ├── moderation/  ReportButton, ReportModal
│   └── review/      RatingStars, ReviewModal
├── pages/           Home, Explore, Requests, Q&A, Detail, Dashboard,
│                    Profile, Messages, Notifications, Community, Moderation, Auth
├── services/        apiClient (+ assetUrl) + feature services
└── lib/             time formatting helpers
```

---

## Tests

```powershell
cd backend
.\mvnw.cmd test
```

**64 integration tests** (H2 + MockMvc): auth 5, chat 9, notifications 10, Q&A 9, reviews 7,
swaps 7, avatars/leaderboard 6, moderation 6, session notes/progress 5.

Live end-to-end scripts (backend must be running):

```powershell
powershell -ExecutionPolicy Bypass -File .\e2e-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\e2e-reviews.ps1
powershell -ExecutionPolicy Bypass -File .\e2e-qa.ps1
powershell -ExecutionPolicy Bypass -File .\e2e-messaging.ps1   # avatars, leaderboard, chat, notifications
```

A headless browser smoke test for the new pages lives at `ui-smoke.mjs`. It covers 65 checks,
including shared session notes, the dashboard learning-progress section, the grouped-skill
request modal on Explore, filing a report from Q&A, and claiming/dismissing it in the moderator
queue. To include the admin checks, start the backend with `ADMIN_USERNAME` + `ADMIN_EMAIL` +
`ADMIN_PASSWORD`, then:

```powershell
$env:SMOKE_ADMIN_USERNAME = "moderator"
$env:SMOKE_ADMIN_PASSWORD = "your-admin-password"
node ui-smoke.mjs
```

Without `SMOKE_ADMIN_USERNAME`, the script still checks reporting and the non-moderator access guard.

---

## Troubleshooting

**`Port 9090 was already in use`** — find and stop the process:

```powershell
Get-NetTCPConnection -LocalPort 9090 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

**`Access denied for user 'root'@'localhost'`** — `DB_PASS` is not set. Export it before starting.

**Frontend shows "API Standby"** — the backend is not reachable. Check the health endpoint and
`VITE_API_BASE_URL`.

**`Field 'X' doesn't have a default value`** — schema drift from an older build. Drop the
database and let Hibernate recreate it (dev only).
