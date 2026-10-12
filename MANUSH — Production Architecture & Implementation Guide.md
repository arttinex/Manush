# MANUSH — Production Architecture & Implementation Guide

Oct 12, 2026 · @shiful

MANUSH is a Bangladesh-focused community assistance platform (help requests, professional services, jobs, blood donation, messaging, and reputation) built as a **single Spring Boot application packaged as one WAR**, backed only by **MongoDB Atlas** (no Redis, no separate microservices, no external object storage), with a production-grade vanilla-JS frontend. This doc is the restructured, implementation-ready version of the original master prompt.

&#91;embedded content: request path · browser to Atlas, 6 internal packages, 4 external providers\]

A request enters through Cloudflare, hits the single WAR, and is handled entirely by one JVM process before touching Atlas.

## Why a monolith, not microservices

- **One deployable artifact.** `manush.war` (or runnable JAR) is the only thing that ships. No service discovery, no inter-service auth, no distributed tracing across network hops — the #1 reason solo-developer microservice projects stall before the demo.
- **Package-per-domain, not service-per-domain.** `auth`, `help`, `job`, `blood`, `jogajog`, `ai`, `admin`, and `common` are Java packages inside one Spring context. Each still follows Controller → Service → Repository → MongoDB. Domains talk to each other through **service interfaces**, never by reaching into another domain's repository — this is what makes the design extractable later.
- **Atlas-only.** One MongoDB Atlas cluster holds every collection, logically namespaced by domain (`auth.users`, `help.help_requests`, etc.). No Redis, no S3/MinIO — GridFS handles binary storage, and TTL collections replace Redis's expiring keys (Section 5).
- **Scaling boundary, stated honestly.** This architecture assumes **one running instance**. In-memory rate limiting and in-process WebSocket session state are correct *for one instance* and become incorrect the moment you run two behind a load balancer. That is a deliberate, documented tradeoff — not an oversight — appropriate for a solo-developer launch and revisited only if real traffic demands horizontal scaling.

## Package layout

```text
com.manush
 ├── auth/        (controller, service, repository, dto, model, mapper)
 ├── help/
 ├── job/
 ├── blood/
 ├── jogajog/      (incl. websocket config)
 ├── ai/           (AiProvider interface + implementations)
 ├── admin/
 ├── common/
 │    ├── security/      (JWT filter, RBAC, CORS)
 │    ├── ratelimit/     (Bucket4j filter)
 │    ├── storage/       (GridFS StorageProvider)
 │    ├── exception/     (@RestControllerAdvice)
 │    └── config/
 └── ManushApplication.java
```

## Provider interfaces (unchanged principle, single process)

Even inside one WAR, never hard-wire a vendor. Every outbound integration sits behind an interface so the implementation can be swapped without touching calling code:

```java
public interface AiProvider { AiResponse generate(String prompt); }
public interface EmailProvider { void send(String to, String subject, String body); }
public interface SmsProvider { void sendOtp(String phone, String code); }
public interface IdentityVerificationProvider { VerificationResult verify(VerificationRequest req); }
public interface StorageProvider { String store(InputStream file, String filename, String contentType); InputStream retrieve(String fileId); }
```

`MockVerificationProvider` and `GridFsStorageProvider` are the default beans for university/dev; swap via `@ConditionalOnProperty` or a profile-specific `@Configuration` when a licensed provider is ready.

## Tech Stack

| Layer | Choice | Notes |
| --- | --- | --- |
| Language | Java 25 |  |
| Framework | Spring Boot 4.x | packaged as WAR, `spring-boot-starter-tomcat` scope `provided` |
| Security | Spring Security + JJWT | access + refresh tokens, RBAC |
| Data | Spring Data MongoDB | single Atlas cluster, GridFS for files |
| Rate limiting | Bucket4j (in-memory) | per-route token buckets, no Redis |
| Realtime | Spring WebSocket + STOMP | in-process simple broker |
| AI | Spring AI | `AiProvider` interface → OpenAI-compatible provider |
| Templating | Thymeleaf | server-rendered pages |
| Frontend JS | Vanilla JS (ES2022 modules, no build step) | see Frontend section |
| Alerts/Dialogs | SweetAlert2 (CDN) | toasts, confirmations, modals |
| Styling | Custom CSS with design tokens + Google Fonts | dark mode via `prefers-color-scheme` + manual toggle |
| Caching | Caffeine (in-process) | `@Cacheable` for read-heavy lookups |
| API docs | springdoc-openapi (Swagger UI) | `/swagger-ui.html` |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers (Mongo) |  |
| Packaging | Maven, `packaging=war` | runnable standalone OR deployable to external Tomcat 10+ |
| Containerization | Single Dockerfile + one `docker-compose.yml` service (`app`) | Atlas is remote, so no local Mongo container needed |
| CI | GitHub Actions (`mvn verify` on push/PR) |  |
| Hosting | Render (or similar) | Cloudflare in front for DNS/WAF/CDN |
| Database | MongoDB Atlas (free/shared or M10+ tier) | 2dsphere + TTL indexes, GridFS |
| Secrets | Environment variables (`.env` locally, platform env vars in prod) | never committed |

Dependencies deliberately **excluded**: Spring Cloud Gateway, Redis, Kafka/RabbitMQ, a separate object-storage SDK (S3/MinIO), Kubernetes. Each is a documented future upgrade path (Section 15), not a missing requirement — none of them are needed to reach a production-correct v1 for a single-instance deployment.

## Frontend & UI System

No React/Vue/Angular. The frontend is Thymeleaf-rendered HTML + vanilla ES modules, but structured so it still *feels* like a modern SPA-grade product, not a CRUD scaffold.

### Visual identity

**Palette — "Golden Luxe"**

| Token | Hex | Role |
| --- | --- | --- |
| `--manush-accent` (cream) | `#EDE7C7` | light backgrounds, text-on-dark, hero panel fill |
| `--manush-primary` (deep red) | `#8B0000` | primary buttons, links, active states |
| `--manush-primary-dark` (maroon) | `#5B0202` | hover/pressed states, secondary accents |
| `--manush-ink` (espresso black) | `#200E01` | dark-mode background, premium dark sections, footer |

A warm, low-saturation palette reads as trustworthy and established rather than the generic SaaS blue — appropriate for a platform asking people to trust it with blood donation and identity verification.

