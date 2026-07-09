# TBZ Field App - Complete Feature and Form Field Inventory

Source-backed inventory of every current Expo Router screen, feature, and user-entered form field in the mobile app.
Use this as the reference when rebuilding the app in Android Studio/Java.

Last audited from source: `app/**/*.tsx`, `components/PortalLoginForm.tsx`, and shared form controls.

---

## Route Map

### Root and Utility Routes

| Route | Screen | Notes |
|---|---|---|
| `/onboarding` | Onboarding | First-run animated intro; final scene embeds portal login. |
| `/portal-login` | Portal Login | Standalone login screen. |
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
| Registration | `/registration`, `/registration/new`, `/registration/crop`, `/registration/growers-list`, `/registration/grower-details`, `/registration/grower-edit`, `/registration/crop-allocation`, `/local-registrations` |
| Inspection | `/inspection`, `/inspection/schedule`, `/inspection/schedules-local`, `/inspection/detail`, `/inspection/lookup`, `/inspection/portal-list`, `/inspection/high-risk`, `/inspection/reports`, `/inspection/field`, `/inspection/nursery`, `/inspection/curing`, `/validation` |
| Marketing/Sales | `/marketing`, `/sales`, `/pending-sales`, `/edit-pending-sale` |
| Permits | `/permits`, `/permit-request`, `/permit-validate`, `/permit-list`, `/permit-detail`, `/permit-group`, `/permit-group-create`, `/permit-group-status`, `/permit-group-detail` |
| Other Operations | `/arbitration`, `/renewal` |

---

## Current Color Theme

The app uses a Golden leaf/TBZ brand theme centered on green, gold, white surfaces, and neutral gray UI `#0B6B3A` and `#C9A227`.


---

## 1. Authentication and Onboarding

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
| 1 | Email | Email text input | Yes | Submitted as `email`, with fallback to `username`. |
| 2 | Password | Password input | Yes | Masked. |
| 3 | Portal Base URL | Text input in API Settings modal | Yes | Sanitized and stored in AsyncStorage. |

**Actions:** Sign In, open API settings, Test Connection.

**Logic:** Handles login cooldown/rate limit, saves access/refresh tokens, registers stored push token if present, supports `returnTo`, and routes to `/login-2fa` when portal reports OTP required.

### 1.3 Two-Factor Login
**Route:** `/login-2fa`

Login-time OTP verification after username/password.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Verification PIN | Numeric input | Yes | 6-digit code. |

**Actions:** Verify & Continue.

**Logic:** Posts the OTP together with login credentials, handles 429 cooldown, stores tokens, and returns to the requested route or home.

### 1.4 Two-Factor Authentication Settings
**Route:** `/two-factor-auth`

Enables or disables authenticator-app 2FA for the logged-in portal account.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Verification code | Numeric input | Yes when enabling | 6-digit code from authenticator after scanning QR. |
| 2 | Verification code | Numeric input | Yes when disabling | Current 6-digit code. |

**Displays:** Current 2FA status, QR code, manual setup key, recovery codes.

**Actions:** Enable Authenticator, Enable 2FA, Disable 2FA, Cancel, Open Web Portal Settings.

**API fallbacks:** Tries `/api/v1/auth/2fa/*` and `/api/v1/auth/two-factor/*`; opens web settings if unsupported.

### 1.5 Change Password
**Route:** `/change-password`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Current Password | Password input | Yes | Sent as `old_password`. |
| 2 | New Password | Password input | Yes | Minimum 8 characters. |
| 3 | Confirm New Password | Password input | Yes | Must match new password. |

**Action:** Update Password.

**Endpoint:** `POST /api/v1/auth/users/change-password/`

---

## 2. Home, Search, Profile, and Static Pages

### 2.1 Home Dashboard
**Route:** `/`

**Fields:** None.

**Displays:** Golden leaf header, unread notification badge, profile dashboard card, counts for growers, inspections due, and permits, plus quick-action tiles.

