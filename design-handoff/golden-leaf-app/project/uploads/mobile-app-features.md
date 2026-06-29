# TBZ Field App - Android Kotlin Feature and Form Field Inventory

Source-backed inventory of every current mobile screen, feature, and user-entered form field in the app.
Use this as the functional reference when rebuilding the app as a native Android application in Android Studio using Kotlin.

Last audited: Django TRMCS backend (`apps/*`, `config/urls.py`) — June 2026.
Legacy mobile source: React Native field app (`app/**/*.tsx`, `components/PortalLoginForm.tsx`).

Each feature section includes a **Backend status (TRMCS)** block mapping screens to the staff web portal and `/api/v1/` REST API.

---

## TRMCS Backend Reference (Web + API)

All mobile features map to the Django staff portal (session auth) and `/api/v1/` REST API (JWT).
Mobile-specific helpers live under `/api/v1/mobile/`.

| Concern | Web (staff portal) | Mobile/API |
|---|---|---|
| Auth | `/accounts/login/`, `/accounts/forgot-password/`, `/accounts/settings/2fa/*` | `POST /api/v1/auth/login/` (+ optional `otp`), `POST /api/v1/auth/token/refresh/`, `POST /api/v1/auth/logout/` |
| Profile/RBAC | `/accounts/profile/` | `GET /api/v1/mobile/me/` (roles + permission flags + module gates) |
| Dashboard KPIs | Module dashboards | `GET /api/v1/mobile/dashboard/` |
| Reference data | Form dropdowns | `GET /api/v1/mobile/reference/` (or granular `/reference/*`) |
| Offline sync | N/A (server-side) | `POST /api/v1/mobile/sync/bulk/`, `GET /api/v1/mobile/sync/status/` |
| Notifications | In-app inbox (web) | `GET /api/v1/notifications/my/` (read-only list) |
| Password reset | `/accounts/forgot-password/` | **Web only** — no JWT API |
| 2FA setup | `/accounts/settings/2fa/*` | **Web only** — login OTP via `POST /api/v1/auth/login/` |
| Change password | `/accounts/change-password/` | `POST /api/v1/auth/users/change-password/` (min 10 chars) |

**TBZ role codenames** (from `UserRole`): `SUPERADMIN`, `ADMIN`, `INSPECTOR`, `RTI`, `DATA_CLERK`, `ARBITRATOR`, `READ_ONLY`, `LICENSING_OFFICER`, `FINANCE`, `CFO`, `AUDITOR`, plus portal roles (`BUYER_USER`, `GROWER_USER`, etc.).

Map mobile personas to backend capabilities (prefer permission flags from `/api/v1/mobile/me/` over hard-coded role names):

**Mobile app login is restricted by role.** JWT login and all `/api/v1/mobile/*` endpoints allow only operational TBZ staff roles: `SUPERADMIN`, `ADMIN`, `INSPECTOR`, `RTI`, `DATA_CLERK`, `ARBITRATOR`, `LICENSING_OFFICER`, `AUDITOR`. The following cannot use the mobile field app: external portal roles (`BUYER_USER`, `GROWER_USER`, `GRADER_USER`, `SALESFLOOR_USER`), `READ_ONLY`, `FINANCE`, and `CFO`. Web portal session login is unchanged for those accounts.

| Mobile persona | Backend mapping |
|---|---|
| Registration Officer | Staff with `growers.create_grower`, `growers.update_grower` |
| Inspector | `INSPECTOR` / `RTI` with `validation.create_validation` |
| Marketing Officer | `DATA_CLERK` with `bale.create_bale` |
| Permit Officer | Staff with `permits.create_permit` |
| Approver/Reviewer | Staff with `growers.approve_grower`, `permits.approve_permit`, `validation.approve_validation` |
| Arbitrator | `ARBITRATOR` with `arbitration.create_arbitration` |

**Delta sync:** List endpoints accept `?updated_after=<ISO-8601>` on growers, inspections, validations, permits, group permits, and several inspectorate resources.

---

## Build Target and Product Rules

The mobile app must be rebuilt as an Android Studio project written in Kotlin. The recommended implementation stack is Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, WorkManager, Retrofit/OkHttp, Kotlin Serialization or Moshi, CameraX/ML Kit barcode scanning, Fused Location Provider, and Android Keystore-backed secure token storage.

This app is offline-first. Every operational form must save locally first, work without network connectivity, show a clear pending/synced/failed status, and sync in the background or manually when the device is online. Network availability must improve the experience, but it must not be required for field capture, inspections, permit lookup from cache, pending sales, permit request drafts, or local review of cached reference data.

The app must support offline mode and sync:
- Local writes are committed to Room before any upload attempt.
- Sync runs through WorkManager with constraints for connectivity, battery, retry/backoff, and optional Wi-Fi-only mode.
- Users can trigger manual sync per record and for all pending records.
- Sync must be idempotent using stable local IDs and idempotency keys so retries do not create duplicates.
- Failed records must remain editable or discardable, with human-readable error advice.
- Conflict records must be marked `needs_review` and shown in their queue until corrected, retried, or discarded.
- Authentication/session failures must not delete offline work; they must pause upload and route the user to login.

The app must support RBAC (role-based access control):
- Read roles, permissions, groups, or capability flags from the authenticated portal profile/token response when available.
- Cache the signed-in user's role profile locally so navigation and form gating still work offline.
- Enforce RBAC in the UI and before sync submission. Hidden or disabled actions must include registration approval, permit approval/rejection, group permit approval/rejection, inspection scheduling, validation submission, sales capture, arbitration, sync settings, and admin/security actions according to role.
- Always treat server authorization as final. If offline role cache allows an action but sync returns 401/403, mark the item failed or `needs_review` with an authorization message.
- Cache roles from JWT (`roles` claim) and permission flags from `GET /api/v1/mobile/me/` (`permissions`, `modules`). Use backend codenames above; UI labels like "Registration Officer" are display-only mappings.

## Route Map

### Root and Utility Routes

| Route | Screen | Notes |
|---|---|---|
| `/onboarding` | Onboarding | First-run animated intro; final scene embeds portal login. |
| `/portal-login` | Portal Login | Standalone login screen. |
| `/forgot-password` | Forgot Password | Password reset email request. |
| `/login-2fa` | Two-Factor Login | Login-time OTP verification. |
| `/` | Home Dashboard | Default tab route, implemented at `app/(tabs)/index.tsx`. |
| `/search` | Global Search | Full search screen. |
| `/explore` | Search Tab Alias | Re-exports `/search`. |
| `/menu` | More Options Modal | Utility modal. |
| `/notifications` | Notifications | Notification inbox and detail bottom sheet. |
| `/profile` | Profile | Account, theme, security, information, logout. |
| `/change-password` | Change Password | Password update form. |
| `/two-factor-auth` | Two-Factor Auth Settings | Enable/disable authenticator app. |
| `/about` | About App | Static information page. |
| `/terms` | Terms & Conditions | Static legal page. |
| `/privacy` | Privacy Policy | Static legal page. |
| `/guidelines` | TBZ Guidelines | Static regulatory guidance page. |
| `/sync-settings` | Sync Settings | Offline sync status, preferences, and manual sync. |
| `/modal` | Demo Modal | Template modal with link back home. |

### Module Routes

| Module | Routes |
|---|---|
| Registration | `/registration`, `/registration/new`, `/registration/crop`, `/registration/growers-list`, `/registration/grower-details`, `/registration/grower-edit`, `/registration/grower-correction`, `/corrections`, `/grower-updates`, `/registration/crop-allocation`, `/local-registrations` |
| Inspection | `/inspection`, `/inspection/schedule`, `/inspection/schedules-local`, `/inspection/detail`, `/inspection/lookup`, `/inspection/portal-list`, `/inspection/high-risk`, `/inspection/reports`, `/inspection/field`, `/inspection/nursery`, `/inspection/curing`, `/validation` |
| Marketing/Sales | `/marketing`, `/sales`, `/pending-sales`, `/edit-pending-sale` |
| Permits | `/permits`, `/permit-request`, `/permit-validate`, `/permit-list`, `/permit-detail`, `/permit-correction`, `/permit-group`, `/permit-group-create`, `/permit-group-status`, `/permit-group-detail`, `/permit-group-correction` |
| Other Operations | `/arbitration`, `/renewal` |

---

## Color Theme (TBZ Field Brand)

The app uses a **Golden Leaf / TBZ field brand** palette defined in OKLCH. Token names describe the **actual color** (warm ivory, leaf green, harvest gold, etc.), not loose metaphors. These seven values are the single source of truth — do not substitute legacy hex values (`#0B6B3A`, `#C9A227`, etc.).

### Brand palette

| Token | OKLCH (exact) | sRGB (Compose) | What it is | UI role |
|---|---|---|---|---|
| **Warm Ivory** | `oklch(0.965 0.018 98)` | `#F6F4E6` | Very light warm off-white | Light backgrounds, cards, text on dark greens |
| **Warm Beige** | `oklch(0.93 0.028 88)` | `#F0E7D3` | Light tan / wheat surface | Secondary surfaces, dividers, input fills, disabled states |
| **Golden Brown** | `oklch(0.487 0.098 78)` | `#7E5709` | Mid amber-brown (hue ~78°, not green) | Secondary buttons, icons, captions, sync-pending chips |
| **Leaf Green** | `oklch(0.36 0.080 138)` | `#26471A` | TBZ primary dark green | Primary buttons, app bar, active nav, links |
| **Deep Leaf Green** | `oklch(0.28 0.08 145)` | `#06320C` | Very dark green | Dark surfaces, splash, light-mode status bar, pressed primary |
| **Harvest Gold** | `oklch(0.85 0.16 80)` | `#FFC039` | Bright golden yellow | Accent — FAB, badges, success emphasis, CTAs on dark |
| **Warm Charcoal** | `oklch(0.18 0.020 88)` | `#151107` | Warm near-black | Body text on light surfaces, dark theme background |

CSS custom properties (for web parity and design handoff):

```css
:root {
  --warm-ivory: oklch(0.965 0.018 98);
  --warm-beige: oklch(0.93 0.028 88);
  --golden-brown: oklch(0.487 0.098 78);
  --leaf-green: oklch(0.36 0.080 138);
  --deep-leaf-green: oklch(0.28 0.08 145);
  --harvest-gold: oklch(0.85 0.16 80);
  --warm-charcoal: oklch(0.18 0.020 88);
}
```

### Material 3 semantic mapping

Map brand tokens to Material 3 color roles in `Theme.kt`. Light and dark schemes must both derive from the palette above — no ad-hoc grays or greens outside these tokens (tint with opacity only when needed).

#### Light theme

| Material 3 role | Token | Notes |
|---|---|---|
| `background` | Warm Ivory | Screen background |
| `surface` | Warm Ivory | Cards, sheets |
| `surfaceVariant` | Warm Beige | Grouped list rows, search bar fill |
| `primary` | Leaf Green | Filled buttons, selected tab, key actions |
| `onPrimary` | Warm Ivory | Text/icons on Leaf Green |
| `primaryContainer` | Warm Beige | Selected list item background |
| `onPrimaryContainer` | Warm Charcoal | Text on Warm Beige containers |
| `secondary` | Golden Brown | Secondary buttons, filter chips |
| `onSecondary` | Warm Ivory | Text on Golden Brown |
| `secondaryContainer` | Warm Beige | Secondary chip background |
| `onSecondaryContainer` | Warm Charcoal | |
| `tertiary` | Harvest Gold | Accent highlights, notification dot |
| `onTertiary` | Warm Charcoal | Text on Harvest Gold |
| `tertiaryContainer` | Harvest Gold @ 40% on Warm Beige | Soft highlight panels |
| `onTertiaryContainer` | Warm Charcoal | |
| `error` | Material default red | Keep M3 error red; do not reuse Leaf Green for errors |
| `onBackground` | Warm Charcoal | Body text |
| `onSurface` | Warm Charcoal | |
| `onSurfaceVariant` | Golden Brown | Captions, hints, metadata |
| `outline` | Golden Brown @ 50% | Borders, text field outlines |
| `outlineVariant` | Warm Beige | Subtle dividers |
| `inverseSurface` | Deep Leaf Green | Snackbars, inverse surfaces |
| `inverseOnSurface` | Warm Ivory | |
| `inversePrimary` | Harvest Gold | Links on inverse surfaces |

Status bar (light): **Deep Leaf Green** background, **Warm Ivory** icons.

#### Dark theme

