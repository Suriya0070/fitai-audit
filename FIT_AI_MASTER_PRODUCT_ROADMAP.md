# FIT AI — MASTER PRODUCT VISION, TECHNICAL AUDIT & DEVELOPMENT ROADMAP

**Date:** 2026-11-03  
**Document version:** 1.0  
**Prepared by:** Senior Android Architect / AI Engineer / Firebase Architect / Product Manager / Technical Co-founder  
**Based on:** Full codebase inspection + `FIT_AI_PRODUCTION_AUDIT.md`  
**Status:** Strategic planning document — no code has been modified

---

## TABLE OF CONTENTS

1. Executive Summary  
2. Complete Fit AI Product Vision  
3. Existing Project Assessment  
4. Existing Feature Inventory  
5. Continue vs Rebuild vs Hybrid Comparison  
6. Final Architectural Recommendation  
7. Complete Feature Gap Analysis  
8. Future Technical Architecture  
9. Exercise-Specific AI Design  
10. Rep-Level Form Analysis System  
11. Intelligent Alert Engine  
12. Client Status and Risk Engine  
13. Trainer Dashboard Design  
14. Nutrition System Design  
15. Community System Design  
16. Real-Time Fitness Architecture  
17. Security and Privacy Requirements  
18. Cost and Scalability Assessment  
19. Release-Wise Development Roadmap  
20. Priority Matrix  
21. Technical Risks  
22. Estimated Development Effort  
23. Immediate Next Actions  
24. **FIT AI's Core Differentiation — Intelligent Trainer Monitoring and Exercise Form Analysis**  
25. Final Strategic Q&A

---

## 1. EXECUTIVE SUMMARY

Fit AI is an Android application that has a genuinely strong technical foundation for its most differentiating feature — AI-powered workout analysis using MediaPipe pose estimation — and a working community platform built on Firebase Firestore. The workout tracking pipeline (camera feed → pose landmarks → rep counting → scoring) is real, functional, and more sophisticated than most indie fitness apps at this stage.

However, the application is not yet a coherent product. It is a collection of working technical modules that have not been assembled into a complete user experience. The trainer platform, which is the most important business differentiator, does not exist at all. The nutrition system exists only as a database model with no user interface. The community posts are stored locally on the device and never reach other users. The release configuration has multiple critical security issues.

**The recommendation is a Hybrid Approach (Option C):** preserve, stabilise and enhance what works; build the trainer platform as a new vertical on the existing Firebase foundation; fix all production blockers before any public release.

The complete Fit AI vision is achievable in four development phases over approximately 10 to 14 months for a small focused team. The 50-client-per-trainer monitoring target is realistic given the right architecture and notification strategy.

---

## 2. COMPLETE FIT AI PRODUCT VISION

### 2.1 Mission

Fit AI exists to make professional fitness coaching accessible and scalable by combining AI-powered movement analysis with intelligent client monitoring tools, allowing one personal trainer to provide meaningful, data-driven coaching to up to 50 clients simultaneously.

### 2.2 Primary Users

**Individual User**
A person who wants to train with AI-guided form analysis, track nutrition, follow workout plans, maintain consistency, and optionally connect with a personal trainer.

**Personal Trainer**
A fitness professional who currently relies on manual observation and memory. Fit AI allows them to extend their reach, monitor client activity between sessions, receive intelligent alerts about client behaviour, and intervene at the right time.

### 2.3 The Core Problem We Are Solving

Traditional personal training has two bottlenecks:

1. **Scale:** A trainer can physically observe only a handful of clients. Between in-person sessions, the trainer has no visibility.
2. **Consistency:** Client results suffer not because the workouts are wrong but because compliance, form, and nutrition suffer between supervised sessions.

Fit AI solves this by being the trainer's eyes between sessions.

### 2.4 The Nine Product Modules

| Module | Name | Core Value |
|---|---|---|
| A | AI Fitness Assistant | Device-local AI workout tracking, form analysis, rep counting |
| B | AI Nutrition Assistant | Photo-based meal logging, nutrition estimation, daily summaries |
| C | Fitness Community | Servers, channels, friends, posts, challenges, leaderboards |
| D | Intelligent Trainer Platform | Client management, monitoring, workout assignment, trainer alerts |
| E | Exercise-Specific Form Analysis | Per-exercise movement algorithms, joint angles, rep scoring |
| F | Intelligent Alert Engine | Smart, grouped, prioritised trainer notifications |
| G | Client Status and Risk Engine | Consistency tracking, dropout prediction, engagement scoring |
| H | Real-Time Fitness Experience | Live video, trainer-client calls, group sessions |
| I | Platform Infrastructure | Auth, billing, admin, analytics, privacy, compliance |

---

## 3. EXISTING PROJECT ASSESSMENT

This assessment is based on full source code inspection of the Android project at `C:\Users\Suriya Sittharth\AndroidStudioProjects\Pravin`. For complete findings see `FIT_AI_PRODUCTION_AUDIT.md`.

### 3.1 Technology Stack Summary

| Layer | Technology | Version | Status |
|---|---|---|---|
| Language | Kotlin | 2.2.10 | Current |
| UI | Jetpack Compose + Material3 | BOM 2026.02.01 | Current |
| Architecture | MVVM + Repository | Manual | Functional |
| Local DB | Room SQLite | 2.7.1 | Functional |
| Prefs | DataStore | 1.1.1 | Functional |
| Auth | Firebase Auth | BOM 33.7.0 | Functional (email only) |
| Cloud DB | Cloud Firestore | BOM 33.7.0 | Functional (partial) |
| AI / Pose | MediaPipe PoseLandmarker | 0.10.14 | Functional (ARM only) |
| Camera | CameraX | 1.4.0 | Functional |
| Video | Stream WebRTC Android | 1.3.3 | Partial |
| DI | None (manual) | — | Technical debt |

**Missing Firebase services:** Storage, Cloud Messaging, Crashlytics, Analytics, App Check, Remote Config, Performance.

### 3.2 What Is Actually Working

The following have been verified by inspecting source code. Working means the feature is implemented end-to-end in the code, not that it has been tested on a device.

| Feature | Verification Basis |
|---|---|
| Email/password sign-in and sign-up | `AuthRepository.kt` — Firebase Auth called, errors mapped |
| Guest/local mode | `signInAsLocalUser()` — UUID stored in DataStore |
| Onboarding (2 steps) | `OnboardingScreen.kt` — saves profile to Room |
| Workout tracking (camera + MediaPipe) | `PoseAnalyzer.kt`, `WorkoutViewModel.kt` — full pipeline |
| Rep counting | `RepCounter.kt` — angle-based counting per exercise |
| Workout scoring (5 metrics) | `WorkoutScoreEngine.kt` — accuracy, balance, stability, symmetry, speed |
| Fatigue and injury risk (heuristic) | `FatiguePredictor.kt` — score-decay based estimate |
| Environment check | `EnvironmentAnalyzer.kt` — brightness, person detection, pose visibility |
| Session timer, rest timer, vibration | `WorkoutViewModel.kt` |
| Workout summary and history | `ProgressScreen.kt`, `WorkoutSession` in Room |
| Progress charts | `ProgressScreen.kt` — custom Compose canvas charts |
| Achievements (local) | `Achievement` entity, checked in `WorkoutViewModel` |
| Streak calculation | `ConsistencyTracker.kt` |
| Community servers | `FirestoreCommunityDataSource.kt` — Firestore-backed |
| Channel messaging | `FirestoreCommunityDataSource.kt` — real-time listeners |
| Friends (requests, accept, search) | `FriendsDataSource.kt` — Firestore-backed |
| Sign out | `ProfileViewModel.signOut()` — clears Firebase auth + DataStore |

### 3.3 What Is Partially Working

| Feature | Issue |
|---|---|
| Community posts | Stored in Room only — not visible to other users |
| Workout routines | Created and stored locally — not synced to Firestore, routine exercises not loaded into workout flow |
| Achievements | Local only — not visible to other users, no global leaderboard |
| Rep Battle | Firestore collections exist, race condition in `submitSet()` |
| Live workout (WebRTC) | Code is present but uses public TURN server with hardcoded credentials |
| Profile public visibility | `syncUserProfile()` only called on friend request, not on sign-up |
| Avatar screen | Fully built (`AvatarScreen.kt`) but no navigation trigger to reach it |

### 3.4 What Does Not Exist

| Feature | Status |
|---|---|
| Trainer registration and profile | Not implemented |
| Trainer dashboard | Not implemented |
| Client-trainer assignment | Not implemented |
| Client workout monitoring | Not implemented |
| Trainer alerts | Not implemented |
| Meal tracking UI | Not implemented (model + DAO exist) |
| Food photo upload | Not implemented |
| AI food recognition | Not implemented |
| Nutrition calorie tracking | Not implemented |
| Push notifications (FCM) | Not implemented |
| Account deletion | Not implemented |
| Firebase Storage integration | Not implemented (no dependency) |
| Subscriptions and payments | Not implemented |
| Admin panel | Not implemented |
| Google Sign-In | UI button disabled |
| Email verification | Not implemented |

### 3.5 Critical Production Blockers (Summary)

Full details in `FIT_AI_PRODUCTION_AUDIT.md` Section 8 and Section 16.

1. Firebase API key hardcoded in `FirebaseConfig.kt`
2. App name and package ID are "Pravin"
3. No Firestore Security Rules deployed
4. Release keystore password fallback is "changeme"
5. No account deletion (Play Store policy requirement)
6. No Crashlytics — zero production visibility
7. Public untrusted TURN server for WebRTC
8. No ProGuard rules — release build may crash

---

## 4. EXISTING FEATURE INVENTORY

### Module A — AI Fitness Assistant

| Capability | Status | Notes |
|---|---|---|
| Camera feed (CameraX) | Working | Front camera, 640×480 |
| Pose landmark detection | Working | MediaPipe LIVE_STREAM mode |
| Environment quality check | Working | Brightness, person detection |
| Rep counting | Working | Angle-threshold per exercise |
| Form scoring (5 metrics) | Working | Score 0–100 per frame |
| Fatigue prediction | Working | Heuristic, not medically validated |
| Exercise instructions | Working | Text + form tips per exercise |
| Workout state machine | Working | Idle→Instructions→EnvironmentCheck→Active→Rest→Summary |
| Session save to Room | Working | `FitnessRepository.saveSession()` |
| Progress charts | Working | Canvas-based Compose charts |
| Achievements | Partial | Local only, 6 types |
| Streak | Working | `ConsistencyTracker` |
| Personalized recommendations | Not implemented | |
| 16 exercise types defined | Working | `ExerciseLibrary.kt` |