**Navigation tiles:** Growers, Permits, Sales, Inspection, Arbitration, Sync.

**Other actions:** Open profile, open notifications, open TBZ regulatory guidelines.

### 2.2 Global Search
**Routes:** `/search`, `/explore`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search registrations, permit | Text input | No | Runs after submit when query length is greater than 1. |

**Searches:** Growers via `/api/v1/growers/growers/?search=`, permits via `/api/v1/permits/transport-permits/?search=`.

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

### 2.4 Profile
**Route:** `/profile`

**Fields:** One toggle.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Mode | Switch | No | Toggles dark/light theme and stores `tbz:theme`. |

**Displays:** Avatar initials, full name, email, login-required empty state.

**Actions:** Change Password, 2F Auth, About App, Terms & Conditions, Privacy Policy, Share This App, Log Out, Log In to Portal.

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
| 5 | Yield per Ha | Read-only calculated display | No | 1-9 ha = 1500 kg/ha; 10+ ha = 3000 kg/ha. |
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
| 2 | Filter by Status | Select | No | All, Draft, Pending, Approved, Active, Rejected, Suspended. |

**Displays:** Paginated cards with grower name, TBZ ID, NRC, province/district, phone.

**Actions:** Search submit, status filter, infinite load more, open grower details.

### 3.6 Grower Details
**Route:** `/registration/grower-details?id=`

**Fields:** None.

**Displays:** Profile header, status, TBZ ID, personal details, contact/location, crop allocations, documents, audit/review information, stop orders.

**Actions:** Edit, Add Crop, Update crop allocation.

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

**Action:** Save Changes.

**Endpoint:** `PATCH /api/v1/growers/growers/<id>/`

### 3.8 Crop Allocation
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

---

## 4. Inspection and Validation Module

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
| 2 | Inspector Name | Text input | Yes | Stored in report audit. |
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

### 4.7 High-Risk Growers
**Route:** `/inspection/high-risk`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Search grower | Text input | No | Searches visible high-risk list. |

**Displays:** Grower name, NRC, province/district, risk score.

**Endpoint:** `GET /api/v1/inspectorate/validations/high-risk/`

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

### 4.10 Nursery Inspection Form
**Route:** `/inspection/nursery`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Seed Variety | Text input | Yes | |
| 2 | Nursery Size / Number of beds | Text input | Yes | |
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

---

## 5. Marketing and Sales Module

### 5.1 Marketing Dashboard
**Route:** `/marketing`

**Fields:** None.

**Tiles:** Capture Sales Data, Bookings/Pending Sales, Group Permits, Permits Overview.

**Logic:** Checks portal base URL before navigating; prompts portal login if not configured.

### 5.2 Sales Capture - Step 1: Permit Validation
**Route:** `/sales`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Sales Floor | Select | Yes | Loaded from legal entities of type `SALES_FLOOR`. |
| 2 | Permit QR Token / Permit Number | Text input plus QR scanner | Yes | Offline mode uses permit number/cache lookup. |

**Displays after validation:** Grower, TBZ ID, permit number, province, district, remaining bales, remaining weight.

**Actions:** Scan QR, Validate Permit Token, Look Up in Cache, Change Permit, Next: Batch Info.

### 5.3 Sales Capture - Step 2: Batch Info

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Province | Read-only text input | Yes | From validated permit/sales floor. |
| 2 | District | Read-only text input | Yes | From validated permit/sales floor. |
| 3 | Buyer / Company | Select | No | Loaded from legal entities of type `BUYER`. |
| 4 | Tobacco Type | Select | Yes | Flue Cured, Burley, Dark Fired. |
| 5 | Season | Text input | Yes | Defaults to current season. |
| 6 | Sale Date (YYYY-MM-DD) | Text input | Yes | Defaults to today. |
| 7 | Grower Representative Name | Text input | No | |

**Actions:** Back, Continue.

### 5.4 Sales Capture - Step 3: Capture Bales