| Material 3 role | Token | Notes |
|---|---|---|
| `background` | Warm Charcoal | Screen background |
| `surface` | Deep Leaf Green | Cards, sheets |
| `surfaceVariant` | Leaf Green | Elevated groups, input fills |
| `primary` | Harvest Gold | Primary actions pop on dark base |
| `onPrimary` | Warm Charcoal | |
| `primaryContainer` | Leaf Green | Selected states |
| `onPrimaryContainer` | Warm Ivory | |
| `secondary` | Golden Brown | Secondary actions |
| `onSecondary` | Warm Ivory | |
| `secondaryContainer` | Deep Leaf Green | |
| `onSecondaryContainer` | Warm Beige | |
| `tertiary` | Harvest Gold | Same accent role as light |
| `onTertiary` | Warm Charcoal | |
| `onBackground` | Warm Ivory | Body text |
| `onSurface` | Warm Ivory | |
| `onSurfaceVariant` | Warm Beige | Captions, hints |
| `outline` | Golden Brown | Borders |
| `outlineVariant` | Leaf Green | Subtle dividers |
| `inverseSurface` | Warm Ivory | |
| `inverseOnSurface` | Warm Charcoal | |
| `inversePrimary` | Leaf Green | |

Status bar (dark): **Warm Charcoal** background, **Warm Ivory** icons.

### Sync and status chips

Use brand tokens for offline/sync affordances (section 9.0.6 item 9):

| Status | Background | Foreground | Border |
|---|---|---|---|
| Pending sync | Warm Beige | Golden Brown | Golden Brown |
| Syncing | Harvest Gold @ 30% on Warm Beige | Warm Charcoal | Golden Brown |
| Synced | Leaf Green @ 15% on Warm Ivory | Leaf Green | — |
| Failed | M3 `errorContainer` | M3 `onErrorContainer` | M3 `error` |
| Needs review | Harvest Gold | Warm Charcoal | Golden Brown |

### Kotlin Compose implementation

Store tokens in `ui/theme/Color.kt`. OKLCH is the design authority; sRGB `Color` values below are derived for Compose runtime.

```kotlin
// ui/theme/Color.kt
object TbzColors {
    // Source-of-truth OKLCH strings
    const val WarmIvoryOklch = "oklch(0.965 0.018 98)"
    const val WarmBeigeOklch = "oklch(0.93 0.028 88)"
    const val GoldenBrownOklch = "oklch(0.487 0.098 78)"
    const val LeafGreenOklch = "oklch(0.36 0.080 138)"
    const val DeepLeafGreenOklch = "oklch(0.28 0.08 145)"
    const val HarvestGoldOklch = "oklch(0.85 0.16 80)"
    const val WarmCharcoalOklch = "oklch(0.18 0.020 88)"

    // sRGB derived from OKLCH tokens
    val WarmIvory = Color(0xFFF6F4E6)
    val WarmBeige = Color(0xFFF0E7D3)
    val GoldenBrown = Color(0xFF7E5709)
    val LeafGreen = Color(0xFF26471A)
    val DeepLeafGreen = Color(0xFF06320C)
    val HarvestGold = Color(0xFFFFC039)
    val WarmCharcoal = Color(0xFF151107)
}

// ui/theme/Theme.kt
private val LightColorScheme = lightColorScheme(
    primary = TbzColors.LeafGreen,
    onPrimary = TbzColors.WarmIvory,
    primaryContainer = TbzColors.WarmBeige,
    onPrimaryContainer = TbzColors.WarmCharcoal,
    secondary = TbzColors.GoldenBrown,
    onSecondary = TbzColors.WarmIvory,
    tertiary = TbzColors.HarvestGold,
    onTertiary = TbzColors.WarmCharcoal,
    background = TbzColors.WarmIvory,
    onBackground = TbzColors.WarmCharcoal,
    surface = TbzColors.WarmIvory,
    onSurface = TbzColors.WarmCharcoal,
    surfaceVariant = TbzColors.WarmBeige,
    onSurfaceVariant = TbzColors.GoldenBrown,
    outline = TbzColors.GoldenBrown.copy(alpha = 0.5f),
    inverseSurface = TbzColors.DeepLeafGreen,
    inverseOnSurface = TbzColors.WarmIvory,
    inversePrimary = TbzColors.HarvestGold,
)

private val DarkColorScheme = darkColorScheme(
    primary = TbzColors.HarvestGold,
    onPrimary = TbzColors.WarmCharcoal,
    primaryContainer = TbzColors.LeafGreen,
    onPrimaryContainer = TbzColors.WarmIvory,
    secondary = TbzColors.GoldenBrown,
    onSecondary = TbzColors.WarmIvory,
    tertiary = TbzColors.HarvestGold,
    onTertiary = TbzColors.WarmCharcoal,
    background = TbzColors.WarmCharcoal,
    onBackground = TbzColors.WarmIvory,
    surface = TbzColors.DeepLeafGreen,
    onSurface = TbzColors.WarmIvory,
    surfaceVariant = TbzColors.LeafGreen,
    onSurfaceVariant = TbzColors.WarmBeige,
    outline = TbzColors.GoldenBrown,
    inverseSurface = TbzColors.WarmIvory,
    inverseOnSurface = TbzColors.WarmCharcoal,
    inversePrimary = TbzColors.LeafGreen,
)
```

**Rules:**
- Profile theme toggle (`tbz:theme` in DataStore) switches between `LightColorScheme` and `DarkColorScheme` only — no third ad-hoc palette.
- Onboarding splash and login hero use **Leaf Green** / **Deep Leaf Green** with **Warm Ivory** / **Harvest Gold** typography.
- Use Material 3 `dynamicColor = false` so system wallpaper theming does not override TBZ brand colors.

---

## 1. Authentication and Onboarding

**Backend status (TRMCS):**
- **Web:** `/accounts/login/`, `/accounts/forgot-password/`, `/accounts/change-password/`, `/accounts/settings/2fa/*`
- **API:** `POST /api/v1/auth/login/`, `POST /api/v1/auth/token/refresh/`, `POST /api/v1/auth/logout/`, `POST /api/v1/auth/users/change-password/`
- **Permissions:** Public login; authenticated endpoints require valid JWT
- **Gaps:** No JWT API for password reset or 2FA setup. Login-time OTP is inline on the login endpoint. Push tokens via `POST /api/v1/mobile/device/register/`.

### 1.1 Onboarding
**Route:** `/onboarding`

Animated first-run sequence with splash, informational scenes, skip/back/next controls, and final login scene.

**Fields:** Uses the same portal login form fields in the final `LoginView`.

**Actions:** Next, Back, Skip, portal sign-in.

**Persistence:** Sets `tbz:onboarding_v2` after successful login.

### 1.2 Portal Login
**Route:** `/portal-login`

Authenticates against the configured TRMCS portal.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Email | Email text input | Yes | Submitted as `email` (`User.USERNAME_FIELD`). |
| 2 | Password | Password input | Yes | Masked. |
| 3 | Portal Base URL | Text input in API Settings modal | Yes | Sanitized and stored in DataStore/secure configuration storage in the Kotlin build. |

**Actions:** Sign In, open API settings, Test Connection.

**API:** `POST /api/v1/auth/login/` — body: `{ "email", "password", "otp?" }`. Returns JWT access/refresh; access token embeds `roles` and `full_name`. Throttle: `login` scope (10/min). Blocks unverified email and `must_change_password` users.

**Web equivalent:** `/accounts/login/` (session); 2FA redirect at `/accounts/login/2fa/`.

**Logic:** Handles login cooldown/rate limit, saves access/refresh tokens, registers stored push token via `POST /api/v1/mobile/device/register/` if present, supports `returnTo`, and routes to `/login-2fa` when server returns `{ "otp": "..." }` validation error (user has 2FA enabled).

### 1.3 Forgot Password
**Route:** `/forgot-password`

Sends a password reset link through the portal. For privacy, unknown email responses should be shown as successful where the portal behaves that way.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Email address | Email text input | Yes | Must be non-empty and contain `@`. Submitted as `email`. |

**Actions:** Send reset link, Try again, Try a different email, Back to sign in.

**Web:** `POST /accounts/forgot-password/` (Django `PasswordResetView` — no JWT API exists).

**Kotlin:** Open `{portal_base_url}/accounts/forgot-password/` in Custom Tab. Show generic success message after submit (same privacy behavior as web). No in-app API call.

**States:** input, sent, error.

### 1.4 Two-Factor Login
**Route:** `/login-2fa`

Login-time OTP verification after username/password.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Verification PIN | Numeric input | Yes | 6-digit code. |

**Actions:** Verify & Continue.

**API:** Same as login — `POST /api/v1/auth/login/` with `{ "email", "password", "otp" }` when `two_factor_enabled` is true on the account.

**Logic:** Posts OTP with login credentials, handles 429 cooldown, stores tokens, and returns to the requested route or home.

### 1.5 Two-Factor Authentication Settings
**Route:** `/two-factor-auth`

Enables or disables authenticator-app 2FA for the logged-in portal account.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Verification code | Numeric input | Yes when enabling | 6-digit code from authenticator after scanning QR. |
| 2 | Verification code | Numeric input | Yes when disabling | Current 6-digit code. |

**Displays:** Current 2FA status, QR code, manual setup key, recovery codes.

**Actions:** Enable Authenticator, Enable 2FA, Disable 2FA, Cancel, Open Web Portal Settings.

**Backend:** No REST endpoints for QR/recovery setup. Web only: `/accounts/settings/2fa/setup/`, `/enable/`, `/disable/`. Mobile should open web settings or defer 2FA enrollment. Login-time OTP is the only mobile API 2FA surface.

**Kotlin:** Replace native enable/disable forms with a single **Open web security settings** action (`CustomTabsIntent` → `/accounts/settings/2fa/setup/`). Display `two_factor_enabled` from cached profile. Keep `/login-2fa` route for OTP entry at sign-in only.

### 1.6 Change Password
**Route:** `/change-password`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Current Password | Password input | Yes | Sent as `old_password`. |
| 2 | New Password | Password input | Yes | Minimum 10 characters; Django password validators apply. |
| 3 | Confirm New Password | Password input | Yes | Must match new password. |

**Action:** Update Password.

**API:** `POST /api/v1/auth/users/change-password/` — body: `{ "old_password", "new_password" }`. Blacklists existing JWTs on success.

**Kotlin:** Validate `new_password.length >= 10` before submit. Mirror server validation errors on `new_password` field. After success, wipe secure token store and navigate to login.

**Web equivalent:** `/accounts/change-password/`.

### 1.7 Kotlin Mobile App — Security Flow Requirements

These rules apply to the native Android rebuild. They reflect what the TRMCS backend actually exposes today.

| Flow | In-app (Kotlin) | Backend | Implementation |
|---|---|---|---|
| Login | Yes | `POST /api/v1/auth/login/` | Email + password; if response includes `{ "otp": "..." }`, show OTP screen and retry same endpoint with `otp`. |
| Login 2FA (OTP at sign-in) | Yes | Same login endpoint | Required when account has `two_factor_enabled`. Not a separate token endpoint. |
| Change password | Yes | `POST /api/v1/auth/users/change-password/` | Enforce **minimum 10 characters** client-side plus Django validators (common password, similarity, etc.). On success, clear tokens and force re-login (server blacklists JWTs). |
| Forgot password | **Web only** | `/accounts/forgot-password/` | No JWT reset API. Open staff portal reset page in Chrome Custom Tab / WebView, or deep-link user to browser. Do not call a non-existent `/api/v1/auth/users/reset-password/`. |
| 2FA setup (QR, enable, disable, recovery codes) | **Web only** | `/accounts/settings/2fa/setup/`, `/enable/`, `/disable/` | No REST endpoints. Profile screen action: **Open security settings on web**. Show read-only `two_factor_enabled` from `GET /api/v1/mobile/me/`. |
| Email verification | **Web only** | `/accounts/verify-email/` | Login blocked until `is_verified`; surface message and link to web verification flow. |
| Must change password | Redirect | Login rejection | If login returns `must_change_password`, route to change-password screen before any field work. |

**Kotlin UI copy (recommended):**
- Forgot password button: *Reset via web portal* (opens `/accounts/forgot-password/` on configured base URL).
- 2FA settings: *Manage two-factor authentication on the web portal* (opens `/accounts/settings/2fa/setup/`).
- Change password validation hint: *At least 10 characters; must meet TBZ password policy.*

**Do not build:** Native forgot-password API client, native TOTP enrollment, or recovery-code storage unless backend adds matching REST endpoints later.

---

## 2. Home, Search, Profile, and Static Pages

**Backend status (TRMCS):**
- **Web:** Staff land on module dashboards (`/growers/`, `/inspections/`, `/marketing/`, `/permits/`); global search at `/search/`
- **API:** `GET /api/v1/mobile/dashboard/`, `GET /api/v1/mobile/me/`, `GET /api/v1/notifications/my/`
- **Permissions:** Dashboard counts are role-scoped; search results depend on capability flags

### 2.1 Home Dashboard
**Route:** `/`

**Fields:** None.

**Displays:** Golden leaf header, unread notification badge, profile dashboard card, counts for growers, inspections due, permits, **returned-for-correction** items, plus quick-action tiles.

**Navigation tiles:** Growers, Permits, Sales, Inspection, Arbitration, Sync.

