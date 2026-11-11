# GitHub Contributions Widget

An **ultra-lightweight** native Android home-screen widget (AppWidget) that pins your GitHub contribution graph to your desktop.

- 📦 **~300KB release APK** (far below the typical 2–3MB)
- 🔋 No background services — refreshes every 6 hours only
- 🎨 Native Canvas rendering with rounded tiles, auto light/dark theme
- 📶 Scrapes the public GitHub contributions page — no login or token required
- 👆 Tap the widget to open your GitHub profile
- 📱 Android 10+ (minSdk 29)

## Features

- 52-week × 7-row contribution heatmap on your home screen (or a fixed span: last half-year / quarter)
- Auto-refresh every 6 hours; redraws on widget resize
- Shows the last cached image when offline — no blank flash
- Custom color palettes (edit `LIGHT` / `DARK` arrays in `ContributionRenderer.kt`)

## Appearance Customization (all in-app, with live preview)

| Setting | Range | Notes |
|---------|-------|-------|
| Theme mode | Follow system / Light / Dark | Applies to both app and widget |
| Time span | Auto / Full year / Half year / Quarter | Auto fits the most recent weeks to the widget size |
| Card corner radius | 0% – 30% | 0 = square corners |
| Empty-tile stroke | 0% – 20% | 0 = no stroke |
| Card background | Theme / Transparent / White / Off-white / Black / Charcoal | |
| Background opacity | 0% – 100% | Semi-transparent card shows the wallpaper through |
| Card margin | 0% – 15% | 0% = card fills the whole widget area |
| Empty-tile contrast | 0% – 100% | Improves visibility of empty and low-contribution tiles |
| Frosted glass frame | Show / Hide | |
| Month label color | Theme / Gray / Slate / White / Black | |
| Month label size | 60% – 200% | Relative to tile size |

## Installation

### Option 1: adb (phone connected)

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Option 2: manual APK install

Send `app-release.apk` to your phone (via chat app / cloud drive / cable) → tap it → allow "Install unknown apps" → install.

### Option 3: Android Studio

Open the project → enable USB debugging on your phone → press Run.

> After installing: **long-press an empty spot on the desktop → Widgets → drag "GitHub 绿墙" to the desktop**. The launcher icon opens an info page.

## Configure Your GitHub Username

No code change or rebuild needed — configure it right in the app:

Open "GitHub 绿墙" → enter your username → tap "Preview & Apply" → the graph is generated instantly and the widget refreshes.

> Optional: preset a default username in `app/src/main/res/values/strings.xml` → `github_username`.

## Building

```bash
# Requirements: JDK 17 + Android SDK (platforms;android-34)
./gradlew assembleDebug     # debug → app/build/outputs/apk/debug/
./gradlew assembleRelease   # release → app/build/outputs/apk/release/ (signed)
```

## Architecture

```
app/src/main/java/com/cat/ghwidget/
├── GithubGraphFetcher.kt    # OkHttp fetch + regex parse of data-date/data-level
├── ContributionRenderer.kt  # Canvas rounded-tile rendering, light/dark palettes + RenderConfig options
├── WidgetStore.kt           # cells and all customization settings in SharedPreferences, last bitmap in cache
├── GithubWidgetProvider.kt  # widget entry: instant cached render → async refresh
└── MainActivity.kt          # config page: username + all appearance options
```

Sole third-party dependency: `com.squareup.okhttp3:okhttp:4.12.0`.

---

中文版本：[README.md](README.md)