**Important limitation:** The MediaPipe PoseLandmarker uses `pose_landmarker_full.task`. This model runs on ARM CPUs only. The app will not perform AI analysis on x86_64 emulators. This is a known, expected limitation and is surfaced correctly in the UI.

**Important limitation:** The current rep counter uses configurable angle thresholds per exercise but does not yet perform exercise-specific joint validation (hip alignment checks, knee valgus detection, torso sway). It counts reps correctly but does not classify form errors at the rep level.

### Module B — AI Nutrition Assistant

| Capability | Status |
|---|---|
| `MealPost` Room entity | Exists |
| `MealPostDao` | Exists |
| Meal tracking screen | Not implemented |
| Photo picker | Not implemented |
| Firebase Storage upload | Not implemented |
| AI food recognition | Not implemented |
| Calorie/macro estimation | Not implemented |
| Nutrition history | Not implemented |
| Daily summary | Not implemented |

### Module C — Fitness Community

| Capability | Status | Sync |
|---|---|---|
| Community servers | Working | Firestore |
| Server channels | Working | Firestore |
| Channel messaging | Working | Firestore real-time |
| Server members | Working | Firestore |
| Community posts | Partial | Room only (local) |
| Friends (add/accept) | Working | Firestore |
| User search | Working | Firestore prefix search |
| Reactions | Partial | Room only |
| Comments | Partial | Room only |
| Challenges | Partial | Room only |
| XP system | Not implemented | |
| Global leaderboard | Not implemented | |

### Module D — Trainer Platform

| Capability | Status |
|---|---|
| Everything | Not implemented |

### Module E–G — AI Form, Alerts, Risk Engine

| Capability | Status |
|---|---|
| Generic rep counting | Working |
| Per-rep form classification | Not implemented |
| Joint angle validation per exercise | Not implemented |
| Alert engine | Not implemented |
| Client risk scoring | Not implemented |

### Module H — Real-Time

| Capability | Status |
|---|---|
| 1:1 WebRTC video call | Partial (public TURN) |
| Pose analysis on live stream | Working (every 6th frame) |
| Rep count sync during call | Working (Firestore) |
| Reconnection logic | Minimal |
| Group sessions | Not implemented |

### Module I — Platform Infrastructure

| Capability | Status |
|---|---|
| Firebase Auth (email) | Working |
| Guest mode | Working |
| Sign out | Working |
| Account deletion | Not implemented |
| Privacy policy | Not implemented |
| FCM notifications | Not implemented |
| Crashlytics | Not implemented |
| Analytics | Not implemented |
| Subscriptions | Not implemented |

---

## 5. CONTINUE VS REBUILD VS HYBRID COMPARISON

### Option A — Continue the Existing Project

**What this means:** Keep all existing code exactly as-is. Fix production blockers incrementally. Build new features on top.

| Factor | Assessment |
|---|---|
| Existing code quality | Moderate. The domain logic (PoseAnalyzer, RepCounter, ScoreEngine) is well-structured. The UI layer is well-implemented in Compose. The data layer has architectural inconsistencies. |
| Architecture | MVVM with manual DI. Functional but fragile. No Hilt. ViewModels instantiate their own dependencies. Testability is poor. |
| Development time | Fastest for near-term features. Slowest long-term due to growing DI and testability debt. |
| Firebase integration | Must add Crashlytics, Storage, FCM, App Check — all straightforward as dependency additions. |
| Database design | Room v3 is usable. The 19-entity schema provides a solid local foundation. `exportSchema = false` is a risk. |
| Security | Multiple critical issues but all fixable without restructuring. |
| Maintainability | Adequate for a small team in the short term. Will degrade as codebase grows without DI. |
| Testing | Currently zero tests. Writing tests without DI framework is painful but possible. |
| New feature integration | The trainer platform is a greenfield addition that does not conflict with existing code. |
| Risk | Low. No migration risk. No user data lost. |

**Verdict for Option A:** Viable for Phase 0 and Phase 1. Will accumulate DI debt. Not ideal beyond 6 months without adding Hilt.

---

### Option B — Rebuild from Scratch

**What this means:** Create a new Android project. Re-implement everything with Hilt, proper repository patterns, Firestore rules, and exercise-specific AI from day one.

| Factor | Assessment |
|---|---|
| Code quality | Would be better from the start. |
| Existing feature reuse | The domain logic (PoseAnalyzer, RepCounter, ExerciseLibrary, WorkoutScoreEngine) can be moved across with minimal modification. The Compose UI screens can be copied. Firebase integration restarts. |
| Development time | 3–4 months to reach feature parity with current state. Then build forward. This is a minimum 4-month delay. |
| Risk | High. Rebuilds routinely take longer than estimated. Features that work today may regress during rebuild. |
| Justification | A rebuild is justified when: existing architecture cannot accommodate required features without fundamental restructuring; existing code contains deep systemic bugs; the team needs to change platform or language. None of these apply here. |
| Cost | 4× the cost of Option C for equivalent outcomes. |

**Verdict for Option B:** Not recommended. The existing code does not have architectural problems that block the required features. The domain logic is the most valuable part of the codebase and is already portable.

---

### Option C — Hybrid Approach (Recommended)

**What this means:** Preserve all working features. Fix all production blockers. Refactor the data layer incrementally. Add Hilt as new ViewModels are created. Build trainer platform as a new vertical. Do not touch working UI screens unnecessarily.

| Factor | Assessment |
|---|---|
| Time to first public release | 6–8 weeks (Phase 0) |
| Time to trainer platform | Phase 2, approximately 4–5 months after Phase 0 |
| Code quality trajectory | Improves over time as new code uses Hilt; old code untouched |
| Feature preservation | All existing working features remain |
| Risk | Low. No migration. Firestore rules added without data loss. |
| DI migration | Introduce Hilt starting with Phase 1 new screens. Existing ViewModels can be migrated screen by screen. |
| Testing | New features get proper unit tests. Existing features get tests over time. |
| Firebase migration | Add Crashlytics, Storage, FCM, App Check as new dependencies — zero disruption to existing features. |

**Specific hybrid decisions:**

| Component | Action |
|---|---|
| `PoseAnalyzer.kt`, `RepCounter.kt`, `WorkoutScoreEngine.kt` | Keep and enhance |
| `WorkoutScreen.kt`, `ProgressScreen.kt` | Keep as-is |
| All Compose UI screens | Keep as-is |
| `FirestoreCommunityDataSource.kt` | Keep, add pagination, fix member count bug |
| `FitnessRepository.kt`, `CommunityRepository.kt` | Keep, add Hilt injection progressively |
| `FitnessDatabase.kt` | Keep, add `exportSchema = true`, add migrations |
| `FirebaseConfig.kt` | Delete — replace with google-services plugin only |
| Community posts | Move from Room to Firestore (migration required) |
| Trainer platform | Build entirely new as a new package `ui/trainer/` |
| Nutrition | Build entirely new as a new package `ui/nutrition/` |
| Push notifications | Add FCM as a new dependency and service |

---

## 6. FINAL ARCHITECTURAL RECOMMENDATION

**Recommendation: Option C — Hybrid Approach**

**Reasoning:**

1. The workout AI pipeline is the core product differentiator and it works. `PoseAnalyzer`, `RepCounter`, and `WorkoutScoreEngine` are correct and performant. They do not need rebuilding. They need enhancement.

2. The Firestore community platform (servers, channels, messages) is functional. It would take months to rebuild to the same level of functionality with real-time listeners, proper channel management, and working friend requests.

3. The architectural weaknesses (no Hilt, no tests, manual DI) are real but they do not block feature development. They are addressable incrementally. A rebuild would recreate them unless there is a disciplined engineer enforcing architecture from day one — and that same discipline can be applied to the existing codebase.

4. The trainer platform is new development regardless of which option is chosen. Building it on top of the existing stable Firebase + Room foundation is lower risk than building it on a freshly written foundation that has not been tested.

5. The estimated cost saving compared to a full rebuild is at least 3–4 months of engineering time. For a startup at this stage, that time difference is the difference between being in market and still building.

**The hybrid approach means the team can:**
- Fix Phase 0 blockers and submit to Play Store within 6–8 weeks
- Deliver a real AI workout tracking MVP in Phase 1
- Launch the trainer platform in Phase 2 without having wasted Phase 0–1 time on a rebuild

---

## 7. COMPLETE FEATURE GAP ANALYSIS

### Gaps Blocking Public Release (Phase 0)

| Gap | Severity | File |
|---|---|---|
| Firebase API key in source code | Critical | `FirebaseConfig.kt` |
| App ID and name are "Pravin" | Critical | `build.gradle.kts`, `strings.xml` |
| No Firestore Security Rules | Critical | Firebase Console |
| Keystore missing / "changeme" fallback | Critical | `build.gradle.kts` |
| No account deletion | Critical (compliance) | Missing |
| No Crashlytics | High | Missing dependency |
| No ProGuard rules | High | `proguard-rules.pro` |
| No backup rules | High | `backup_rules.xml` |
| No privacy policy | High | Missing |
| TURN server is public/untrusted | High | `WebRtcManager.kt` |

### Gaps Blocking MVP Quality (Phase 1)

| Gap | Severity |
|---|---|
| Community posts are local-only | High |
| No message pagination | Medium |
| No email verification | Medium |
| `sessionScoreHistory` unbounded | Medium |
| Member count not decremented on leave | Low |
| No FCM notifications | Medium |
| Rep-level form classification | High (differentiator) |
| Exercise-specific joint validation | High (differentiator) |

### Gaps Blocking Trainer Platform (Phase 2)

| Gap | Severity |
|---|---|
| No trainer role in authentication | Critical |
| No trainer registration flow | Critical |
| No client-trainer Firestore relationship model | Critical |
| No workout assignment system | Critical |
| No trainer dashboard screen | Critical |
| No alert engine | Critical |
| No client status/risk engine | Critical |
| No per-rep form data storage | High |
| No Cloud Functions for triggered notifications | High |

### Gaps Blocking Nutrition System (Phase 3)

| Gap | Severity |
|---|---|
| No Firebase Storage dependency | Critical |
| No meal tracking UI | Critical |
| No food recognition API integration | Critical |
| No nutrition history screen | High |
| No daily summary | High |
| No meal reminder (FCM) | Medium |

---

