# SkillSwap — Codebase Analysis & Completion Roadmap

> Stack: Spring Boot 3.4.3 (Java 21) + React 19 / Vite 8 + MySQL 8
> Backend runs on **http://localhost:9090**, frontend on **http://localhost:5173**

---

## 1. What exists today

### Backend (`/backend`) — Spring Boot, layered by feature module

| Layer | Files | Status |
|---|---|---|
| App bootstrap | `SkillSwapApplication`, `HealthController` | ✅ Working |
| Config | `SecurityConfig`, `DataInitializer` | ✅ Working |
| Security | `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails(Service)` | ✅ Working (HS256, 24h token) |
| Auth | `AuthController` → `AuthService` → `UserRepository` | ✅ Register + Login |
| Users | `UserController` → `UserService` | ✅ GET/PUT `/me`, avatar upload |
| Skills | `SkillController` → `SkillService`, `Skill` + `UserSkill` entities | ✅ Catalog, my-skills, marketplace, matching |
| Swaps | `SwapController` → `SwapService` (5 statuses, Jitsi link) | ✅ Complete |
| Learning | `LearningController` → `LearningService` | ✅ Free shared learning sessions |
| Session notes | `SessionNoteController` → `SessionNoteService` | ✅ Shared swap/learning notes |
| Progress | `ProgressController` → `ProgressService` | ✅ Dashboard learning summary |
| Reviews | `ReviewController` → `ReviewService` | ✅ Ratings + public profiles |
| Q&A | `ProblemController` → `ProblemService` | ✅ Problems, answers, reputation |
| Chat | `ConversationController` → `ConversationService` | ✅ 1:1 messages, unread counts |
| Notifications | `NotificationController` → `NotificationService` | ✅ In-app + SMTP email |
| Community | `CommunityController` → `CommunityService` | ✅ Leaderboard |
| Moderation | `ReportController`, `AdminModerationController` → `ModerationService` | ✅ Reports, queue, audit, suspension |
| Errors | `GlobalExceptionHandler` + 3 custom exceptions | ✅ Working |
| DB | MySQL, `ddl-auto: update`, 32 seeded skills, 3 roles | ⚠️ Dev-only |
| Tests | 93 integration tests | ✅ Passing |

### Frontend (`/frontend`) — React 19 + Vite, React Router

| Area | Status |
|---|---|
| `App.jsx` — routes only | ✅ |
| `context/` — `AuthContext`, `ToastContext` | ✅ |
| `pages/` — Home, Explore, Requests, Q&A, Dashboard, Auth, Profile, **Messages**, **Notifications**, **Community**, **Moderation** | ✅ |
| `components/` — Navbar, NotificationBell, Avatar, Modal, cards, modals, **ReportButton/ReportModal** | ✅ |
| `services/` — auth, skill, swap, learning, review, problem, message, notification, community, **report/moderation**, **session/progress** | ✅ |
| `index.css` — light/dark theme tokens | ✅ |

### What works end-to-end (verified)

Register → JWT → session restore → add teach/learn skills → marketplace search →
propose a skill swap or shared same-skill learning session → accept (Jitsi link) → complete → history.
**21/21 live checks pass** via `e2e-smoke.ps1`.

### What is still UI-only or missing

- **Forgot password / Google login** — not implemented.
- **Review moderation** — reviews cannot be edited, deleted, or reported.
- **Real-time transport** — messaging uses polling, not WebSockets.
- **Email delivery** — notifications are mirrored by SMTP when `MAIL_HOST`,
  `MAIL_USERNAME`, `MAIL_PASSWORD` and `MAIL_FROM` are configured. In-app
  notifications remain the source of truth if SMTP is unavailable.

---

## 2. Bugs found and fixed

