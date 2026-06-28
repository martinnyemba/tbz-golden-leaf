# TBZ Golden Leaf — project structure

Single Android app at `/home/ubuntu/AndroidStudioProjects/TBZGoldenLeaf`. Spec reference: `FEATURES.md` (copy of TRMCS `docs/mobile-app-features.md`).

## Package layout

```text
zm.co.tbz.goldenleaf/
├── GoldenLeafApp.kt
├── MainActivity.kt
├── core/
│   ├── di/AppModule.kt           # Room + DAO providers
│   ├── network/
│   │   ├── NetworkModule.kt      # Retrofit, OkHttp, Json
│   │   └── PortalSettings.kt     # Runtime portal base URL
│   ├── rbac/AccessControlService.kt
│   └── security/TokenStore.kt
├── data/
│   ├── local/
│   │   ├── TrmsDatabase.kt
│   │   ├── dao/
│   │   ├── entity/
│   │   └── preferences/UserPreferences.kt   # DataStore (portal URL, theme, sync prefs)
│   ├── remote/
│   │   ├── AuthInterceptor.kt
│   │   ├── ApiResult.kt
│   │   ├── api/TrmcsApi.kt
│   │   └── dto/
│   ├── repository/
│   └── sync/                     # SyncCoordinator + all Workers (no worker/ subfolder)
├── domain/                       # Optional use cases (empty for now)
└── ui/
    ├── auth/                     # Login, onboarding, session ViewModels
    ├── home/                     # Dashboard
    ├── registration/             # Grower registration module
    │   ├── RegistrationModels.kt
    │   ├── RegistrationViewModel.kt
    │   ├── RegistrationComponents.kt
    │   ├── RegistrationScreens.kt
    │   └── RegistrationFormScreens.kt
    ├── profile/
    ├── sync/                     # Sync settings screen
    ├── modules/                  # Inspection, marketing, permits hubs
    ├── components/
    ├── navigation/
    └── theme/
```

## Rules

- **One worker location:** all WorkManager classes live in `data/sync/` (not `data/sync/worker/`).
- **One UI module folder per feature:** `ui/auth`, `ui/home`, etc. (not `ui/screens/auth/`).
- **Preferences:** `data/local/preferences/` (not `core/data/`).
- **API helpers:** `data/remote/ApiResult.kt` (not `core/network/`).

## Resources

- Launcher + drawables: `app/src/main/res/`
- Asset index: `app/src/main/res/ASSETS.md`
