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
| RBAC service | Done | Menu, permit approval, module gating |
| Document upload (grower photos) | Done | Queued after registration; multipart upload post-sync |

## §1 Authentication — Done (except web-only 2FA settings)

## §2 Home / utility

| Route | Status |
|---|---|
| Dashboard, search, notifications, profile, menu, sync | Done |
| Static pages (about, terms, privacy, guidelines) | Done | Bundled copy in `AppStaticContent` |

## §3 Registration — Done (corrections inbox includes all entity types)

## §4 Inspection

| Route | Status |
|---|---|
| Core inspection module | Done |
| `/inspection/lookup` | Done | Cached grower search + schedule shortcut |

## §5 Marketing — Done (QR scanner included)

## §6 Permits — Done (transport + group + corrections)

## §7 Other

| Route | Status |
|---|---|
| `/arbitration` | Done | Bale form + barcode scan + sync bulk |
| `/renewal` | Done | Grower search → crop allocation flow |

## Remaining optional enhancements

1. Fetch static legal copy from TRMCS CMS/API when endpoint exists
2. Arbitration pending/resolved list screens (web parity)
3. Pre-fill schedule inspection from lookup grower selection