| # | Severity | Bug | Fix |
|---|---|---|---|
| 1 | 🔴 Critical | `SkillService` looked users up **by email**, but the JWT principal carries the **username** → every `/api/users/skills` call 404'd. | Now `findByUsernameOrEmail` |
| 2 | 🔴 Critical | Skill endpoints returned **raw arrays** while every other endpoint returned the `{success,message,data}` envelope → frontend `res.data` was `undefined`, so the catalog and "My Skills" never populated. | Normalized all skill endpoints to the envelope |
| 3 | 🔴 Critical | Swap feature had **no UI** despite a complete backend. | Built `SwapPage`, `ProposeSwapModal`, `SwapCard` |
| 4 | 🟠 High | No 401/403 JSON entry point — Spring returned an HTML page, so axios showed "Request failed with status code 401". | `exceptionHandling` with JSON writers + axios auto-logout |
| 5 | 🟠 High | `UserResponse` (incl. **email**) embedded in swap payloads → any user could read another's email. | `PublicUserResponse` (no email) |
| 6 | 🟠 High | Demo `initialPeople` injected when the API returned `[]` → an empty DB looked populated. | Removed; real empty states |
| 7 | 🟠 High | Hardcoded MySQL password committed in `application.yml`. | `${DB_PASS:}` + `.env.example` |
| 8 | 🟡 Medium | `SwapService` never verified the offered/requested skills belonged to the right users. | Ownership checks added |
| 9 | 🟡 Medium | Duplicate pending swaps allowed between the same pair. | `existsActiveDuplicate` guard |
| 10 | 🟡 Medium | `Navbar` read `currentUser.name` (API returns `fullName`) → avatar always "U". | Fixed |
| 11 | 🟡 Medium | `/api/marketplace/matches` was caught by the permitAll wildcard → `principal` null → 500. | Specific rule ordered first |
| 12 | 🟡 Low | `index.html` title was `frontend`; `App.css` was dead Vite template code. | Fixed / deleted |

### Remaining known issues

| # | Severity | Issue |
|---|---|---|
| 13 | 🟠 High | JWT in `localStorage` → readable by any XSS payload |
| 14 | 🟠 High | `refresh-expiration-ms` configured but no refresh endpoint; access token dies after 24h |
| 15 | 🟡 Medium | `searchTeachers` loads all rows then filters in memory (full scan) |
| 16 | 🟡 Medium | Catch-all handler returns raw exception messages to clients |
| 17 | 🟡 Medium | Pagination exists only for Q&A; marketplace and requests still load all rows |
| 18 | 🟡 Medium | `ddl-auto: update` — no migration history |
| 19 | 🟡 Low | CORS hardcoded to localhost |
| 20 | 🟡 Low | No rate limiting on auth endpoints |

---

## 3. API surface