**Other actions:** Open profile, open notifications, open TBZ regulatory guidelines.

**API:** `GET /api/v1/mobile/dashboard/` — role-scoped counts:
- `INSPECTOR|ADMIN|SUPERADMIN|RTI`: `pending_registrations`, `returned_registrations`, `pending_permits`, `returned_permits`, `scheduled_inspections`, optional `my_province_growers`
- `DATA_CLERK` (plus above permit counts): `returned_group_permits` (submitted by current user)
- `DATA_CLERK|ADMIN|SUPERADMIN`: `bales_today`, `bales_this_season`
- All roles: `unread_notifications`

**Web:** Module-specific dashboards; no single unified mobile-style dashboard on web.

### 2.2 Global Search
**Routes:** `/search`, `/explore`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search registrations, permit | Text input | No | Runs after submit when query length is greater than 1. |

**Searches (mobile scope):**
- Growers: `GET /api/v1/growers/growers/?search=`
- Permits: `GET /api/v1/permits/transport-permits/?search=`

**Web (`/search/`):** Broader search — also bales, validations, arbitrations (permission-gated). Kotlin should not assume web parity unless user capabilities allow.

**Result navigation:** Grower results open `/registration/grower-details`; permit results open `/permit-detail`.

### 2.3 Notifications
**Route:** `/notifications`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search notifications | Text input | No | Searches subject, message, and reference. |

**Filters:** All, Archive, Favorite.

**Displays:** Channel icon, subject, message preview, relative time, sent/pending/failed status, reference, unread marker.

**Actions:** Pull to refresh, open detail bottom sheet, mark favorite, swipe archive/unarchive, swipe delete.

**Storage:** Local read/favorite/archive/deleted state in `tbz_notif_state_v1`; notification data cached offline.

**API:** `GET /api/v1/notifications/my/` — fields: `id`, `channel`, `subject`, `message`, `status`, `reference`, `created_at`, `sent_at`. No server-side favorite/archive/delete/mark-read API; those states are client-only.

### 2.4 Profile
**Route:** `/profile`

**Fields:** One toggle.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Mode | Switch | No | Toggles dark/light theme (see **Color Theme** section) and stores `tbz:theme`. |

**Displays:** Avatar initials, full name, email, login-required empty state.

**Actions:** Change Password, 2F Auth, About App, Terms & Conditions, Privacy Policy, Share This App, Log Out, Log In to Portal.

**API:** Prefer `GET /api/v1/mobile/me/` for RBAC (returns `permissions` map and `modules` booleans for nav gating). Basic profile: `GET /api/v1/auth/me/` — `id`, `email`, `roles`, `province`, `district`.

### 2.5 More Options
**Route:** `/menu`

**Fields:** None.

**Actions:** Sync Settings, Help & Support entry with no route, About Application entry with no route, Close.

### 2.6 Static Information Screens

| Route | Screen | Fields | Content |
|---|---|---|---|
| `/about` | About App | None | App version, TRMCS summary, compliance/enforcement, key features. |
| `/terms` | Terms & Conditions | None | Static terms content. |
| `/privacy` | Privacy Policy | None | Legal/privacy policy text. |
| `/guidelines` | TBZ Regulatory Guidelines | None | Registration, movement/permit, and marketing compliance notes. |
| `/modal` | Demo Modal | None | Template modal with link back home. |

---

## 3. Registration Module

**Backend status (TRMCS):**
- **Web:** `/growers/register/`, `/growers/registrations/`, `/growers/<uuid>/`, `/growers/<uuid>/update/` (crop renewal), `/growers/<uuid>/approve/`
- **API:** `/api/v1/growers/growers/`, `/api/v1/growers/crop-allocations/`, `/api/v1/growers/sponsors/`, `/api/v1/growers/grower-seasons/`
- **Permissions:** create `growers.create_grower`; update `growers.update_grower`; approve `growers.approve_grower` / `growers.reject_grower`; crop `growers.create_crop`
- **Offline sync:** `POST /api/v1/mobile/sync/bulk/` — `POST growers/growers/`, `POST growers/crop-allocations/`, `PATCH growers/growers/{id}/`, `POST growers/growers/{id}/approve-reject/`, `POST growers/growers/{id}/resubmit-corrections/`
- **Business rules:** Grower statuses: `DRAFT`, `PENDING`, `RETURNED_FOR_CORRECTION`, `APPROVED`, `ACTIVE`, `REJECTED`, `SUSPENDED`. Approve action: `approve` | `return_for_correction` | `reject`. Crop hectarage 0.5–300; barns 1–20; sponsor **or** `is_self_sponsored=true`. Yield server-calculated: ≤9 ha → 1500 kg/ha; >9 ha → 3000 kg/ha.

### 3.1 Registration Dashboard
**Route:** `/registration`

**Fields:** None.

**Displays:** Total portal growers, local pending sync, synced, failed, recent saved local registrations.

**Actions:** Register, View all saved registrations, open portal grower list, open local registrations by sync status, local registration card options.

### 3.2 New Grower Registration - Step 1: Personal and Location Details
**Route:** `/registration/new`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Grower Type | Radio | Yes | Individual/company. |
| 2 | First Name | Text input | Yes | |
| 3 | Middle Name | Text input | No | |
| 4 | Last Name | Text input | Yes | |
| 5 | NRC / Passport / PACRA | Text input | Yes | |
| 6 | Sex | Select | Yes | Male/Female. |
| 7 | Date of Birth | Date picker | No | Stored as `YYYY-MM-DD`. |
| 8 | Grower Category | Select | Yes | Small Scale, Commercial, Company. |
| 9 | Country | Select | Yes | Phone country/dial code. |
| 10 | Local Number | Numeric text input | Yes | Non-digits stripped; leading zero removed when combined. |
| 11 | Email Address | Email text input | No | |
| 12 | Address / Farm / Plot Number | Text input | Yes | |
| 13 | Town / Village | Text input | Yes | |
| 14 | Province | Select | Yes | Zambia provinces. |
| 15 | District | Select | Yes | Dependent on province. |
| 16 | GPS Latitude | Text input | No | Auto-captured when possible. |
| 17 | GPS Longitude | Text input | No | Auto-captured when possible. |
| 18 | Profile Photo | Camera/gallery upload | Yes | Inline camera and picker support. |
| 19 | ID Front | Camera/gallery upload | Yes | |
| 20 | ID Back | Camera/gallery upload | Yes | |

**Inline modal fields:** Portal Base URL, Email, Password, used if a portal session is required.

**Actions:** Cancel, Next, capture photo, flip camera, close camera, upload/select media.

### 3.3 New Grower Registration - Step 2: Crop Info
**Route:** `/registration/crop`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Crop Type | Select | Yes | Tobacco type. |
| 2 | Sponsor | Select | Conditional | Optional when self-sponsored is selected. |
| 3 | Self-Sponsored | Checkbox | Conditional | Required if no sponsor. |
| 4 | Hectarage | Decimal input | Yes | Supports decimals. |
| 5 | Yield per Ha | Read-only calculated display | No | ≤9 ha = 1500 kg/ha; >9 ha = 3000 kg/ha (server-calculated in `CropAllocation.save()`). |
| 6 | Number of Barns | Numeric input | Yes | |
| 7 | Barn Type | Select | Yes | API options with fallback. |
| 8 | Strings per Barn | Numeric input | Yes | |
| 9 | GPS Latitude | Text input | No | Auto-captured. |
| 10 | GPS Longitude | Text input | No | Auto-captured. |

**Actions:** Back, Submit.

**Behavior:** Saves the complete registration locally, queues it for sync, and uses sponsor/barn reference data from API/cache.

### 3.4 Local Registrations
**Route:** `/local-registrations?filter=`

**Fields:** None.

**Filters:** All, Pending, Synced, Failed.

**Displays:** Local grower name, NRC/ID, location, timestamps, sync status, last error.

**Actions:** Edit/resume draft, delete local registration, sync one item, sync all pending, continue a cleared form.

### 3.5 Portal Growers List
**Route:** `/registration/growers-list`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search TBZ ID, NRC, Name | Search text input | No | Applies to portal growers and offline cache fallback. |
| 2 | Filter by Status | Select | No | All, Draft, Pending, Returned for Correction, Approved, Active, Rejected, Suspended. |

**Displays:** Paginated cards with grower name, TBZ ID, NRC, province/district, phone.

**Actions:** Search submit, status filter, infinite load more, open grower details.

**API:** `GET /api/v1/growers/growers/?search=&status=&updated_after=` — delta sync supported.

### 3.6 Grower Details
**Route:** `/registration/grower-details?id=`

**Fields:** None.

**Displays:** Profile header, status, TBZ ID, personal details, contact/location, crop allocations, documents, audit/review information, stop orders.

**Actions:** Edit, Add Crop, Update crop allocation. When status is `RETURNED_FOR_CORRECTION`, primary action becomes **Fix & Resubmit** → `/registration/grower-correction?id=`.

### 3.7 Grower Edit
**Route:** `/registration/grower-edit?id=`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | First Name | Text input | Yes | Save requires first or last name. |
| 2 | Middle Name | Text input | No | |
| 3 | Last Name | Text input | Yes | Save requires first or last name. |
| 4 | NRC / PACRA | Text input | No | |
| 5 | Sex | Select | No | Blank, MALE, FEMALE. |
| 6 | Date of Birth (YYYY-MM-DD) | Text input | No | |
| 7 | Phone Number | Phone input | No | |
| 8 | Email | Email input | No | |
| 9 | Address / Farm | Text input | No | |
| 10 | Town / Village | Text input | No | |
| 11 | Province | Select | Yes | |
| 12 | District | Text input | Yes | |
| 13 | Profile Photo | Camera/gallery upload | No | Optional replacement photo. |
| 14 | ID Front | Camera/gallery upload | No | Optional replacement document image. |
| 15 | ID Back | Camera/gallery upload | No | Optional replacement document image. |

**Action:** Save Changes.

**Endpoint:** `PATCH /api/v1/growers/growers/<id>/`

**Sync bulk:** `PATCH growers/growers/{id}/` via `POST /api/v1/mobile/sync/bulk/`.

**Offline behavior:** If the update cannot reach the portal, save a grower edit draft locally and place it in the grower updates queue for sync.

### 3.8 Grower Updates Queue
**Route:** `/grower-updates`

**Fields:** None.

**Filters:** All, Pending, Synced, Failed.

**Displays:** Pending grower edit drafts, changed field names, created timestamp, sync status, last sync error, and KPIs for pending/failed/synced updates.

**Actions:** Open the grower edit form, sync one update, sync all pending/failed updates, delete a pending update.

**Offline behavior:** Queue is fully available offline. Sync actions are disabled or blocked with an offline message until connectivity returns.

### 3.9 Crop Allocation
**Route:** `/registration/crop-allocation?growerId=&cropId=`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Crop Type | Select | Yes | |
| 2 | Sponsor | Select | Conditional | Optional if self-sponsored. |
| 3 | Self-Sponsored | Checkbox | Conditional | Required if no sponsor. |
| 4 | Hectarage | Decimal input | Yes | |
| 5 | Number of Barns | Numeric input | Yes | |
| 6 | Barn Type | Select | Yes | |
| 7 | Strings per Barn | Numeric input | Yes | |
| 8 | GPS Latitude | Text input | No | Auto-captured when available. |
| 9 | GPS Longitude | Text input | No | Auto-captured when available. |

**Actions:** Cancel, Save/Update.

**Web equivalent:** `/growers/<uuid>/update/?crop_id=` — same crop allocation form; creates internal `GrowerRenewal` record on save (no dedicated renewal API).

**API:** `POST /api/v1/growers/crop-allocations/` or `POST /api/v1/growers/grower-seasons/{id}/add-crop/`. Past-season records are read-only.

### 3.10 Returned for Correction — Grower Registration
**Route:** `/registration/grower-correction?id=`

**Who can open:** Original submitter, users with `growers.update_grower`, or grower portal owner linked to the record.

**Displays:**
- Status badge `RETURNED_FOR_CORRECTION`
- TBZ correction reason (`correction_reason`, `correction_requested_at`, reviewer name when available)
- Same personal/location fields as grower edit (section 3.7)
- Document re-upload prompts when reason references photos/NRC

**Fields:** Same editable set as section 3.7 plus read-only correction banner.

**Actions:**
- **Save draft locally** — queue `PATCH growers/growers/{id}/` (and optional `POST /api/v1/mobile/growers/{id}/documents/` for photos)
- **Resubmit for review** — after corrections saved, queue `POST growers/growers/{id}/resubmit-corrections/` (empty body). Status becomes `PENDING`.

**Web equivalent:** `/growers/<uuid>/corrections/`

**API:** `PATCH /api/v1/growers/growers/{id}/` then `POST /api/v1/growers/growers/{id}/resubmit-corrections/`

