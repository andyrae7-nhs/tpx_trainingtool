# TPX / GROW

**A personalised progression and training tool for TPXimpact people.**

Pick your job role and grade, and TPX Grow compares you against the Progression Framework for that role. It then suggests training that closes your gaps. Along the way you keep an evidence journal for your end-of-year assessment, log and review what you've learned, earn badges and see what colleagues are up to. Pip the sprout keeps you company.

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, PostgreSQL (H2 for local dev) |
| **Frontend** | Next.js 14 (App Router, TypeScript), styled in the TPXimpact brand |
| **AI** | Anthropic Claude via the Messages API. Optional: there is a built-in rule-based fallback |
| **Packaging** | Docker and Docker Compose |

---

## Features

| Feature | Where | What it does |
|---|---|---|
| **Login system** | `/login`, `/register` | Email and password accounts with BCrypt hashes and JWT sessions. You can restrict sign-ups to one email domain. |
| **Personalised to your role** | `/onboarding`, `/profile` | You choose one of the 21 job roles in the framework, plus your current grade and target grade. Everything else is tailored to that choice. |
| **Gap analysis** | `/progression` | Shows each technical skill, behaviour and impact area for your role. For each one you see where you rate yourself, what your target grade expects and the gap between them. You can open the two level descriptions side by side, and preview any other grade. |
| **AI training plan** | `/plan` | Sends your biggest gaps and a shortlist of matching resources to Claude, which picks training for each gap. Each pick comes with a reason, a practical on-the-job action and some quick wins. With no API key, a rule-based matcher does the job instead. |
| **Training library** | `/library` | About 100 courses, books, events, guides and internal Consulting Skills programmes. **Every link sits next to a short description** of what it is, plus its type, level, cost and format. Colleague reviews show beside each one. |
| **Evidence journal** | `/journal` | Log what you did and the impact it had, then tag each entry to framework items. **Export** groups your evidence by skill, behaviour and impact, ready to **copy and paste** into your progression assessment. You can also download it as `.txt`, and optionally have Claude draft a summary paragraph for each item. |
| **Learning log** | `/learning` | Track courses, books, events and programmes as planned, in progress or completed. Give each a star rating, write a review and share it with colleagues. |
| **Achievements** | `/achievements` | 20 badges across Getting started, Evidence, Learning, Together and Mastery. XP, levels (Seedling → Forest) and daily streaks. Pip celebrates each new badge with confetti. |
| **Leaderboard** | `/leaderboard` | XP rankings for everyone, your capability or the people you follow. Filter by this week, this month or all time. |
| **Community** | `/community`, `/people/:id` | An informal feed with posts, likes and comments, and a way to follow colleagues. Badges and shared reviews post to the feed automatically, so friends can see what you're learning. |
| **Pip, your assistant** | Everywhere | A pop-up helper in the spirit of the old Office assistant, as an original sprout character. Pip offers tips for each page and chats about your gaps and evidence, using Claude when configured. You can hide Pip if you want peace and quiet. |

---

## Quick start (Docker)

You need Docker Desktop, or Docker Engine with Compose v2.

```bash
git clone <this repo> && cd tpx_trainingtool
cp .env.example .env          # optional: add ANTHROPIC_API_KEY for AI features
docker compose up --build
```

Then open **http://localhost:3000**.

- The demo login is `demo@example.com` / `password123`.
- Six fictional colleagues are seeded so the leaderboard and feed have content. Set `SEED_DEMO_DATA=false` to turn this off.

| Service | URL | Notes |
|---|---|---|
| Frontend | http://localhost:3000 | Next.js. Proxies `/api/*` to the backend, so the browser never needs CORS |
| Backend API | http://localhost:8080/api | Spring Boot |
| Health | http://localhost:8080/actuator/health | Used by the Compose health check |
| Postgres | internal (`db:5432`) | Data lives in the `db-data` volume |

To reset everything: `docker compose down -v`.

## Configuration

Set these in `.env`, or as environment variables on the backend container.