- **Typography pairing — three families, each with one job, never mixed:**
  - **Script** (`Pinyon Script`) — exactly one accent word per view: the "Manush" wordmark in the nav/hero, a single emphasized word in a section eyebrow. Never body text, never more than a few words — mirrors the "Golden" treatment in the reference mark.
  - **Serif** (`Playfair Display`) — all H1/H2 headings and the landing page's hero line. Carries the "Luxe" weight: confident, editorial, not corporate.
  - **Sans** (`Inter`) — everything functional: body copy, buttons, forms, nav links, tables, Jogajog messages. Inter also has full Bangla-adjacent Latin + strong readability at small sizes for data-dense admin screens.

```html
<h1 class="brand-hero">
  <span class="script">Manush</span>
  <span class="serif">— People helping people</span>
</h1>
```

```css
:root {
  --manush-primary: #8B0000;
  --manush-primary-dark: #5B0202;
  --manush-accent: #EDE7C7;
  --manush-ink: #200E01;
  --manush-bg: #FAF8F2;
  --manush-surface: #FFFFFF;
  --manush-text: #1A1210;
  --manush-text-muted: #6B5B52;
  --manush-border: #E6DFC8;
  --manush-danger: #8B0000;
  --manush-success: #1E7A4C;
  --font-display: 'Playfair Display', serif;
  --font-script: 'Pinyon Script', cursive;
  --font-body: 'Inter', sans-serif;
  --radius-sm: 8px; --radius-md: 16px; --radius-lg: 28px;
  --shadow-3d-sm: 0 2px 4px rgba(32,14,1,.08), 0 1px 2px rgba(32,14,1,.12);
  --shadow-3d-md: 0 8px 24px rgba(32,14,1,.14), 0 2px 6px rgba(32,14,1,.10);
  --shadow-3d-lg: 0 20px 48px rgba(32,14,1,.22), 0 6px 16px rgba(32,14,1,.12);
}
[data-theme="dark"] {
  --manush-bg: #200E01; --manush-surface: #2B1506;
  --manush-text: #EDE7C7; --manush-text-muted: #C9B98F;
  --manush-border: #3C2410;
}
```

### 3D effect & depth system

The "3D" feel comes from a **layered shadow + micro-transform system** applied consistently, not from heavy skeuomorphism:

```css
.card-3d {
  background: var(--manush-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-3d-md);
  transform: translateY(0);
  transition: transform .18s ease, box-shadow .18s ease;
  position: relative;
}
.card-3d::before { /* beveled top highlight, fakes a premium embossed edge */
  content: ''; position: absolute; inset: 0 0 auto 0; height: 1px; border-radius: var(--radius-md) var(--radius-md) 0 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,.5), transparent);
}
.card-3d:hover { transform: translateY(-4px); box-shadow: var(--shadow-3d-lg); }

.btn-3d {
  background: var(--manush-primary); color: var(--manush-accent);
  border-radius: var(--radius-sm); box-shadow: var(--shadow-3d-sm);
  transform: translateY(0); transition: transform .1s ease, box-shadow .1s ease;
}
.btn-3d:hover { background: var(--manush-primary-dark); box-shadow: var(--shadow-3d-md); }
.btn-3d:active { transform: translateY(2px); box-shadow: none; } /* physical "press" feedback */
```

Apply `.card-3d` to post cards, professional profile cards, and the leaderboard rows; `.btn-3d` to primary CTAs only (Ask for Help, Donate Blood, Apply) — secondary/tertiary buttons stay flat so the depth system still reads as hierarchy, not decoration everywhere.

### JS architecture (no build step, still modular)

Use native ES modules loaded with `type="module"` — gives you imports/exports without Webpack/Vite:

```text
/static/js/
 ├── core/
 │    ├── api.js          (fetch wrapper: JWT header, refresh-on-401, JSON errors → SweetAlert)
 │    ├── auth.js         (token storage, logout, route guards)
 │    ├── toast.js        (thin wrapper around SweetAlert2 Toast mixin)
 │    └── ws.js           (STOMP client singleton for Jogajog)
 ├── components/
 │    ├── post-card.js
 │    ├── help-request-form.js
 │    ├── nearby-map.js
 │    └── leaderboard-table.js
 └── pages/
      ├── home.js
      ├── ask-help.js
      └── jogajog.js
```

`api.js` is the backbone — every fetch goes through it so error handling, loading states and auth headers are consistent instead of copy-pasted per page:

```js
export async function apiFetch(path, options = {}) {
  const res = await fetch(`/api/v1${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${getAccessToken()}`, ...options.headers }
  });
  if (res.status === 401) { await tryRefresh(); return apiFetch(path, options); }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    Swal.fire({ icon: 'error', title: 'Something went wrong', text: body.message || 'Please try again.' });
    throw new Error(body.message);
  }
  return res.status === 204 ? null : res.json();
}
```

### SweetAlert2 — where it replaces native `alert`/`confirm`

| Native | Replace with |
| --- | --- |
| `alert('Saved')` | `Toast.fire({ icon: 'success', title: 'Help request posted' })` (top-corner, auto-dismiss) |
| `confirm('Delete?')` | `Swal.fire({ title: 'Delete this post?', icon: 'warning', showCancelButton: true, confirmButtonColor: 'var(--manush-danger)' })` |
| Blocking form errors | Inline field errors (never a SweetAlert per field — reserve SweetAlert for page-level outcomes) |
| Blood request emergency confirm | A styled modal with urgency color, not a generic confirm |
| Report submitted | Success toast + optimistic UI update (comment/post marked "Reported") |

Load via CDN per your own frontend-design constraints:

```html
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11.14.5/dist/sweetalert2.all.min.js"></script>
```

### Required states (every page, not just happy path)

- **Loading:** skeleton cards (CSS shimmer), never a spinner-only blank page for feeds.
- **Empty:** illustration + one-line message + a clear CTA ("No help requests near you yet — Ask for Help").
- **Error:** inline retry button, SweetAlert for anything that blocks the whole page.
- **Offline/slow network:** a thin top banner ("Reconnecting…") driven by `navigator.onLine` + WS disconnect events — important for Jogajog specifically.

### Responsive + accessibility baseline

- Mobile-first CSS, nav collapses to a bottom tab bar on small screens (Home / Find / Ask / Jogajog / Profile) — closer to how Bangladeshi users actually hold a phone than a hamburger-only menu.
- All interactive elements keyboard-reachable, visible `:focus-visible` ring using `--manush-accent`.
- Color is never the only signal (urgency badges carry both color and text: "EMERGENCY", not just red).
- `prefers-reduced-motion` respected for transitions.

This stack — vanilla JS modules + SweetAlert2 + a real token system — produces a frontend that reads as deliberately designed rather than "a professor's CRUD demo," while staying inside your stated constraint of no frontend framework.

## Database Design — MongoDB Atlas Only

One Atlas cluster. Collections are namespaced by domain prefix to keep logical separation even though everything lives in one database (or one database per domain if you prefer — both work since there's no cross-service boundary to protect).

| Domain | Collections |
| --- | --- |
| auth | `users`, `refresh_tokens`, `verification_states` |
| help | `posts`, `comments`, `reactions`, `reports`, `help_requests`, `help_matches` |
| job | `professionals`, `jobs`, `job_applications`, `ratings` |
| blood | `blood_donors`, `blood_requests`, `donation_records` |
| jogajog | `conversations`, `messages` |
| points | `points_ledger`, `badges`, `leaderboard_cache` |
| platform (replaces Redis) | `otp_codes` (TTL), `login_attempts` (TTL), `idempotency_keys` (TTL) |
| files | GridFS buckets: `fs.files` / `fs.chunks` (profile pics, post images, chat attachments, verification docs) |

### Key indexes

```js
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ phone: 1 }, { unique: true, sparse: true });
db.posts.createIndex({ createdAt: -1 });
db.posts.createIndex({ category: 1, createdAt: -1 });
db.posts.createIndex({ userId: 1 });
db.professionals.createIndex({ profession: 1, verificationStatus: 1 });
db.professionals.createIndex({ location: '2dsphere' });
db.jobs.createIndex({ status: 1, category: 1 });
db.blood_donors.createIndex({ bloodGroup: 1, available: 1 });
db.blood_donors.createIndex({ location: '2dsphere' });
db.help_requests.createIndex({ location: '2dsphere' });
db.messages.createIndex({ conversationId: 1, createdAt: -1 });

// TTL replacements for Redis
db.otp_codes.createIndex({ createdAt: 1 }, { expireAfterSeconds: 300 }); // 5 min
db.login_attempts.createIndex({ createdAt: 1 }, { expireAfterSeconds: 900 }); // 15 min
db.idempotency_keys.createIndex({ createdAt: 1 }, { expireAfterSeconds: 86400 });
```

### Location documents (privacy-aware)

```json
{
  "type": "Point",
  "coordinates": [90.4125, 23.8103],
  "area": "Uttara",
  "city": "Dhaka"
}
```

Exact coordinates are stored but **never returned verbatim** in nearby-search responses — the DTO layer rounds to \~500m jitter or returns only `area`/`distanceKm`, per Section 57's privacy rule.

### Image & file storage — Google Drive (instead of GridFS)

To keep setup to zero extra infrastructure, image/file storage uses a **shared Google Drive folder** rather than GridFS or S3:

- Create one Drive folder (e.g. `manush-uploads`), set sharing to "Anyone with the link — Viewer".
- The backend's `StorageProvider` implementation uploads the file to that folder via the **Google Drive API** (service account credentials, not a personal OAuth login) and stores only the returned **Drive file ID** + a direct-view URL on the document — never the binary itself in MongoDB.

```java
@Component
@ConditionalOnProperty(name = "manush.storage.provider", havingValue = "gdrive")
public class GoogleDriveStorageProvider implements StorageProvider {
    private final Drive driveService; // built from a service-account key (GOOGLE_SERVICE_ACCOUNT_JSON)
    private final String folderId;    // GDRIVE_FOLDER_ID

    public String store(InputStream file, String filename, String contentType) {
        File metadata = new File().setName(UUID.randomUUID() + "-" + sanitize(filename)).setParents(List.of(folderId));
        File uploaded = driveService.files().create(metadata, new InputStreamContent(contentType, file)).execute();
        return uploaded.getId(); // store THIS in Mongo, not the bytes
    }
    public String toViewUrl(String fileId) {
        return "https://drive.google.com/uc?export=view&id=" + fileId; // renders inline in <img>
    }
}
```

A post/profile document stores just:

```json
{ "imageFileId": "1aBcD3eFgH...", "imageUrl": "https://drive.google.com/uc?export=view&id=1aBcD3eFgH..." }
```

**Env vars needed:**

```text
GOOGLE_SERVICE_ACCOUNT_JSON=<base64-encoded service account key>
GDRIVE_FOLDER_ID=<the shared folder's id from its URL>
```

**Tradeoffs, stated honestly:**

- No fine-grained per-file access control — anything in the folder is viewable by link, so this is **not** suitable for NID/verification documents (those should stay out of Drive; use a private, auth-gated endpoint backed by a small `verification_documents` GridFS or Base64-in-Mongo fallback instead, since they're low-volume and sensitive).
- Fine for public-facing images: post photos, profile pictures, job/professional portfolio images — exactly the bulk of your upload volume, and it costs nothing beyond free Drive storage.
- `StorageProvider` stays the same interface either way, so swapping to S3/GridFS later only means writing a new implementation class, not touching any calling code.

### Known limitation to document, not hide

Atlas's free/shared (M0) tier has no continuous backup and a lower storage ceiling. State this explicitly in your README/docs as a known constraint of the deployment tier, not a gap in the design — upgrading to M10+ for backups is a one-line config change, not an architecture change.

## Security & Rate Limiting (Atlas-only, no Redis)

### Authentication

- Spring Security + JWT (short-lived access token \~15 min, refresh token \~30 days stored hashed in `refresh_tokens` with device metadata).
- Passwords: BCrypt, strength 12.
- Roles: `USER`, `PROFESSIONAL`, `ADMIN` via `@PreAuthorize`; `ADMIN` is never a public registration option — seeded via a one-time `CommandLineRunner` guarded by an env var, or promoted manually in Atlas.
- Resource ownership checks in the service layer (`@PostAuthorize` or explicit `if (!post.getUserId().equals(currentUserId)) throw new ForbiddenException()`), not left to the frontend.

### Rate limiting — Bucket4j, in-memory, single instance

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private Bucket resolveBucket(String key, int capacity, Duration period) {
        return buckets.computeIfAbsent(key, k ->
            Bucket.builder().addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, period))).build());
    }
}
```

| Route group | Limit | Key |
| --- | --- | --- |
| Login | 5 / minute | IP + email |
| Register | 5 / 10 minutes | IP |
| Create post | 10 / minute | userId |
| Comment | 30 / minute | userId |
| Jogajog message | 60 / minute | userId |
| Manush AI chat | 20 / minute | userId |
| Blood request | 5 / hour | userId |

All limits are externalized to `application.yml` (`manush.ratelimit.login.capacity=5`), never hardcoded, satisfying the original "must be configurable" rule without Redis.

**Why in-memory is correct here, not a shortcut:** rate limiting protects one running process from abuse. A single deliberately-run instance makes a local bucket map *more* correct than a distributed one — Redis-backed limiting only becomes necessary once you run multiple instances behind a load balancer sharing state. State this as a known scaling boundary in your docs.

### OTP & login-attempt tracking — TTL collections replace Redis keys

```java
@Document("otp_codes")
record OtpCode(@Id String id, String phone, String codeHash, Instant createdAt) {}
// TTL index on createdAt (300s) auto-expires the document — identical behavior to a Redis EX key
```

### File upload security

- Validate MIME type **and** magic bytes (not just the extension or client-sent `Content-Type`).
- Reject by extension allow-list per upload type (images: jpg/png/webp; docs: pdf).
- Max size enforced both client-side (fast feedback) and server-side (authoritative).
- Server-generated filename (`UUID + sanitized original extension`) — original filename never trusted or used for storage paths.
- Verification/NID documents: stored in GridFS, served only via an authenticated, ownership-checked endpoint, never a public URL.

### NoSQL injection & input validation

- Bean Validation (`@Valid`, `@NotBlank`, `@Pattern`) on every request DTO.
- Never build Mongo queries by string-concatenating user input; use `Criteria`/`Query` builders or repository method names so operators like `$where`/`$ne` can't be injected via a crafted JSON body.
- Centralized `@RestControllerAdvice` returns the standard error shape (Section 14) and never leaks a stack trace.

### Audit logging

A `audit_logs` collection (admin bans, verification approvals, report resolutions) capturing `who`, `what`, `when`, `resource`, `ip` — explicitly excluding passwords, tokens, and raw NID data.

## Auth & Onboarding Flow

1. **Register** — name, email or phone, password, `bloodGroup` (mandatory per Section 17), role selection limited to `USER` or `PROFESSIONAL` (never `ADMIN`).
2. **Verify** — email link or SMS OTP (6-digit, stored hashed in `otp_codes` with the 5-minute TTL index; rate-limited 5/10min).
3. **Profile completion** — area/city (from browser Geolocation, with explicit permission prompt), profile photo (GridFS), optional profession fields if `PROFESSIONAL`.
4. **Professional verification (optional at this stage)** — routed through `IdentityVerificationProvider` → `MockVerificationProvider` returns `PENDING → VERIFIED` after a simulated delay in dev; swappable for a licensed provider in prod. Status surfaces on the profile as a badge, never silently assumed.
5. **Login** — email/phone + password → access + refresh JWT pair. Rate-limited 5/minute by IP+email combination to blunt credential stuffing without needing a distributed limiter.
6. **Session refresh** — silent refresh via `/api/v1/auth/refresh`; `api.js`'s `tryRefresh()` handles this transparently so the user is never bounced to a login screen mid-session unless the refresh token itself is expired or revoked.

Admin accounts are never exposed in the public registration DTO's role enum — the DTO only accepts `USER`/`PROFESSIONAL`; `ADMIN` is set only by a seed script or direct Atlas promotion, consistent with Section 5's rule.

### Sign up / Sign in with Google (OAuth2)

Added as a second registration path alongside email/password — same `USER`/`PROFESSIONAL` role constraint applies, Google sign-in can never mint an `ADMIN` account either.

**What you need:** a Google Cloud project → **APIs & Services → Credentials → Create OAuth client ID** (type: Web application) → add your domain + `http://localhost:8080` to authorized origins, and `/login/oauth2/code/google` to authorized redirect URIs. You get:

```text
GOOGLE_OAUTH_CLIENT_ID=xxxxx.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxxxxxxxxx
```

**Spring Security config** — `spring-boot-starter-oauth2-client` handles the whole redirect dance:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_OAUTH_CLIENT_ID}
            client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET}
            scope: [openid, profile, email]
```

**Linking a Google identity to your own `User` document** (first login creates the account, later logins just log in — never a duplicate):

```java
@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String googleId = oauthUser.getAttribute("sub");

        User user = userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(new User(email, name, googleId, Role.USER)));
        // bloodGroup, area/city still collected via a "complete your profile" step post-signup — OAuth doesn't give you those

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        res.sendRedirect("/auth/callback?access=" + accessToken + "&refresh=" + refreshToken);
    }
}
```

**Frontend** — one button, no custom popup/SDK code needed since Spring Security handles the redirect:

```html
<a href="/oauth2/authorization/google" class="btn-3d btn-google">
  <img src="/static/img/google-icon.svg" alt="" /> Continue with Google
</a>
```

**Why this matters for the profile model:** a Google-signup user still lacks `bloodGroup` and `area`/`city` (Section 17 requires these) — so the post-OAuth redirect lands on a mandatory **"Complete your profile"** screen before the user can access Home, Ask Help, or Blood Bank. This is enforced server-side (`profileComplete: boolean` flag checked by a filter), not just a frontend nudge the user can skip by typing a URL directly.

## Home Feed, Ask Help, Find Help

### Post categories (unchanged from original spec)

`GENERAL_HELP · SCAM · AWARENESS · BLOOD_DONATION · EMERGENCY_HELP · JOB_OPPORTUNITY · LOST_AND_FOUND · COMMUNITY_NOTICE · FOOD_SHARING · GIVE_AWAY · OTHER`

### Ask Help — lifecycle

`REQUESTED → MATCHING → ACCEPTED → IN_PROGRESS → COMPLETED | CANCELLED | DISPUTED`

Urgency: `LOW · NORMAL · HIGH · EMERGENCY` — `EMERGENCY` posts get a distinct visual treatment (red urgency badge, pinned near top of relevant feeds) but never bypass human/admin moderation, per Section 58.

### Find Help — geospatial architecture

```text
Browser Geolocation API (user grants permission)
        ↓
   latitude + longitude
        ↓
MongoDB 2dsphere $nearSphere query
        ↓
  Nearby professionals / helpers / open requests