**Sync bulk:** `PATCH growers/growers/{id}/` then `POST growers/growers/{id}/resubmit-corrections/` (resubmit only valid when server status is `RETURNED_FOR_CORRECTION`).

**Offline behavior:** Save edits locally first; resubmit item stays blocked until PATCH sync succeeds or local draft matches server state.

**Notifications:** Push/in-app event `GROWER_CORRECTION_REQUESTED` links deep-link to this screen.

### 3.11 Corrections Inbox (Cross-module)
**Route:** `/corrections`

**Fields:** Optional filter chips — All, Growers, Permits, Group Permits.

**Displays:** Unified list of records in `RETURNED_FOR_CORRECTION` from local cache + delta sync (`?status=RETURNED_FOR_CORRECTION` on growers, transport permits, group permits). Each row: entity type, name/number, TBZ reason snippet, returned date, sync state.

**Actions:** Open grower correction, permit correction, or group permit correction screen; sync one; sync all pending correction resubmits.

**Dashboard entry:** Home KPI tiles tap through to filtered inbox (`returned_registrations`, `returned_permits`, `returned_group_permits` from `GET /api/v1/mobile/dashboard/`).

---

## 4. Inspection and Validation Module

**Backend status (TRMCS):**
- **Web:** `/inspections/`, `/inspections/create/`, `/inspections/<id>/validate|nursery|field|curing/`, `/inspections/arbitration/`, `/inspections/calendar/`
- **API:** `/api/v1/inspectorate/inspections/`, `validations/`, `nursery-inspections/`, `field-inspections/`, `curing-inspections/`, `stakeholders/`
- **Permissions:** conduct `validation.create_validation`; schedule create requires `inspections.schedule_inspection`; approve validation `validation.approve_validation` (web only)
- **Province scoping:** Users with role `INSPECTOR` may only create/view records in their assigned `user.province`
- **Reference:** `GET /api/v1/mobile/reference/grower-validation-choices/`, `/reference/nursery-choices/`, `/reference/field-choices/`, `/reference/curing-choices/`
- **Offline sync:** `POST inspectorate/inspections/`, `inspectorate/validations/`, `nursery-inspections/`, `field-inspections/`, `curing-inspections/` via sync bulk

### 4.1 Inspection Dashboard
**Route:** `/inspection`

**Fields:** None.

**Displays:** Scheduled, in progress, completed, high-risk growers, scheduled pending sync, conducted pending sync, type filter pills, sync state.

**Actions:** Schedule, Sync now, open local schedules, open conducted reports, open high-risk growers, open portal lists by status/type, open lookup.

### 4.2 Schedule Inspection
**Route:** `/inspection/schedule`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Grower | Search autocomplete | Yes | Search by name, TBZ ID, or NRC; auto-selects single result. |
| 2 | Assign Inspector | Search autocomplete | Yes | Search inspector; must resolve to selected user. |
| 3 | Inspection Type | Select | Yes | Grower Validation, Nursery Inspection, Field Inspection, Curing Inspection. |
| 4 | Scheduled Date | Date picker | Yes | Defaults to current selected date. |
| 5 | Province | Select | Yes | Auto-filled from grower when possible. |
| 6 | District | Select or text input | Yes | Loaded by province, with text fallback. |
| 7 | Notes | Multiline text input | No | |

**Actions:** Clear selected grower/inspector, Schedule.

**Behavior:** Saves schedule to local database first and can edit local draft schedules via route params.

**API payload:** `POST /api/v1/inspectorate/inspections/` — `{ grower, inspector, inspection_type, scheduled_date, province, district, notes }`. Sync bulk: `POST inspectorate/inspections/`. Create permission: `inspections.schedule_inspection`.

### 4.3 Local Scheduled Inspections
**Route:** `/inspection/schedules-local`

**Fields:** None.

**Displays:** Local schedule cards with inspection type, province, district, date, notes, sync status, errors/conflicts.

**Actions:** Edit, delete, sync one, Sync Pending.

### 4.4 Inspection Detail
**Route:** `/inspection/detail?id=`

**Fields:** None.

**Displays:** Grower, inspector, scheduled date, type, status, province/district, notes, audit metadata.

**Actions:** Start Grower Validation, Start Nursery/Field/Curing Inspection, open grower profile.

### 4.5 Inspection Lookup
**Route:** `/inspection/lookup`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Select Tobacco Stage | Select | Yes | Nursery, Field, Curing. |
| 2 | Inspector Name | Text input | Yes | Legacy RN field; backend audit uses authenticated user (`submitted_by` / `created_by`). |
| 3 | Grower ID or NRC Number | Search input | Yes | Used to retrieve grower. |

**Displays:** Retrieved grower profile, sponsor, location, hectarage, GPS, device ID.

**Actions:** Retrieve Grower, refresh GPS, Start Inspection.

### 4.6 Portal Inspections List
**Route:** `/inspection/portal-list?status=&title=&type=`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search grower | Text input | No | Filters by grower name/search term. |
| 2 | Type filter | Pills | No | All, Grower Validation, Nursery, Field, Curing. |

**Displays:** Server-side inspection cards with grower, TBZ ID, inspector, type, scheduled date, location, status.

**Actions:** Refresh, open inspection detail.

**API:** `GET /api/v1/inspectorate/inspections/?status=&inspection_type=&search=&updated_after=`

### 4.7 High-Risk Growers
**Route:** `/inspection/high-risk`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search grower | Text input | No | Searches visible high-risk list. |

**Displays:** Grower name, NRC, province/district, risk score.

**Backend:** No dedicated `GET /api/v1/inspectorate/validations/high-risk/` endpoint. High-risk is computed server-side as validations with `risk_score >= 70` (web inspection KPI at `/inspections/`, risk dashboard at `/risk/`).

**Kotlin options:**
1. Derive locally from cached `GET /api/v1/inspectorate/validations/` where `risk_score >= 70`
2. Request a new backend endpoint (not implemented today)

### 4.8 Inspection Reports Queue
**Route:** `/inspection/reports`

**Fields:** None.

**Displays:** Conducted offline inspection reports, stage, grower, timestamp, sync status, error/conflict advice.

**Actions:** Edit, delete, sync one, Sync Pending.

### 4.9 Field Inspection Form
**Route:** `/inspection/field`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Transplanted Hectarage | Decimal input | Yes | |
| 2 | Crop Stage | Select | Yes | Main Field, Topping, Reaping, Curing, Grading, Storage. |
| 3 | Plant Population | Select | Yes | Full, Acceptable, Poor. |
| 4 | Crop Uniformity | Select | Yes | Uniform, Moderate, Variable. |
| 5 | Fertilizer Application | Select | Yes | Adequate, Inadequate, Excessive, None. |
| 6 | Pest/Disease Status | Select | Yes | None, Low, Moderate, High. |
| 7 | Weed Control | Select | Yes | Good, Fair, Poor. |
| 8 | Irrigation Status | Select | Yes | Adequate, Inadequate, Not Applicable. |
| 9 | Inspector Remarks | Multiline input | Yes | |

**Auto data:** GPS and device ID.

**Action:** Save Inspection.

**API:** `POST /api/v1/inspectorate/field-inspections/` — key fields: `inspection`, `grower`, `transplanted_hectarage`, `crop_stage`, `plant_population`, `crop_uniformity`, `fertilizer_application`, `pest_disease_status`, `weed_control`, `irrigation_status`, `gps_latitude`, `gps_longitude`, `device_id`, `inspector_remarks`. Choices from `GET /api/v1/mobile/reference/field-choices/`.

### 4.10 Nursery Inspection Form
**Route:** `/inspection/nursery`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Seed Variety | Text input | Yes | |
| 2 | Nursery Size / Number of beds | Text input | Yes | API field: `nursery_size_beds`. |
| 3 | Date of Sowing | Text input | Yes | `YYYY-MM-DD`. |
| 4 | Germination Status | Select | Yes | Good, Fair, Poor. |
| 5 | Seedling Condition | Select | Yes | Healthy, Stressed, Diseased. |
| 6 | Water Source | Select | Yes | Rainfall, Borehole, River, Irrigation, Other. |
| 7 | Pest/Disease Presence | Select | Yes | Yes/No. |
| 8 | Pest/Disease Notes | Multiline input | No | Optional. |
| 9 | Fertilizer Used | Select | Yes | Yes/No. |
| 10 | Fertilizer Notes | Multiline input | No | Optional. |
| 11 | Chemicals Used | Select | Yes | Yes/No. |
| 12 | Chemicals Notes | Multiline input | No | Optional. |
| 13 | Inspector Remarks | Multiline input | Yes | |

**Action:** Save Inspection.

**API:** `POST /api/v1/inspectorate/nursery-inspections/` — choices from `GET /api/v1/mobile/reference/nursery-choices/`.

### 4.11 Curing Inspection Form
**Route:** `/inspection/curing`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Number of Barns | Numeric input | Yes | |
| 2 | Type of Barns | Select | Yes | Flue Cured Barn, Bulk Cure Barn, Air Cured Barn, Fire Cured Barn. |
| 3 | Curing Cycles | Numeric input | Yes | |
| 4 | Fuel Source | Select | Yes | Wood, Coal, Gas, Electric, Other. |
| 5 | Curing Status | Select | Yes | Not Started, In Progress, Completed. |
| 6 | Leaf Quality | Select | Yes | Good, Fair, Poor. |
| 7 | Grading Status | Select | Yes | Not Graded, In Progress, Graded. |
| 8 | Inspector Remarks | Multiline input | Yes | |

**Action:** Save Inspection.

**API:** `POST /api/v1/inspectorate/curing-inspections/` — `barn_type` uses grower `BarnType` choices from reference data; other choices from `GET /api/v1/mobile/reference/curing-choices/`.

### 4.12 Grower Validation
**Route:** `/validation?inspectionId=&grower=&growerId=&editingReportId=`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Grower | Read-only text input | Yes | Supplied by route params or schedule. |
| 2 | NRC Number | Read-only text input | No | Loaded from grower. |
| 3 | Sex | Select | Yes | Male/Female. |
| 4 | GPS Latitude | Decimal input | Yes | Auto-captured but editable. |
| 5 | GPS Longitude | Decimal input | Yes | Auto-captured but editable. |
| 6 | Crop Stage | Select | Yes | API reference, with fallback options. |
| 7 | Tobacco Type | Select | Yes | API reference, with fallback options. |
| 8 | Tobacco Variety | Text input | Yes | |
| 9 | Validated Hectarage | Decimal input | Yes | |
| 10 | Yield per Ha (kg) | Decimal input | Yes | Auto-derived from hectarage but editable. |
| 11 | Sponsor | Select | No | Placeholder is Self-Sponsored. |
| 12 | Types of Barns | Select | Yes | API reference, with fallback options. |
| 13 | Number of Barns | Numeric input | Yes | |
| 14 | Is Barn Capacity Sufficient? | Toggle pills | Yes | YES/NO. |
| 15 | Stakeholders Present | Multi-select pills | No | Loaded from mobile reference endpoint. |
| 16 | Province | Select | Yes | API reference. |
| 17 | District | Select or text input | Yes | Select when reference data exists; text fallback. |
| 18 | Device ID | Text input | No | Auto-populated. |
| 19 | Remark by Inspector | Multiline input | No | |

**Action:** Submit Grower Validation.

**Behavior:** Saves report locally as a conducted inspection pending sync; supports editing existing pending report.

**API:** `POST /api/v1/inspectorate/validations/` — key fields: `inspection`, `grower`, `crop_allocation`, `nrc_number`, `sex`, `gps_latitude`, `gps_longitude`, `crop_stage`, `tobacco_type`, `tobacco_variety`, `validated_hectarage`, `yield_per_hectare`, `sponsor`, `barn_type`, `number_of_barns`, `barn_capacity_sufficient`, `stakeholders_present[]`, `province`, `district`, `device_id`, `inspector_remarks`. Validated hectarage 0.5–300; barns 1–20. Stakeholders from `GET /api/v1/inspectorate/stakeholders/` or mobile reference bundle.

**Web approval (RTI/admin):** `POST .../validations/{id}/approve/` and `/reject/` at `/inspections/<id>/approve-validation/`.

---

## 5. Marketing and Sales Module

**Backend status (TRMCS):**
- **Web:** `/marketing/capture/`, `/marketing/capture/validate-permit/`, `/marketing/bookings/`
- **API:** `POST /api/v1/permits/verify-qr/`, `POST /api/v1/marketing/bales/bulk-create/`, `GET /api/v1/marketing/bales/`
- **Permissions:** `bale.create_bale` (typically `DATA_CLERK` role)
- **Reference:** Sales floors and buyers from `GET /api/v1/mobile/reference/` (`salesfloors[]`, `buyers[]`); rejection reasons from `reference/rejection-reasons/`
- **Offline sync:** `POST marketing/bales/bulk-create/` via sync bulk (requires `permit_token` at top level; send `Idempotency-Key` header on direct API calls)