## 8. FUTURE TECHNICAL ARCHITECTURE

### 8.1 Architecture Overview

The architecture follows the principle: **compute on device, store events in the cloud, notify intelligently.**

```
┌────────────────────────────────────────────────────────────┐
│                     Android Application                     │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │   UI Layer  │  │ Domain Layer │  │    Data Layer      │ │
│  │  (Compose)  │  │  (pure KT)   │  │ (Room + Firestore) │ │
│  └──────┬──────┘  └──────┬───────┘  └────────┬───────────┘ │
│         │                │                    │             │
│  ┌──────▼────────────────▼────────────────────▼──────────┐  │
│  │              ViewModel (Hilt-injected)                 │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌────────────────┐  ┌──────────────────────────────────┐   │
│  │   CameraX +    │  │         MediaPipe                 │   │
│  │   Image Proxy  │  │   PoseLandmarker (ARM, LIVE)      │   │
│  └────────────────┘  └──────────────────────────────────┘   │
└─────────────────────────────┬──────────────────────────────┘
                              │ Structured workout events only
                              │ (NOT camera frames)
┌─────────────────────────────▼──────────────────────────────┐
│                      Firebase Backend                       │
│  ┌──────────────┐  ┌─────────────┐  ┌────────────────────┐  │
│  │ Firebase Auth│  │  Firestore  │  │  Cloud Functions   │  │
│  └──────────────┘  └─────────────┘  └────────────────────┘  │
│  ┌──────────────┐  ┌─────────────┐  ┌────────────────────┐  │
│  │   Firebase   │  │     FCM     │  │     App Check      │  │
│  │   Storage    │  │  (alerts)   │  │   (abuse guard)    │  │
│  └──────────────┘  └─────────────┘  └────────────────────┘  │
│  ┌──────────────┐  ┌─────────────┐                          │
│  │ Crashlytics  │  │  Analytics  │                          │
│  └──────────────┘  └─────────────┘                          │
└────────────────────────────────────────────────────────────┘
```

### 8.2 On-Device vs Cloud Responsibilities

**Runs entirely on device (never uploaded):**
- Camera frames
- Raw pose landmark coordinates
- Per-frame score calculations
- Rep detection logic
- Fatigue estimates

**Uploaded as structured events:**
- Completed workout session summary (exercise, duration, reps, scores)
- Per-rep form assessment (exercise, rep number, form score, error type, severity)
- Form alerts (pattern-based, not per-frame)
- Meal photo (user-initiated, explicit consent)

**Stored in Firestore and visible to trainer:**
- Workout session records (when client grants trainer access)
- Rep-level form summaries
- Consistency indicators
- Alert events
- Trainer notes

**Rationale:** On-device processing respects user privacy, reduces Firestore costs, removes network latency from the workout experience, and avoids the technical impossibility of uploading 30fps camera frames.

### 8.3 Dependency Injection (Hilt)

New ViewModels, repositories, and data sources introduced in Phase 1 and beyond should use Hilt. Existing ViewModels can be migrated one screen at a time. The `@AndroidEntryPoint` pattern works with Compose.

### 8.4 Firebase Services — Responsibilities

| Service | Purpose |
|---|---|
| Firebase Auth | Identity. Email/password + Google Sign-In. Trainer vs user role stored in Firestore user document. |
| Cloud Firestore | All cloud data: sessions, community, trainer-client, alerts, notifications. |
| Firebase Storage | Meal photos, profile pictures, server icons. |
| Cloud Functions | Triggered on Firestore writes: send FCM when new alert created; calculate daily client summaries; clean up stale WebRTC docs. |
| FCM | Deliver alerts to trainers. Deliver workout/meal reminders to clients. |
| App Check | Prevent API abuse from non-app clients. |
| Crashlytics | Catch all production crashes automatically. |
| Analytics | Track retention, feature usage, funnel metrics. |

### 8.5 Firestore Data Model (Future)

```
users/{userId}
  - name, email, role (user | trainer), avatarUrl, isPublic

userProfiles/{userId}
  - age, height, weight, gender, fitnessLevel, bio
  - (synced from Room on update)

workoutSessions/{sessionId}
  - userId, exercise, duration, repCount, totalScore, timestamp
  - (written by Cloud Function trigger or direct client write)

repFormEvents/{repId}
  - sessionId, userId, exercise, repNumber, formScore,
    errorType, errorSeverity, confidence, timestamp

trainerProfiles/{trainerId}
  - displayName, bio, specialisation, certifications, avatarUrl
  - maxClients (50)

clientAssignments/{assignmentId}
  - trainerId, clientId, status (pending | active | ended)
  - assignedAt, workoutPlan, notes

trainerAlerts/{alertId}
  - trainerId, clientId, alertType, severity
  - title, body, evidence, timestamp, acknowledged

clientStatusSnapshots/{clientId}
  - trainerId, workoutConsistency, nutritionConsistency,
    formQuality, engagementScore, dropoutRisk, lastUpdated

servers/{serverId}  (existing)
channels/{channelId}  (existing)
messages/{messageId}  (existing + pagination)
posts/{postId}  (move from Room to Firestore)
```

### 8.6 Offline Behaviour

- Enable Firestore offline persistence from Phase 1
- Room remains the source of truth for workout sessions (always local-first)
- Workout sessions written to Room immediately; synced to Firestore when online
- Community messages and posts require connectivity; show appropriate empty state when offline

---

## 9. EXERCISE-SPECIFIC AI DESIGN

### 9.1 Design Principles

The current `RepCounter.kt` uses configurable angle thresholds per exercise and works correctly for counting. The next level of improvement is exercise-specific joint validation: checking not just whether a rep was completed but whether it was performed correctly.

**This does not require a new AI model.** MediaPipe PoseLandmarker provides 33 landmarks with x, y, z coordinates and visibility confidence. All form analysis is computed geometry — angles, distances, alignment checks — applied to these coordinates.

**Important limitations to document:**
- Landmark detection quality depends on camera angle, lighting, and clothing
- Some landmarks (depth z-coordinate) are less reliable than x, y
- Visibility confidence below 0.5 should mark that landmark as unreliable
- These limitations must be communicated to users and trainers — this is not medical-grade analysis
- The system should never make absolute safety claims based on pose data alone

### 9.2 MediaPipe Landmark Reference

Key landmark indices used across exercises:

| Index | Name | Used in |
|---|---|---|
| 0 | Nose | Reference |
| 11 | Left shoulder | All exercises |
| 12 | Right shoulder | All exercises |
| 13 | Left elbow | Push-up, curl, press |
| 14 | Right elbow | Push-up, curl, press |
| 15 | Left wrist | Push-up, curl, press |
| 16 | Right wrist | Push-up, curl, press |
| 23 | Left hip | All exercises |
| 24 | Right hip | All exercises |
| 25 | Left knee | Squat, lunge |
| 26 | Right knee | Squat, lunge |
| 27 | Left ankle | Push-up, squat, lunge |
| 28 | Right ankle | Push-up, squat, lunge |

### 9.3 Joint Angle Calculation

```kotlin
// Proposed: domain/geometry/AngleCalculator.kt
object AngleCalculator {
    fun angleBetweenThreePoints(
        a: NormalizedLandmark,  // e.g. hip
        b: NormalizedLandmark,  // e.g. knee (vertex)
        c: NormalizedLandmark   // e.g. ankle
    ): Double {
        val radians = atan2(c.y() - b.y(), c.x() - b.x()) -
                      atan2(a.y() - b.y(), a.x() - b.x())
        var angle = abs(Math.toDegrees(radians))
        if (angle > 180.0) angle = 360.0 - angle
        return angle
    }

    fun isLandmarkVisible(landmark: NormalizedLandmark, threshold: Float = 0.5f): Boolean =
        landmark.visibility().getOrDefault(0f) >= threshold
}
```

### 9.4 Squat — Exercise AI Specification

**Required landmarks:** Left/right hip (23, 24), left/right knee (25, 26), left/right ankle (27, 28), left/right shoulder (11, 12)

**Joint angles:**
- Primary: Knee angle = angle(hip, knee, ankle)
- Secondary: Torso angle = angle(shoulder, hip, knee)

**Rep phases:**
- `STANDING`: knee angle > 160°
- `DESCENDING`: knee angle decreasing, 100–160°
- `BOTTOM`: knee angle < 100°
- `ASCENDING`: knee angle increasing after bottom

**Rep completion condition:** Full cycle from STANDING → BOTTOM → STANDING, with knee angle reaching below 100°.

**Form checks:**

| Check | Condition | Error Code | Severity |
|---|---|---|---|
| Knee valgus (collapse) | Left knee x significantly inside left ankle x, or right knee x outside right ankle x | KNEE_VALGUS | HIGH |
| Insufficient depth | Bottom knee angle > 115° | INSUFFICIENT_DEPTH | MEDIUM |
| Torso lean | Torso angle < 45° at bottom | EXCESSIVE_FORWARD_LEAN | MEDIUM |
| Knee past toes | Knee x well ahead of toe x | KNEES_FORWARD | LOW |

**Configurable thresholds (proposed defaults, must be calibrated):**

```kotlin
data class SquatThresholds(
    val repBottomAngle: Float = 100f,         // below this = bottom of rep
    val repTopAngle: Float = 160f,            // above this = standing
    val depthMinAngle: Float = 115f,          // below this = adequate depth
    val torsoLeanMinAngle: Float = 45f,       // above this = acceptable torso
    val kneeValgusTolerancePx: Float = 0.03f  // normalised x distance
)
```

**Camera requirement:** Side view (90° to movement direction) preferred for depth; front view acceptable for knee valgus. Both are useful; neither is perfect alone.

**Known limitations:** Depth is difficult to measure accurately from front-facing camera (limited z-axis reliability). Knee valgus detection works better from front view. When camera angle is unknown, confidence should be lowered.

### 9.5 Push-Up — Exercise AI Specification

**Required landmarks:** Left/right shoulder (11, 12), left/right elbow (13, 14), left/right wrist (15, 16), left/right hip (23, 24), left/right ankle (27, 28)

**Joint angles:**
- Elbow angle = angle(shoulder, elbow, wrist)
- Body alignment angle = angle(shoulder, hip, ankle)

**Rep phases:**
- `UP`: elbow angle > 160°
- `DESCENDING`: elbow angle decreasing
- `BOTTOM`: elbow angle < 90°
- `ASCENDING`: elbow angle increasing after bottom

**Form checks:**