```

No external geocoding API call on every search — Atlas's native `2dsphere` index does the proximity math. A geocoding provider (interface-based, swappable) is called only when converting a typed address to coordinates, not on every nearby-search request.

```java
Query query = new Query(Criteria.where("location").nearSphere(point).maxDistance(radiusInRadians));
```

### Feed performance

- Denormalized counters (`reactionCount`, `commentCount`, `shareCount`) updated via atomic `$inc`, never recalculated per request.
- Cursor-based pagination for the main feed (stable under inserts, unlike offset pagination).
- Caffeine `@Cacheable` on category/location-filtered feed queries with a short TTL (e.g. 30s) to absorb repeat scroll-refreshes without hammering Atlas.

### Community safety (Section 10, unchanged)

Report statuses: `UNVERIFIED → REPORTED → UNDER_REVIEW → CORROBORATED → CONFIRMED | REJECTED`. A user accusation never auto-labels anyone; only an admin transition to `CONFIRMED` carries consequence, and that transition is audit-logged (Section 5).

## Professional System & Jobs

### Professional profile fields

`profession · skills[] · experience · serviceArea (2dsphere) · availability · workingHours · rating · completedJobs · verificationStatus (PENDING | VERIFIED | REJECTED | EXPIRED) · helpingPoints`

Example professions: Electrician, Plumber, Carpenter, Mechanic, AC Technician, Driver, Delivery Worker, Tutor, Computer Technician, Freelancer.

### Job fields & lifecycle

`title · description · category · location · salary range · skills[] · employmentType · status`

Job status: `OPEN → CLOSED | CANCELLED` Application status: `APPLIED → UNDER_REVIEW → ACCEPTED | REJECTED | WITHDRAWN`

### End-to-end flow

`Job posted → Apply → Verification check → Employer review → Accept → Jogajog (negotiate details) → Work → Completion → Rating → Helping Points`

This is the concrete instance of the platform's central idea (Section 69): a completed job isn't just a transaction, it feeds the same reputation ledger as a free help request or a blood donation.

### Verification gate

A `PROFESSIONAL` can create a profile and even receive informal help requests while `verificationStatus = PENDING`, but job applications to paid/formal postings require `VERIFIED` — enforced in `JobApplicationService`, not just hidden in the UI, so the rule can't be bypassed via direct API calls.

## Blood Bank

Every user registers with a `bloodGroup` ∈ `{A+, A-, B+, B-, AB+, AB-, O+, O-}` — mandatory at signup, not optional, since it's the seed data the whole matching system depends on.

### Donor profile

`bloodGroup · available (boolean) · location (2dsphere) · lastDonationDate · donationStatus`

### Blood request fields

`bloodGroup · hospital/location · requiredUnits · urgency (NORMAL | URGENT | EMERGENCY) · requiredDate · description`

### Request lifecycle

`REQUESTED → MATCHING → DONOR_CONTACTED → ACCEPTED → COMPLETED | CANCELLED`

### Matching query

```java
Query query = new Query(Criteria.where("bloodGroup").is(requiredGroup)
    .and("available").is(true)
    .and("location").nearSphere(hospitalPoint).maxDistance(radiusMeters));
```

Results expose only `area`, `distanceKm`, `bloodGroup`, `availability`, and `helpingPoints` — never exact coordinates or a home address, satisfying Section 18's privacy rule.

### Notification on match

Nearby eligible donors are notified (in-app + email/SMS via the provider interfaces) when a compatible request is created within their radius; donors are never auto-committed — they opt in through an explicit accept action, which transitions the request to `DONOR_CONTACTED`.

## Jogajog (Messaging)

Single-instance STOMP over WebSocket — Spring's built-in **simple broker** holds every session in-process. This is correct for one running instance; it only becomes a limitation if you later scale to 2+ instances (at which point messages wouldn't fan out across processes), which is a documented future step, not a current gap.

```text
User A → WebSocket (STOMP) → Spring SimpleBroker (in-process) → WebSocket (STOMP) → User B
```

### Features

- One-to-one chat, conversation list, cursor-paginated message history.
- Delivered/read receipts stored per-message, pushed over the same socket.
- Unread counts maintained as a denormalized counter on the conversation document (same pattern as feed counters — avoid recomputation).
- Online/offline presence tracked via WebSocket connect/disconnect events, held in a `ConcurrentHashMap<userId, sessionCount>` in the `jogajog` package (in-process, same scaling note as rate limiting).
- Block/report a conversation reuses the same moderation primitives as post reports (Section 10).

### Frontend integration

`core/ws.js` holds a single STOMP client; `pages/jogajog.js` subscribes to `/user/queue/messages` and renders incoming messages live, falling back to a "Reconnecting…" banner (Section 3) on disconnect rather than failing silently.

## Helping Points, Badges, Leaderboard

### Award flow (never self-awarded)

`Help completed → Requester confirms completion → Rating submitted → PointsService.award() → points_ledger entry → User.helpingPoints incremented`

Points are written to an **append-only `points_ledger`** collection (event sourcing style) with `userId, reason, points, sourceId, createdAt`; the user's cached `helpingPoints` total is derived from it, never edited directly — this gives you a free audit trail and makes anti-gaming review possible.

### Point-earning events (example weights)

| Event | Points |
| --- | --- |
| Completed help request (requester-confirmed) | 20 |
| Blood donation (admin/hospital-confirmed) | 50 |
| Successful professional service (rated ≥4★) | 15 |
| Verified useful community report | 10 |
| Job completed (employer-confirmed) | 25 |

### Anti-gaming rules

- A rating/confirmation can only come from the **other party**, never the point-earner themselves (`if (confirmerId.equals(helperId)) reject`).
- Idempotency key per completion event (`helpRequestId` or `jobId`) prevents duplicate point awards from retried requests.
- Daily/weekly point caps per category to blunt farming (e.g. max 5 "completed help" awards counted per day toward the leaderboard, configurable).
- Suspicious-pattern flags (same two users repeatedly completing trivial helps) surface in the admin dashboard for manual review — never auto-banned.

### Badges

`First Helper · Community Helper · Blood Hero · Trusted Professional · Top Contributor · Emergency Helper` — awarded by a scheduled job evaluating ledger totals/thresholds, not computed live on every page load.

### Leaderboard

Categories × periods: `{Top Helpers, Top Blood Donors, Top Professionals, Community Contributors} × {Weekly, Monthly, All-Time}`. Computed via a scheduled Mongo aggregation into `leaderboard_cache`, refreshed on an interval (e.g. every 10 minutes) rather than aggregated on every request — the read path is just a cache lookup.

Helping points measure **contribution**, explicitly not trustworthiness or professional competence (Section 21) — this distinction is stated in the UI copy itself (e.g. a tooltip on the points badge), not left implicit.

## Manush AI

Lives as the `ai` package (not a separate service), using Spring AI against an `AiProvider` interface so the underlying model vendor can change without touching callers.

### Core principle

> **AI interprets requests; deterministic backend logic performs the actual matching, authorization, and final decisions.**

### Use case: natural-language help request (Bangla/Banglish support)

```text
User: "Bhai amar Uttara te ajke ekjon plumber lagbe."
        ↓ AiProvider.extract(prompt)
{ "category": "PLUMBER", "location": "Uttara", "urgency": "NORMAL", "date": "TODAY" }
        ↓