### 5.1 Marketing Dashboard
**Route:** `/marketing`

**Fields:** None.

**Tiles:** Capture Sales Data, Bookings/Pending Sales, Group Permits, Permits Overview.

**Logic:** Checks portal base URL before navigating; prompts portal login if not configured.

### 5.2 Sales Capture - Step 1: Permit Validation
**Route:** `/sales`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Sales Floor | Select | Yes | From `salesfloors[]` in mobile reference bundle (active `SalesFloor` records). |
| 2 | Permit QR Token / Permit Number | Text input plus QR scanner | Yes | Offline mode uses permit number/cache lookup. |

**Displays after validation:** Grower, TBZ ID, permit number, province, district, remaining bales, remaining weight.

**API:** `POST /api/v1/permits/verify-qr/` — body: `{ "permit_token", "salesfloor_id" }`. Returns validity, grower name, TBZ ID, bales, validity dates, status. Remaining balance is derived from permit detail or capture-time errors.

**Web equivalent:** `/marketing/capture/validate-permit/`.

**Actions:** Scan QR, Validate Permit Token, Look Up in Cache, Change Permit, Next: Batch Info.

### 5.3 Sales Capture - Step 2: Batch Info

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Province | Read-only text input | Yes | From validated permit/sales floor. |
| 2 | District | Read-only text input | Yes | From validated permit/sales floor. |
| 3 | Buyer / Company | Select | No | From `buyers[]` in mobile reference bundle (`LegalEntity` type `BUYER`). |
| 4 | Tobacco Type | Select | Yes | From `tobacco_types[]` in reference bundle (e.g. Flue Cured, Burley, Dark Fired). |
| 5 | Season | Text input | Yes | Defaults to current season. |
| 6 | Sale Date (YYYY-MM-DD) | Text input | Yes | Defaults to today. |
| 7 | Grower Representative Name | Text input | No | |

**Actions:** Back, Continue.

### 5.4 Sales Capture - Step 3: Capture Bales

Repeating bale rows.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Ticket No. | Text input plus barcode scanner | Yes | Per bale. API field: `bale_ticket_number`. |
| 2 | Grade | Text input | Yes | Per bale. API field: `grade_mark`. |
| 3 | Wt (kg) | Decimal input | Yes | Per bale. API field: `weight_kg` (20.00–135.00). |
| 4 | Moist % | Decimal input | No | Per bale. API field: `moisture_percent`. |
| 5 | Price | Decimal input | No | Per bale. API field: `price_per_kg` ($0.10–$5.80 if BOUGHT). |
| 6 | Outcome | Select | Yes | Bought or Rejected. API field: `status` = `BOUGHT` \| `REJECTED`. |
| 7 | Rejection Reason | Select | Conditional | Required when rejected. Codes: `NESTED`, `HIGH_MOISTURE`, `LOW_MOISTURE`, `NTRM`, `OVERWEIGHT`, `UNDERWEIGHT`, `NO_SALE`. |

**Displays:** Pending offline badge, offline warning banner, and Today's Captures list from recent bales when online.

**Actions:** Scan barcode per row, add bale row, delete row, Back, Submit Batch, Save Offline.

**Endpoint:** Online submit posts to `/api/v1/marketing/bales/bulk-create/`; failed/offline submissions are queued.

**API payload:**

```json
{
  "grower": "<uuid>",
  "season": "2026",
  "permit_token": "<required QR token — single or group permit>",
  "bales": [
    {
      "grower": "<same uuid>",
      "salesfloor": "<uuid>",
      "tobacco_type": "<code>",
      "bale_ticket_number": "...",
      "grade_mark": "...",
      "weight_kg": 20.00,
      "moisture_percent": null,
      "price_per_kg": null,
      "status": "BOUGHT",
      "rejection_reason": null,
      "buyer": null,
      "sale_date": "YYYY-MM-DD",
      "grower_rep_name": ""
    }
  ]
}
```

Rules: 1–500 bales per submission; same grower, salesfloor, and tobacco_type across the batch. Group permit tokens fall back to the grower's group entry if single-permit validation fails.

### 5.5 Pending Sales Queue
**Route:** `/pending-sales`

**Fields:** None.

**Displays:** Sync status, conflict category, bale count, grower, sales floor, permit number, sale date, advice/error, last attempt.

**Actions:** Sync All, Retry, Edit & Retry, Discard, Clear Synced, pull to refresh.

### 5.6 Edit Pending Sale
**Route:** `/edit-pending-sale?id=`

**Read-only context:** Grower, permit, bale count, tobacco type, bale table.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Sales Floor | Select | Yes | Existing value can be corrected. |
| 2 | Buyer / Company | Select | No | Includes no-buyer option. |
| 3 | Season | Text input | Yes | |
| 4 | Sale Date (YYYY-MM-DD) | Text input | Yes | |
| 5 | Grower Representative | Text input | No | |

**Actions:** Save Changes, Save & Retry Submission.

---

## 6. Permits Module

**Backend status (TRMCS):**
- **Web:** `/permits/`, `/permits/apply/`, `/permits/validate/`, `/permits/group/*`
- **API:** `/api/v1/permits/transport-permits/`, `/api/v1/permits/group-permits/`, `POST /api/v1/permits/verify-qr/`
- **Permissions:** create `permits.create_permit`; approve/reject `permits.approve_permit` / `permits.reject_permit`; view `permits.view_permit`
- **Offline sync:** Transport and group permits via sync bulk — create, PATCH correction edits, approve-reject, resubmit-corrections, group entries/submit.
- **Approve actions:** `approve`, `reject`, `return_for_correction` (reason required for reject/correction)

### 6.1 Permits Dashboard
**Route:** `/permits`

**Fields:** None.

**Displays:** Active permits, pending, expired, total requested, recent permits list.

**Actions:** Validate, Request, open filtered permit lists, open permit detail.

### 6.2 Permit Request - Step 1: Grower Information
**Route:** `/permit-request`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Grower | Search input | Yes | Search by name, ID, or NRC; selected result becomes the grower. |
| 2 | Grower Category | Select | Yes | Small Scale, Commercial, Company. |
| 3 | Province | Read-only text input | No | Auto-filled from selected grower. |
| 4 | District | Read-only text input | No | Auto-filled from selected grower. |

**Actions:** Search, select grower, clear selected grower, Cancel, Continue.

### 6.3 Permit Request - Step 2: Tobacco and Transport Details

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Total Bales | Numeric input | Yes | |
| 2 | Weight (kg) | Decimal input | Yes | |
| 3 | Purpose | Select | Yes | Sales, Processing, Storage, Export. |
| 4 | Tobacco already bought | Checkbox | No | Indicates pre-sold tobacco. |
| 5 | Vehicle License Plate | Text input | Yes | |
| 6 | Origin Province | Select | Yes | |
| 7 | Origin District | Select | Yes | Dependent on province. |
| 8 | Destination Sales Floor | Select or text input | Yes | Select when sales floor reference data exists; text fallback. |

**Actions:** Back, Continue.

### 6.4 Permit Request - Step 3: Buyer Information and Notes

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Assigned Buyer | Select | No | Includes None. |
| 2 | Buyer has accepted / confirmed purchase | Checkbox | No | |
| 3 | Comments | Multiline text input | No | |

**Action:** Submit.

**Behavior:** Checks API reachability, checks approved grower validation before submit (`PermitService.check_can_issue_permit`), queues permit request offline on network/server failure.

**API:** `POST /api/v1/permits/transport-permits/` — requires approved grower validation; fields include `grower`, `grower_category`, `total_bales`, `total_weight_kg`, `license_plate`, `origin_province`, `origin_district`, `destination_sales_floor`, `purpose`, `buyer`, `is_bought`, `buyer_accepted`, `comments`.

### 6.5 Permit Validate
**Route:** `/permit-validate`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Sales Floor | Select | Yes | Loaded from active sales floors. |
| 2 | Permit QR Token / Permit Number | Text input plus QR scanner | Yes | Offline mode uses permit number cache lookup. |

**Displays:** Validated grower, permit number, province, district, remaining bales, remaining weight, cache warning.

**Actions:** Scan QR, Validate Permit Token, Look Up in Cache, Change Permit.

**API:** Same as sales capture — `POST /api/v1/permits/verify-qr/` with `{ "permit_token", "salesfloor_id" }`.

### 6.6 Permit List
**Route:** `/permit-list?filter=ALL|ACTIVE|PENDING|EXPIRED`

**Fields:** None.

**Displays:** Filtered permit cards with permit number, grower, origin, destination sales floor, bales, weight, status.

**Actions:** Open permit detail.

**API:** `GET /api/v1/permits/transport-permits/?status=&search=&updated_after=`

### 6.7 Permit Detail
**Route:** `/permit-detail?id=`

**Fields in review modal:**

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Review action | Segmented buttons | Yes | Approve, Reject, or Return for Correction. |
| 2 | Valid From | Date picker | Yes if approving | Must not be in the past. |
| 3 | Valid To | Date picker | Yes if approving | Must not be before Valid From. |
| 4 | Rejection / correction reason | Multiline text input | Yes if rejecting or returning | Required for `reject` and `return_for_correction`. |

**Displays:** Permit hero, QR code button for approved permits, KPIs, grower/tobacco details, transport route, buyer information, comments, rejection reason, audit trail, action panel.

**Actions:** Review & Approve, Approve Permit, Reject Permit, Print / Download PDF, Mark as Used, QR Code, Back to Permits.

**API actions:** `POST .../transport-permits/{id}/approve-reject/`, `POST .../mark-used/`, `GET .../print/`, `POST .../resubmit-corrections/`.

### 6.8 Group Permits Dashboard and Flow
**Route:** `/permit-group`

**Modes:** List/dashboard, create header, create entries, validate, approve, detail.

**Dashboard displays:** Draft, pending review, active/approved KPIs, group permit list, status pills when enabled.

**Actions:** Validate, New Group Permit, status filters, load more, open detail.

### 6.9 New Group Permit - Header
**Routes:** `/permit-group` create mode, `/permit-group-create`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | License Plate | Text input | Yes | |
| 2 | Origin Province | Select | Yes | |
| 3 | Origin District | Select | Yes | Dependent on province. |
| 4 | Destination Salesfloor | Select | Yes | Sales floor reference data/cache. |
| 5 | Purpose | Select | Yes | Sales, Processing, Storage, Export. |
| 6 | Comments | Multiline text input | No | |

**Actions:** Cancel, Continue.

**Endpoint:** `POST /api/v1/permits/group-permits/` then `POST .../group-permits/{id}/entries/` per grower, then `POST .../group-permits/{id}/submit/`.

**Sync gap:** ~~Group permit flows are **not** routed through `SyncBulkService`~~ Group permit create, entries, submit, approve-reject, correction PATCH, and resubmit are routed through sync bulk (see section 6.16).

### 6.10 New Group Permit - Grower Entries
**Route:** `/permit-group` create entries mode

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search grower | Search input | Yes before adding entry | Search by name, TBZ ID, or NRC. |
| 2 | Grower Category | Select | Yes | Small Scale, Commercial, Company. |
| 3 | Total Bales | Numeric input | Yes | |
| 4 | Weight (Kg) | Decimal input | Yes | |
| 5 | Notes | Multiline text input | No | Per entry. |

**Actions:** Search, select grower, Add Entry, remove entry, Submit for Approval, Back to List.

**Rule:** Group permit must contain at least 2 growers before submission.

### 6.11 Group Permit Validate
**Route:** `/permit-group` validate mode

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Permit Token (QR Code Text) | Multiline text input plus QR scanner | Yes | Scans/pastes group permit QR token. |

**Displays:** Valid/not-valid result, permit number, license plate, destination, valid-to, status, entries.

**API:** `POST /api/v1/permits/group-permits/validate-qr/` — body: `{ "permit_token" }`.

### 6.12 Group Permit Review
**Route:** `/permit-group` approve mode

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Action | Segmented buttons | Yes | Approve, Reject, or Return for Correction. |
| 2 | Valid From | Date picker | Yes if approving | |
| 3 | Valid To | Date picker | Yes if approving | |
| 4 | Reason | Multiline text input | Yes if rejecting or returning | Required for `reject` and `return_for_correction`. |

**API:** `POST /api/v1/permits/group-permits/<id>/approve-reject/`

### 6.13 Group Permit Status List
**Route:** `/permit-group-status?status=`

**Fields:** None.

**Displays:** Group permits filtered by status, permit number/draft state, license plate, grower count, bales, weight, destination, valid-until, status.

**Actions:** View, Edit draft, Review pending permit if user can approve, Load more, Open portal group permit page when API route is unavailable.

### 6.14 Group Permit Detail
**Route:** `/permit-group-detail?id=`

**Fields:** None.

**Displays:** Group permit hero, origin, purpose, validity, totals, rejection reason, grower manifest, remaining bales/weight, entry status.