| Check | Condition | Error Code | Severity |
|---|---|---|---|
| Hip sag | Hip y significantly above shoulder–ankle line | HIP_SAG | HIGH |
| Hip elevation | Hip y significantly below shoulder–ankle line | HIP_PIKE | MEDIUM |
| Insufficient depth | Bottom elbow angle > 105° | INSUFFICIENT_DEPTH | MEDIUM |
| Head position | Nose position relative to shoulder (neck extension) | HEAD_FORWARD | LOW |

**Camera requirement:** Side view strongly preferred. Front view provides limited information for push-up form.

### 9.6 Bicep Curl — Exercise AI Specification

**Required landmarks:** Shoulder (11 or 12), elbow (13 or 14), wrist (15 or 16) — typically one arm at a time. Both sides for bilateral.

**Primary angle:** Elbow angle = angle(shoulder, elbow, wrist)

**Rep phases:**
- `DOWN`: elbow angle > 150°
- `CURLING`: angle decreasing
- `TOP`: elbow angle < 40°
- `LOWERING`: angle increasing after top

**Form checks:**

| Check | Condition | Error Code | Severity |
|---|---|---|---|
| Shoulder rotation | Shoulder x/y displacement during rep | SHOULDER_MOVEMENT | MEDIUM |
| Torso sway | Change in shoulder position relative to hip during rep | TORSO_SWAY | MEDIUM |
| Incomplete extension | Bottom elbow angle < 140° | INCOMPLETE_EXTENSION | LOW |
| Incomplete curl | Top elbow angle > 55° | INCOMPLETE_CURL | LOW |

**Camera requirement:** Side view for elbow angle accuracy. Front view can detect bilateral asymmetry.

### 9.7 Shoulder Press — Exercise AI Specification

**Required landmarks:** Shoulder (11, 12), elbow (13, 14), wrist (15, 16), hip (23, 24) for back arch

**Primary angle:** Elbow angle = angle(shoulder, elbow, wrist)

**Rep phases:**
- `DOWN`: elbow at or below shoulder height, elbow angle ~90°
- `PRESSING`: angle increasing
- `TOP`: elbow fully extended, angle > 160°
- `LOWERING`: angle decreasing

**Form checks:**

| Check | Condition | Error Code | Severity |
|---|---|---|---|
| Excessive back arch | Hip forward of shoulder alignment | BACK_ARCH | HIGH |
| Uneven press | Left/right wrist y-coordinates significantly different | ASYMMETRIC_PRESS | MEDIUM |
| Incomplete extension | Top elbow angle < 150° | INCOMPLETE_EXTENSION | LOW |
| Head forward | Nose forward of shoulders at top | HEAD_FORWARD | LOW |

### 9.8 Lunge — Exercise AI Specification

**Required landmarks:** Left/right hip (23, 24), left/right knee (25, 26), left/right ankle (27, 28), shoulders for torso upright

**Primary angle:** Front knee angle = angle(hip, knee, ankle) on leading leg

**Rep phases:**
- `STANDING`: both knees at ~160°
- `STEPPING`: one foot forward
- `BOTTOM`: front knee angle 80–90°
- `RETURNING`: returning to standing

**Form checks:**

| Check | Condition | Error Code | Severity |
|---|---|---|---|
| Front knee over toe | Knee x past ankle x significantly | KNEE_OVER_TOE | MEDIUM |
| Torso lean | Shoulders not upright over hips | TORSO_LEAN | MEDIUM |
| Balance loss | Rapid lateral hip displacement | BALANCE | LOW |
| Insufficient depth | Bottom front knee angle > 110° | INSUFFICIENT_DEPTH | LOW |

### 9.9 Proposed Implementation: ExerciseFormAnalyzer

```kotlin
// Proposed: domain/formanalysis/ExerciseFormAnalyzer.kt

sealed class FormError(
    val code: String,
    val message: String,
    val severity: ErrorSeverity
) {
    data class KneeValgus(val confidence: Float) :
        FormError("KNEE_VALGUS", "Keep your knees aligned over your toes", ErrorSeverity.HIGH)
    data class HipSag(val confidence: Float) :
        FormError("HIP_SAG", "Keep your hips aligned with your shoulders", ErrorSeverity.HIGH)
    data class TorsoSway(val confidence: Float) :
        FormError("TORSO_SWAY", "Avoid swinging your body", ErrorSeverity.MEDIUM)
    data class InsufficientDepth(val achievedAngle: Float) :
        FormError("INSUFFICIENT_DEPTH", "Try to go a little deeper", ErrorSeverity.MEDIUM)
    data class IncompleteExtension(val achievedAngle: Float) :
        FormError("INCOMPLETE_EXTENSION", "Complete the full range of motion", ErrorSeverity.LOW)
}

enum class ErrorSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class RepFormAssessment(
    val repNumber: Int,
    val formScore: Float,          // 0-100
    val errors: List<FormError>,
    val confidence: Float,         // 0-1, based on landmark visibility
    val timestamp: Long
)
```

The `ExerciseFormAnalyzer` interface should be implemented per exercise:
- `SquatFormAnalyzer`
- `PushUpFormAnalyzer`
- `BicepCurlFormAnalyzer`
- `ShoulderPressFormAnalyzer`
- `LungeFormAnalyzer`

Each analyzer receives the full set of landmarks for a completed rep and returns a `RepFormAssessment`.

---

## 10. REP-LEVEL FORM ANALYSIS SYSTEM

### 10.1 Data Model

Each completed repetition should produce a record that is stored locally in Room and, for trainer-assigned clients, synced to Firestore.

**Proposed Room entity:**
```
RepFormRecord
  id: Long (autoincrement)
  sessionId: Long (FK to WorkoutSession)
  userId: String
  exerciseType: String
  repNumber: Int
  formScore: Float
  primaryError: String? (error code, nullable)
  errorSeverity: String? (LOW/MEDIUM/HIGH/CRITICAL)
  landmarkConfidence: Float
  timestamp: Long
```

**Proposed Firestore document (trainer-accessible):**
```
repFormEvents/{userId}_{sessionId}_{repNumber}
  userId, sessionId, exercise, repNumber,
  formScore, primaryError, errorSeverity, confidence, timestamp
```

### 10.2 Pattern Detection

A single incorrect rep does not constitute a form problem. The system must detect patterns.

**Alert generation rules (proposed, must be calibrated per exercise):**

```
Consecutive error rule:
  If same error code appears in 3+ consecutive reps with HIGH severity:
    → generate FORM_PATTERN_ALERT

Proportion rule:
  If same error code appears in > 40% of reps in a set (min 5 reps):
    → generate FORM_PATTERN_ALERT

Safety rule:
  If CRITICAL severity error is detected in any rep:
    → generate FORM_SAFETY_ALERT immediately

Confidence gate:
  If landmark confidence < 0.4 for the relevant joint:
    → do not count this frame's assessment toward pattern detection
    → note: "detection unreliable for this rep"
```

These thresholds are proposals. They must be validated with real users performing real exercises. Different exercises will require different calibration.

### 10.3 Real-Time Client Feedback

When a form pattern is detected during a live workout, the client should see an immediate notification on screen.

Examples:
- "Keep your hips level — they're sagging on the way down"
- "Good depth on those last 3 reps"
- "Slow down — your reps are becoming faster but shallower"

This feedback is generated locally on the device and does not require a network connection.

### 10.4 Trainer-Facing Form Summary

After a session is uploaded, the trainer should see:
- Total reps: 24
- Form score: 78 / 100
- Main issue: Hip alignment (8 of 24 reps affected)
- Most recent 3 reps: all correct
- Severity: Medium

This is more useful than raw data.

---

## 11. INTELLIGENT ALERT ENGINE

### 11.1 Design Principles

The alert engine must not be a notification spam machine. A trainer managing 50 clients cannot process 50 individual notifications per day. The system must:

1. Group related alerts together
2. Suppress duplicates within a configurable window
3. Prioritise by severity and actionability
4. Distinguish real issues from data gaps

**Critical principle:** Missing data does not prove poor behaviour. A missing meal update could mean: the client forgot to log, the app crashed, they had an unusual day, or they are disengaging. The trainer should see the observation ("no meal updates for 2 days") without the system concluding "client is not eating properly."

### 11.2 Alert Types

| Alert Type | Trigger | Delivery | Priority |
|---|---|---|---|
| `FORM_SAFETY` | CRITICAL severity form error detected | Immediate FCM | P0 |
| `FORM_PATTERN` | 3+ consecutive same error, HIGH severity | End of workout | P1 |
| `MISSED_WORKOUT` | Scheduled workout not completed by end of day | Daily digest | P2 |
| `REPEATED_MISS` | 2+ missed workouts in 7 days | Daily digest | P1 |
| `NO_MEAL_LOG` | No meal entries for configured period | Daily digest | P2 |
| `CONSISTENCY_DECLINE` | Rolling 7-day workout rate < 50% of target | Weekly digest | P2 |
| `ENGAGEMENT_DECLINE` | App opens declining over 2 weeks | Weekly digest | P3 |
| `DROPOUT_RISK` | Composite score crosses threshold | Weekly digest | P2 |

### 11.3 Alert Deduplication

```
Deduplication window per (clientId, alertType): 24 hours
  → Do not create the same alert type for the same client more than once per day

Exception: FORM_SAFETY alerts are never deduplicated
```

### 11.4 Delivery Strategy

**Immediate delivery (FCM to trainer):** Only for P0 alerts (critical safety).

**End-of-workout summary (FCM to trainer):** Form pattern alerts grouped per client per session.

**Daily digest (single FCM to trainer):** All P1–P2 alerts from the day, grouped. One notification, one tap to view summary.

**Weekly digest:** P3 alerts and engagement trends. Delivered Monday morning.

### 11.5 Firestore Alert Schema

```
trainerAlerts/{alertId}
  trainerId: String
  clientId: String
  clientName: String
  alertType: String
  severity: String (P0/P1/P2/P3)
  title: String
  body: String
  evidence: Map<String, Any>  // supporting data
  createdAt: Timestamp
  acknowledged: Boolean
  acknowledgedAt: Timestamp?
  trainerNote: String?
  escalatedAt: Timestamp?     // if not acknowledged within X hours
```

### 11.6 Cloud Function: Alert Creation

Alert creation should happen in a Cloud Function, not in the client app. This prevents:
- Clients generating fake alerts
- Alert storms from app restarts
- Duplicate alerts from multiple devices