| Variable | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | *(blank)* | Turns on Claude for plans, Pip chat and evidence summaries. Blank means the built-in rules are used |
| `ANTHROPIC_MODEL` | `claude-sonnet-4-5` | Any Messages API model id. See [the models list](https://docs.claude.com/en/docs/about-claude/models) |
| `ANTHROPIC_BASE_URL` | `https://api.anthropic.com` | Point at a proxy or gateway if needed |
| `ANTHROPIC_TIMEOUT_SECONDS` | `45` | Read timeout for AI calls |
| `JWT_SECRET` | dev value | **Change this.** It must be at least 32 characters |
| `JWT_EXPIRY_HOURS` | `72` | How long a login lasts |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | H2 in memory | JDBC connection settings. Compose sets these for Postgres |
| `ALLOWED_EMAIL_DOMAIN` | *(blank)* | For example `tpximpact.com`, to only allow sign-ups from that domain |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Only needed if the browser calls the API directly |
| `SEED_DEMO_DATA` | `true` | Seeds demo users when the database is empty |
| `BACKEND_URL` *(frontend build arg)* | `http://backend:8080` | Where Next.js proxies `/api` to |

## Deploying

The two images are stateless apart from Postgres, so they run anywhere that runs containers, such as Azure Container Apps, AWS ECS/App Runner, GCP Cloud Run or Kubernetes.

1. **Database:** provision a managed PostgreSQL 14+ and set `DB_URL=jdbc:postgresql://<host>:5432/<db>`, `DB_USER` and `DB_PASSWORD`. Tables are created and updated automatically (`ddl-auto: update`). For production, consider adding Flyway migrations.
2. **Backend:** `docker build -t tpx-grow-api ./backend`. Run it with `JWT_SECRET`, the DB variables and `SEED_DEMO_DATA=false`, plus `ANTHROPIC_API_KEY` if you want AI features (store it as a secret). The container listens on 8080; use `/actuator/health` for probes.
3. **Frontend:** `docker build --build-arg BACKEND_URL=https://<internal-api-host> -t tpx-grow-web ./frontend`. It listens on 3000. Expose only the frontend publicly; the API can stay internal because the frontend proxies to it.
4. Put HTTPS in front, for example with your platform's ingress or load balancer.

## Local development without Docker

```bash
# Backend (needs JDK 21 + Maven). Uses in-memory H2 by default.
cd backend && mvn spring-boot:run

# Frontend (needs Node 20+)
cd frontend && npm install && npm run dev      # http://localhost:3000, proxies to :8080
```

**Frontend-only work:** if you don't have Java handy, `node tools/mock-api/server.js` starts a lightweight in-memory mock of the API on port 8080. It covers the main flows and uses the same framework and catalogue data.

Run the backend tests with `cd backend && mvn test`. `ApiSmokeTest` walks through registering, onboarding, gap analysis, getting a plan, adding a journal entry, exporting, achievements and the leaderboard.

---

## Data sources

### Progression framework

`backend/src/main/resources/data/framework.json` is generated from the two Progression Framework spreadsheets:

- **`Progression Framework - DT billable skills.xlsx`** (v1.0, Progression Assessment 2026). This gives 21 roles across Delivery, Design and Tech & Data. It also sets the expected skill level (Learner → Contributor → Skilled → Expert → Leader) at each grade, from Graduate (6) to Principal (11), with the descriptor for each level.
- **`Progression Framework - behaviours and impact matrix.xlsx`** (v3.0). This gives the five behaviours, each with a descriptor for every grade, and the impact areas.

To regenerate it after the spreadsheets change:

```bash
pip install openpyxl
python3 tools/extract_framework.py "Progression Framework - DT billable skills.xlsx" \
  "Progression Framework - behaviours and impact matrix.xlsx" backend/src/main/resources/data/framework.json
```

How the gap analysis works:

- **Technical skills:** your self-rating is compared with the level expected at your target grade. The gap is the number of levels between them. If a role has no expectation at your current grade (for example, Technology Consultant starts at Senior), you start at "Not yet started".
- **Behaviours and impact:** you rate which grade's descriptor matches you best, and that is compared with your target grade. Junior and Graduate share one descriptor.
- **Before you rate yourself:** you're assumed to be at the expected level for your current grade.

### Training catalogue

`backend/src/main/resources/data/catalogue.json` is generated by `tools/build_catalogue.py`, which is the easiest place to add or edit resources. It contains:

- The **Consulting Skills programme** modules from the T&D deck, across Client Relationships, Collaborative Working, Commercial Stewardship and Foundation.
- Internal activities such as Communities of Practice, lunch and learns, bid shadowing and mentoring.
- Public resources, including GOV.UK, W3C, cloud providers, well-known books, courses and UK events.

Every resource has a description, and each one is tagged. `TagMatcher` maps framework items to those tags so the rule-based recommender and the AI shortlist can match resources to gaps. Internal programmes have no URL yet; add links to your LMS when you have them.

---

## API reference

Everything is under `/api`. Endpoints marked 🔒 need an `Authorization: Bearer <token>` header, where the token comes from register or login. Errors return `{status, error, message, timestamp}`.

### Auth and profile
| Method | Path | Body / query | Returns |
|---|---|---|---|
| POST | `/auth/register` | `{email, password, displayName}` | `201 {token, user}` |
| POST | `/auth/login` | `{email, password}` | `{token, user}` |
| GET 🔒 | `/me` | | Your profile, level and streak. Also updates your daily streak |
| PUT 🔒 | `/me` | `{displayName?, bio?, avatarColor?, roleId?, currentGrade?, targetGrade?}` | Updated profile |
| GET 🔒 | `/users?q=` | Name search. Blank returns the top 50 by XP | `User[]`, with `isFollowing` |
| GET 🔒 | `/users/{id}` | | Public profile |

### Framework (public)
| Method | Path | Returns |
|---|---|---|
| GET | `/framework/roles` | `[{id, name, capability, practice, skillCount}]` |
| GET | `/framework/roles/{id}` | The role, with each skill's definition, level descriptors and expected level per grade |
| GET | `/framework/grades` | `[{code: "G6".."G12", name, number}]` |
| GET | `/framework/skill-levels` | `["Learner","Contributor","Skilled","Expert","Leader"]` |
| GET | `/framework/behaviours` | Behaviours with a descriptor per grade |
| GET | `/framework/impacts` | `{areas, items}` |

### Progression 🔒
| Method | Path | Body / query | Returns |
|---|---|---|---|
| GET | `/progression/gap` | `?targetGrade=G10` to preview a grade other than your target | `GapReport`: `skills[]`, `behaviours[]`, `impacts[]` and a `summary` with readiness % and gap counts |
| GET | `/progression/assessments` | | Your saved self-ratings |
| PUT | `/progression/assessments` | `[{itemType: SKILL\|BEHAVIOUR\|IMPACT, itemId, level, note?}]`, where `level` is a skill level for skills and a grade code otherwise | Updated `GapReport` |

### Recommendations and Pip 🔒
| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/recommendations` | | Builds and saves a new `Plan`: `{generatedBy: claude\|rules, summary, items[{gap, suggestions[{resource, reason}], action}], quickWins[]}` |
| GET | `/recommendations/latest` | | The last plan, or `204` if there isn't one |
| GET | `/ai/status` | | `{enabled, model}` |
| POST | `/pip/chat` | `{message, history?: [{role, content}]}` | `{reply, source, suggestions[]}` |
| GET | `/pip/tip` | | `{tip}` |

### Catalogue (public)
| Method | Path | Query | Returns |
|---|---|---|---|
| GET | `/catalogue` | `q`, `type` (COURSE, BOOK, EVENT, PROGRAMME, ARTICLE), `tag` | `Resource[]`: `{id, title, type, provider, url, description, tags, level, duration, cost, format}` |
| GET | `/catalogue/{id}` | | `Resource` |
| GET | `/catalogue/tags` | | `string[]` |

### Evidence journal 🔒
| Method | Path | Body / query | Returns |
|---|---|---|---|
| GET | `/journal` | | Your entries, newest first, with framework refs resolved to names |
| POST | `/journal` | `{title, body?, impact?, entryDate?, refs?: ["SKILL:<id>", "BEHAVIOUR:<id>", "IMPACT:<id>"]}` | `201` entry |
| PUT | `/journal/{id}` | Same as POST | Updated entry |
| DELETE | `/journal/{id}` | | `204` |
| GET | `/journal/export` | `from`, `to` (YYYY-MM-DD) and `polish=true` for AI summaries | `{text, entryCount, generatedBy}`: plain text grouped by framework item, ready to paste |

### Learning log 🔒
| Method | Path | Body / query | Returns |
|---|---|---|---|
| GET | `/learning` | | Your items |
| POST | `/learning` | `{catalogueId}` to add from the library, or `{type, title, provider?, url?, status?}` | `201` item |
| PUT | `/learning/{id}` | Any of `{status, rating (1-5), review, shared, completedOn, ...}` | Updated item. Completing an item and sharing a review both earn XP, and a shared review posts to the feed |
| DELETE | `/learning/{id}` | | `204` |
| GET | `/learning/reviews` | `catalogueId?` | Reviews colleagues have shared |

### Gamification 🔒
| Method | Path | Query | Returns |
|---|---|---|---|
| GET | `/achievements` | | `{level, streakDays, achievements[{code, title, description, icon, category, bonusXp, earned, earnedAt}]}` |
| GET | `/achievements/users/{id}` | | That person's earned badges |
| POST | `/achievements/unseen` | | Badges unlocked since the last call. The frontend uses this to celebrate |
| GET | `/leaderboard` | `scope=all\|capability\|friends`, `period=all\|month\|week` | Ranked rows |
| GET | `/activity` | | Your 20 most recent XP events |

### Community 🔒
| Method | Path | Body / query | Returns |
|---|---|---|---|
| GET | `/social/feed` | `scope=following\|everyone\|user`, `userId`, `page`, `size` | `Post[]`, with likes and comments |
| POST | `/social/posts` | `{content}` | `201 Post` |
| DELETE | `/social/posts/{id}` | | `204` (your own posts only) |
| POST | `/social/posts/{id}/like` | | Toggles your like |
| POST | `/social/posts/{id}/comments` | `{content}` | `201 Post` |
| DELETE | `/social/comments/{id}` | | `204` |
| POST / DELETE | `/social/follow/{userId}` | | Follow or unfollow |
| GET | `/social/following`, `/social/followers` | `userId?` | `User[]` |

### Earning XP

| Activity | XP |
|---|---|
| Complete a piece of learning | 30 |
| Complete your profile | 20 |
| Add a journal entry | 15 |
| Generate a training plan | 10 |
| Share a review | 10 |
| Export your evidence | 10 |
| Save your self-assessment | 5 |
| Log a new piece of learning | 5 |
| Write a post | 5 |
| Visit each day | 3 |
| Comment | 2 |
| Follow someone | 2 |
| Chat with Pip | 1 |

Badges add bonus XP on top. Levels need 50 × n × (n−1) XP: 0, 100, 300, 600, 1,000 and so on.

---

## Project structure

```
backend/                     Spring Boot API
  src/main/java/com/tpximpact/trainingtool/
    ai/            Claude client, recommendation engine, Pip
    catalogue/     Training catalogue + tag matching
    config/        App properties, demo data seeder
    framework/     Progression framework loader + endpoints
    gamification/  XP, levels, achievements, leaderboard
    journal/       Evidence journal + export
    learning/      Learning log + reviews
    progression/   Self-assessment + gap analysis
    security/      JWT auth
    social/        Posts, comments, likes, follows
    user/          Accounts + profiles
  src/main/resources/data/   framework.json, catalogue.json
frontend/                    Next.js app
  app/            One folder per page
  components/     AppShell, Pip, ResourceCard, PostCard, UI bits
  lib/            API client, auth context, types
tools/
  extract_framework.py      Spreadsheets → framework.json
  build_catalogue.py        Builds catalogue.json
  mock-api/server.js        Mock API for frontend-only development
docker-compose.yml
```

## Brand

The frontend follows the TPXimpact brand style guide:

- **Colours:** the pastels `#c8e9ff`, `#ffcfca`, `#cafce5` and `#e7d2ff` are the primary palette, always with dark text. Headlines are black on pastel, and muted blue (`#0252bb`) on white. Dark-blue panels carry white text.
- **Fonts:** Oswald Bold in all caps for headlines, Playfair Display Medium for subtitles and DM Sans for body text, all loaded from Google Fonts.
- **Cyan:** the cyan slash (`#00B8FF`) appears only in the product mark.
- **Writing:** copy is plain British English in the active voice.
- **Logo:** swap the text mark in `components/AppShell.tsx` for the official logo from the Asset Library if you'd like.
