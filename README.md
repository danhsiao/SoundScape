# SoundScape

SoundScape is a community-driven Android application that visualizes real-time and historical noise levels to help UW–Madison students discover quiet study spots or lively hangout locations around campus. The app pairs microphone readings with GPS coordinates and presents the data through a modern Jetpack Compose interface, complete with maps, analytics, and quick navigation between core experiences.

---

## Table of Contents

1. [Features](#features)
2. [Architecture & Tech Stack](#architecture--tech-stack)
3. [Project Structure](#project-structure)
4. [Getting Started](#getting-started)
5. [Configuration & Secrets](#configuration--secrets)
6. [Running & Testing](#running--testing)
7. [Roadmap](#roadmap)
8. [Team](#team)
9. [License](#license)

---

## Features

- **Home Dashboard**
  - Quick access cards for Map, Scan, History, Analytics, and Settings.
  - Recent sound events summary populated from mock data (swap with backend data later).

- **Interactive Map**
  - Google Maps heatmap visualization using weighted sound events.
  - Sound level filters (quiet / moderate / loud) and recenter controls.
  - Tap any heatmap region to inspect the nearest event.

- **Scan Experience**
  - Guided 5–10 second microphone capture flow (currently simulated).
  - Environment labeling with common campus locations and custom entries.
  - Placeholder location and backend integration hooks for future work.

- **History**
  - Chronological list of recordings with decibel levels, timestamps, environment labels, and coordinates.

- **Analytics**
  - Summary metrics (total events, average decibel, breakdown by sound type).
  - **Top 5 Quiet Spots Now** surfaced from recent events (user feedback highlight).
  - Noise forecasts using mock EWMA predictions with directional trend indicators.
  - Peak hours, top locations, and location stats for advanced insight.

- **Settings**
  - Dark mode, notifications, and location tracking toggles (UI only for now).
  - Data contribution preferences (share data, anonymous mode) for future privacy support.
  - Noise unit selection (dB, dB SPL, dB A-weighted).
  - Account and About placeholders for upcoming backend integration.

---

## Architecture & Tech Stack

| Layer | Details |
| --- | --- |
| UI | Jetpack Compose, Material 3 components, Navigation Compose |
| Data | Firestore-backed repositories, session manager, view models |
| Maps | Google Maps Compose utilities, Heatmap overlays (`android-maps-utils`) |
| Language/Build | Kotlin, Gradle (KTS), compile/target SDK 34, min SDK 26 |
| Theming | Custom Material 3 color scheme with dark/light support |

### Key Packages
- `com.cs407.soundscape.ui.screens` – Screen composables for each feature surface.
- `com.cs407.soundscape.navigation` – Bottom navigation + nav graph wiring.
- `com.cs407.soundscape.data` – Shared data models, repositories, and view models.

---

## Project Structure

```
SoundScape/
├─ app/
│  ├─ build.gradle.kts           # Android module configuration
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/cs407/soundscape/
│     │  ├─ MainActivity.kt
│     │  ├─ navigation/
│     │  ├─ ui/screens/
│     │  └─ data/
│     └─ res/                    # (default Android resources)
├─ secrets.properties            # Local Google Maps API key (not in VCS)
├─ local.defaults.properties     # Optional default secret values
└─ README.md
```

---

## Getting Started

### Prerequisites
- Android Studio Giraffe (2022.3.1) or newer.
- Android SDK 34 & Android SDK Build-Tools 34.x installed.
- Kotlin 1.9.x toolchain bundled with Android Studio.
- Google Maps API key (for map features).

### Clone & Open
```bash
git clone <your-repo-url>
cd SoundScape
```

Open the project in Android Studio. Allow Gradle sync to complete and install any prompted SDK components.

---

## Configuration & Secrets

The project uses the [Google Maps Platform Secrets Gradle Plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin) to inject API keys.

1. Create a file named `secrets.properties` at the project root (same directory as this README).
2. Add your Maps API key:
   ```properties
   MAPS_API_KEY=YOUR_REAL_GOOGLE_MAPS_KEY
   ```
3. **Do not commit** `secrets.properties`. The file is ignored by default.

An optional `local.defaults.properties` file can provide fallback values, but production builds must use real keys.

---

### Manual Verification Checklist
- **Home:** Confirm quick action cards and recent events appear.
- **Map:** Verify heatmap renders and filters toggle sound ranges.
- **Scan:** Start/stop recording simulation, test environment dropdown & custom text.
- **History:** Confirm environment labels and coordinates show for each event.
- **Analytics:** Review Top Quiet Spots, forecasts, peak hours, and stats.
- **Settings:** Toggle data contribution switches and change the noise unit.

### Automated Tests
No automated tests have been written yet. Future work will add:
- Unit tests for repositories / ViewModels (once introduced).
- UI tests using Compose testing framework.

---

## Roadmap

| Milestone | Target | Status |
| --- | --- | --- |
| UI layout (Home, Scan, History, Settings) | Oct 27 | ✅ Complete with mock data |
| Mic input & GPS capture | Nov 10 | ⏳ In progress (UI scaffolding done) |
| Heatmap & analytics with forecasting | Nov 24 | 🔄 Heatmap done, forecasting mocked, backend pending |
| Final polish & live backend | Dec 08 | 🗓 Planned |

Planned enhancements:
- Connect to Supabase/Firebase for real recordings and analytics aggregation.
- Implement EWMA or similar forecasting on real data.
- Add authentication, user profiles, and data contribution preferences storage.
- Persist settings using DataStore or backend sync.
- Support offline caching & battery optimizations.

---

## Team

| Name | Email | GitHub |
| --- | --- | --- |
| Daniel Hsiao | dhsiao3@wisc.edu | [@danhsiao](https://github.com/danhsiao) |
| Ricky Das | sdas46@wisc.edu | [@RickyDas999](https://github.com/RickyDas999) |
| Timmy Aziz | tgaziz@wisc.edu | [@Timmy-Aziz](https://github.com/Timmy-Aziz) |
| Michael Tang | mltang2@wisc.edu | [@mltang2](https://github.com/mltang2) |

---
---

If you have questions, file an issue in the repository or reach out to any team member listed above. Enjoy exploring the SoundScape! 🎧🗺️
