# BodyLens — Android (Kotlin + Jetpack Compose, Clean Architecture)

On-device body-composition tracker. The user captures fitted-clothing photos, the app
estimates circumferences and body-fat **on the device**, stores a private progress
timeline, and generates a rule-based exercise plan. **No `INTERNET` permission in the MVP
— body imagery never leaves the phone.**

## Architecture

Three layers, dependencies point inward (`presentation → domain ← data`). The `domain`
layer is pure Kotlin with no Android imports, so the business rules are unit-testable in
isolation.

```
com.bodylens
├── core/            cross-cutting: Result type, dispatchers, theme, navigation, result bus
├── domain/          PURE KOTLIN — no Android deps
│   ├── model/       entities (BodyMeasurements, BodyComposition, ExercisePlan, UserProfile…)
│   ├── repository/  interfaces (the ports)
│   ├── service/     BodyFatCalculator (US-Navy), ExercisePlanner (rules engine)
│   └── usecase/     one responsibility each, injected into ViewModels
├── data/            implements the domain ports
│   ├── local/db/    Room (progress timeline)
│   ├── local/datastore/  DataStore (profile + onboarding)
│   ├── ml/          BodyAnalyzer port + SilhouetteMeasurer (real geometry) + MediaPipe impl
│   ├── mapper/      entity ↔ domain
│   └── repository/  repository implementations + default exercise catalog
├── di/              Hilt modules
└── feature/         Compose UI, one package per screen (ViewModel + Screen)
    ├── onboarding/  18+ age-gate + consent + profile
    ├── capture/     CameraX guided capture + body-guide overlay
    ├── analysis/    result (body-fat as a RANGE + measurements)
    ├── progress/    home dashboard / timeline
    ├── plan/        generated workout plan
    └── settings/    privacy controls incl. delete-all
```

### Splitting into Gradle modules later
Each top-level package maps cleanly to a future module: `:core`, `:domain` (Kotlin-only
`java-library`), `:data`, and a `:feature:*` module per screen. The package boundaries
already respect that split, so modularization is a mechanical move, not a rewrite.

## Tech stack
Kotlin 2.1 · Compose (Material 3) · Hilt · Coroutines/Flow · Navigation-Compose ·
Room · DataStore · CameraX · MediaPipe Tasks (Vision) · Coil.

## What's fully implemented
- Clean Architecture skeleton wired end-to-end with Hilt.
- **BodyFatCalculator** — real US-Navy metric formula, ACE categories, ±3.5% band.
- **ExercisePlanner** — real rules engine (equipment/level/goal filter + day split) over a
  starter catalog.
- **SilhouetteMeasurer** — the real px→cm + ellipse-perimeter circumference geometry.
- Room timeline, DataStore profile, onboarding age-gate, CameraX capture with shutter,
  result screen, progress dashboard, plan screen, delete-all privacy flow.

## What needs wiring (not architecture — assets/inference)
`data/ml/MediaPipeBodyAnalyzer.kt` is the **only** stubbed piece. To make analysis live:
1. Drop `pose_landmarker_full.task` + a segmenter `.task` into `app/src/main/assets/`.
2. Build the `PoseLandmarker` + `ImageSegmenter` in `MediaPipeBodyAnalyzer`.
3. Implement `extractWidths()` to scan the segmentation mask at each landmark row and
   return pixel widths — these flow into the already-real `SilhouetteMeasurer`.

The contract (`BodyAnalyzer`) means none of the domain/UI changes when you do this.

## Build
Open in Android Studio (Ladybug+), let it sync, run on a device/emulator with API 26+.
A camera is required for capture. No keys or backend needed for the MVP.

## Design / responsibility notes
- Body-fat is always shown as a **range with a disclaimer**, never a single clinical number.
- Backups are disabled (`allowBackup=false` + extraction rules) because imagery is sensitive.
- The optional future LLM layer should personalize the **text** of the plan from the
  structured profile only — never from photos.
