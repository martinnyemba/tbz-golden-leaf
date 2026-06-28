# TBZ Golden Leaf — Android drawable & mipmap assets

Generated from TRMCS `static/icons/icon-512.png` (launcher + logos) and other `static/images/` assets.

## Launcher icons (`mipmap-*`)

| File | Use |
|---|---|
| `ic_launcher.png` | Square launcher icon (full TBZ logo from `icon-512.png`) |
| `ic_launcher_round.png` | Round launcher icon |

Densities: `mipmap-mdpi` (48px) through `mipmap-xxxhdpi` (192px).

Adaptive layers (API 26+): `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` reference:
- `@drawable/ic_launcher_foreground` — cropped `icon-512.png` logo, **74% fill**, transparent background (safe zone for circular masks)
- `@drawable/ic_launcher_background` — solid **Deep Leaf Green** `#06320C`

Legacy `mipmap-*/ic_launcher.png` uses the same cropped logo at **80% fill** on Deep Leaf Green.

## Illustrations & branding (`drawable/`)

| Resource | Source | Compose / XML usage |
|---|---|---|
| `R.drawable.ill_under_maintenance` | `under-maintenance.png` | Maintenance / offline server screens |
| `R.drawable.banner_tbz_board` | TBZ board banner JPG | Login, About, onboarding header |
| `R.drawable.logo_tbz_golden_leaf_128` | `icon-512.png` | Toolbar, compact branding |
| `R.drawable.logo_tbz_golden_leaf_256` | `icon-512.png` | Login card, profile |
| `R.drawable.logo_tbz_golden_leaf_512` | `icon-512.png` | Large hero / about |
| `R.drawable.logo_tbz_full_256` | `icon-512.png` | About screen (alias size) |
| `R.drawable.logo_tbz_full_512` | `icon-512.png` | Splash with org name |
| `R.drawable.logo_tbz_maskable` | `icon-512.png` | Adaptive / marketing preview |
| `R.drawable.splash_logo` | `icon-512.png` (cropped, centered, 82% fill) on Deep Leaf Green `#06320C` | Splash screen |
| `R.drawable.icon_tbz` | `icon-192.png` | In-app PWA-style icon |
| `R.drawable.icon_tbz_apple` | `apple-touch-icon.png` | Legacy / compact icon |

## Compose examples

```kotlin
Image(
    painter = painterResource(R.drawable.logo_tbz_golden_leaf_256),
    contentDescription = "TBZ Golden Leaf",
)

Image(
    painter = painterResource(R.drawable.ill_under_maintenance),
    contentDescription = "Under maintenance",
    modifier = Modifier.fillMaxWidth(),
)

AsyncImage // or Image with banner_tbz_board for login header
```

## Brand colors used in generated launcher backgrounds

- Launcher adaptive background: **Deep Leaf Green** `#06320C` (`TbzColors.DeepLeafGreen`)
- App theme: Warm Ivory `#F6F4E6`, Deep Leaf Green `#06320C`, Harvest Gold `#FFC039`, Warm Charcoal `#151107`