Repeating bale rows.

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Ticket No. | Text input plus barcode scanner | Yes | Per bale. |
| 2 | Grade | Text input | Yes | Per bale. |
| 3 | Wt (kg) | Decimal input | Yes | Per bale. |
| 4 | Moist % | Decimal input | No | Per bale. |
| 5 | Price | Decimal input | No | Per bale. |
| 6 | Outcome | Select | Yes | Bought or Rejected. |
| 7 | Rejection Reason | Select | Conditional | Required when outcome is Rejected. |

**Displays:** Pending offline badge, offline warning banner, and Today's Captures list from recent bales when online.

**Actions:** Scan barcode per row, add bale row, delete row, Back, Submit Batch, Save Offline.

**Endpoint:** Online submit posts to `/api/v1/marketing/bales/bulk-create/`; failed/offline submissions are queued.

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

**Behavior:** Checks API reachability, checks approved grower validation before submit, queues permit request offline on network/server failure.

### 6.5 Permit Validate
**Route:** `/permit-validate`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Sales Floor | Select | Yes | Loaded from active sales floors. |
| 2 | Permit QR Token / Permit Number | Text input plus QR scanner | Yes | Offline mode uses permit number cache lookup. |

**Displays:** Validated grower, permit number, province, district, remaining bales, remaining weight, cache warning.

**Actions:** Scan QR, Validate Permit Token, Look Up in Cache, Change Permit.

### 6.6 Permit List
**Route:** `/permit-list?filter=ALL|ACTIVE|PENDING|EXPIRED`

**Fields:** None.

**Displays:** Filtered permit cards with permit number, grower, origin, destination sales floor, bales, weight, status.

**Actions:** Open permit detail.

### 6.7 Permit Detail
**Route:** `/permit-detail?id=`

**Fields in review modal:**

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Review action | Segmented buttons | Yes | Approve or Reject. |
| 2 | Valid From | Date picker | Yes if approving | Must not be in the past. |
| 3 | Valid To | Date picker | Yes if approving | Must not be before Valid From. |
| 4 | Rejection Reason | Multiline text input | Yes if rejecting | |

**Displays:** Permit hero, QR code button for approved permits, KPIs, grower/tobacco details, transport route, buyer information, comments, rejection reason, audit trail, action panel.

**Actions:** Review & Approve, Approve Permit, Reject Permit, Print / Download PDF, Mark as Used, QR Code, Back to Permits.

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

**Endpoint:** `POST /api/v1/permits/group-permits/`

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

**Endpoint:** `POST /api/v1/permits/group-permits/validate-qr/`

### 6.12 Group Permit Review
**Route:** `/permit-group` approve mode

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Action | Segmented buttons | Yes | Approve or Reject. |
| 2 | Valid From | Date picker | Yes if approving | |
| 3 | Valid To | Date picker | Yes if approving | |
| 4 | Reason | Multiline text input | Yes if rejecting | |

**Endpoint:** `POST /api/v1/permits/group-permits/<id>/approve-reject/`

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

---

## 7. Arbitration and Renewal

### 7.1 Arbitration
**Route:** `/arbitration`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Arbitrator Name | Text input | Yes | Audit trail. |
| 2 | Grower ID | Text input | Yes | |
| 3 | Bale ID | Text input plus barcode scanner | Yes | Scanner writes scanned value into Bale ID. |
| 4 | Grade | Text input | No | |
| 5 | Date | Text input | Yes | `YYYY-MM-DD`. |
| 6 | Rejected | Select | Yes | Yes/No. |
| 7 | Reason of rejection | Select | Required if rejected | Nested, High Moisture, Low Moisture, NTRM, Overweight, Underweight, No sale. |

**Actions:** Scan Bale ID barcode, Submit Arbitration.

**Current behavior:** Validates locally and shows submitted alert; no API persistence in this screen.