```
Trigger: Firestore write to workoutSessions/{sessionId}
Action:
  1. Load client assignment for this user
  2. Check if trainer is assigned
  3. Evaluate form summaries for pattern alerts
  4. Check workout schedule compliance
  5. Create alert documents if thresholds met
  6. Send FCM to trainer if P0 or P1
```

---

## 12. CLIENT STATUS AND RISK ENGINE

### 12.1 Client Health Score Dimensions

Each assigned client should have a continuously updated status snapshot with five dimensions:

| Dimension | What It Measures | Data Source |
|---|---|---|
| Workout Consistency | % of scheduled workouts completed in last 14 days | `workoutSessions` + schedule |
| Nutrition Logging | % of expected meal logs submitted in last 7 days | `mealPosts` in Firestore |
| Form Quality | Average form score across last 5 sessions | `repFormEvents` aggregation |
| Fitness Progress | Trend in performance score over last 30 days | `workoutSessions` trend |
| Engagement | App opens, messages, posts in last 14 days | `Analytics` events |

Each dimension is scored 0–100. The dimensions are displayed individually — they are not combined into a single opaque score that hides which area is problematic.

### 12.2 Dropout Risk Indicator

**This is an indicator, not a diagnosis.**

The dropout risk is calculated as a weighted composite:

```
dropoutRisk = (
  (100 - workoutConsistency) * 0.35 +
  (100 - nutritionLogging) * 0.20 +
  (100 - engagement) * 0.30 +
  (100 - formQuality) * 0.15
) / 100

risk level:
  0.0–0.3  → Low
  0.3–0.6  → Moderate (shown to trainer)
  0.6–0.8  → High (alert generated)
  0.8+     → Very High (escalated alert)
```

The trainer sees **why** the score changed: "Workout consistency dropped from 85% to 40% this week. Last session 9 days ago."

**Limitations to document:**
- Missing data inflates all risk scores
- A user on holiday will appear high-risk
- A user who exercises outside the app will appear inactive
- These are indicators based on in-app data only

### 12.3 Update Frequency

Client status snapshots are recalculated by a Cloud Function:
- On every new `workoutSession` write
- On every new `mealPost` write
- On a scheduled daily job at midnight (catches days with no activity)

---

## 13. TRAINER DASHBOARD DESIGN

### 13.1 Dashboard Overview Screen

The trainer's first screen after login shows a summary panel:

```
┌────────────────────────────────────────────┐
│  My Clients                    [48 clients] │
├────────────────────────────────────────────┤
│  🟢 Active today              12           │
│  🔴 Missed workout            5            │
│  ⚠️  Form issues              3            │
│  📉 Consistency declining     7            │
│  🚨 High-risk                 2            │
├────────────────────────────────────────────┤
│  Alerts today                 8  [View All]│
└────────────────────────────────────────────┘
```

Below this, a client list sorted by urgency (highest risk first). Each client row shows:
- Avatar + name
- Last active
- Consistency badge (green / amber / red)
- Active alert count

### 13.2 Client Detail Screen

Tapping a client shows:

**Tab 1 — Overview**
- Stats row: sessions this week, avg form score, streak, active days
- Risk indicators: 5 dimension bars (0–100)
- Latest workout: exercise, reps, score, timestamp

**Tab 2 — Workouts**
- Workout history with scores
- Form score trend chart
- Rep-level form log for last session
- Common form errors summary

**Tab 3 — Nutrition**
- Meal log history (visible with client permission)
- Daily nutrition summary

**Tab 4 — Progress**
- Performance trend chart
- Exercise-specific scores over time

**Tab 5 — Alerts**
- All alerts for this client, newest first
- Trainer note field per alert

**Tab 6 — Notes**
- Trainer text notes, timestamped
- Assigned workout plan

### 13.3 Trainer Registration Flow

1. Sign up with email
2. Select role: "I am a trainer"
3. Complete trainer profile (name, bio, specialisation, certifications)
4. Set maximum client capacity
5. Generate invite link / code for clients

### 13.4 Client Assignment Flow

**Trainer side:**
1. Tap "Add Client"
2. Search by email or name
3. Send trainer request

**Client side:**
1. Receives notification: "Personal trainer [name] wants to connect with you"
2. Views trainer profile
3. Accepts or declines
4. If accepted: grants trainer access to workout sessions, meal logs, form data

**Privacy principle:** The client decides what the trainer can see. Granular permissions: workout data (on by default), meal data (opt-in), live session access (opt-in).

### 13.5 Firestore Structure for Trainer Platform

```
trainerProfiles/{trainerId}           — trainer metadata
clientAssignments/{assignmentId}      — relationships
  trainerId, clientId, status, grantedPermissions

clientStatusSnapshots/{clientId}      — updated by Cloud Function
  trainerId, workoutConsistency, nutritionLogging,
  formQuality, fitnessProgress, engagement, dropoutRisk

trainerAlerts/{alertId}              — created by Cloud Function
  trainerId, clientId, alertType, severity, acknowledged

trainerNotes/{noteId}                — trainer private notes
  trainerId, clientId, text, timestamp
```

### 13.6 Implementation Approach

- Trainer dashboard is a new screen set in a new package `ui/trainer/`
- Access gated by `role == "trainer"` check on `users/{userId}` Firestore document
- `TrainerDashboardViewModel` reads `clientAssignments` where `trainerId == currentUserId`
- `ClientDetailViewModel` reads `clientStatusSnapshots`, `workoutSessions`, `trainerAlerts` for the selected client
- All Firestore reads must be gated by Security Rules: trainer can only read data for clients where a valid active `clientAssignment` exists

### 13.7 50-Client Capacity Feasibility

**Technical feasibility:** Firestore can support a trainer reading status for 50 clients. With proper indexing:
- One query for `clientAssignments where trainerId == X` (50 documents max)
- 50 `clientStatusSnapshots` fetched by document ID (1 batch get)
- Alerts fetched with `where trainerId == X and acknowledged == false` (indexed)

**Notification feasibility:** With digest batching (daily summary = 1 FCM per trainer), the alert volume remains manageable regardless of client count.

**Target validation:** The claim "one trainer can effectively manage 50 clients" depends on the quality of the alert engine and the trainer's workflow. It is a product design target that must be validated with real trainers. The architecture supports it technically.

---

## 14. NUTRITION SYSTEM DESIGN

### 14.1 On-Device Flow

```
User taps camera icon in Meal Log screen
    → Camera picker (CameraX or system picker)
    → Photo captured
    → Shown to user for confirmation
    → User taps "Log this meal"
    → Photo uploaded to Firebase Storage: meals/{userId}/{timestamp}.jpg
    → Firestore document created: mealPosts/{mealId}
        { userId, imageUrl, timestamp, aiStatus: "pending" }
    → Cloud Function triggered by Storage upload
    → AI API called (Google Vision AI or OpenAI GPT-4V)
    → Firestore document updated:
        { aiStatus: "complete", recognizedFoods: [...], calories, protein, carbs, fat }
    → Client app receives Firestore update, shows nutrition result
```

### 14.2 AI Food Recognition Options

| Option | Accuracy | Cost | Privacy | Notes |
|---|---|---|---|---|
| Google Cloud Vision AI (Food) | Moderate | ~$0.0015/image | Image sent to Google | Recognises food categories, not precise macros |
| OpenAI GPT-4V | High | ~$0.01/image | Image sent to OpenAI | Better context understanding; can describe portions |
| Nutritionix API | Moderate | $0.005/call | Text query only | Accurate macros from food name; combine with Vision |
| On-device TFLite food model | Low–Moderate | Free | Private | Limited food categories; poor generalisation |
| Custom fine-tuned model | High | Infrastructure cost | Private | Requires training data; 6–12 months to build |

**Recommended approach for Phase 3:**
- Use GPT-4V (or equivalent) to identify foods from the photo
- Pass the identified food names to Nutritionix or USDA FoodData API for calorie/macro data
- This separates image recognition from nutrition lookup, improving accuracy of macros

**Important disclaimers to surface to users:**
- AI calorie estimates are approximate (±20–30% is typical)
- Portion size estimation from photos is inherently imprecise
- This is not a medical nutrition tool

### 14.3 Meal Data Schema

```
mealPosts/{mealId}
  userId: String
  imageUrl: String
  timestamp: Timestamp
  mealType: String (breakfast | lunch | dinner | snack)
  aiStatus: String (pending | complete | failed)
  recognizedFoods: List<String>
  estimatedCalories: Int?
  estimatedProtein: Float?
  estimatedCarbs: Float?
  estimatedFat: Float?
  userConfirmed: Boolean
  userOverrideCalories: Int?
  trainerVisible: Boolean
```

### 14.4 Privacy

- Meal photos stored in Firebase Storage under `meals/{userId}/`
- Firestore Security Rules: only the owning user and their assigned trainer (with permission) can read
- Photo sent to AI API through Cloud Function — client never calls AI API directly
- User can delete meal posts (Storage + Firestore document)
- Storage retention policy: photos may be automatically deleted after 90 days (cost management)

---

## 15. COMMUNITY SYSTEM DESIGN

### 15.1 Current State

The server/channel/messaging system (Modules C-Community) is functional and Firestore-backed. It works. The main gap is that community posts, reactions, and comments are stored only in Room and never reach Firestore.

### 15.2 Required Changes

**Move posts to Firestore:**
```
posts/{postId}
  serverId, channelId, userId, content, mediaUrls,
  timestamp, editedAt, isDeleted, likeCount
```