**Action:** Back to Group Permits.

### 6.15 Returned for Correction — Transport Permit
**Route:** `/permit-correction?id=`

**Who can open:** Original submitter or users with `permits.create_permit`.

**Displays:**
- Status badge `RETURNED_FOR_CORRECTION`
- TBZ correction reason (`correction_reason`)
- Editable transport fields from permit request steps (sections 6.3–6.4): bales, weight, purpose, license plate, origin, destination, buyer, comments

**Actions:**
- **Save corrections** — `PATCH /api/v1/permits/transport-permits/{id}/` (sync bulk: `PATCH permits/transport-permits/{id}/`)
- **Resubmit for review** — `POST /api/v1/permits/transport-permits/{id}/resubmit-corrections/` (sync bulk supported). Status becomes `PENDING`.

**Web equivalent:** `/permits/<uuid>/corrections/`

**Permit list filter:** `/permit-list?filter=RETURNED` or status `RETURNED_FOR_CORRECTION`.

**Permit detail:** When status is `RETURNED_FOR_CORRECTION`, show **Fix & Resubmit** instead of review actions for submitter; approvers see read-only reason.

### 6.16 Returned for Correction — Group Permit
**Route:** `/permit-group-correction?id=`

**Who can open:** Original submitter or users with `permits.create_permit`.

**Displays:**
- Status badge `RETURNED_FOR_CORRECTION`
- TBZ correction reason
- Editable header fields (section 6.9) and grower manifest entries (section 6.10)
- Validation hint when manifest still requires ≥2 growers before resubmit

**Actions:**
- **Save header** — `PATCH /api/v1/permits/group-permits/{id}/`
- **Add/remove entries** — same entry endpoints as create flow (`POST .../entries/`, remove via API when online)
- **Resubmit for review** — `POST /api/v1/permits/group-permits/{id}/resubmit-corrections/` (sync bulk supported)

**Web equivalent:** `/permits/group/<uuid>/corrections/`

**Group permit status list:** Filter `RETURNED_FOR_CORRECTION`; row action **Fix & Resubmit**.

---

## 7. Arbitration and Renewal

**Backend status (TRMCS):**
- **Web arbitration:** `/inspections/arbitration/`, pending/resolved queues, bale lookup AJAX, disposal flow
- **API arbitration:** `POST /api/v1/inspectorate/arbitrations/` — permission `arbitration.create_arbitration`
- **Web renewal:** `/growers/<uuid>/update/` (crop allocation update for current season)
- **API renewal:** No dedicated renewal endpoint; use crop allocation create/update APIs

### 7.1 Arbitration
**Route:** `/arbitration`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Bale ID | Text input plus barcode scanner | Yes | Scanned ticket. API field: `bale_id`. Server resolves grower, grade, batch from ticket. |
| 2 | Date | Text input | Yes | `YYYY-MM-DD`. API field: `arbitration_date`. |
| 3 | Rejected | Select | Yes | Yes/No. API field: `is_rejected`. |
| 4 | Reason of rejection | Select | Required if rejected | Codes: `NESTED`, `HIGH_MOISTURE`, `LOW_MOISTURE`, `NTRM`, `OVERWEIGHT`, `UNDERWEIGHT`, `NO_SALE`. API field: `rejection_reason`. |
| 5 | Inspector remarks | Multiline input | No | API field: `inspector_remarks`. |
| 6 | Is final | Hidden/default | No | API field: `is_final` (optional boolean). |

**Removed from Kotlin (backend handles via auth):** Arbitrator Name, Grower ID, Grade — audit uses authenticated `arbitrated_by`; server populates grower/grade from bale record.

**Actions:** Scan Bale ID barcode, Submit Arbitration.

**API:** `POST /api/v1/inspectorate/arbitrations/` — sync bulk: `POST inspectorate/arbitrations/`.

**Web:** Full workflow at `/inspections/arbitration/` with pending/resolved lists and bale disposal for rejected bales.

### 7.2 Renewal
**Route:** `/renewal`

Maps to **season crop update**, not a standalone API. The legacy RN screen collected free-text fields; the Kotlin app should implement renewal using the crop allocation flow (section 3.9).

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Grower | Search/select | Yes | Resolve grower by NRC/TBZ ID before editing crop. |
| 2 | Sponsor | Select | Conditional | From reference `sponsors[]` or self-sponsored. |
| 3 | Crop Type | Select | Yes | From `tobacco_types[]` in reference bundle. |
| 4 | Hectarage | Decimal input | Yes | 0.5–300 ha. |
| 5 | Yield per Ha (Kg) | Read-only display | Yes | Server-calculated: ≤9 ha → 1500; >9 ha → 3000. |
| 6 | Number of Barns | Numeric input | Yes | 1–20. |
| 7 | Types of Barns | Select | Yes | From `barn_types[]` in reference bundle. |
| 8 | Number of Strings in a Barn(s) | Numeric input | Yes | API field: `number_of_strings_per_barn`. |
| 9 | GPS Coordinates | Text input | No | `gps_latitude`, `gps_longitude`. |

**Action:** Save crop update (renewal).

**Web:** `/growers/<uuid>/update/?crop_id=` — permissions: `growers.create_crop`, `growers.update_crop`, or `growers.submit_crop`. Creates internal `GrowerRenewal` record on save (admin-visible; no public API).

**API:** `POST /api/v1/growers/crop-allocations/` or update via grower season `add-crop` / PATCH allocation. Queue through sync bulk as crop allocation.

---

## 8. RBAC Requirements

RBAC must be implemented as a first-class app capability, not as display-only metadata. The Kotlin app should have a central `AccessControlService` or equivalent domain component that receives the current user, role names, permission strings, and locally cached capability flags, then exposes checks used by navigation, screens, buttons, and sync workers.

**Source of truth:** `GET /api/v1/mobile/me/` returns `roles`, `permissions` (boolean map from `PolicyEngine.context_dict()`), and `modules` (nav gates). JWT access token also embeds `roles`.

**Mobile app access (backend enforced):** Accounts must hold at least one allowed role (`SUPERADMIN`, `ADMIN`, `INSPECTOR`, `RTI`, `DATA_CLERK`, `ARBITRATOR`, `LICENSING_OFFICER`, `AUDITOR`) and must not be external portal-only users. `READ_ONLY`, `FINANCE`, and `CFO` are rejected at JWT login and on every `/api/v1/mobile/*` request.

**Key permission codenames:**

| Mobile capability | Backend permission(s) |
|---|---|
| Schedule inspections (API create) | `inspections.schedule_inspection` |
| Schedule inspections (web + API) | `inspections.schedule_inspection` |
| Conduct inspections/validation | `validation.create_validation` |
| Capture sales/bales | `bale.create_bale` |
| Request permits | `permits.create_permit` |
| Approve/reject permits | `permits.approve_permit`, `permits.reject_permit` |
| Approve growers | `growers.approve_grower`, `growers.reject_grower` |
| Arbitration | `arbitration.create_arbitration` |

### 8.1 Suggested Role Capability Matrix

| Capability | Admin | Registration Officer | Inspector | Marketing Officer | Permit Officer | Approver/Reviewer | Arbitrator |
|---|---|---|---|---|---|---|---|
| Login/profile/security | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| View dashboard/search/notifications | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| Create grower registration | Yes | Yes | No | No | No | No | No |
| Edit grower details | Yes | Yes | No | No | No | Review only | No |
| View grower details | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| Create/update crop allocation | Yes | Yes | No | No | No | Review only | No |
| Schedule inspections | Yes | No | Yes* | No | No | Yes* | No |
| Conduct validation/inspection | Yes | No | Yes | No | No | No | No |
| View inspection reports | Yes | Yes | Yes | No | Yes | Yes | No |
| Capture sales/bales | Yes | No | No | Yes | No | No | No |
| Manage pending sales sync | Yes | No | No | Yes | No | No | No |
| Request permits | Yes | Yes | No | Yes | Yes | No | No |
| Validate permits | Yes | No | No | Yes | Yes | Yes | No |
| Approve/reject permits | Yes | No | No | No | No | Yes | No |
| Create group permits | Yes | No | No | Yes | Yes | No | No |
| Approve/reject group permits | Yes | No | No | No | No | Yes | No |
| Arbitration submission | Yes | No | No | No | No | No | Yes |
| Sync settings/manual sync | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| API settings/base URL | Yes | Configurable | Configurable | Configurable | Configurable | Configurable | Configurable |

The exact role names should map to backend `UserRole` codenames and permission flags from `/api/v1/mobile/me/`. Prefer granular `permissions` over hard-coded role names.

### 8.2 RBAC Implementation Rules

- Navigation destinations must be hidden or disabled when the user lacks permission.
- Destructive or review actions must be checked again when pressed and again before sync upload.
- Offline-created records must store the creator user ID, role snapshot, and permission snapshot used at creation time.
- When a permission changes while records are pending, sync should submit with the current token and current permissions. A 403 should mark the record failed or `needs_review`.
- Cached RBAC data must include `userId`, `displayName`, `email`, `roles`, `permissions`, `lastFetchedAt`, and `expiresAt`.
- Offline RBAC cache may allow continued field work for the last signed-in user, but admin/security-sensitive actions should require a fresh session when possible.

---

## 9. Sync and Offline Architecture

**Backend sync endpoint:** `POST /api/v1/mobile/sync/bulk/` (not `/api/v1/sync/bulk/`).
**Sync status:** `GET /api/v1/mobile/sync/status/` — returns `server_time`, `reference_version`, `api_version`.
**Reference invalidation:** Compare `reference_version` from sync status against cached reference bundle version.

**Sync item shape:**

```json
{
  "client_id": "local-uuid",
  "idempotency_key": "optional-stable-retry-key",
  "operation": "POST|PATCH",
  "endpoint": "growers/growers/",
  "payload": {},
  "queued_at": "ISO-8601"
}
```

`idempotency_key` is optional; when omitted the server uses `client_id`. A successful item is stored for 7 days and replays the same `server_data` on retry (`replay: true` in the per-item result). Reusing the key with a different payload returns HTTP 409 for that item.

Max 200 items per batch. Returns `207 Multi-Status` when any item fails.

### 9.0 Local-First Architecture — Research and Recommended Design

This section records the architecture decision for the Kotlin field app: what industry practice recommends, what fits TRMCS specifically, and what the mobile team must build.

#### 9.0.1 TRMCS context (design inputs)

| Question | TRMCS answer | Design implication |
|---|---|---|
| Backend / cloud database? | **Django REST + PostgreSQL** via `/api/v1/` and `POST /api/v1/mobile/sync/bulk/` | Custom sync layer required. Firebase, Realm Sync, and MongoDB Atlas Device SDK are **not** applicable. |
| Data complexity? | **Relational entities** (growers, permits, inspections, bales) plus **multipart photos** (NRC, profile) and **GPS** on forms | Room relational schema + app-private file storage for attachments. JSON document DB adds complexity without benefit. |
| Concurrent multi-user edits? | **Mostly no** — agents create their own registrations, inspections, and sales batches. **Selective overlap** on shared master records: grower/permit approval, `RETURNED_FOR_CORRECTION` resubmits, RTI review queues | Server-wins on workflow state transitions. Client-wins on unsynced local creates (via `client_id` / idempotency). Human review for validation/409 conflicts. |
| Real-time push from server? | **No WebSocket/SSE API today.** FCM push tokens supported; in-app notifications via pull | Delta sync + WorkManager pull is the MVP download strategy. FCM may **nudge** a pull; it does not replace it. |

#### 9.0.2 Core storage layer — Room (recommended)

**Verdict: use Room as the single source of truth.** Do not adopt Realm or a third-party sync SDK for v1.