All responses use `{ success, message, data, timestamp }`.

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/health` | — | Health pill |
| POST | `/api/auth/register` | — | Returns JWT + user |
| POST | `/api/auth/login` | — | Returns JWT + user |
| GET | `/api/users/me` | ✅ | Restore session |
| PUT | `/api/users/me` | ✅ | Update profile |
| POST | `/api/users/me/avatar` | ✅ | Multipart `file`, ≤2 MB, magic-byte checked |
| DELETE | `/api/users/me/avatar` | ✅ | Remove photo |
| GET | `/api/files/**` | — | Public uploaded assets |
| GET | `/api/users/{id}/profile` | — | Public profile, **no email** |
| GET | `/api/skills?query=&category=` | — | Catalog |
| GET | `/api/skills/categories` | — | |
| GET | `/api/marketplace?query=&skillId=` | — | Teachers |
| GET | `/api/marketplace/matches?offered=&wanted=` | ✅ | Mutual swap matches |
| GET | `/api/users/skills` | ✅ | My skills |
| POST | `/api/users/skills` | ✅ | Add skill |
| DELETE | `/api/users/skills/{id}` | ✅ | Remove skill |
| POST | `/api/swaps` | ✅ | Propose |
| GET | `/api/swaps/incoming` | ✅ | |
| GET | `/api/swaps/outgoing` | ✅ | |
| GET | `/api/swaps/all` | ✅ | |
| GET | `/api/swaps/{id}` | ✅ | Participants only — room page |
| PUT | `/api/swaps/{id}/accept` | ✅ | Receiver only — Jitsi link |
| PUT | `/api/swaps/{id}/decline` | ✅ | Receiver only |
| PUT | `/api/swaps/{id}/complete` | ✅ | Either participant |
| PUT | `/api/swaps/{id}/cancel` | ✅ | Sender only |
| POST | `/api/learning` | ✅ | Free shared learning session |
| GET | `/api/learning/{incoming,outgoing,all}` | ✅ | |
| GET | `/api/learning/{id}` | ✅ | Participants only — room page |
| PUT | `/api/learning/{id}/{accept,decline,cancel,complete}` | ✅ | Role-checked |
| GET | `/api/sessions/{swap\|learning}/{id}/notes` | ✅ | Participants only; unlocked after accept |
| POST | `/api/sessions/{swap\|learning}/{id}/notes` | ✅ | Shared session note |
| PUT | `/api/sessions/{swap\|learning}/{id}/notes/{noteId}` | ✅ | Note author only |
| DELETE | `/api/sessions/{swap\|learning}/{id}/notes/{noteId}` | ✅ | Note author only |
| GET | `/api/progress/me` | ✅ | Learning progress: sessions, notes, skills |
| POST | `/api/reviews` | ✅ | Completed swap or session |
| GET | `/api/reviews/pending` | ✅ | Awaiting your rating |
| GET | `/api/reviews/user/{userId}` | — | Public |
| GET | `/api/problems?query=&category=&status=&page=&size=` | — | Paginated Q&A feed |
| GET | `/api/problems/{id}` | — | Detail, increments views |
| GET | `/api/problems/mine` | ✅ | Your questions |
| POST | `/api/problems` | ✅ | Ask |
| POST | `/api/problems/{id}/answers` | ✅ | Answer |
| PUT | `/api/problems/{id}/answers/{answerId}/accept` | ✅ | Author only |
| PUT | `/api/problems/{id}/{close,reopen}` | ✅ | Author only |
| GET | `/api/community/top-contributors?limit=` | — | Public leaderboard |
| GET | `/api/conversations` | ✅ | Inbox, latest activity first |
| POST | `/api/conversations` | ✅ | Get-or-create `{ userId }` |
| GET | `/api/conversations/unread-count` | ✅ | Navbar badge |
| GET | `/api/conversations/{id}` | ✅ | Participant only |
| GET | `/api/conversations/{id}/messages?before=&size=` | ✅ | Oldest-first page |
| POST | `/api/conversations/{id}/messages` | ✅ | Participant only |
| PUT | `/api/conversations/{id}/read` | ✅ | Marks the other side's messages read |
| GET | `/api/notifications?page=&size=` | ✅ | Newest first |
| GET | `/api/notifications/unread-count` | ✅ | Bell badge |
| PUT | `/api/notifications/{id}/read` | ✅ | Owner only |
| PUT | `/api/notifications/read-all` | ✅ | Owner only |
| POST | `/api/reports` | ✅ | Report a problem, answer, or user |
| GET | `/api/reports/mine` | ✅ | Reporter history |
| GET | `/api/admin/reports?status=&targetType=&page=&size=` | 🛡️ | Moderation queue |
| GET | `/api/admin/reports/{id}` | 🛡️ | Full context + audit trail |
| PUT | `/api/admin/reports/{id}/claim` | 🛡️ | Open → in review |
| PUT | `/api/admin/reports/{id}/resolve` | 🛡️ | Dismiss, hide content, or suspend |
| PUT | `/api/admin/problems/{id}/restore` | 🛡️ | Restore hidden question |
| PUT | `/api/admin/answers/{id}/restore` | 🛡️ | Restore hidden answer |
| PUT | `/api/admin/users/{id}/reactivate` | 🛡️ | Clear suspension |
| GET | `/api/admin/stats` | 🛡️ | Queue and suspension counts |

---

## 4. Roadmap

### Phase 0 — Stabilize the foundation — ✅ DONE

- [x] **0.1** Router + `AuthContext` + `ToastContext`; `App.jsx` split into 6 pages
- [x] **0.2** 401/403 JSON handlers + axios auto-logout
- [x] **0.3** Secrets externalized; `.env.example` added
- [x] **0.4** Demo fallback removed — real empty states
- [x] **0.5** Vite proxy (`/api` → `localhost:9090`) + `.env.example`
- [x] **0.6** 11 integration tests (auth + swap lifecycle + authorization)

### Phase 1 — Make Skill Swap real — ✅ DONE

- [x] **1.1** `Connect` → `ProposeSwapModal` with offered/requested skill pickers
- [x] **1.2** `/swaps` dashboard with Incoming / Outgoing / History tabs and status-aware actions
- [x] **1.3** Navbar badge polling pending incoming swaps (60 s + on focus)
- [x] **1.4** `GET /api/marketplace/matches` — mutual swap matching
- [x] **1.5** Swap validation: sender must teach offered, receiver must teach requested, no duplicates
- [x] **1.6** `PublicUserResponse` — emails no longer exposed

**Verified:** `e2e-smoke.ps1` → 21/21 checks against live backend + MySQL.

---

### Phase 1.5 — Shared learning sessions — ✅ DONE

Users can choose between two free learning options: trade different skills through a mutual swap,
or request a shared session to study the same skill with a peer. Shared sessions do not require
the requester to offer a skill in return.

- [x] `POST /api/learning` validates the teacher teaches the requested skill and blocks duplicates/self-requests.
- [x] Incoming, outgoing, and combined request views; accept/decline/cancel/complete lifecycle.
- [x] `RequestModal` offers both skill swaps and same-skill shared learning.
- [x] `/requests` unifies swaps and shared sessions in one inbox.

---

### Phase 2 — Problems / Q&A module — ✅ DONE

The **third way to use SkillSwap**: ask a question and let whoever knows answer. Reading is public
so the page works logged out.

- [x] **2.1** `modules/problem/` — `Problem`, `Answer`, `ProblemStatus`, repositories, DTOs
- [x] **2.2** `GET /api/problems?query=&category=&status=&page=&size=` — paginated feed with
      search + category + status filters
- [x] **2.3** `GET /api/problems/{id}` — detail with answers; increments `views`
- [x] **2.4** `GET /api/problems/mine` — your own questions
- [x] **2.5** `POST /api/problems`, `POST /api/problems/{id}/answers` — JWT required
- [x] **2.6** `PUT /api/problems/{id}/answers/{answerId}/accept` — author only; marks
      `SOLVED` and awards the answerer **10 reputation points**
- [x] **2.7** `PUT /api/problems/{id}/{close,reopen}` — author-only lifecycle
- [x] **2.8** Rules: one answer per user per question; the asker
      cannot answer their own question; only one accepted answer; closed questions reject answers
- [x] **2.9** `ProblemsPage` (`/qa`) — search, category/status filters, All vs My questions,
      and `AskQuestionModal`
- [x] **2.10** `ProblemDetailPage` (`/qa/:id`) — answers, accept buttons for the author,
      close/reopen, reputation note
- [x] **2.11** Navbar **Q&A** link; `helpfulPoints` + Q&A stats shown on public profiles

**Verified:** `ProblemFlowIntegrationTest` 9/9 (in a 27-test suite) and
`e2e-qa.ps1` → 37/37 checks against the live backend + MySQL.

---

### Phase 3 — Profiles, reviews & reputation — ✅ DONE

**Reviews, public profiles, avatars, Q&A reputation and the contributor leaderboard
are shipped.**

- [x] **3.1** `Review` entity — anchored to a completed `swap` *or* `learningRequest`
      (exactly one), with `reviewer`, `reviewee`, `rating (1–5)`, `comment`.
- [x] **3.2** One review per participant per interaction, enforced by a DB unique constraint
      *and* a service check. Cannot review an incomplete interaction or someone else's.
- [x] **3.3** `GET /api/reviews/pending` — completed sessions awaiting your rating;
      surfaced as a **"To review"** tab in `/requests`.
- [x] **3.4** `avgRating` + `reviewCount` aggregated onto `PublicProfileResponse` and every
      marketplace listing (one bulk query, not N+1).
- [x] **3.5** `ProfilePage` at `/profile/:userId` — teaches, wants to learn, reviews, and
      a request button per skill. Linked from Explore cards.
- [x] **3.6** Explore cards now show real star ratings instead of the skill category.
- [x] **3.7** Avatar upload (`POST /api/users/me/avatar`) — 2 MB cap, PNG/JPEG/GIF/WebP
      magic-byte validation, generated filenames, old file cleanup, public
      `/api/files/**` serving. Upload/replace/remove UI on the dashboard; the avatar is
      used in the navbar, Explore, profiles, reviews, requests, Q&A and messages.
- [x] **3.8** Reputation: `helpfulPoints` awarded on accepted answers, shown on the public
      profile alongside questions/answers/accepted counts.
- [x] **3.9** `GET /api/community/top-contributors` + `/community` leaderboard
      (rank, reputation, accepted/answers/questions, rating). Linked from Q&A and the footer.
- [x] **3.10** Explore discovery refresh — teacher-skill rows grouped by person, real bio/city/
      joined/helpful-point context, category tiles, top-rated and free spotlights, search/free/sort
      filters, progressive “show more” results, and a skill picker inside the request modal.

**Verified:** `AvatarAndCommunityIntegrationTest` 6/6; `e2e-reviews.ps1`; overall
`mvn test` → **51/51** (Auth 5, Chat 9, Notifications 9, Problems 9, Reviews 7,
Swaps 6, Avatars/Community 6).

---

### Phase 4 — Messaging & notifications — ✅ DONE

- [x] `Conversation` + `Message` entities; canonical user pair with a unique constraint,
      `lastMessageAt`, per-message `readAt`.
- [x] `GET /api/conversations`, `POST /api/conversations` (get-or-create),
      `GET/POST /api/conversations/{id}/messages`, `PUT /{id}/read`,
      `GET /unread-count`. Only participants can read or write; non-participants get 404.
- [x] **Polling for v1** — open thread every 4 s, inbox every 15 s, navbar unread every 45 s.
      No WebSocket server needed; the service API is ready for a STOMP upgrade.
- [x] Notification table + bell dropdown for swap proposed/accepted/declined/completed/
      cancelled, learning requested/accepted/declined/completed/cancelled and answer accepted.
      Unread badges, individual/mark-all read, full
      `/notifications` page.
- [x] Email via `spring-boot-starter-mail`, sent **asynchronously** on a bounded
      pool and best-effort. Every notification is mirrored to the recipient's
      registered address when SMTP is configured (`MAIL_HOST`, `MAIL_USERNAME`,
      `MAIL_PASSWORD`, `MAIL_FROM`). Branded HTML + plain-text alternative, absolute
      frontend links, masked addresses in logs, and a synchronous
      `POST /api/notifications/test-email` for setup checks. In-app delivery is
      stored first and remains available if SMTP fails.
- [x] `/messages` two-pane inbox with avatars, unread badges, day separators, older-message
      paging, Enter-to-send and read ticks. "Message" buttons on profiles and request cards.

**Verified:** `ChatFlowIntegrationTest` 9/9, `NotificationFlowIntegrationTest` 9/9,
frontend production build clean.

---

### Phase 4.5 — Reporting & moderation — ✅ DONE

- [x] `reports` + `moderation_actions` tables; reporter/admin references stored as IDs so the
      audit trail survives account changes.
- [x] Report questions, answers, and user profiles from Q&A pages and public profiles.
      Self-reports and duplicate open reports are rejected; 10 reports per user per 24 hours.
- [x] Admin-only `/api/admin/**` queue with status/type filters, priority (high for
      harassment/hate/impersonation), duplicate counts, claimant, and full target preview.
- [x] Actions: dismiss, hide content, suspend account. Hiding an accepted answer rolls back its
      10 reputation points and reopens the question.
- [x] Suspension is enforced in `CustomUserDetails.isEnabled()` **and** the JWT filter, so a token
      issued before suspension stops working immediately.
- [x] Reversible: restore hidden questions/answers and reactivate accounts; every action appends
      to the audit trail.
- [x] `/admin/reports` UI with stats, triage list, detail pane, action confirmation notes, and
      decision history. Navbar link appears only for `ROLE_ADMIN`.
- [x] Moderator bootstrap via `ADMIN_USERNAME` (promote) or `ADMIN_USERNAME` + `ADMIN_EMAIL` +
      `ADMIN_PASSWORD` (create). The last admin cannot be suspended through a report.

**Verified:** `ModerationFlowIntegrationTest` 6/6; `mvn test` → **59/59**; production build clean.

---

### Phase 4.6 — Session notes & learning progress — ✅ DONE

- [x] `session_notes` table with `session_type` + `session_id` (swap or learning), author, title,
      note body, and optional next step. Notes unlock once a request is accepted and remain
      editable after completion.
- [x] Shared notes API for both session kinds; participants can read, authors can edit/delete,
      outsiders get a 404.
- [x] Video room page gets a **Session notes** panel with add/edit/delete, next-step callouts, and
      an empty state. Completed history cards link back to the notes.
- [x] `GET /api/progress/me` — read model built from completed swaps/sessions: learning sessions,
      teaching sessions, notes written, and per-skill completion counts/stages. Explicit LEARN
      skills appear at zero sessions so goals are visible before the first booking.
- [x] Dashboard **Learning progress** section with summary counts, progress bars, milestone stages,
      teacher names, last-session dates, and links to the latest notes.

**Verified:** `SessionNotesProgressIntegrationTest` 5/5; `mvn test` → **64/64**;
`node ui-smoke.mjs` → **65/65**; production build clean.

---

### Phase 5 — Sessions & scheduling (optional, 5+ days)

- [ ] Availability slots per user; conflict check on `scheduledAt`.
- [ ] Session lifecycle: add an explicit `NO_SHOW` outcome (accept → complete already works).
- [ ] Calendar `.ics` invite in the confirmation email.

---

### Phase 6 — Production readiness (3–4 days)

- [ ] **Migrations:** add Flyway, baseline the schema, remove `ddl-auto: update`.
- [ ] **Pagination:** `Pageable` on marketplace, problems, swaps, messages.
- [ ] **Query fix:** push marketplace filtering into SQL:

```java
@Query("""
  SELECT us FROM UserSkill us JOIN FETCH us.user JOIN FETCH us.skill s
  WHERE us.skillType = 'TEACH'
    AND (:query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
                      OR LOWER(us.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')))
  """)
Page<UserSkill> findTeachers(@Param("query") String query, Pageable pageable);
```

- [ ] **Rate limiting** on `/api/auth/**` (Bucket4j).
- [ ] **Refresh tokens** — rotate, store hashed, `POST /api/auth/refresh`,
      `POST /api/auth/logout`. Access token in memory; refresh token in an
      `HttpOnly; Secure; SameSite=Strict` cookie.
- [ ] **Hardening:** validate `JWT_SECRET` length at startup, correlation IDs,
      stop leaking exception messages, add Actuator health/metrics.
- [ ] **Docker:** `backend/Dockerfile`, `frontend/Dockerfile` (nginx),
      `docker-compose.yml` with MySQL + volumes.
- [ ] **CI:** GitHub Actions — `mvn verify`, `npm ci && npm run build && npm run lint`.
- [ ] **Frontend polish:** skeleton loaders, error boundaries, `aria-*` labels,
      focus trap in modals, `<button disabled>` while submitting.
- [ ] **Docs:** keep `README.md` current.

---

## 5. Data model additions

```
users ──< user_skills >── skills
users ──< problems ──< answers
users ──< reviews (reviewer / reviewee) >── swaps
users ──< notifications
users ──< conversations ──< messages
users ──< reports ──< moderation_actions (IDs, append-only)
users ──< session_notes (session_type + session_id → swap or learning)
users ──< availability
swaps ──< sessions
```

Add indexes on `skill_swap_requests(sender_id, receiver_id, status)`,
`problems(status, created_at)`, `messages(conversation_id, id)`,
`conversations(user_one_id, user_two_id)` (unique),
`notifications(user_id, read_at, created_at)`,
`session_notes(session_type, session_id)` / `session_notes(author_id)`, and
`reports(status, created_at)` / `reports(target_type, target_id)`.

---

## 6. Security checklist

- [x] No secrets in git
- [x] 401/403 JSON handlers + frontend auto-logout
- [x] Public payloads exclude email
- [ ] `JWT_SECRET` ≥ 64 chars, validated at boot
- [ ] Refresh-token rotation + `HttpOnly` cookie
- [ ] Rate limit auth endpoints
- [ ] CORS allowlist from config, not hardcoded
- [x] File-upload MIME + size validation (2 MB, magic bytes, generated names)
- [x] Admin routes require `ROLE_ADMIN`; suspensions invalidate existing JWTs
- [ ] HTTPS only in prod; `Secure` cookies; HSTS
- [ ] Dependency scanning (`mvn dependency-check`, `npm audit`)

---

## 7. Testing

| Layer | Tool | Coverage |
|---|---|---|
| Integration | `@SpringBootTest` + MockMvc + H2 | ✅ 64 tests: auth, swaps, sessions, session notes/progress, reviews, Q&A, chat, notifications, avatars, leaderboard, moderation |
| Unit | JUnit 5 + Mockito | ⬜ services |
| Repository | `@DataJpaTest` | ⬜ custom queries |
| Frontend | Vitest + Testing Library | ⬜ contexts, interceptors |
| E2E | `e2e-*.ps1` | ✅ smoke 21, reviews 29, Q&A 37, messaging/avatars 49 |
| Browser E2E | Chrome DevTools Protocol | ✅ `ui-smoke.mjs` 65 checks across all pages, notes, progress, reporting, and the moderation queue |

---

## 8. Definition of done

1. Two users can register, add teach/learn skills, and find each other.
2. A swap can be proposed, accepted (with a working video link), completed, and reviewed —
   with correct permissions at every step.
3. Problems can be posted, answered, and marked solved; points update.
4. Dashboard stats come from the database.
5. No hardcoded credentials, no demo fallbacks, no dead code.
6. `mvn test` and `npm run build` pass in CI; `docker compose up` works.
7. A new developer can go from `git clone` to running locally using only the README.

---

## 9. Run it

```powershell
# Backend (needs MySQL on :3306)
cd backend
$env:DB_PASS = "your_password"
.\mvnw.cmd spring-boot:run          # http://localhost:9090

# Frontend
cd frontend
npm install
npm run dev                          # http://localhost:5173

# Verify
cd ..
powershell -ExecutionPolicy Bypass -File .\e2e-smoke.ps1
```

---

## 10. Recommended order

```
Phase 0  (stabilize)        ── ✅ done
   └─ Phase 1 (swap UI)     ── ✅ done
      └─ Phase 1.5 (shared learning sessions) ── ✅ done
             └─ Phase 3 (reviews + profiles + avatars + leaderboard) ── ✅ done
                  └─ Phase 2 (Q&A)    ── ✅ done
                       └─ Phase 4 (messaging + notifications) ── ✅ done
                            └─ Phase 4.5 (reporting + moderation) ── ✅ done
                                 └─ Phase 4.6 (session notes + progress) ── ✅ done
                                             └─ Phase 5 (scheduling) ── optional
                                                   └─ Phase 6 (prod) ── ship
```