### 7.2 Renewal
**Route:** `/renewal`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Inspector Name | Text input | Yes | Audit trail. |
| 2 | NRC Number | Text input | Yes | |
| 3 | Sponsor | Select | Yes | Self-sponsorship, Sponsor A, Sponsor B. |
| 4 | Crop Type | Select | Yes | Flue Cured Tobacco, Burley, Dark Fired Tobacco. |
| 5 | Hectarage | Decimal input | Yes | |
| 6 | Yield per Ha (Kg) | Read-only text input | Yes | Auto-calculated from hectarage. |
| 7 | Number of Barns | Numeric input | No | |
| 8 | Types of Barns | Select | No | Traditional, Modern, Other. |
| 9 | Number of Strings in a Barn(s) | Numeric input | No | |
| 10 | GPS Coordinates | Text input | No | Placeholder says auto-captured. |

**Action:** Submit registration update / renewal.

**Current behavior:** Validates locally and shows submitted alert; no API persistence in this screen.

---

## 8. Sync and Offline Architecture

### 8.1 Sync Settings
**Route:** `/sync-settings`

| # | Field | Type | Required | Notes |
|---|---|---|---|---|
| 1 | Automatic sync | Switch | No | Controls background sync while working. |
| 2 | Sync only on Wi-Fi | Switch | No | Skips sync on cellular/metered connections. |

**Displays:** Pending records, last sync, current portal base URL.

**Actions:** Sync now, Portal settings.

**Manual sync outcomes:** ok, up_to_date, no_session, offline, wifi_only, auto_disabled, upload_only_metered.

### 8.2 Offline Components

| Component | Description |
|---|---|
| SQLite database | Offline queue and local records via `expo-sqlite`. |
| Offline cache | Reference/entity cache with TTL for growers, permits, sales floors, buyers, sponsors, notifications, dashboard counts. |
| Offline queue | Grower registrations, inspection schedules, inspection reports, sales batches, and permit requests. |
| Background sync | Defined at app root and registered on startup. |
| Network state | Detects online/offline and supports Wi-Fi-only preferences. |
| Idempotency | Queue items use stable IDs/keys to reduce duplicate submissions. |
| Conflict handling | Queue screens display failed/needs-review status, conflict category, advice, and raw errors where available. |

### 8.3 Common Sync Statuses

| Status | Meaning |
|---|---|
| pending | Saved locally and waiting to sync. |
| synced | Successfully uploaded. |
| failed | Upload failed and can be retried or edited. |
| needs_review | Conflict needs user review before retry/discard. |

### 8.4 Conflict Categories

| Category | Meaning |
|---|---|
| DUPLICATE | Duplicate record such as bale ticket or NRC. |
| LICENSE_VIOLATION | Permit/licence invalid, expired, or not allowed. |
| QUOTA_EXCEEDED | Permit bale or weight quota exceeded. |
| GROWER_STATUS | Grower inactive, suspended, or otherwise not eligible. |
| AUTH_ERROR | Session expired or not authorized; login required. |
| RATE_LIMITED | Server rejected due to too many requests. |
| NETWORK_ERROR | Offline or network request failed. |
| UNKNOWN | Unclassified server/API error. |

---

## 9. Screen and Field Summary

| Area | Screens/routes | User-entered fields |
|---|---:|---:|
| Authentication and onboarding | 5 | 9 |
| Home/search/profile/static utility | 9 | 2 plus 1 theme toggle |
| Registration | 8 | 53 |
| Inspection and validation | 12 | 66 |
| Marketing and sales | 6 | 21 plus repeating bale rows |
| Permits and group permits | 14 | 45 |
| Arbitration and renewal | 2 | 17 |
| Sync settings | 1 | 2 toggles |
| **Total** | **57 route entries including aliases/modes** | **215+ including repeating rows** |

Notes:
- Counts include route aliases and modal/flow modes documented above.
- Read-only displays are documented separately from editable fields.
- Dynamic API-driven selects are listed by screen with fallback/static options where visible in source.