| Option | Fit for TRMCS | Verdict |
|---|---|---|
| **Room (SQLite + ORM)** | Native Kotlin/Coroutines/Flow; relational schema mirrors Django models; Google-recommended for offline-first with custom backends ([Android offline-first guide](https://developer.android.com/topic/architecture/data-layer/offline-first)) | **Recommended** |
| Realm / MongoDB Device SDK | Built for document sync to MongoDB Atlas; conflicts with REST + PostgreSQL authority; adds vendor lock-in | **Reject for v1** |
| Raw SQLite | Same persistence as Room without type-safe DAOs, migrations, or Flow integration | **Reject** — use Room |

**Rule:** UI and ViewModels **never** call Retrofit directly for operational screens. They observe Room via `Flow` / `StateFlow`. Network I/O lives in the data layer only (Repository + SyncWorker).

**Schema split (minimum):**

| Layer | Room tables / stores | Purpose |
|---|---|---|
| **Operational cache** | `growers`, `transport_permits`, `group_permits`, `inspections`, `validations`, … | Read models for lists, detail screens, search, offline lookup |
| **Local drafts** | `grower_registrations`, `grower_edits`, `inspection_reports`, `pending_sales`, … | In-progress forms not yet fully queued |
| **Outbox** | `offline_queue` | One row per mutation to upload; canonical sync payload + metadata |
| **Sync cursors** | `sync_cursors` (`entity_type`, `updated_after`, `last_success_at`) | Per-entity delta download bookmarks |
| **Reference** | `reference_provinces`, `reference_districts`, `reference_salesfloors`, … | Snapshot from `GET /api/v1/mobile/reference/` |
| **Session** | DataStore + EncryptedSharedPreferences / Keystore | Tokens, preferences, RBAC cache metadata (not hot-path relational data) |
| **Attachments** | App-private filesystem + `attachment_refs` table | Photo paths linked to queue rows; upload via multipart after JSON sync |

**Standard columns on every operational / outbox row:**

| Column | Type | Notes |
|---|---|---|
| `local_id` | UUID | Stable client primary key; never reused |
| `remote_id` | UUID nullable | Set after first successful upload |
| `sync_status` | enum | `pending`, `syncing`, `synced`, `failed`, `needs_review`, `blocked` |
| `idempotency_key` | string | Defaults to `local_id`; sent as sync bulk `client_id` / optional `idempotency_key` |
| `updated_at_local` | epoch ms | Device time for UI ordering only — **not** conflict authority |
| `updated_at_server` | ISO-8601 nullable | From API `updated_at` on download merge |
| `last_sync_error` | string nullable | Human-readable + raw API error JSON |
| `conflict_category` | enum nullable | Maps to section 9.7 |

#### 9.0.3 Reactive data flow engine

Follow Google's offline-first data layer: **Repository mediates between Room and network; UI observes Room only.**

```
┌─────────────┐     collect StateFlow      ┌──────────────┐
│ Compose UI  │ ◄───────────────────────── │  ViewModel   │
└─────────────┘                            └──────┬───────┘
                                                  │ suspend / Flow
                                           ┌──────▼───────┐
                                           │  Repository  │
                                           └──┬────────┬──┘
                              observe Flow   │        │  enqueue sync
                                           ┌─▼──┐   ┌─▼────────────┐
                                           │Room│   │ SyncCoordinator│
                                           │DAO │   │ (schedules WM) │
                                           └────┘   └────────────────┘
```

**Read path (online or offline):**

1. ViewModel collects `repository.observeGrowers(filter)` → Room DAO `Flow<List<GrowerEntity>>`.
2. UI renders immediately from cache (stale-while-revalidate).
3. If cache age exceeds TTL **and** network available, Repository schedules a delta pull Worker — UI does not block.

**Write path (always local-first):**

1. Validate form in ViewModel (include offline RBAC checks from cached `/mobile/me/`).
2. Single Room `@Transaction`: upsert operational/draft row + insert `offline_queue` row with `sync_status = pending`.
3. Copy photos to app-private storage; store paths on queue row.
4. Call `SyncCoordinator.scheduleUpload()` → WorkManager one-time unique work.
5. Room Flow emits → UI shows row with "Pending sync" instantly.

**Do not** tie UI refresh to HTTP response callbacks. The Worker updates Room on success/failure; Flow propagates the new state.

#### 9.0.4 Synchronization engine — Outbox + WorkManager

**Verdict: Outbox pattern + WorkManager.** Matches [Android guidance](https://developer.android.com/topic/architecture/data-layer/offline-first) for persistent upload queues and exponential backoff.

**Reject for MVP:**

| Approach | Why not for TRMCS v1 |
|---|---|
| WebSockets / SSE | No server endpoint; adds infra complexity; field agents do not need sub-second multi-user co-editing |
| Foreground-only sync | Sync stops when app is killed; unacceptable for field work |
| Periodic raw AlarmManager | Unreliable on modern Android; WorkManager is the supported API |
| Online-only Retrofit | Violates offline-first requirement |

**Workers to implement:**

| Worker | Type | Constraints | Responsibility |
|---|---|---|---|
| `UploadSyncWorker` | `CoroutineWorker`, unique `ExistingWorkPolicy.KEEP` | `NetworkType.CONNECTED` (+ `UNMETERED` when Wi-Fi-only pref) | Drain `offline_queue` → batch `POST /api/v1/mobile/sync/bulk/` (≤200 items) + multipart document uploads |
| `DeltaDownloadWorker` | periodic (e.g. 15–30 min) + one-time on login/FCM | `NetworkType.CONNECTED` | Pull `?updated_after=` per entity type; upsert Room cache |
| `ReferenceRefreshWorker` | one-time after login + when `reference_version` changes | `NetworkType.CONNECTED` | `GET /api/v1/mobile/reference/` full replace of reference tables |
| `SessionRefreshWorker` | chained before upload if token near expiry | connected | `POST /api/v1/auth/token/refresh/`; on failure pause uploads, keep queue |

**Upload ordering (mandatory):**

1. Refresh session if needed.
2. Reference version check (cheap `GET /mobile/sync/status/`).
3. Upload queue rows **oldest first**, respecting dependencies (section 9.5 step 4).
4. Batch into sync bulk where endpoints match; fall back to direct Retrofit for multipart-only paths.
5. On partial `207` response, mark each item independently (`uploaded` / `failed` / replay).
6. Rewrite `remote_id` on dependents when parent receives server ID.

**Download ordering:**

1. Read `sync_cursors.updated_after` per entity (`growers`, `transport_permits`, `group_permits`, `inspections`, `validations`).
2. Paginate list APIs until `next` is null.
3. Upsert into Room cache; advance cursor to max `updated_at` seen.
4. Never delete local `pending` / `needs_review` rows during download merge.

**Retry policy:** `Result.retry()` with WorkManager exponential backoff (start 30s, cap ~6 hours). Cap manual retries in UI after N attempts → `needs_review`.

#### 9.0.5 Conflict resolution strategy (TRMCS-specific)

TRMCS is **not** a collaborative real-time editor. Conflict rules are **per operation type**, not one global LWW.

| Scenario | Strategy | Client behaviour |
|---|---|---|
| **New local create** (grower, inspection, permit request) | **Idempotent client-wins** | Server replays same `client_id` for 7 days (`replay: true`). UI marks synced. |
| **Same idempotency key, different payload** | **Server rejects (409)** | Mark queue row `needs_review`; show "duplicate key, different data"; user must discard or create new `local_id`. |
| **PATCH grower/permit while `RETURNED_FOR_CORRECTION`** | **Server validates + accepts** | Normal upload; resubmit transitions to `PENDING`. |
| **PATCH master record changed on server since cache** | **Server-wins on workflow fields** | No optimistic locking/ETag on API today. Download merge overwrites cached read model. Pending local PATCH still uploads; server validation errors → `VALIDATION_ERROR` or `GROWER_STATUS`. |
| **Approver action (approve/reject/return)** | **Server-wins always** | Sync bulk calls domain services; 403 → `AUTH_ERROR`. |
| **Duplicate business key** (NRC, bale ticket) | **Server rejects** | Map to `DUPLICATE`; user edits or discards. |
| **Bale capture retry after timeout** | **Idempotent replay** | Critical path — never create second batch on replay. |
| **Two agents edit same grower offline** | **Rare; last successful upload wins on PATCH** | Mitigation: download delta before edit on shared records; show `updated_at_server` in detail UI; future: optional ETag |

**Do not use device clock for merge authority.** Use server `updated_at` for cache ordering only. Unsynced local rows always take UI precedence until upload completes or user discards.

**Semantic merge is not required for MVP.** TBZ forms are single-writer with server validation. Implement `needs_review` screens instead of field-level merge UI.

#### 9.0.6 What the Kotlin team must build

| # | Deliverable | Description |
|---|---|---|
| 1 | `:core:data` module | Room database, DAOs, entities, type converters, migrations |
| 2 | `:core:network` module | Retrofit API interfaces, auth interceptor, error envelope parser |
| 3 | `:core:sync` module | `SyncCoordinator`, `UploadSyncWorker`, `DeltaDownloadWorker`, `ReferenceRefreshWorker`, queue drain logic |
| 4 | Per-domain repositories | `GrowerRepository`, `PermitRepository`, `InspectionRepository`, `SalesRepository` — each exposes `Flow` reads + suspend writes |
| 5 | `OfflineQueueDao` | CRUD + observe pending counts + dependency resolution queries |
| 6 | `SyncCursorStore` | Persist per-entity `updated_after` |
| 7 | `AttachmentStore` | Copy camera/gallery files to internal storage; cleanup after confirmed upload |
| 8 | `AccessControlService` | Offline RBAC from cached `/mobile/me/` |
| 9 | Compose UI sync affordances | Status chips (`Pending`, `Synced`, `Failed`, `Needs review`), queue screens (sections 3.4, 3.8, 9.1), `/corrections` inbox |
| 10 | Instrumentation tests | Room migration tests; Worker tests with `TestListenableWorkerBuilder`; idempotent upload replay |

**Package layout (recommended):**

```text
app/
  ui/              # Compose screens, ViewModels
  domain/          # Use cases (optional thin layer)
  data/
    local/         # Room entities, DAOs, Database
    remote/        # Retrofit services, DTOs
    repository/    # Offline-first repositories
    sync/          # Workers, SyncCoordinator, mappers
```

**Reference samples:** Google's [Now in Android](https://github.com/android/nowinandroid) (WorkManager + Room sync patterns) and the [offline-first architecture guide](https://developer.android.com/topic/architecture/data-layer/offline-first) for read/write queue separation.

---

### 9.1 Sync Settings
**Route:** `/sync-settings`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Automatic sync | Switch | No | Controls background sync while working. |
| 2 | Sync only on Wi-Fi | Switch | No | Skips sync on cellular/metered connections. |

**Displays:** Pending records, last sync, current portal base URL.

**Actions:** Sync now, Portal settings.

**Manual sync outcomes:** ok, up_to_date, no_session, offline, wifi_only, auto_disabled, upload_only_metered.

### 9.2 Offline-First Data Stores

The Kotlin app should use Room as the source of truth for operational data and queues, DataStore for preferences, and EncryptedSharedPreferences or Android Keystore-backed storage for tokens and sensitive secrets.

| Store/Table | Required contents |
|---|---|
| `users/session_cache` | Current user profile, roles, permissions, token metadata, portal base URL metadata. |
| `reference_cache` | Provinces, districts, crop stages, tobacco types, barn types, sponsors, sales floors, buyers, stakeholders, grower summaries, permits, dashboard counts, notifications. High-risk derived client-side from validations (`risk_score >= 70`). |
| `offline_queue` | One row per pending mutation with collection, local ID, remote ID if known, payload JSON, attachments metadata, idempotency key, dependency keys, status, retry count, next retry time, last error, conflict category. |
| `grower_registrations` | Complete new grower registration payload, crop allocation payload, photos/document paths, sync state. |
| `grower_edits` | Grower profile patch payload, changed fields, optional replacement photo/document paths, sync state. |
| `inspection_schedules` | Local scheduled inspection drafts and server-backed schedules cached for offline viewing. |
| `inspection_reports` | Grower validation, nursery, field, and curing inspection reports with audit/GPS/device data. |
| `pending_sales` | Validated permit context, sales batch details, bale rows, offline barcode data, sync state. |
| `permit_requests` | Permit request steps and attachments/reference IDs where applicable. |
| `notifications` | Notification cache plus local read/favorite/archive/deleted state. |
| `sync_audit` | Sync run history, last success/failure per queue type, and conflict diagnostics. |

### 9.3 Offline Components

| Component | Description |
|---|---|
| Room database | Offline queue, local operational records, reference cache, and sync audit trail. |
| DataStore preferences | Theme, onboarding seen state, auto-sync enabled, Wi-Fi-only sync, portal base URL, last sync metadata. |
| Secure token store | Access token, refresh token, refresh expiry, and session lock state. |
| Offline cache | Reference/entity cache with TTL for growers, permits, sales floors, buyers, sponsors, notifications, dashboard counts, and RBAC profile. |
| Offline queue | Grower registrations, grower edits, grower/permit/group **correction resubmits**, inspection schedules, inspection reports, sales batches, permit requests, permit reviews, group permits, arbitration records. |
| Background sync | WorkManager unique periodic work plus one-time sync jobs after record save and when connectivity returns. |
| Network state | Detects online/offline and supports Wi-Fi-only preferences. |
| Idempotency | Queue items use stable IDs/keys to reduce duplicate submissions. |
| Conflict handling | Queue screens display failed/needs-review status, conflict category, advice, and raw errors where available. |
| Attachments | Photo/document files are copied to app-private storage, linked to queue rows, uploaded as multipart, and retained until sync is confirmed. |
| Dependencies | Child records that depend on a server ID must wait until the parent sync returns and local foreign keys are rewritten. |

### 9.4 Required Queue Collections

| Collection | Created by | Sync action |
|---|---|---|
| `grower_registrations` | New grower registration flow | `POST growers/growers/` then `POST growers/crop-allocations/` |
| `grower_edits` | Grower edit flow | `PATCH growers/growers/{id}/` |
| `grower_corrections` | Grower returned for correction | `PATCH growers/growers/{id}/` then `POST growers/growers/{id}/resubmit-corrections/` |
| `permit_corrections` | Transport permit returned for correction | `PATCH permits/transport-permits/{id}/` then `POST .../resubmit-corrections/` |
| `group_permit_corrections` | Group permit returned for correction | `PATCH permits/group-permits/{id}/`, optional entry changes, then `POST .../resubmit-corrections/` |
| `grower_reviews` | Grower approval flow | `POST growers/growers/{id}/approve-reject/` (actions include `return_for_correction`) |
| `inspection_schedules` | Schedule inspection flow | `POST inspectorate/inspections/` |
| `inspection_reports` | Grower validation, nursery, field, curing | `POST inspectorate/validations/`, `nursery-inspections/`, `field-inspections/`, or `curing-inspections/` |
| `pending_sales` | Sales capture wizard | `POST marketing/bales/bulk-create/` (include top-level `permit_token`) |
| `permit_requests` | Permit request wizard | `POST permits/transport-permits/` |
| `permit_reviews` | Permit detail review modal | `POST permits/transport-permits/{id}/approve-reject/` |
| `group_permits` | Group permit create flow | `POST permits/group-permits/`, entries, submit via sync bulk |
| `group_permit_reviews` | Group permit review flow | `POST permits/group-permits/{id}/approve-reject/` |
| `grower_documents` | Post-registration photo upload | `POST /api/v1/mobile/growers/{id}/documents/` (multipart) |
| `arbitration_records` | Arbitration form | `POST inspectorate/arbitrations/` |

### 9.5 Sync Algorithm

1. Persist the user's form submission locally with status `pending`.
2. Build a canonical payload and idempotency key (use `Idempotency-Key` header on direct REST calls; store key on queue row for sync bulk).
3. Check connectivity, Wi-Fi-only preference, valid session, RBAC permission, and dependency resolution.
4. Upload oldest eligible records first by collection priority: parent records, then dependent children.
5. **Upload path:** Use `POST /api/v1/mobile/sync/bulk/` for supported endpoints; use direct REST for file uploads and online bale capture (with `Idempotency-Key`).
6. On success, store remote IDs, mark the row `synced`, and rewrite dependent local references.
7. **Download path:** Pull entity deltas via domain list APIs with `?updated_after=`; refresh reference bundle if version changed.
8. On retryable failure, increment retry count and set exponential backoff with jitter.
9. On validation/conflict/authorization failure, mark `failed` or `needs_review` with conflict category and advice.
10. Never delete local work automatically unless the synced record has been safely acknowledged and no dependent local references need it.

### 9.6 Common Sync Statuses

| Status | Meaning |
|---|---|
| pending | Saved locally and waiting to sync. |
| synced | Successfully uploaded. |
| failed | Upload failed and can be retried or edited. |
| needs_review | Conflict needs user review before retry/discard. |
| blocked | Waiting for dependency, login, RBAC permission, or required reference data. |
| syncing | Temporary in-progress state shown only while a sync worker owns the row. |

### 9.7 Conflict Categories

| Category | Meaning |
|---|---|
| DUPLICATE | Duplicate record such as bale ticket or NRC. |
| LICENSE_VIOLATION | Permit/licence invalid, expired, or not allowed. |
| QUOTA_EXCEEDED | Permit bale or weight quota exceeded. |
| GROWER_STATUS | Grower inactive, suspended, or otherwise not eligible. |
| AUTH_ERROR | Session expired or not authorized; login required. |
| RATE_LIMITED | Server rejected due to too many requests. |
| NETWORK_ERROR | Offline or network request failed. |
| DEPENDENCY_MISSING | Parent local record has not synced or remote ID is unavailable. |
| VALIDATION_ERROR | Required field, format, or business rule rejected by server. |
| UNKNOWN | Unclassified server/API error. |

### 9.8 Android Permissions Needed

| Permission/capability | Used for |
|---|---|
| Camera | Profile/photo documents, permit QR scanner, bale barcode scanner. |
| Media picker/storage access | Selecting existing profile/document images. |
| Fine/coarse location | GPS capture for grower registration, inspections, validation, and renewal. |
| Network state | Online/offline banners, sync constraints, Wi-Fi-only mode. |
| Notifications | Push or local notifications for sync failures, assigned inspections, and portal notices. |
| Biometric/device credentials | Optional app unlock if security settings are enabled. |

### 9.9 Recommended Sync Architecture (Kotlin + TRMCS)

**See section 9.0** for storage, reactive UI, WorkManager, and conflict-resolution decisions. This subsection covers TRMCS-specific API wiring only.

**Verdict: maintain the offline-first model and the existing `mobile_api` sync endpoint.** Do not drop sync in favor of online-only REST.

#### How the mobile app connects

```
┌─────────────────────────────────────────────────────────────┐
│ Kotlin Field App                                            │
│  Room (source of truth) │ DataStore │ Keystore (JWT)        │
└───────────┬─────────────────────────────┬───────────────────┘
            │                             │
   Reads / delta pull              Writes / uploads
            │                             │
            ▼                             ▼
┌───────────────────────┐   ┌───────────────────────────────┐
│ Domain REST APIs      │   │ POST /api/v1/mobile/sync/bulk/│
│ /api/v1/growers/…     │   │ (batch offline queue items)   │
│ /api/v1/inspectorate/ │   └───────────────────────────────┘
│ /api/v1/marketing/…   │                 │
│ /api/v1/permits/…     │                 ▼
│ ?updated_after=…      │   SyncBulkService → serializers /
└───────────────────────┘   domain services (same rules as API)
            │
            ▼
┌───────────────────────┐
│ Mobile helpers        │
│ /api/v1/mobile/       │
│  reference/ me/       │
│  dashboard/ sync/     │
│  device/register/     │
└───────────────────────┘
```

| Operation type | Preferred channel | Notes |
|---|---|---|
| Bootstrap reference data | `GET /api/v1/mobile/reference/` | Cache locally; refresh when `reference_version` changes (`GET /api/v1/mobile/sync/status/`). |
| Profile, RBAC, dashboard | `GET /api/v1/mobile/me/`, `/dashboard/` | Refresh on login and periodic foreground refresh. |
| List/download entities | Domain APIs with `?updated_after=` | Growers, permits, inspections, validations. ISO-8601 and `YYYY-MM-DD HH:MM:SS` accepted (backend normalizes URL-decoded `+` offsets). |
| Offline mutations (most) | `POST /api/v1/mobile/sync/bulk/` | Batch up to 200 items; per-item success/failure in `207` response. Includes group permits, bale bulk capture, and `return_for_correction` on approve actions. |
| Grower ID photos | Two-step | Sync grower JSON via bulk, then `POST /api/v1/mobile/growers/{id}/documents/` (multipart). |
| Idempotent retries (bales) | Sync bulk or direct REST | Sync bulk replays successful items by `client_id` / `idempotency_key` for 7 days; direct REST also supports `Idempotency-Key` header. |
| Push tokens | `POST /api/v1/mobile/device/register/` | After login. |

#### Kotlin sync worker (recommended)

1. **Upload phase:** Drain Room `offline_queue` oldest-first; respect dependency order (grower before crop allocation before permit tied to grower).
2. **For each row:** Prefer sync bulk for supported endpoints; fall back to direct Retrofit call for attachments or when bulk returns `Unsupported sync operation`.
3. **Download phase:** Pull deltas using last successful `updated_after` timestamps per entity type; merge into Room cache.
4. **Reference phase:** If `reference_version` changed, refresh `/api/v1/mobile/reference/`.
5. **Session phase:** Refresh JWT via `POST /api/v1/auth/token/refresh/`; on 401, pause uploads and prompt login (never delete queued work).

Use WorkManager with network + optional unmetered constraints matching sync-settings toggles.

#### Should sync bulk be maintained?

**Yes.** Reasons:
- Single authenticated batch for mixed offline work (grower + inspection + bale in one round trip).
- Server-side audit trail in `mobile_sync_queue_entry` for support and compliance.
- Reuses the same serializers and business rules as the public API.
- Already covered by integration tests (`tests/integration/test_mobile_sync_flow.py`).

**Limitations to plan around (not reasons to remove sync bulk):**
- Duplicate dispatch table in `SyncBulkService` (parallel to viewsets) — partial refactor started (`execute_bulk_bale_create`, grower documents service).
- Sync bulk replays by `client_id` / `idempotency_key`; direct REST `Idempotency-Key` remains available for online-only calls.

---

## 11. Backend `mobile_api` — Review and Refactor Assessment

**Verdict: improve incrementally; no full rewrite required.** The `apps/mobile_api` app is the correct boundary (reference bundle, profile/dashboard, device tokens, sync bulk). Domain logic correctly lives in growers/inspectorate/marketing/permits apps.

### What is solid (keep)

| Component | Role |
|---|---|
| `reference_service.py` | Cached lookup bundle; permission-scoped fields |
| `profile_service.py` | RBAC flags and module gates for mobile nav |
| `device_service.py` | Push token registration |
| `SyncBulkView` + `SyncQueueEntry` | Offline upload batch + audit log |
| Integration tests | Auth, sync bulk, reference cache, delta filters |

### What needs improvement (prioritized)

| Priority | Issue | Status |
|---|---|---|
| P1 | Group permits absent from `SyncBulkService` | **Done** — create, entries, submit, approve-reject routed in sync bulk. |
| P1 | Grower registration photos not supported in sync bulk JSON | **Done** — `POST /api/v1/mobile/growers/{id}/documents/` multipart endpoint. |
| P1 | Bale sync bulk omits `permit_token` / batch creation path | **Done** — `_bulk_bale_create` calls shared `execute_bulk_bale_create()`. |
| P2 | Duplicate logic in `SyncBulkService._dispatch` vs viewsets | Open — bale capture extracted; other routes still inline. |
| P2 | Approve/reject sync handlers ignore `return_for_correction` | **Done** — grower and permit handlers extended. |
| P2 | `updated_after` ISO-8601 causes 500 on some list endpoints | **Done** — `parse_updated_after_param()` in `apps/core/selectors/api_selectors.py`. |
| P3 | No idempotency on sync bulk items | **Done** — replays via `client_id` / optional `idempotency_key` (7-day TTL, payload hash mismatch → 409). |
| P3 | Inspection schedule permission inconsistency | **Done** — `InspectionViewSet.create_permission` → `inspections.schedule_inspection`. |
| P3 | No high-risk list endpoint | Open — optional mobile aggregate or client-side derivation. |

### Refactor vs improve

| Approach | When | For this project |
|---|---|---|
| **Improve only** | App boundary is correct; gaps are missing routes and drift from domain APIs | **Recommended.** Ship Kotlin app against current API; patch `mobile_api` in parallel. |
| **Partial refactor** | Extract sync dispatch into registry mapping `endpoint → service callable` | Do after Kotlin MVP if sync maintenance cost rises. |
| **Full rewrite** | Wrong architecture, unused, or unsafe | **Not warranted.** `mobile_api` is small (~11 files), tested, and actively used. |

### Connection checklist for Kotlin developers

1. Configure `portal_base_url` → all API calls to `{base}/api/v1/…`
2. Login → store access/refresh in EncryptedSharedPreferences / Keystore
3. Fetch `GET /api/v1/mobile/me/` → cache `permissions` + `modules` for offline RBAC
4. Fetch `GET /api/v1/mobile/reference/` → seed Room reference tables
5. Field work → write to Room first; enqueue sync rows
6. WorkManager → `POST /api/v1/mobile/sync/bulk/` + delta pulls + direct REST for exceptions
7. Security flows per section 1.7 (web-only reset/2FA setup; in-app change password min 10)

---

## 10. Screen and Field Summary

| Area | Screens/routes | User-entered fields |
|---|---:|---:|
| Authentication and onboarding | 6 | 10 |
| Home/search/profile/static utility | 9 | 2 plus 1 theme toggle |
| Registration | 12 | 56 |
| Inspection and validation | 12 | 66 |
| Marketing and sales | 6 | 21 plus repeating bale rows |
| Permits and group permits | 14 | 45 |
| Arbitration and renewal | 2 | 15 |
| Sync settings | 1 | 2 toggles |
| **Total** | **66 route entries including aliases/modes** | **219+ including repeating rows** |

Notes:
- Counts include route aliases and modal/flow modes documented above.
- Read-only displays are documented separately from editable fields.
- Dynamic API-driven selects are listed by screen with fallback/static options where visible in source.
- Backend mapping blocks document TRMCS web/API state as of June 2026; verify against OpenAPI at `/api/docs/` when implementing.
- Section 1.7 defines Kotlin security flows (web-only password reset and 2FA setup; change password min 10 chars).
- Section 9.9 and 11 cover sync architecture and backend `mobile_api` improvement priorities.
