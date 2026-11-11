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
| Language | English / 中文 | Defaults to English; switching applies instantly (no restart) |

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

## Project Structure

```
GitHub-Contributions-Widget/
├── app/
│   ├── build.gradle.kts              # minSdk 29 / targetSdk 34, version, signing, dependencies
│   ├── proguard-rules.pro            # R8 rules
│   ├── signing/release.jks           # release keystore (not committed)
│   └── src/main/
│       ├── AndroidManifest.xml       # component registration: MainActivity + AppWidgetProvider
│       ├── java/com/cat/ghwidget/
│       │   ├── GithubGraphFetcher.kt    # fetches github.com/users/<name>/contributions,
│       │   │                            #   regex handles both SVG <rect> and HTML <td> layouts
│       │   ├── ContributionRenderer.kt  # ★ widget visuals: Canvas drawing of tiles / corners /
│       │   │                            #   strokes / month labels / card bg / contrast via RenderConfig
│       │   ├── WidgetStore.kt           # SharedPreferences: cells, username,
│       │   │                            #   all customization settings, last rendered bitmap cache
│       │   ├── GithubWidgetProvider.kt  # widget lifecycle: instant cache → async refresh → resize redraw
│       │   └── MainActivity.kt          # ★ app UI logic: username, preview, all settings
│       └── res/
│           ├── layout/activity_main.xml     # ★ app UI layout (cards, input, settings)
│           ├── layout/widget_github.xml     # ★ widget container (ImageView + glass frame)
│           ├── values/colors.xml            # ★ light theme semantic color tokens
│           ├── values-night/colors.xml      # ★ dark theme semantic color tokens
│           ├── values/strings.xml           # ★ UI strings (English default; source for hot language switch)
│           ├── values-zh/strings.xml        # ★ Chinese strings
│           ├── drawable/
│           │   ├── widget_background.xml    # ★ widget frosted-glass frame
│           │   ├── bg_card.xml              # in-app card background
│           │   ├── bg_input.xml             # input field background
│           │   ├── bg_button_primary.xml    # primary button background
│           │   ├── pill_bg.xml / pill_bg_active.xml  # pill styles (normal / selected)
│           │   ├── ic_launcher.xml          # launcher icon (108dp diagonal gradient vector)
│           │   ├── ic_launcher_bg.xml       # adaptive icon background layer
│           │   └── ic_launcher_fg.xml       # adaptive icon foreground layer
│           ├── mipmap-anydpi-v26/ic_launcher.xml  # adaptive icon declaration
│           └── xml/github_widget_info.xml   # widget metadata: default size, 6h refresh, resizable
├── README.md / README_EN.md
└── agent.md                          # project intent and constraints for future agents
```

### Changing the UI? Look here

| What you want to change | Which file |
|-------------------------|------------|
| The graph itself (tiles, gaps, corners, strokes, month labels, color algorithm) | `ContributionRenderer.kt` |
| Widget frame (frosted glass, padding, overall layout) | `res/layout/widget_github.xml` + `res/drawable/widget_background.xml` |
| Widget size, refresh interval, resizability | `res/xml/github_widget_info.xml` |
| App UI layout / adding new settings | `res/layout/activity_main.xml` + `MainActivity.kt` |
| Light/dark palettes | `res/values/colors.xml` / `res/values-night/colors.xml` |
| All strings, default username | `res/values/strings.xml` (English default) / `res/values-zh/strings.xml` (Chinese) |
| UI language / default language | `langMode` in `WidgetStore.kt` (default `LANG_EN`) + `MainActivity.attachBaseContext` |
| Button / input / pill styles | `res/drawable/bg_*.xml`, `pill_bg*.xml` |
| App icon | `res/drawable/ic_launcher*.xml` |
| Version number | `app/build.gradle.kts` → `versionCode` / `versionName` |

### Data flow

```
GitHub public page ──► GithubGraphFetcher (fetch + parse)
                           │
                           ▼
                    WidgetStore (SharedPreferences: cells + settings)
                           │
        ┌──────────────────┴───────────────────┐
        ▼                                      ▼
ContributionRenderer (renders via RenderConfig)  MainActivity (preview + edit settings)
        │                                      │
        ▼                                      ▼
GithubWidgetProvider ──RemoteViews──► home-screen widget (ImageView shows Bitmap)
```

Sole third-party dependency: `com.squareup.okhttp3:okhttp:4.12.0`.

---

中文版本：[README.md](README.md)