HelpService performs the actual 2dsphere nearby search — AI never touches the database directly
```

### Other use cases

- Helping-point recommendations ("You have 850 points, what can I do?" → suggests nearby opportunities, never fabricated ones — grounded in actual open requests the backend fetches first).
- Help request classification into the fixed category enum.
- Job description generation / post summarization.
- Spam and scam-risk *assistance* — flags for human review, never an automatic takedown.
- Duplicate post detection to reduce feed clutter.

### Hard boundaries (enforced in code, not just policy)

Manush AI must **never** independently:

- Decide criminal guilt or scam-accusation outcomes
- Make medical emergency determinations
- Decide trustworthiness or finalize hiring
- Auto-ban a user
- Make identity-verification decisions

Every AI output that could affect another user's standing routes through the same admin moderation workflow as a human-submitted report — the AI's output is treated as a *suggestion* object, never a state transition by itself.

### Rate limiting & cost control

AI endpoints are rate-limited (20/min/user, Section 5) and usage is logged per user to bound both cost and abuse, since every call incurs a real provider cost unlike the rest of the stack.

## Admin Dashboard

### Metrics tiles

`Total Users · Active Users · Professionals · Verified Professionals · Help Requests · Completed Helps · Blood Requests · Active Blood Donors · Jobs · Reports · Pending Verifications`

All computed from scheduled aggregations cached in a small `admin_metrics_cache` document (same pattern as the leaderboard cache) — the dashboard reads a cache, it does not scan full collections on every page load.

### Charts

Users over time · help requests · completed helps · blood requests · job activity · reports — rendered client-side from a lightweight `/api/v1/admin/metrics/timeseries` endpoint.

### Moderation workflow

`Report filed → UNVERIFIED/REPORTED → admin opens report → UNDER_REVIEW → evidence gathered → CORROBORATED or REJECTED → (if corroborated) action taken → CONFIRMED, audit-logged`

Admin actions always go through the audit log (Section 5): who banned/verified/rejected what, when, from which IP. Reporter identity is never exposed to the reported party, consistent with Section 10.

## API Design & Error Handling

### Layering (unchanged principle)

`Request DTO → Controller → Service → Repository → Document` and back out as `Document → Service → Response DTO → Controller`. MongoDB documents are never returned directly from a controller. Java `record`s for DTOs; Lombok for entities/services where it removes boilerplate without hiding logic.

### Representative endpoints

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | rate-limited 5/10min |
| POST | `/api/v1/auth/login` | rate-limited 5/min |
| POST | `/api/v1/auth/refresh` |  |
| GET/PUT | `/api/v1/users/me` |  |
| POST | `/api/v1/help` | create help request |
| GET | `/api/v1/help/nearby` | 2dsphere query |
| POST | `/api/v1/help/{id}/accept` \| `/complete` | lifecycle transitions |
| POST | `/api/v1/jobs` \| GET `/api/v1/jobs` |  |
| POST | `/api/v1/jobs/{id}/apply` | requires `VERIFIED` for formal postings |
| GET | `/api/v1/blood/donors/nearby` |  |
| POST | `/api/v1/blood/requests` | rate-limited 5/hour |
| GET | `/api/v1/conversations` \| `/api/v1/messages` | paginated |
| POST | `/api/v1/ai/chat` \| `/api/v1/ai/help/analyze` | rate-limited 20/min |

All list endpoints paginate (page/size or cursor); all mutating endpoints validate with `@Valid` and return the standard error shape on failure.

### Standard error response

```json
{
  "timestamp": "2026-10-12T09:30:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Help request not found",
  "path": "/api/v1/help/123"
}
```

Produced centrally by one `@RestControllerAdvice` for the whole application (not per-package) — it never leaks a stack trace, and maps domain exceptions (`ResourceNotFoundException`, `ForbiddenException`, `ValidationException`, `RateLimitExceededException`) to the right HTTP status consistently across every module.

### Documentation

springdoc-openapi auto-generates Swagger UI at `/swagger-ui.html` from the same DTOs and controller annotations — no hand-maintained API doc that drifts from the code.

## Packaging & Deployment

### WAR packaging

```xml
<packaging>war</packaging>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-tomcat</artifactId>
  <scope>provided</scope>
</dependency>
```

```java
@SpringBootApplication
public class ManushApplication extends SpringBootServletInitializer {
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(ManushApplication.class);
  }
  public static void main(String[] args) { SpringApplication.run(ManushApplication.class, args); }
}
```

This single artifact runs two ways with zero code changes: standalone with the embedded Tomcat (local dev, Render), or dropped into an external Tomcat 10+ (Jakarta EE namespace — matches Spring Boot 4) if a university server requires traditional WAR deployment.

### Docker — one service, not nine

Because Atlas is remote, there is **no local MongoDB container** and **no Redis container**:

```yaml
# docker-compose.yml
services:
  app:
    build: .
    ports: ["8080:8080"]
    env_file: .env
```

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre
COPY --from=build /app/target/manush.war /app/app.war
ENTRYPOINT ["java", "-jar", "/app/app.war"]
```

### Environment variables (never hardcoded)

```text
MONGODB_URI=mongodb+srv://...
JWT_SECRET=...
AI_API_KEY=...
EMAIL_API_KEY=...
SMS_API_KEY=...
```

`.env.example` ships in the repo with placeholder values only; real secrets live in `.env` locally (git-ignored) and as platform environment variables on Render in production.

### Deployment pipeline

```text
GitHub push → GitHub Actions (mvn verify) → Render build → Render deploy
                                                   ↓
                                            Cloudflare (DNS, WAF, CDN) in front
                                                   ↓
                                            MongoDB Atlas (remote, managed)
```

### CI (minimal, still real)

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '25', distribution: 'temurin' }
      - run: ./mvnw clean verify
