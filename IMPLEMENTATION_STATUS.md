# TBZ Golden Leaf — implementation status

Spec: `docs/mobile-app-features.md` (backend copy at repo root `FEATURES.md` if synced).

Legend: **Done** | **Partial** | **Not started**

## Shell & infrastructure

| Area | Status | Notes |
|---|---|---|
| Kotlin + Compose + Hilt + Room + WorkManager + Retrofit | Done | |
| TBZ brand theme (OKLCH tokens) | Done | `ui/theme/` |
| Offline queue + sync bulk upload | Done | `UploadSyncWorker` |
| Delta download growers/permits/group permits/inspections | Done | `DeltaDownloadWorker` |
| Reference refresh worker | Done | |
| Session refresh worker | Done | |
| RBAC service | Partial | `AccessControlService`; menu + permit approval gating |
| Document upload (grower photos) | Done | Queued after registration; multipart upload post-sync |

## §1 Authentication

| Route | Status |
|---|---|
| `/onboarding` | Done |
| `/portal-login` | Done |
| `/forgot-password` | Done | Custom Tab to portal |
| `/login-2fa` | Done |
| `/change-password` | Done |
| `/two-factor-auth` | Not started | Web-only per spec |

## §2 Home / utility

| Route | Status |
|---|---|
| `/` dashboard | Done | KPIs from API |
| `/search` | Done | Room-backed growers + permits |
| `/notifications` | Done | List + mark read |
| `/profile` | Done |
| `/menu` | Done | RBAC-filtered modules |
| `/sync-settings` | Done |
| Static pages (about, terms, privacy, guidelines) | Partial | Placeholder copy |

## §3 Registration

| Route | Status |
|---|---|
| `/registration` hub | Done |
| `/registration/new` + crop step | Done | 2-step wizard |
| `/local-registrations` | Done |
| `/registration/growers-list` | Done |
| `/registration/grower-details` | Done |
| `/registration/grower-edit` | Done |
| `/registration/grower-correction` | Done |
| `/grower-updates` | Done |
| `/registration/crop-allocation` | Done |
| `/corrections` | Done | Growers + transport + group permits |
| Photo sync to `mobile/growers/{id}/documents/` | Done | After grower POST sync |

## §4 Inspection

| Route | Status |
|---|---|
| `/inspection` dashboard | Done |
| `/inspection/schedule` | Done |
| `/inspection/schedules-local` | Done |
| `/inspection/detail` | Done |
| `/inspection/portal-list` | Done |
| `/inspection/reports` | Done |
| `/inspection/high-risk` | Done | From cached validations |
| `/inspection/lookup` | Partial | Basic grower lookup |
| `/inspection/field` | Done |
| `/inspection/nursery` | Done |
| `/inspection/curing` | Done |
| `/validation` | Done |

## §5 Marketing

| Route | Status |
|---|---|
| `/marketing` dashboard | Done |
| `/sales` (3-step capture) | Done | QR scan via ML Kit |
| `/pending-sales` | Done |
| `/edit-pending-sale` | Done |
| Barcode scanner (CameraX/ML Kit) | Done | `ui/components/QrScannerScreen.kt` |

## §6 Permits

| Route | Status |
|---|---|
| `/permits` dashboard | Done |
| `/permit-request` (3-step) | Done |
| `/permit-validate` | Done | Text + QR scanner |
| `/permit-list` | Done |
| `/permit-detail` | Done | Approve/reject/return panel when `permits.approve_permit` |
| `/permit-correction` | Done | PATCH + resubmit via sync bulk |
| `/permit-group` hub | Done |
| `/permit-group/create` | Done | Header + entries (min 2 growers) |
| `/permit-group/validate` | Done | QR + `validate-qr` API |
| `/permit-group/status` | Done |
| `/permit-group/detail` | Done | Review panel + correction link |
| `/permit-group/correction` | Done | Header edit + add entries + resubmit |

## §7 Other

| Route | Status |
|---|---|
| `/arbitration` | Partial | Hub stub |
| `/renewal` | Not started |

## Next priorities

1. Pull static legal copy from TRMCS or bundled assets
2. Arbitration full form module
3. Renewal module
4. Inspection lookup enhancements