This requires a migration: clear all local post data (it was never shared anyway — no user has seen anyone else's posts). This is acceptable because local posts had no cloud existence.

**Move reactions and comments to Firestore:**
```
reactions/{reactionId}
  postId, userId, emoji, timestamp

comments/{commentId}
  postId, userId, content, timestamp, isDeleted
```

### 15.3 Pagination

All Firestore queries in `FirestoreCommunityDataSource.kt` need `.limit()` and cursor-based pagination:
- Messages: `.limit(50)`, `startAfter(lastVisible)` for loading older messages
- Posts: `.limit(20)`, load more on scroll
- Public servers: `.limit(30)`, search uses prefix query with `.startAt()` and `.endAt()`

### 15.4 Challenges and Leaderboards

- Challenges should be created in Firestore (trainer or server admin creates)
- Participation tracked in Firestore
- Leaderboard is a Firestore query ordered by score
- XP system: every workout session completion grants XP, stored in `userProfiles/{userId}.totalXp`

---

## 16. REAL-TIME FITNESS ARCHITECTURE

### 16.1 Current WebRTC Assessment

The existing WebRTC implementation in `WebRtcManager.kt` uses Stream WebRTC Android SDK with Firestore as the signaling channel. The signaling logic is functional. The critical problem is the TURN server.

| Component | Status |
|---|---|
| ICE/STUN/TURN config | Public relay — must replace |
| Signaling (Firestore) | Working |
| Host/guest role assignment | Working |
| Camera/audio toggle | Working |
| Pose analysis on video | Working (every 6th frame) |
| Rep count sync | Working (Firestore subcollection) |
| Reconnection | Minimal — no ICE restart |
| Cleanup of stale documents | Missing |

### 16.2 TURN Server Options

| Provider | Monthly Cost (estimate) | Reliability | Setup |
|---|---|---|---|
| Self-hosted Coturn (VPS) | $5–20/month server + traffic | High | Requires DevOps |
| Twilio TURN | ~$0.40/GB relayed + $0.10/session | High | Zero DevOps |
| Metered.ca | Free tier (1TB/mo) → paid | High | Zero DevOps |
| Daily.co / Agora | Higher | Very high | Managed SDK |

**Recommendation for Phase 0:** Replace the public `openrelayproject` with a Metered.ca account (free tier is sufficient for early testing). Generate private credentials. This is a configuration change in `WebRtcManager.kt`, not a code rewrite.

**Recommendation for Phase 4:** Evaluate switching from custom WebRTC to Agora or Daily.co if group sessions (3+ participants) are required. Managing WebRTC manually at scale is engineering-intensive.

### 16.3 Reconnection Logic

Add ICE restart capability:
- On `PeerConnection.IceConnectionState.DISCONNECTED` → start 30-second reconnect timer
- On timeout → call `peerConnection.restartIce()` and re-offer
- Notify both parties of reconnection attempt

### 16.4 Document Cleanup

Cloud Function: every 24 hours, delete `live_calls` documents where `startedAt < now - 1 hour`. This prevents Firestore cost accumulation from abandoned calls.

---

## 17. SECURITY AND PRIVACY REQUIREMENTS

### 17.1 Firestore Security Rules — Phase 0 Requirements

The minimum rules required before any public release:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users can read/write their own document only
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }

    // Public servers — read by all authenticated users, write only by owner
    match /servers/{serverId} {
      allow read: if request.auth != null && resource.data.isPublic == true;
      allow write: if request.auth.uid == resource.data.ownerId;
    }

    // Messages — read by channel members, write only by authenticated user for their own
    match /messages/{messageId} {
      allow read: if request.auth != null;
      allow create: if request.auth.uid == request.resource.data.senderId;
      allow update, delete: if request.auth.uid == resource.data.senderId;
    }

    // Trainer alerts — only the trainer can read
    match /trainerAlerts/{alertId} {
      allow read, write: if request.auth.uid == resource.data.trainerId;
    }

    // Client status — trainer can read if assignment exists
    match /clientStatusSnapshots/{clientId} {
      allow read: if request.auth.uid == clientId ||
        exists(/databases/$(database)/documents/clientAssignments/$(request.auth.uid + "_" + clientId));
    }
  }
}
```

These are starting rules. They must be tested with the Firebase Emulator before deployment. More granular rules are required for channels, members, and trainer-client relationships.

### 17.2 Authentication Security

| Requirement | Priority |
|---|---|
| Email verification required before full access | P1 |
| Account deletion (Firebase Auth + Firestore + Storage + Room) | P0 (compliance) |
| Re-authentication before account deletion | P1 |
| Guest users cannot access trainer features | P0 |
| Guest users cannot access community server writes | P1 |
| No credentials hardcoded in source | P0 |

### 17.3 Data Privacy

- Camera frames are never uploaded
- MediaPipe processes frames entirely on-device
- Pose landmark coordinates are not stored
- Only structured workout events (scores, rep counts, form assessments) leave the device
- Meal photos are explicitly user-initiated uploads
- Users must be clearly informed what data is shared with their trainer
- Account deletion must remove: Firebase Auth, all Firestore documents owned by user, all Storage files owned by user
- Android backup rules must exclude Room database and DataStore (user session data)

### 17.4 AI Limitations Disclosure

The following limitations must be disclosed to users and trainers:

- "Exercise form analysis is based on camera-based pose estimation and may be inaccurate depending on lighting, camera angle, and clothing."
- "Rep counting and form scoring are not medical assessments."
- "Calorie and nutrition estimates are approximate and should not replace professional dietary advice."
- "Dropout risk indicators are predictions based on in-app data only and may not reflect the user's actual situation."

These disclosures belong in the app's privacy policy and in onboarding.

### 17.5 Play Store Compliance

| Requirement | Status |
|---|---|
| Account deletion | Must implement before launch |
| Privacy policy URL | Must create and link |
| Data Safety form | Must complete in Play Console |
| Sensitive permissions rationale | CAMERA — justified; must explain in store listing |
| Crashlytics consent (EU) | Consider Firebase's data processing terms |

---

## 18. COST AND SCALABILITY ASSESSMENT

### Assumptions
- Average session: 30 minutes, 1 session/day/active user
- Trainer: manages 30 active clients on average
- Community: 20% of users join at least one server
- Nutrition: 30% of users log at least one meal per day
- Live workout: 5% of users use weekly
- All user counts are ACTIVE users (DAU), not downloads

### 18.1 Firebase Costs at Scale

| Scale | Firestore Reads/day | Firestore Writes/day | Storage | FCM | AI (food) | TURN | Monthly Est. |
|---|---|---|---|---|---|---|---|
| 100 DAU | ~200K | ~50K | < 1 GB | Free | $0.50 | Free (Metered) | < $5 |
| 1,000 DAU | ~2M | ~500K | ~5 GB | Free | $5 | Free–$10 | ~$30–60 |
| 10,000 DAU | ~20M | ~5M | ~50 GB | ~$5 | $50 | $50–200 | ~$250–600 |
| 50,000 DAU | ~100M | ~25M | ~250 GB | ~$25 | $250 | $250–1,000 | ~$1,500–3,000 |

**Cost optimization flags:**
- At 10,000 DAU, Firestore read cost is the primary driver. Message pagination is essential.
- Food AI cost scales with meal log volume. Cache results by image hash for duplicate meals.
- TURN cost scales with live workout usage. Consider restricting live workouts to verified users.
- At 50,000 DAU, consider enabling Firestore offline persistence to reduce reads per session.

### 18.2 Firebase Is Appropriate Until...

Do NOT migrate away from Firebase based on user count alone. Migrate when:
- Monthly Firebase bill exceeds $3,000 and a self-hosted alternative provides clear savings
- Query requirements exceed Firestore's capabilities (complex joins, full-text search)
- Trainer analytics require aggregations that Firestore cannot serve efficiently

At current scale, none of these conditions apply. Firebase is the right choice.

### 18.3 AI Inference Costs

MediaPipe on-device: **$0.00 per inference**. This is the correct approach and is already implemented.

Food recognition via GPT-4V: **$0.01–0.02 per meal photo**. At 300 active nutrition users logging 2 meals/day = $6/day = ~$180/month at 10K DAU. Manageable.

---

## 19. RELEASE-WISE DEVELOPMENT ROADMAP

---

### PHASE 0 — FOUNDATION AND PRODUCTION READINESS

**Objective:** Fix all production blockers. Make the existing working features safe to release publicly.

**Duration estimate:** 4–6 weeks (1–2 engineers)

**Features:**
- Rotate Firebase API key; remove `FirebaseConfig.kt`; rely solely on google-services.json
- Rename app: application ID `com.fitai.app`, app name "Fit AI", update all string references
- Generate proper release keystore; store securely; configure CI variables
- Remove "changeme" fallback from `build.gradle.kts`
- Write and deploy Firestore Security Rules (minimum set)
- Add Firebase Crashlytics
- Add Firebase App Check (Play Integrity)
- Design and add custom app icon
- Add ProGuard keep rules (MediaPipe, WebRTC, Room, Firebase)
- Fix Android backup rules (exclude Room DB and DataStore)
- Implement account deletion (Firebase Auth + Firestore + DataStore + Room)
- Add privacy policy URL to onboarding / sign-up screen
- Replace public TURN with private credentials (Metered.ca free tier)
- Paginate Firestore message queries (`.limit(50)`, cursor)
- Fix `memberCount` decrement bug in `leaveServer()`
- Cap `sessionScoreHistory` to rolling 500 entries
- Gate all Firestore calls on `FirebaseAuth.currentUser != null`
- Add email verification after sign-up

**Existing modules to reuse:** Everything.  
**New components required:** Account deletion screen, Firestore rules file, ProGuard rules.  
**Backend changes:** Deploy Firestore Security Rules. Enable App Check in Firebase Console.  
**Testing requirements:** Write unit tests for `ConsistencyTracker`, `RepCounter`, `AuthRepository` error mapping. Test Firestore rules with Firebase Emulator.  

**Definition of Done:**
- Release APK signed with production keystore
- Crashlytics showing no critical crashes in test build
- Firestore Security Rules deployed and tested
- Privacy policy published and linked
- Account deletion functional end-to-end
- All P0 items from `FIT_AI_PRODUCTION_AUDIT.md` resolved

**Release criteria:** Internal testing only. Not yet public.

---

### PHASE 1 — PUBLIC FITNESS MVP

**Objective:** Ship a high-quality workout tracking app with community features. This is the first version real users will experience.

**Duration estimate:** 6–10 weeks (2 engineers)

**Features:**

*Workout AI (enhancement):*
- Implement `ExerciseFormAnalyzer` per-exercise joint validation (squats, push-ups, bicep curls, shoulder press, lunges)
- Store `RepFormRecord` in Room per rep
- Show real-time form feedback during active workout
- Show rep-level form breakdown in workout summary
- Migrate `repFormEvents` from local to also synced to Firestore (for later trainer visibility)

*Community (fix):*
- Move community posts from Room to Firestore
- Move reactions and comments to Firestore
- Add pagination to server search and message loading
- Enable Firestore offline persistence

*Profile (fix):*
- Sync user profile to Firestore on sign-up (not just on friend request)
- Add Google Sign-In
- Add profile photo upload (Firebase Storage — profile photos, not workout footage)

*Push notifications:*
- Add FCM dependency
- Implement workout reminder notifications (scheduled by Cloud Function based on user's weekly goal)
- Show notification permission prompt at appropriate time (not on first launch)

*Infrastructure:*
- Add Firebase Analytics event tracking (workout started, workout completed, auth event)
- Enable Firestore offline persistence

**Existing modules to reuse:** All existing screens, all existing domain logic, Room database.  
**New components:** `ExerciseFormAnalyzer` family, `RepFormRecord` Room entity, FCM service, Firebase Storage integration, new Firestore collections for posts/reactions/comments.  
**Backend changes:** New Firestore collections, Cloud Functions for workout reminders, Storage rules.  
**Testing requirements:** Unit tests for all new `FormAnalyzer` classes. Integration tests for post/reaction sync. Instrumented test for sign-up → onboarding → workout → summary flow.  

**Definition of Done:**
- All 5 exercise form analyzers implemented and manually tested
- Posts appear on other users' devices in real time
- Workout reminders delivered reliably
- No P0 or P1 crashes in Crashlytics for 2 weeks of testing

**Release criteria:** Public Play Store release (early access or full release).

---

### PHASE 2 — INTELLIGENT TRAINER PLATFORM

**Objective:** Launch the trainer-client relationship system. This is the primary business differentiator.

**Duration estimate:** 10–14 weeks (2–3 engineers)

**Features:**

*Trainer registration:*
- Role selection during sign-up ("I am a trainer" vs "I am training")
- Trainer profile creation screen (name, bio, specialisation, certifications)
- Trainer role stored in Firestore `users/{id}.role`

*Client management:*
- Send client invite by email or link
- Client receives and accepts trainer request
- Client grants trainer access to workout data and form events
- Trainer views list of all assigned clients

*Trainer dashboard:*
- Dashboard overview screen with client summary counters
- Client list sorted by urgency (dropout risk, missed workouts, alerts)
- Client detail screen (6 tabs: overview, workouts, nutrition, progress, alerts, notes)
- Form score trend chart per client
- Rep-level form log per session per client

*Alert engine:*
- Cloud Functions for alert creation (triggered on session write)
- Form pattern alert generation
- Missed workout alert (daily Cloud Function job)
- Alert acknowledgement from trainer dashboard
- Daily digest FCM to trainer

*Client status engine:*
- `clientStatusSnapshots` updated by Cloud Function
- 5-dimension scoring per client
- Dropout risk indicator with reason

*Trainer notes:*
- Trainer can add text notes per client
- Notes stored in `trainerNotes` Firestore collection

*Communication:*
- Trainer can message client directly (extend existing messaging to support direct messages)

**Existing modules to reuse:** Firebase Auth, Firestore infrastructure, navigation, Compose UI patterns, FCM.  
**New components:** All trainer screens, alert engine Cloud Functions, client status Cloud Function, `TrainerDashboardViewModel`, `ClientDetailViewModel`, new Firestore collections.  
**Backend changes:** 5 new Cloud Functions, 6 new Firestore collections, new Security Rules for trainer-client relationships.  
**Testing requirements:** Unit tests for alert generation logic, client status calculation. Integration tests for trainer-client assignment flow. Firestore rules tests for trainer access to client data.  

**Definition of Done:**
- A trainer can sign up, create profile, invite and accept 5 test clients
- Dashboard shows real client data from completed workouts
- Form pattern alerts generated and delivered to trainer
- Daily digest sent at configured time
- Security rules prevent trainer from accessing non-assigned client data

**Release criteria:** Beta with 5–10 real trainers and their clients. Gather feedback before scaling.

---

### PHASE 3 — AI NUTRITION AND ENGAGEMENT

**Objective:** Add the nutrition tracking vertical and enhance engagement mechanics.

**Duration estimate:** 8–12 weeks (2 engineers)

**Features:**

*Nutrition:*
- Meal logging screen with camera
- Firebase Storage upload for meal photos
- Cloud Function → AI API for food recognition and macro estimation
- Meal history screen per user
- Daily nutrition summary (calories, protein, carbs, fat)
- Trainer visibility into client meal logs (with permission)
- Missing meal update alert for trainer

*Engagement:*
- XP system (workout completion, streak, form milestones)
- Global leaderboard (server and friends-level)
- Enhanced achievements (unlockable badges, visible to community)
- Weekly challenge system (trainer-created or community-created)
- In-app badge showcase on profile

*Advanced analytics:*
- AI workout recommendations based on history and gaps
- Weekly performance insights (personalized, generated by Cloud Function)
- Trainer client performance summary reports (weekly PDF or in-app)

**Existing modules to reuse:** Firebase Storage infrastructure (from Phase 1 profile photos), FCM, Firestore, meal model and DAO.  
**New components:** Meal tracking screens, nutrition Cloud Function, food AI integration, XP engine.  
**Backend changes:** Cloud Functions for nutrition AI, food recognition API integration, XP calculation.  

**Definition of Done:**
- User can photograph a meal and receive calorie estimate within 10 seconds
- Trainer can see client meal log in dashboard
- XP is awarded and displayed correctly

**Release criteria:** Feature flag rollout to 20% of users initially.

---

### PHASE 4 — ADVANCED FITNESS ECOSYSTEM

**Objective:** Monetise the platform. Expand live capabilities. Build admin infrastructure.

**Duration estimate:** 12–16 weeks (3 engineers)

**Features:**

*Live workout enhancement:*
- Dedicated TURN server (Coturn or Agora migration)
- ICE restart / reconnection logic
- Group workout sessions (3+ participants) — requires architectural decision (mesh vs SFU)
- Screen share (trainer shows workout demo)

*Subscriptions and payments:*
- Google Play Billing API integration
- Free tier: personal workout tracking, basic community
- Pro tier: AI nutrition, advanced analytics, rep-level form data
- Trainer tier: dashboard, client management, alert engine
- Subscription status gated in Security Rules via Firebase Auth custom claims

*Admin:*
- Trainer verification system
- Content moderation for community posts
- User management
- Analytics dashboard

*Advanced AI:*
- Personalized workout program generation (based on history and goals)
- Injury prevention suggestions (based on form pattern data, clearly labeled as informational only)
- Dynamic difficulty progression recommendations

**Existing modules to reuse:** All of the above.  
**New components:** Billing integration, admin screens, SFU infrastructure (if group video).  
**Backend changes:** Subscription management Cloud Functions, admin Firestore collections, custom auth claims for subscription tier.  

**Definition of Done:**
- Trainer can subscribe and access Pro tier
- Subscription status enforced in Security Rules
- Admin can suspend a user or remove a post

**Release criteria:** Staged rollout. Revenue tracking verified.

---

## 20. PRIORITY MATRIX

| Task | Impact | Effort | Phase | Priority |
|---|---|---|---|---|
| Fix Firebase API key exposure | High | Low | 0 | P0 |
| Rename app from Pravin to Fit AI | High | Low | 0 | P0 |
| Firestore Security Rules | High | Medium | 0 | P0 |
| Account deletion | High | Medium | 0 | P0 |
| Release keystore | High | Low | 0 | P0 |
| Crashlytics | High | Low | 0 | P0 |
| ProGuard rules | High | Low | 0 | P0 |
| Custom app icon | High | Low | 0 | P0 |
| Privacy policy | High | Low | 0 | P0 |
| Replace TURN server | High | Low | 0 | P0 |
| Exercise form analyzers (5 exercises) | Very High | Medium | 1 | P1 |
| Community posts → Firestore | High | Medium | 1 | P1 |
| FCM workout reminders | Medium | Medium | 1 | P1 |
| Message pagination | Medium | Low | 1 | P1 |
| Rep-level form storage | High | Medium | 1 | P1 |
| Trainer registration and profile | Very High | Medium | 2 | P1 |
| Client assignment flow | Very High | High | 2 | P1 |
| Trainer dashboard | Very High | High | 2 | P1 |
| Alert engine (Cloud Functions) | High | High | 2 | P1 |
| Client status engine | High | High | 2 | P1 |
| Meal logging UI | Medium | Medium | 3 | P2 |
| Food AI recognition | Medium | High | 3 | P2 |
| XP and leaderboards | Medium | Medium | 3 | P2 |
| Google Sign-In | Low | Medium | 1 | P2 |
| Group live sessions | Medium | Very High | 4 | P3 |
| Subscriptions (Play Billing) | High | High | 4 | P2 |
| Admin panel | Low | High | 4 | P3 |

---

## 21. TECHNICAL RISKS

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| MediaPipe accuracy issues on real devices | Medium | High | Test on 10+ physical devices before Phase 1 release. Document known device/angle/lighting limitations. |
| Firestore cost explosion from unbounded reads | Medium | High | Implement pagination before Phase 1. Monitor with Firebase cost alerts from day one. |
| WebRTC P2P failure rate (high NAT environments) | Medium | High | Replace public TURN immediately. Monitor connection success rate. |
| Cloud Function cold starts causing delayed alerts | Low | Medium | Keep functions warm with scheduled pings. Use minimum instance configuration. |
| Form analyzer false positives confusing users | High | Medium | Start with low sensitivity thresholds. Tune per exercise with real data. Always show confidence. |
| Food AI inaccuracy complaints | High | Medium | Set explicit disclaimers. Allow user correction of AI estimates. Never claim medical accuracy. |
| Dropout prediction misinterpretation | Medium | High | Always show the reason. Never show a single number without context. Train trainers on limitations. |
| App name change breaking existing users | Low | Medium | The app has no public users yet — safe to rename before first release. |
| Room migration failure after schema changes | Low | High | Enable `exportSchema = true` immediately. Write migration tests. |
| Google Play rejection for missing features | High | High | Account deletion and privacy policy are required — implement before first submission. |

---

## 22. ESTIMATED DEVELOPMENT EFFORT

Estimates assume a team of 2 Android engineers and 1 part-time Firebase/backend engineer.

| Phase | Features | Duration |
|---|---|---|
| Phase 0 — Foundation | 18 blockers + infrastructure | 4–6 weeks |
| Phase 1 — Fitness MVP | Form analysis, community sync, FCM | 6–10 weeks |
| Phase 2 — Trainer Platform | Registration, dashboard, alerts, risk engine | 10–14 weeks |
| Phase 3 — Nutrition | Meal logging, food AI, engagement | 8–12 weeks |
| Phase 4 — Ecosystem | Billing, live enhancement, admin | 12–16 weeks |

**Total to Phase 2 (first trainer-capable release):** approximately 5–7 months  
**Total to Phase 4 (full vision):** approximately 10–14 months

**If team is 1 engineer:** multiply all estimates by 1.6–2×.

**Biggest time risk:** Exercise-specific form analysis (Phase 1) requires both implementation and calibration time. Rushing this risks embarrassing false positive form errors that will annoy early users.

---

## 23. IMMEDIATE NEXT ACTIONS

These are the first 14 actions in execution order. Complete them before writing any new feature code.

1. **Rotate Firebase API key** in Firebase Console → download new `google-services.json` → add to `.gitignore` → delete `FirebaseConfig.kt` → update `FitAIApplication.kt` to remove manual Firebase init
2. **Rename application ID** from `com.jsf.app.pravin` to `com.fitai.app` in `build.gradle.kts`
3. **Update app name** in `res/values/strings.xml` from "Pravin" to "Fit AI". Update theme name references.
4. **Generate production keystore** with `keytool -genkeypair`. Store the `.jks` file outside the project. Set `KEYSTORE_PASS` and `KEY_PASS` environment variables. Remove the `?: "changeme"` fallbacks.
5. **Write `firestore.rules`** with minimum authentication and ownership rules. Test with Firebase Emulator Suite. Deploy.
6. **Add Crashlytics** dependency to `build.gradle.kts`. Initialize in `FitAIApplication`. Verify crash reports appear in Firebase Console.
7. **Add Firebase App Check** dependency. Configure Play Integrity provider. Enable enforcement in Firebase Console (start in unenforced/monitoring mode).
8. **Add ProGuard rules** to `proguard-rules.pro` for MediaPipe, WebRTC, Firebase, Room.
9. **Fix `backup_rules.xml`** to exclude Room database file and DataStore preferences.
10. **Implement account deletion** in `ProfileViewModel` and `ProfileScreen`: call `FirebaseAuth.currentUser.delete()` + delete Firestore user documents + clear DataStore + clear Room. Add re-authentication step.
11. **Create and publish privacy policy**. Add the URL to the sign-up screen and Play Store listing.
12. **Create private Metered.ca TURN account**. Update `WebRtcManager.kt` ICE server configuration with private credentials. Do not commit credentials to source — use `local.properties` or environment variable.
13. **Add `.limit(50)` and cursor pagination** to `FirestoreCommunityDataSource.getMessagesForChannel()`.
14. **Design and produce custom app icon** for all density sizes + adaptive icon. Replace the default Android launcher icon.

After all 14 are complete: run a full regression test on workout tracking, community, and auth flows. Then submit for internal test track on Play Store.

---

## 24. FIT AI'S CORE DIFFERENTIATION — INTELLIGENT TRAINER MONITORING AND EXERCISE FORM ANALYSIS

### What Makes Fit AI Different

The fitness app market is mature and crowded. There are dozens of workout trackers, calorie counters, and social fitness apps. Most of them are digital versions of a paper workout log.

Fit AI is different because it addresses a structural problem in the personal training industry that software has not yet solved well.

---

### The Structural Problem

A personal trainer's ability to help their clients is directly limited by time and presence.

In a 1:1 session, the trainer can observe form, correct errors, and adjust the program in real time. But most clients train 3–5 times per week and see their trainer once or twice. Between those sessions, the trainer is blind.

Clients who train without supervision:
- Develop and reinforce poor movement habits
- Miss workouts without the trainer's knowledge
- Skip meal logging or nutrition targets
- Begin disengaging before the trainer notices
- Eventually drop out

The trainer finds out retrospectively — if at all. By that time, the client may have hurt themselves, lost motivation, or cancelled their subscription.

---

### How Fit AI Solves This

**1. AI-powered form analysis between sessions**

Fit AI uses MediaPipe's on-device pose estimation to analyse exercise form in real time on the client's device. This is not a gimmick. The system computes joint angles, body alignment, and movement trajectories for each exercise and evaluates whether each repetition was performed correctly.

The analysis happens entirely on device — no camera frames are uploaded. The client sees immediate, understandable feedback during the workout. The trainer receives a structured summary after the session.

This is qualitatively different from a simple rep counter. A rep counter tells you that a set was completed. Fit AI's form analysis tells the trainer that 8 of 24 reps showed hip alignment issues — a pattern that indicates a mobility limitation or fatigue response that the trainer should address.

**2. Intelligent trainer monitoring at scale**

The trainer dashboard aggregates client data into a prioritised view. Instead of opening 50 profiles, the trainer sees immediately:

- Which clients worked out today
- Which clients missed scheduled sessions
- Which clients have repeated form issues requiring correction
- Which clients show declining consistency
- Which clients appear to be disengaging

The alert engine groups and prioritises information so that the trainer spends time on intervention, not on information gathering.

This is the difference between a trainer who reacts after clients drop out and a trainer who intervenes before it happens.

**3. Nutrition and workout in one platform**

Most fitness apps address either workout tracking or nutrition tracking. Combining both in one platform, with the trainer having visibility into both, allows a holistic picture of the client's compliance and progress that neither stream alone can provide.

**4. Scalability**

The target is one trainer managing 50 clients effectively. Without intelligent automation, this is not realistically achievable. With Fit AI's alert engine and dashboard, the trainer's attention is directed to the right clients at the right time. The 80% of clients who are training consistently require almost no trainer time. The 20% who need help are surfaced automatically.

---

### What Is Genuinely Differentiating

| Feature | Genuine Differentiator | Standard Feature |
|---|---|---|
| Per-rep form analysis with exercise-specific algorithms | Yes | |
| Trainer alert engine with intelligent grouping | Yes | |
| Client dropout risk prediction | Yes | |
| Trainer dashboard for 50 clients | Yes | |
| On-device pose estimation (no upload) | Yes | |
| Nutrition visibility for trainer | Yes | |
| Rep counting | | Standard |
| Workout history | | Standard |
| Community servers | | Standard |
| Streak tracking | | Standard |
| Calorie tracking | | Standard |

---

### What We Are Not Claiming

- This is not a medical device. Form analysis is based on geometric pose estimation from a 2D camera and has real limitations.
- AI form feedback should never override a qualified trainer's assessment.
- Dropout risk scores are indicators based on in-app data, not psychological assessments.
- Calorie estimates from food photos are approximate by nature.
- The 50-client-per-trainer target is a design objective that must be validated with real trainers in real conditions.

---

### The Business Value

For the trainer: Fit AI allows them to offer a higher-quality service to more clients. A trainer currently managing 15–20 clients could extend to 30–40 without reducing quality of care.

For the client: Fit AI provides coaching-level feedback between expensive in-person sessions. The camera becomes a knowledgeable training partner.

For the platform: The trainer subscription is the primary revenue driver. Trainers pay for the tools. Clients may have a free tier with premium features available. This is a B2B2C model that aligns the financial incentive with product quality.

---

## 25. FINAL STRATEGIC Q&A

**1. Should we continue the existing project, rebuild it, or use a hybrid approach?**

**Hybrid approach (Option C).** The workout AI pipeline, Firestore community platform, and Compose UI are all genuinely functional and took real engineering effort. Rebuilding would require 3–4 months to reach the current level of functionality before building any new features. The architectural weaknesses (no Hilt, no tests) are real but are addressable incrementally without a rebuild. The trainer platform is new development regardless of the approach chosen.

---

**2. What is already working?**

The following are implemented end-to-end and functional:
- Email/password authentication with Firebase
- MediaPipe on-device pose estimation and rep counting
- 5-metric workout scoring (accuracy, balance, stability, symmetry, speed)
- CameraX camera feed for workout tracking
- Environment quality check before workout
- Workout state machine (idle → instructions → environment check → active → rest → summary)
- Workout session save to Room
- Progress screen with custom charts
- Consistency tracking and streaks
- Community servers, channels, and real-time messaging via Firestore
- Friends system via Firestore
- Basic achievements (local)

---

**3. What are the most important missing features?**

In order of business impact:

1. Exercise-specific form analysis with rep-level feedback — the primary technical differentiator
2. Trainer registration, client assignment, and trainer dashboard — the primary business differentiator
3. Intelligent alert engine — makes the trainer platform scalable
4. Community posts synced to Firestore — community is currently broken for multi-user use
5. FCM push notifications — required for workout reminders and trainer alerts
6. Firestore Security Rules — required before any public data is shared
7. Account deletion — required by Play Store policy
8. Meal tracking and nutrition system

---

**4. What should we build first?**

Phase 0 blockers first — non-negotiable. Then Phase 1 form analysis enhancement, then Phase 2 trainer platform. Do not start Phase 2 trainer features until Phase 0 and Phase 1 are complete. The trainer dashboard is only valuable if the form analysis data feeding it is reliable.

---

**5. What should we postpone?**

- Group live video sessions (Phase 4) — requires WebRTC SFU infrastructure, complex to build reliably
- Admin panel — not needed before first 1,000 users
- Subscriptions and payments — can operate free for the first 3–6 months to validate product-market fit
- AI food analysis — technically complex; implement after trainer platform is live and validated
- Advanced AI workout recommendations — after sufficient data is collected from real users

---

**6. How can we realistically achieve the 50-client-per-trainer monitoring objective?**

This requires four things working together:

1. **Quality form data:** Each client's workout must produce reliable, structured form data (exercise, reps, errors, severity). This requires the exercise-specific form analyzers built in Phase 1.

2. **Intelligent alert engine:** Alerts must be grouped and prioritised. A daily digest that surfaces the 5–10 clients who need attention is far more useful than 50 individual notifications.

3. **Dashboard designed for scale:** The trainer must be able to scan all 50 clients in under 2 minutes and identify who needs intervention without opening individual profiles.

4. **Client data quality:** Clients must actually use the app consistently. This requires good UX, reminders, and engagement features.

The 50-client target is technically supportable by the architecture described in this document. Whether it is achievable in practice depends on trainer adoption and client compliance — both of which must be validated through real-world testing with early trainer partners.

---

**7. What is the immediate next development milestone?**

**Milestone: Phase 0 Complete**

The milestone is reached when:
- The app is submitted to the Play Store internal test track under the `com.fitai.app` application ID
- Firebase API key is rotated and no longer in source code
- Firestore Security Rules are deployed and tested
- Crashlytics is active and showing data
- Account deletion works end-to-end
- Privacy policy is published and linked
- Release APK is signed with a proper production keystore

**Estimated time to milestone:** 4–6 weeks with one focused engineer.

This milestone is not glamorous. It does not add visible features. But it transforms the project from an insecure prototype into a production-grade foundation that can be built on safely.

---

*End of FIT AI Master Product Vision, Technical Audit & Development Roadmap — v1.0 — 2026-11-03*