```

### Observability

Spring Boot Actuator's `/actuator/health` auto-reports Mongo connectivity via `MongoHealthIndicator` — a real liveness probe to show a professor or a hosting platform, not a hand-rolled stub.

## Development Phases (Solo Developer)

1. **Foundation** — WAR-packaged project skeleton, Atlas connection, JWT auth, roles, Bucket4j filter skeleton, base error handling.
2. **Landing page & feed** — Thymeleaf landing page, home feed, posts/comments/reactions, reports.
3. **Help Service** — Ask Help, Find Help, 2dsphere nearby search, help lifecycle.
4. **Professionals & Jobs** — profile system, job postings, applications, ratings.
5. **Blood Bank** — donor profiles, requests, 2dsphere matching.
6. **Jogajog** — WebSocket/STOMP messaging, conversation list, read receipts.
7. **Verification** — `MockVerificationProvider`, admin moderation queue.
8. **Helping Points** — ledger, badges, leaderboard aggregation job.
9. **Manush AI** — Spring AI integration, Bangla/Banglish extraction, recommendations.
10. **Hardening** — Caffeine caching, finalize all rate limits, GridFS file-serving security review.
11. **Deployment** — Docker image, GitHub Actions CI, Render deploy, Cloudflare DNS/WAF, Actuator health checks live.
12. **Optional stretch** — Kafka/RabbitMQ event bus, distributed tracing, Elasticsearch search, advanced analytics — only if time and real usage justify it; never added just to pad the architecture diagram.

### MVP cut line

If time runs short, ship with: Landing Page, Auth, Profile, Home Feed, Ask/Find Help, Professional Profiles, Jobs, Blood Bank, Jogajog, Helping Points, Basic Admin Panel. Defer: Manush AI, advanced verification, advanced analytics — all addable later without rearchitecting, since they're packages, not services.

### Guiding priority order (unchanged from the original spec)

`Correctness → Security → Maintainability → Performance → Scalability`

Never trade a working core product for infrastructure nobody is using yet.

## Fuzzy Search — MongoDB Atlas Search

Implemented now (payment/escrow deferred). Atlas Search is built into your existing cluster — no new service, no new credentials beyond `MONGODB_URI` you already have.

### What it covers

- **Find Help / professional search** — "elctrcian near Uttara" still matches "Electrician".
- **Jobs search** — typo-tolerant title/skill matching.
- **Post search** — matching across post titles/descriptions.

### Setup (one-time, per collection)

In Atlas UI → **Search** tab → Create Search Index on `professionals`, `jobs`, and `posts`:

```json
{
  "mappings": {
    "dynamic": false,
    "fields": {
      "profession": { "type": "string", "analyzer": "lucene.standard" },
      "skills": { "type": "string", "analyzer": "lucene.standard" },
      "title": { "type": "string", "analyzer": "lucene.standard" },
      "description": { "type": "string", "analyzer": "lucene.standard" }
    }
  }
}
```

### Query — fuzzy match via aggregation

```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.stage("""
    {
      "$search": {
        "index": "default",
        "text": {
          "query": "%s",
          "path": ["profession", "skills"],
          "fuzzy": { "maxEdits": 2, "prefixLength": 1 }
        }
      }
    }
    """.formatted(searchTerm)),
    Aggregation.limit(20)
);
List<Professional> results = mongoTemplate.aggregate(agg, "professionals", Professional.class).getMappedResults();
```

`fuzzy.maxEdits: 2` tolerates up to 2 character typos/transpositions — enough for real-world typing on a phone keyboard without false-positiving on genuinely different words.

### Where it lives

A `SearchService` in the `common` package (used by both `help` and `job` domains, since both need the same fuzzy-match behavior) — keeps the Atlas Search query logic in one place rather than duplicated per controller.

### Endpoint

```text
GET /api/v1/search?q=elctrcian&type=professional
GET /api/v1/search?q=plumbr&type=job
```

Rate-limited the same as other read-heavy endpoints (reuse the existing Bucket4j filter, e.g. 30/min) since Atlas Search queries cost more than a simple indexed lookup.

## Performance & Optimization Checklist

A single-instance Atlas-only app still needs real performance discipline — these apply even without Redis/CDN infra being mandatory on day one.

### Caching & data fetching

| Item | How, in this stack |
| --- | --- |
| Cache API responses | Caffeine `@Cacheable` on read-heavy, slow-changing endpoints (leaderboard, admin metrics, category lists) |
| Load balancer | Not needed at single-instance; Render's own edge handles TLS/health checks — revisit only at multi-instance scale |
| Cache expensive queries | Materialize aggregations into a cache collection (`leaderboard_cache`, `admin_metrics_cache`, Section 11/13) refreshed on a schedule, never recomputed per request |
| Add CDN | Cloudflare (already in the architecture, Section 1) caches static assets (CSS/JS/fonts/images) at the edge automatically |
| Server-side caching | Thymeleaf fragment caching for rarely-changing partials (nav, footer) via Spring's `CacheManager` |

### Frontend rendering & assets

| Item | How |
| --- | --- |
| Compress images | Serve WebP where supported, compress on upload before sending to Google Drive storage (Section 4) |
| Loading skeletons | CSS shimmer skeletons for feed/card lists (Section 3) — never a blank page while fetching |
| Split code into chunks | Per-page vanilla JS modules (Section 3's `pages/*.js`) load only what that page needs — natural code-splitting without a bundler |
| Lighthouse audit | Run before each major release; target 90+ on Performance/Accessibility/Best Practices; track regressions in CI as a manual gate |
| Minify JS and CSS | A simple Maven/npm script step (esbuild or a Maven minify plugin) in the build, output to `/static` |
| Add lazy loading | `loading="lazy"` on all feed/profile images; `IntersectionObserver` for infinite-scroll feed pagination |
| Defer non-critical scripts | SweetAlert2 and any analytics script loaded with `defer`; only `core/api.js` and `core/auth.js` block render |
| Unnecessary re-renders (anti-pattern) | N/A in the React sense, but the vanilla-JS equivalent: avoid re-querying/re-building the whole feed DOM on every WebSocket event — patch only the changed card/message node |

### Network & payloads

| Item | How |
| --- | --- |
| Debounce input handlers | Search-as-you-type (Find Help, Jobs) debounced \~300ms before firing the Atlas Search request — avoids hammering the fuzzy-search endpoint on every keystroke |
| Compress API payloads | Enable `server.compression.enabled=true` in Spring Boot (gzip) for JSON responses over a size threshold |

### Dependencies & build hygiene

| Item | How |
| --- | --- |
| Unused dependencies (anti-pattern) | Run `mvn dependency:analyze` periodically; remove anything flagged unused — keeps the WAR lean and the attack surface small |

These are implementation habits, not new architecture — they slot into the monolith and Atlas-only design already specified without adding a single new service or credential.

## Real-Time Nearby Feed Suggestions (Spring AI)

Geo filtering narrows candidates first (cheap, exact); Spring AI ranks relevance second; delivery reuses the Jogajog WebSocket layer — no new infrastructure.

```text
New help post saved
        ↓
2dsphere query → users within radius (candidate set, fast)
        ↓
Spring AI: embed the post + rank candidates by relevance
        ↓
Push to online users via WebSocket · offline users see it ranked in their next feed load
```

### Step 1 — geo narrows candidates (AI never runs against the whole user base)

```java
@EventListener
@Async
public void onHelpPostCreated(HelpPostCreatedEvent event) {
    HelpRequest post = event.getPost();
    Query nearby = new Query(Criteria.where("location").nearSphere(post.getLocation()).maxDistance(5000));
    List<User> candidates = mongoTemplate.find(nearby, User.class);
    feedRankingService.rankAndDeliver(post, candidates);
}
```

### Step 2 — relevance ranking

Cheap default: rule-based scoring (skill match + profession match + distance decay) — zero added cost, good enough for most cases. Smarter option: Atlas **Vector Search** + Spring AI `EmbeddingModel` — embed each post once on creation and each user's interest profile periodically (not per request), then combine geo + vector similarity in one aggregation:

```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.stage("{ \"$geoNear\": { \"near\": %s, \"distanceField\": \"dist\", \"maxDistance\": 5000, \"spherical\": true } }".formatted(post.getLocation().toJson())),
    Aggregation.stage("{ \"$vectorSearch\": { \"index\": \"user_interest_vector_index\", \"path\": \"interestVector\", \"queryVector\": %s, \"numCandidates\": 50, \"limit\": 10 } }".formatted(Arrays.toString(postVector)))
);
```

This ranks "near AND relevant" above "near but irrelevant" — a plumber 4km away outranks a random user 1km away for a plumbing request.

### Step 3 — delivery via the existing Jogajog WebSocket

```java
public void rankAndDeliver(HelpRequest post, List<ScoredUser> ranked) {
    for (ScoredUser su : ranked) {
        if (presenceService.isOnline(su.getUserId())) {
            messagingTemplate.convertAndSendToUser(su.getUserId(), "/queue/feed-suggestions",
                new FeedSuggestionDto(post.getId(), post.getTitle(), post.getCategory(), su.getScore()));
        } // offline users see it ranked higher next time they load /api/v1/help/nearby
    }
}
```

```js
// pages/home.js — same STOMP client as Jogajog (core/ws.js)
stompClient.subscribe('/user/queue/feed-suggestions', (msg) => {
  feedStore.prependLive(JSON.parse(msg.body)); // slides into the feed with a subtle "New near you" animation
});
```

### Why this shape, not an LLM call per request

- Geo filter runs first, so AI only ever scores an already-small nearby set — never your whole user base.
- Embeddings are computed once per post/profile, not per feed load — Atlas Vector Search does the expensive matching, keeping this inside the existing 20/min AI rate limit (Section 5) instead of needing a far larger budget.
- Reuses infrastructure already built for Jogajog (WebSocket, presence tracking) rather than adding a second real-time channel.

## Navigation

### Landing page navbar (public, before login)

```text
[script-logo] Manush          How it works   Blood Bank   Professionals   Manush AI          [Sign In]  [Join MANUSH]
```

- Logo wordmark uses `--font-script` for "Manush" (Section 3) — the one deliberate script-font moment in the whole chrome.
- Nav links use `--font-body` (Inter), never serif/script — UI chrome stays functional, not decorative.
- `Sign In` renders as a text link; `Join MANUSH` renders as `.btn-3d` (Section 3's elevated button) — visually signals the primary action.
- Sticky on scroll, background fades from transparent (over the hero) to `--manush-surface` with `--shadow-3d-sm` once scrolled, a common "premium site" cue.
- Mobile: logo + a single hamburger collapsing into a full-screen overlay menu (not a dropdown) — given the nav holds marketing links + two CTAs, a dropdown reads cramped at 360px width.

### Authenticated navbar

```text
[script-logo] Manush   Home   Find Help   Ask Help   Jobs   Blood Bank   Community   Leaderboard   Manush AI   Jogajog 🔔   [avatar ▾]
```

- `Jogajog` carries an unread-count badge (Section 10's denormalized unread counter); a bell icon next to it surfaces general notifications separately from messages — keep these two visually distinct, since conflating "someone messaged you" with "your post got a comment" is a common source of missed messages.
- `[avatar ▾]` opens a dropdown: Profile, Helping Points & Badges, Settings, (Professional-only: My Services), Log out.
- **Mobile:** collapses to a **fixed bottom tab bar** (Section 3), not a hamburger — 5 slots: `Home · Find · Ask · Jogajog · Profile`. The remaining items (Jobs, Blood Bank, Community, Leaderboard, Manush AI) live inside a "More" sheet reachable from the Profile tab or a 6th "More" icon — this matches how people actually hold and thumb-reach a phone, rather than forcing every top-level item into a cramped hamburger menu.
- `Ask Help` gets slight visual emphasis (accent-colored icon or a small dot) in both desktop and mobile nav — it's the platform's core action, and burying it at the same visual weight as `Community` undersells the product's main value prop.

### Admin navbar (ADMIN role only)

Replaces the standard authenticated nav entirely once logged in as `ADMIN` (admins don't need the consumer feed nav):

```text
[script-logo] Manush Admin     Dashboard   Users   Professionals   Verification   Reports   Moderation   Analytics   Audit Logs          [avatar ▾]
```

Rendered as a left sidebar on desktop (admin dashboards are data-dense and benefit from a persistent vertical nav + more horizontal room for tables/charts), collapsing to a top bar + hamburger on mobile/tablet — admins are overwhelmingly desktop users, so this is the one nav that doesn't need a bottom-tab mobile treatment.

### Route guarding (not just hiding links)

Both navs are a UI convenience, never the actual security boundary — every admin route is also protected server-side via `@PreAuthorize("hasRole('ADMIN')")`, and the frontend route guard in `core/auth.js` redirects unauthenticated/under-privileged users before a page even attempts its first `apiFetch()` call, avoiding a flash of content the user isn't allowed to see.
