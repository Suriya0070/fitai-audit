# FIT AI — PRODUCTION READINESS & TECHNICAL AUDIT

**Date:** 2026-11-03  
**Auditor Role:** Senior Software Architect / Staff Backend Engineer / Mobile Security Engineer / DevOps Engineer / QA Lead / Startup CTO  
**Scope:** Full codebase audit, security review, performance analysis, Play Store readiness  
**Verdict:** See Section 18

---

## 1. EXECUTIVE SUMMARY

FitAI is a Kotlin/Jetpack Compose Android application that combines AI-powered workout tracking (MediaPipe pose estimation), a community platform (Discord-like servers/channels), live peer-to-peer video workouts (WebRTC), and social features (friends, challenges, Rep Battle). The core workout tracking feature is **genuinely innovative and functional**. However, the application has **critical blockers** preventing a safe public launch.

The most severe issues are:

- The Firebase API key is **hardcoded in version-controlled source code**.
- The application ID and app name are **"Pravin"**, not "FitAI".
- The **default release keystore password is "changeme"**.
- There are **zero Firebase Security Rules** visible — Firestore is likely fully open.
- No Cloud Storage integration, no Crashlytics, no FCM push notifications.
- Trainer, payment, subscription, calorie, and admin features are **completely missing**.
- The app uses a **public, untrusted TURN server** for WebRTC with hardcoded credentials.
- Only **2 placeholder tests** exist across the entire codebase.

The app is **NOT ready for public Play Store launch** in its current state.

---

## 2. EXISTING TECHNOLOGY STACK

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material3 | BOM 2026.02.01 |
| Min SDK | Android 7.0 | API 24 |
| Target SDK | API 37 | |
| Compile SDK | API 37 | |
| Architecture | MVVM + Repository | |
| Local Database | Room (SQLite) | 2.7.1 |
| Preferences | DataStore Preferences | 1.1.1 |
| Navigation | Navigation Compose | 2.8.9 |
| DI | Manual (no Hilt/Koin) | |
| Auth | Firebase Auth (email/password) | BOM 33.7.0 |
| Cloud DB | Cloud Firestore | BOM 33.7.0 |
| AI/Pose | MediaPipe PoseLandmarker | 0.10.14 |
| Camera | CameraX | 1.4.0 |
| Video | Stream WebRTC Android | 1.3.3 |
| Image Loading | Coil | 2.7.0 |
| Serialization | Gson | 2.11.0 |
| Build | Gradle 9.2.1 + KSP | |
| AGP | 9.2.1 | |

**Not present:** Hilt/Dagger, Firebase Storage, Firebase Cloud Messaging (FCM), Firebase Crashlytics, Firebase Analytics, Firebase App Check, Firebase Remote Config, Firebase Functions, Retrofit/OkHttp, Stripe/payment SDK.

---

## 3. COMPLETE PROJECT ARCHITECTURE

### Module Structure
Single-module Android application.

### Package Structure (`com.jsf.app.pravin`)
```
├── FitAIApplication.kt         — App entry point, Firebase init
├── MainActivity.kt             — Single Activity, NavHost, bottom nav
├── data/
│   ├── db/                     — Room database (FitnessDatabase v3)
│   │   ├── FitnessDatabase.kt  — 19 entities, migrations 1→2→3
│   │   └── *Dao.kt (17 DAOs)
│   ├── model/                  — Room entities + data classes
│   ├── preferences/            — UserPreferences (DataStore)
│   ├── remote/
│   │   ├── FirebaseConfig.kt   — HARDCODED API KEY ⚠️
│   │   ├── FirestoreCommunityDataSource.kt
│   │   ├── FriendsDataSource.kt
│   │   ├── RepBattleDataSource.kt
│   │   └── WebRtcSignalingService.kt
│   ├── repository/
│   │   ├── FitnessRepository.kt    — Room-only
│   │   └── CommunityRepository.kt  — Firestore + Room hybrid
│   └── di/
│       └── DatabaseModule.kt
├── domain/                     — Business logic
│   ├── PoseAnalyzer.kt         — MediaPipe integration
│   ├── RepCounter.kt, WorkoutScoreEngine.kt
│   ├── EnvironmentAnalyzer.kt, FatiguePredictor.kt
│   ├── ConsistencyTracker.kt, ProgressEngine.kt
│   └── ExerciseLibrary.kt, ExerciseDefinition.kt
└── ui/
    ├── navigation/Screen.kt
    ├── components/             — Reusable Compose components
    ├── screens/                — 17 screen packages
    └── theme/                  — Color.kt, Theme.kt, Type.kt
```

### Data Flow
1. **Local Data (Room):** User profile, workout sessions, routines, challenges, posts, meals, notifications, achievements. Accessed via `FitnessRepository` and `CommunityRepository`.
2. **Cloud Data (Firestore):** Servers, channels, messages, server members, friends, rep battles, WebRTC signaling. Accessed via `FirestoreCommunityDataSource`, `FriendsDataSource`, `RepBattleDataSource`, `WebRtcSignalingService`.
3. **Hybrid inconsistency:** Workout routines and community posts exist in Room (local) but **are not synced to Firestore**, meaning data is siloed per device.

### Navigation
- Single `NavHost` in `MainActivity.kt:162`
- 18 named routes in `Screen.kt`
- No deep-link support, no notification-driven navigation

---

## 4. CURRENT FEATURE INVENTORY

| Feature | Status |
|---|---|
| Email/Password auth | Implemented |
| Local/Guest mode | Implemented |
| Google Sign-In | UI stub only (disabled button) |
| Onboarding | Implemented |
| User profile (local) | Implemented |
| Workout tracking (camera + AI) | Implemented |
| Rep counting (MediaPipe) | Implemented |
| Environment quality check | Implemented |
| Workout scoring (5 metrics) | Implemented |
| Fatigue/injury prediction | Implemented |
| Workout summary + history | Implemented |
| Achievements | Partially implemented (local only) |
| Streaks | Implemented |
| Progress screen with charts | Implemented |
| Personal records | Implemented |
| Community servers | Implemented (Firestore) |
| Server channels (text/feed) | Implemented |
| Channel messaging | Implemented (Firestore) |
| Community posts | Implemented (Room, local) |
| Friends (add/accept/search) | Implemented (Firestore) |
| Live workout (WebRTC 1:1) | Partially implemented |
| Rep Battle | Partially implemented (Firestore) |
| Workout routines | Partially implemented (Room local) |
| Challenges | Partially implemented (Room local) |
| Meal tracking | Model only (no UI) |
| Food image uploads | Not implemented |
| AI food analysis | Not implemented |
| Nutrition/calorie tracking | Not implemented |
| Trainer role/dashboard | Not implemented |
| Trainer-client management | Not implemented |
| Push notifications (FCM) | Not implemented |
| Missed workout alerts | Not implemented |
| Subscriptions/payments | Not implemented |
| Admin panel | Not implemented |
| Account deletion | Not implemented |
| Privacy policy | Not implemented |

---

## 5. FEATURE IMPLEMENTATION STATUS

### 5.1 User Registration and Login
**Status: Implemented and Verified**
- `AuthScreen.kt`, `AuthViewModel.kt`, `AuthRepository.kt`
- Firebase email/password sign-up, sign-in, password reset — all functional
- Error mapping for common Firebase error codes
- Local guest mode via `signInAsLocalUser()` generates `local_${UUID}` — bypasses Firebase entirely
- **Issue:** Guest users can join Firestore community servers, creating orphaned records
- **Issue:** No email verification required before login

### 5.2 User Profile Management
**Status: Partially Implemented**
- `ProfileScreen.kt`, `ProfileViewModel.kt`
- Profile stored in Room (`user_profiles`, PK = 1)
- Edit via bottom sheet works (name, age, height, weight, bio, gender, level)
- **Issue:** Profile is **not synced to Firestore**. Other users cannot see your profile name/details. Friend search only works because `FriendsDataSource.syncUserProfile()` writes a minimal public record on friend request, not on signup.
- **Issue:** `UserProfile.id = 1` hardcoded primary key — no multi-account support on same device
- **Issue:** `avatarUrl` field exists in model but profile photo uploads are not implemented

### 5.3 Personal Trainer Registration
**Status: Not Implemented**
- No trainer role in authentication
- `ServerRole.COACH` exists in DB (`CommunityRepository.kt:128`) but is just a community server role
- No dedicated trainer onboarding, trainer profile, or trainer-specific UI

### 5.4 Trainer-Client Relationship Management
**Status: Not Implemented**

### 5.5 Workout Plans
**Status: Partially Implemented**
- `WorkoutRoutine` and `RoutineExercise` entities exist in Room
- `CreateRoutineScreen.kt`, `RoutineDetailScreen.kt`, `ServerRoutinesScreen.kt` are implemented
- **Issue:** Routines stored in Room (local) only, not synced to Firestore
- **Issue:** Starting a routine from `RoutineDetailScreen` navigates to `WorkoutScreen` but passes no routine context — the routine exercises are not loaded into the workout flow
- **Issue:** `workoutRoutineDao().getRoutinesForServer()` works but there is no cross-device visibility

### 5.6 Workout Tracking
**Status: Implemented and Verified**
- `WorkoutScreen.kt`, `WorkoutViewModel.kt`
- States: Idle → Instructions → EnvironmentCheck → Active → Rest → Summary
- MediaPipe live stream mode, front camera, 640×480 resolution
- 5-metric scoring: accuracy, balance, stability, symmetry, speed
- Set/rep cycle with 60s rest timer, vibration + beep on rest end
- Sessions saved to Room via `FitnessRepository.saveSession()`
- **Limitation:** Requires ARM device; MediaPipe native library does not load on x86_64 emulators (correctly surfaced to user in UI)

### 5.7 Exercise and Repetition Tracking
**Status: Implemented and Verified**
- `RepCounter.kt`, `WorkoutScoreEngine.kt`, `PoseAnalyzer.kt`
- 16 exercise types defined in `ExerciseLibrary.kt` with form tips and muscle targeting
- `sessionScoreHistory` (CopyOnWriteArrayList) accumulates every frame result — memory leak risk for long sessions

### 5.8 Meal Tracking
**Status: Model/DAO Only — No UI**
- `MealPost` entity, `MealPostDao`, `meal_posts` table exist
- `CommunityRepository.getMealsByUser()` exists
- No dedicated meal tracking screen, no calorie entry, no meal history UI

### 5.9 Food Image Uploads
**Status: Not Implemented**
- No Firebase Storage dependency in `build.gradle.kts`
- No image picker implementation

### 5.10 AI Food Analysis
**Status: Not Implemented**
- `MealPost.aiAnalysis` field exists as a String placeholder
- No API integration (OpenAI Vision, Google Vision, etc.)

### 5.11 Daily Calorie and Nutrition Tracking
**Status: Not Implemented**

### 5.12 Progress Tracking
**Status: Implemented and Verified**
- `ProgressScreen.kt`, `ProgressViewModel.kt`
- Three tabs: Overview, By Exercise, Muscles
- Charts: `ProgressChart`, `RadarChart`, `CalendarHeatmap`, `MuscleGroupChart`
- Personal records, weekly trends, AI insights (based on local data)
- **Limitation:** All data is local (Room) — no cross-device sync

### 5.13 Streaks, XP, Rewards and Gamification
**Status: Partially Implemented**
- `ConsistencyTracker.kt` calculates day streaks from session timestamps
- `Achievement` entity with 9 types (first workout, streak 7/30, rep milestones, perfect score)
- Achievement unlock dialog shown during workout summary
- Avatar system with 5 levels based on streak
- **Missing:** XP system, points, leaderboard, rewards, daily challenges

### 5.14 Community and Social Interactions
**Status: Partially Implemented (Functional)**
- Servers, channels (text, feed, announcements) — Firestore-backed, real-time
- Channel chat messaging — Firestore-backed, real-time
- Community feed posts — Room-only (not shared, local only)
- `FriendsScreen.kt`, `FriendsDataSource.kt` — friend requests, accept/decline
- Reactions, comments exist in Room but are **not synced to Firestore**
- **Issue:** Community posts are local-only — defeating the purpose of shared feeds

### 5.15 Trainer Dashboard
**Status: Not Implemented**

### 5.16 Trainer Monitoring Multiple Clients
**Status: Not Implemented**

### 5.17 Trainer-Client Notifications
**Status: Not Implemented**

### 5.18 Missed Workout Alerts
**Status: Not Implemented** — FCM not configured

### 5.19 Missed Meal Update Alerts
**Status: Not Implemented**

### 5.20 Live Sessions and Video Calls
**Status: Partially Implemented**
- `LiveWorkoutScreen.kt`, `LiveWorkoutViewModel.kt`, `WebRtcManager.kt`
- Stream WebRTC SDK (io.getstream:stream-webrtc-android:1.3.3)
- Firebase Firestore as signaling channel via `WebRtcSignalingService.kt`
- 1:1 video call with host/guest roles, camera toggle, mute, camera flip
- Pose detection runs on live video frames during call
- Rep counts synced to Firestore `live_calls/{callId}/rep_counts`
- **Issue:** TURN server is `openrelay.metered.ca` with hardcoded public credentials — unreliable, insecure, not suitable for production
- **Issue:** No ICE restart on connection failure, limited reconnect logic
- **Issue:** Only supports 1:1 calls, not group sessions

### 5.21 Subscription and Payment Functionality
**Status: Not Implemented**

### 5.22 Admin Functionality
**Status: Not Implemented**

### 5.23 Settings and Account Management
**Status: Minimal**
- Fitness goal and weekly session goal set during onboarding
- Sign out implemented
- **Missing:** Account deletion, notification preferences, privacy settings, data export

---

## 6. FIREBASE BACKEND AUDIT

### 6.1 Firebase Authentication
**Configured:** Yes  
**Initialized:** `FitAIApplication.kt:18` via manual `FirebaseOptions.Builder`  
**Also loaded via:** `google-services.json` plugin  
**Used in:** `AuthRepository.kt`  
**Functions:** email/password sign-in, sign-up, password reset, auth state listener  
**Issues:**
- No email verification required after sign-up
- No account deletion (`FirebaseAuth.currentUser?.delete()`)
- No re-authentication before sensitive operations
- Guest mode (`local_${UUID}`) creates locally stored user IDs that can interact with Firestore

### 6.2 Cloud Firestore
**Configured:** Yes  
**Initialized:** `FirebaseFirestore.getInstance()` in each data source  
**Collections used:**
- `servers` — public/private server documents
- `channels` — server channel documents
- `messages` — chat messages
- `server_members` — membership with role
- `users` — minimal public profiles
- `friend_requests` — pending/accepted/declined requests
- `friendships` — accepted friendship pairs
- `rep_battles` — active battle documents
- `rep_battle_entries` — per-user rep submissions
- `live_calls` — WebRTC signaling documents
  - `live_calls/{id}/host_ice` — ICE candidates subcollection
  - `live_calls/{id}/guest_ice` — ICE candidates subcollection
  - `live_calls/{id}/rep_counts` — live rep count subcollection

**Real-time Listeners:** All collection flows use `addSnapshotListener` with `awaitClose { reg.remove() }` — correctly disposed.

**Critical Issues:**
- **No Firestore Security Rules visible in source code.** Without rules, the default is likely open read/write to all authenticated users. There is no rule enforcement preventing User A from writing to User B's data, deleting any server, or reading all messages.
- `searchPublicServers()` fetches ALL public servers from Firestore and filters client-side — no query limit
- `getMessagesForChannel()` fetches ALL messages with no pagination — will cause excessive reads as messages grow
- No composite indexes verified — queries with multiple `whereEqualTo` clauses may fail silently in production
- `leaveServer()` sets `isActive=false` but does not decrement `memberCount` in the server document — count diverges

### 6.3 Firebase Realtime Database
**Status: Not configured, not used.**

### 6.4 Firebase Storage
**Status: Not configured, not used.**
No `firebase-storage` dependency. Profile photos, server icons, meal photos, and post media cannot be uploaded.

### 6.5 Cloud Functions
**Status: Not configured, not used.**
All business logic runs client-side. No server-side validation, no triggered functions.

### 6.6 Firebase Cloud Messaging (FCM)
**Status: Not configured, not used.**
No `firebase-messaging` dependency. No push notifications possible.

### 6.7 Firebase Analytics
**Status: Not configured, not used.**

### 6.8 Firebase Crashlytics
**Status: Not configured, not used.**
If the app crashes in production, there is no automatic crash reporting.

### 6.9 Firebase App Check
**Status: Not configured, not used.**
The Firebase project is open to API abuse from any client.

### 6.10 Firebase Remote Config
**Status: Not configured, not used.**

### 6.11 Firebase Performance Monitoring
**Status: Not configured, not used.**

---

## 7. DATABASE DESIGN AUDIT

### Local Room Database (v3, `fitness_db`)

| Table | Primary Key | Notes |
|---|---|---|
| `user_profiles` | `id = 1` (hardcoded) | Single profile per device |
| `workout_sessions` | autoincrement Long | Local only, not synced |
| `servers` | String | Redundant local copy of Firestore data |
| `server_members` | autoincrement + unique index | Redundant |
| `channels` | String | Redundant |
| `messages` | String | Redundant |
| `workout_routines` | String | Local only, not synced |
| `routine_exercises` | autoincrement | |
| `challenges` | String | Local only |
| `challenge_progress` | autoincrement | Local only |
| `posts` | String | Local only |
| `meal_posts` | String | Local only |
| `notifications` | autoincrement | Local only |
| `invites` | String | Local only |
| `live_sessions` | String | Local only |
| `achievements` | autoincrement | Local only |
| `reactions` | autoincrement | Local only |
| `comments` | String | Local only |
| `server_roles` | autoincrement | Local only |

**Issues:**
- `user_profiles.id = 1` is hardcoded — only one profile per installation, no multi-account
- Server/channel/message tables exist in Room but are never populated — `CommunityRepository` reads servers from Firestore but saves locally only for routines, challenges, posts
- `exportSchema = false` — no schema migration tracking, risks silent corruption on version mismatch
- No cascade deletes — orphaned records accumulate (e.g., `RoutineExercise` records remain when `WorkoutRoutine` is deleted)
- `Invite` uses `code` field but `inviteDao().getInviteByCode()` is defined — invite flow is unfinished

### Firestore Collections

| Collection | Documents | Sub-collections |
|---|---|---|
| `servers` | Per server | none |
| `channels` | Per channel | none |
| `messages` | Per message | none |
| `server_members` | `{serverId}_{userId}` | none |
| `users` | Per user | none |
| `friend_requests` | `{from}_{to}` | none |
| `friendships` | `{uid1}_{uid2}` | none |
| `rep_battles` | Per battle | `rep_battle_entries` |
| `live_calls` | Per call | `host_ice`, `guest_ice`, `rep_counts` |

**Issues:**
- No atomic transactions — e.g., joining a server increments `memberCount` but this is not done atomically
- `live_calls` ICE candidate subcollections are never cleaned up after call ends
- `friend_requests` document ID = `{from}_{to}` — a user can only send one request to another user ever (cannot re-request after decline)
- `friendships` stores denormalized name/level at creation time — stale if user updates profile

---

## 8. SECURITY VULNERABILITIES AND RISKS

### CRITICAL

**SEC-01 — Firebase API Key Hardcoded in Source Code**  
File: `app/src/main/java/com/jsf/app/pravin/data/remote/FirebaseConfig.kt:21`  
File: `app/google-services.json:17`  
The Firebase API key `AIzaSyD7LtNkYPerl4Mjddx-tLTPczfBVS3akps` and all project identifiers are committed to source control in plain text. If this repository is public or leaked, any attacker can:
- Enumerate all public Firestore documents
- Create Firebase Auth accounts
- Flood the project with data (without App Check)
- Incur excessive Firebase costs

**Recommendation:** Rotate the API key immediately in the Firebase Console. Move configuration to `local.properties` (gitignored) or use environment variables in CI. In Android, the API key in `google-services.json` is unavoidable in the APK but should never be in a `.kt` constant file that gets committed. Add `google-services.json` to `.gitignore` for staging/production variants.

---

**SEC-02 — Release Keystore Password is "changeme"**  
File: `app/build.gradle.kts:29`  
```kotlin
storePassword = System.getenv("KEYSTORE_PASS") ?: "changeme"
keyPassword = System.getenv("KEY_PASS") ?: "changeme"
```
If `KEYSTORE_PASS` and `KEY_PASS` environment variables are not set at build time (e.g., on a dev machine), the keystore is signed with `"changeme"`. Any build artifact signed with this password is compromised.

**Recommendation:** Never use a fallback password. Remove the `?: "changeme"` fallback. If env vars are absent, the build should fail.

---

**SEC-03 — No Firebase Security Rules (Presumed Open)**  
No security rules file is present in the repository. The Firestore database is presumably using default rules or rules from an earlier configuration. Without rules:
- Any authenticated user can read all server messages
- Any authenticated user can modify any server
- Any authenticated user can delete any message or channel
- Local guest users (`local_*`) that happen to have Firebase connectivity can read all community data

**Recommendation:** Add `firestore.rules` to the project and implement user-scoped rules. Verify and test with the Firebase emulator before launch.

---

### HIGH

**SEC-04 — Public TURN Server with Hardcoded Credentials**  
File: `app/src/main/java/com/jsf/app/pravin/ui/screens/livestream/WebRtcManager.kt:127-138`  
The app uses `turn:openrelay.metered.ca` with hardcoded credentials `openrelayproject/openrelayproject`. This is a free public relay:
- Anyone can use these credentials — not exclusive to FitAI
- The service may be unavailable, rate-limited, or shut down at any time
- Peer video traffic routes through a third-party server you do not control
- This violates user privacy expectations

**Recommendation:** Set up a dedicated TURN server (Coturn, Twilio TURN, or Metered) with private credentials before launch.

---

**SEC-05 — Guest Mode Allows Community Access Without Verification**  
File: `app/src/main/java/com/jsf/app/pravin/data/remote/auth/AuthRepository.kt:84`  
`signInAsLocalUser()` creates an anonymous local ID without Firebase Auth. These users can still call all Firestore operations if Firebase Auth state is checked inconsistently (some data sources check `FirebaseConfig.isConfigured`, some don't). This is a design inconsistency that could allow unintended access.

**Recommendation:** Either fully isolate guest users from Firestore, or migrate guest mode to Firebase Anonymous Auth.

---

**SEC-06 — No Input Validation on Message Content**  
File: `FirestoreCommunityDataSource.kt:85-87`  
Messages are sent to Firestore with no length validation, no content filtering, no rate limiting. Users can send arbitrarily large message bodies.

**Recommendation:** Validate message length client-side and implement Cloud Functions for server-side enforcement.

---

**SEC-07 — Account Deletion Not Implemented**  
Google Play Store policy (effective 2024) **requires** apps that allow account creation to also provide account deletion. The app has sign-out but no account deletion. This is a **Play Store compliance blocker**.

---

### MEDIUM

**SEC-08 — Backup Rules Not Configured**  
File: `app/src/main/res/xml/backup_rules.xml` — template/empty  
File: `app/src/main/res/xml/data_extraction_rules.xml` — template/empty  
`AndroidManifest.xml` has `allowBackup="true"` with no exclusion rules. The Room database (which contains workout history, profile data) and DataStore (which contains the session user ID) will be backed up to Google's cloud and can be restored to new devices, potentially leading to account confusion.

**Recommendation:** Configure backup rules to exclude Room database and DataStore.

---

**SEC-09 — No Email Verification**  
Users can sign up with any email address and immediately access the full app without verifying ownership. This allows fake accounts and makes account recovery unreliable.

---

**SEC-10 — Debug Logging in Production**  
Multiple `android.util.Log.w()` and `android.util.Log.e()` calls throughout `FirestoreCommunityDataSource.kt`, `FriendsDataSource.kt`, `RepBattleDataSource.kt`, `WebRtcManager.kt`. These remain in release builds unless explicitly stripped.

---

**SEC-11 — WebRTC SDP Stored in Firestore Without Expiration**  
File: `WebRtcSignalingService.kt:37-54`  
SDP offers/answers and ICE candidates are stored in the `live_calls` Firestore collection indefinitely. Old call documents accumulate without cleanup.

---

### LOW

**SEC-12 — No Certificate Pinning**  
Firebase SDK connections are not pinned. Not a critical issue for Firebase-native SDKs, but notable for compliance contexts.

**SEC-13 — No Rate Limiting on Auth**  
Firebase Auth rate-limits by default, but no client-side throttling is implemented for failed sign-in attempts.

---

## 9. PERFORMANCE AND SCALABILITY ASSESSMENT

### 9.1 Readiness by Scale

**100 users:** The app will function adequately. Firestore usage will be minimal. No performance issues expected.

**1,000 users:** Functional but Firestore read costs begin to accumulate. `getPublicServers()` downloads all public server documents on every subscription. If there are 50 public servers and 1,000 users subscribe, that's 50,000 reads on startup alone.

**10,000 users:** Significant performance degradation expected:
- `getMessagesForChannel()` fetches ALL messages with no pagination limit — a popular channel with 10,000 messages will download them all on connect
- `searchPublicServers()` filters client-side — downloading thousands of server documents per search
- No Firestore offline persistence configured — every app open triggers fresh reads
- Concurrent Firestore listeners across active users will stress the project's quota

**50,000 users:** Not supportable without the following changes:
- Pagination on all collection queries (Firestore cursors)
- Server-side search (Algolia, Typesense, or Firestore composite queries)
- Message pagination (limit + startAfter cursor)
- Firestore offline persistence enabled

**100,000 users:** Requires architectural redesign:
- Cloud Functions for business logic and validation
- Scheduled cleanup of stale live_call documents
- Firebase App Check to prevent abuse
- CDN for media (Firebase Storage)
- FCM for notifications instead of polling

### 9.2 Specific Bottlenecks

**Bottleneck 1 — Unbounded session score history**  
File: `WorkoutViewModel.kt:83`  
`sessionScoreHistory` is a `CopyOnWriteArrayList<WorkoutScore>` that collects a `WorkoutScore` entry every camera frame (~30 per second). A 10-minute workout produces ~18,000 entries in memory. A 1-hour workout produces ~108,000 entries. This is a memory pressure issue.

**Bottleneck 2 — Client-side Firestore filtering**  
File: `FirestoreCommunityDataSource.kt:34-38`  
`searchPublicServers()` subscribes to ALL public servers and filters by name in Kotlin. As server count grows, this becomes a full table scan.

**Bottleneck 3 — No message pagination**  
File: `FirestoreCommunityDataSource.kt:68-74`  
`getMessagesForChannel()` fetches all messages. Popular channels will become unbearable.

**Bottleneck 4 — Duplicate data listeners**  
`CommunityRepository.kt` creates `FirestoreCommunityDataSource` as a default parameter — every call to `CommunityRepository()` creates a new datasource with new listener registrations. In `MainActivity.kt:332`, `CommunityRepository(db)` is created inside a `remember` block which is correct, but this pattern is fragile.

**Bottleneck 5 — No Room database cleanup**  
Workout sessions, notifications, and posts accumulate indefinitely with no purge policy.

---

## 10. GOOGLE PLAY STORE READINESS

### 10.1 Critical Blockers

| Issue | Severity | File |
|---|---|---|
| Application ID is `com.jsf.app.pravin` — not FitAI-branded | BLOCKER | `build.gradle.kts:10` |
| App name is "Pravin" | BLOCKER | `res/values/strings.xml` |
| App icon is default Android launcher icon | BLOCKER | `res/mipmap-*/` |
| No custom app icon for Play Store listing | BLOCKER | |
| Account deletion not implemented | BLOCKER (compliance) | — |
| No privacy policy URL | BLOCKER (compliance) | — |
| Release keystore fallback password "changeme" | BLOCKER | `build.gradle.kts:29` |
| `fitai-release.jks` keystore file does not exist at expected path | BLOCKER | `build.gradle.kts:28` |

### 10.2 Play Store Configuration

| Item | Status |
|---|---|
| Application ID | `com.jsf.app.pravin` — must change to `com.fitai.app` or similar |
| Version Code | 1 |
| Version Name | "1.0" |
| Min SDK | API 24 (Android 7.0) — acceptable |
| Target SDK | API 37 — acceptable |
| Compile SDK | API 37 — acceptable |
| Release minify | Enabled |
| Shrink resources | Enabled |
| ProGuard | Minimal rules (no Firebase/WebRTC/MediaPipe rules) |

### 10.3 Declared Permissions

```
CAMERA — required for workout + live video
VIBRATE — required for rest timer
INTERNET — required
POST_NOTIFICATIONS — required (but FCM not implemented)
RECORD_AUDIO — required for WebRTC
MODIFY_AUDIO_SETTINGS — required for WebRTC
```

`android.hardware.camera` declared as `required="false"` — correct for non-camera devices.

### 10.4 ProGuard Gaps
File: `app/src/main/keepRules/rules.keep` — empty  
File: `app/proguard-rules.pro` — not read (does not appear to exist)

The release build may strip critical classes for MediaPipe, Firebase, WebRTC, or Room if not properly kept. No rules for:
- `com.google.mediapipe.**`
- `org.webrtc.**`
- Room entities and DAOs
- Firebase model serialization

### 10.5 Data Safety Declaration
The app collects:
- Name, age, height, weight, gender (from onboarding)
- Workout performance data
- Camera feed (processed locally, not transmitted)
- Email address (Firebase Auth)
- Community messages

A Data Safety declaration is **required** on Play Store. Not yet prepared.

### 10.6 Crash Reporting
**Crashlytics: Not configured.** If the app crashes in production, you have no visibility.

---

## 11. TESTING COVERAGE

### Existing Tests
| Test | Type | Status |
|---|---|---|
| `ExampleUnitTest.addition_isCorrect` | Unit | Placeholder (2+2=4) |
| `ExampleInstrumentedTest.useAppContext` | Instrumented | Placeholder (checks package name) |

**No domain logic tests. No repository tests. No ViewModel tests. No UI tests. No security tests.**

### Missing Test Coverage

| Area | Test Type | Priority |
|---|---|---|
| AuthRepository sign-in/sign-up flow | Unit | P0 |
| AuthRepository error mapping | Unit | P0 |
| Firestore security rules | Firebase emulator integration | P0 |
| RepCounter algorithm accuracy | Unit | P1 |
| WorkoutScoreEngine calculations | Unit | P1 |
| ConsistencyTracker streak logic | Unit | P1 |
| ProgressEngine report generation | Unit | P1 |
| FitnessRepository CRUD operations | Unit | P1 |
| Room migration 1→2→3 | Integration | P1 |
| WorkoutViewModel state machine | Unit | P1 |
| AuthViewModel state transitions | Unit | P1 |
| Offline behaviour (no network) | Integration | P1 |
| Camera permission denied flow | UI (Espresso/Compose) | P1 |
| Server join/leave membership | Integration | P2 |
| Friend request accept/decline | Integration | P2 |
| WebRTC connection lifecycle | Integration | P2 |

---

## 12. MISSING FEATURES

The following features were identified in the pitch deck and/or audit scope but are completely absent from the codebase:

1. **Personal Trainer role and management** — No trainer registration, no trainer dashboard, no client assignment
2. **Meal tracking UI** — Model and DAO exist but no screen, no entry flow
3. **Food image upload and AI analysis** — Zero implementation
4. **Daily calorie/nutrition tracking** — Zero implementation
5. **Push notifications (FCM)** — Zero implementation
6. **Missed workout/meal alerts** — Depends on FCM
7. **Subscription and payment** — Zero implementation
8. **Admin panel** — Zero implementation
9. **Account deletion** — Required by Play Store policy
10. **Community post cloud sync** — Posts are local-only
11. **Profile photo upload** — Firebase Storage not integrated
12. **Google Sign-In** — Button disabled, not implemented
13. **Invite system** — Invite model and DAO exist, flow is unfinished

---

## 13. UNNECESSARY OR REDUNDANT COMPONENTS

1. **Room tables for servers/channels/messages** — These are Firestore-backed. The Room tables in `FitnessDatabase` for `servers`, `server_members`, `channels`, `messages` appear to be unused replicas. `CommunityRepository` reads servers from Firestore only, never from Room. These tables add confusion without value.

2. **`data/di/DatabaseModule.kt`** — Exists but does not inject via Hilt/Koin. It is unused dead code.

3. **`Screen.Avatar` route** — `AvatarScreen.kt` and `AvatarViewModel.kt` exist and are in the nav graph but there is no navigation trigger to this screen in the main UI.

4. **`WorkoutRoutineDao` / `ChallengeDao` Room tables** — These are implemented locally but the features don't function cross-device and the UIs are incomplete.

---

## 14. TECHNICAL DEBT

1. **Manual DI everywhere** — `FitnessDatabase.getInstance(context)`, `FitnessRepository(db)`, `CommunityRepository(db)`, `AuthRepository(application)` are all manually instantiated. Across `MainActivity.kt`, `AuthViewModel`, `HomeViewModel`, `WorkoutViewModel`, `ProfileViewModel` etc. Without Hilt, each ViewModel instantiates its own dependencies. This leads to multiple FitnessDatabase instances if not careful (currently protected by singleton pattern).

2. **Missing proguard rules** — MediaPipe, WebRTC, Room, Firebase all require specific keep rules. The release build may crash silently.

3. **`sessionScoreHistory` unbounded list** — `WorkoutViewModel.kt:83`. Should be bounded with a sliding window.

4. **No coroutine error handling at repository layer** — Several `runCatching` blocks silently swallow exceptions.

5. **`CommunityRepository` creates `FirestoreCommunityDataSource` as a default parameter** — Makes testing impossible without dependency injection. The datasource creates Firestore listeners on construction.

6. **`UserProfile` primary key 1** — Requires complete model refactor if multi-account is ever needed.

7. **App name "Pravin" throughout** — Package name, app label, theme name all use "Pravin". Renaming this consistently requires careful Find & Replace across all files.

---

## 15. ESTIMATED INFRASTRUCTURE COST FACTORS

At 100 users (light usage):
- **Firestore reads:** ~50,000–200,000/day → Free tier ($0)
- **Firebase Auth:** Free tier
- **Firestore Storage:** < 1 GB → Free tier

At 1,000 active users:
- **Firestore reads:** ~500K–2M/day → Approaching Spark limit; Blaze plan needed (~$0.06/100K reads)
- **TURN server:** Public relay = $0 but unreliable

At 10,000 active users:
- **Firestore reads:** ~5M–20M/day → ~$30–120/day on Blaze plan without optimization
- **TURN server:** Dedicated Coturn VPS: $20–50/month; Twilio TURN: ~$0.40/GB of relayed media

**Immediate cost risks:**
- No Firebase App Check → anyone can spam reads/writes at your expense
- Unbounded message loading → Firestore costs scale linearly with message count per channel

---

## 16. PRODUCTION BLOCKERS

These issues **must** be resolved before any public release:

| # | Blocker | File | Complexity |
|---|---|---|---|
| PB-01 | Firebase API key hardcoded | `FirebaseConfig.kt` | LOW |
| PB-02 | App name is "Pravin" | `strings.xml`, `build.gradle.kts`, theme | LOW |
| PB-03 | Application ID is `com.jsf.app.pravin` | `build.gradle.kts` | LOW |
| PB-04 | No custom app icon | `res/mipmap-*/` | LOW |
| PB-05 | Release keystore fallback "changeme" | `build.gradle.kts` | LOW |
| PB-06 | `fitai-release.jks` keystore missing | project root | MEDIUM |
| PB-07 | Firestore Security Rules not deployed | Firebase Console | MEDIUM |
| PB-08 | Account deletion not implemented | new screen | MEDIUM |
| PB-09 | No privacy policy | legal + link in app | LOW |
| PB-10 | Crashlytics not configured | `build.gradle.kts` | LOW |
| PB-11 | ProGuard rules missing | `proguard-rules.pro` | LOW |
| PB-12 | Backup rules expose user data | `backup_rules.xml` | LOW |
| PB-13 | TURN server uses public unreliable relay | `WebRtcManager.kt` | MEDIUM |

---

## 17. RECOMMENDED ARCHITECTURE

The current Firebase + Room architecture is **appropriate for MVP scale** (up to ~5,000 users). Do not add a Node.js backend, PostgreSQL, Redis, or Cloud Run before validating the product with real users. The following incremental changes are recommended:

### For MVP Launch
1. Firestore Security Rules (required)
2. Firebase App Check (recommended)
3. Firebase Crashlytics (required for observability)
4. Paginate Firestore queries (messages, servers)
5. Dedicated TURN server
6. Account deletion flow

### Post-MVP (1,000+ users)
1. Firebase Cloud Functions for: member count management, notification triggers, cleanup jobs
2. FCM for push notifications
3. Firebase Storage for profile photos and community media
4. Cloud posts (move from Room-only to Firestore)

### Post-PMF (10,000+ users)
1. Firestore offline persistence
2. Firestore TTL policies for stale data
3. CDN for media
4. Dedicated search service (Algolia) for server/user discovery

---

## 18. PRIORITIZED ACTION PLAN

---

### P0 — CRITICAL BEFORE LAUNCH

| ID | Priority | Problem | Evidence | Proposed Solution | Files Affected | Complexity | Mandatory |
|---|---|---|---|---|---|---|---|
| A01 | P0 | Firebase API key in source code | `FirebaseConfig.kt:21`, `google-services.json:17` | Rotate key in Firebase Console. Move `google-services.json` to `.gitignore`. Remove `FirebaseConfig.kt` and rely on google-services plugin only | `FirebaseConfig.kt`, `FitAIApplication.kt`, `build.gradle.kts` | LOW | YES |
| A02 | P0 | Release keystore fallback "changeme" | `build.gradle.kts:29` | Remove `?: "changeme"` fallback; build should fail if env vars not set | `build.gradle.kts` | LOW | YES |
| A03 | P0 | No Firestore Security Rules | Firebase Console | Write and deploy `firestore.rules` scoped to authenticated user IDs | New: `firestore.rules` | MEDIUM | YES |
| A04 | P0 | App ID / name is "Pravin" | `build.gradle.kts:10`, `strings.xml` | Rename app ID to `com.fitai.app` or similar; update all string references | `build.gradle.kts`, `strings.xml`, theme files | LOW | YES |
| A05 | P0 | Account deletion not implemented | No implementation | Add "Delete Account" in ProfileScreen: call `Firebase Auth.delete()` + delete user Firestore documents + clear DataStore/Room | `ProfileScreen.kt`, new composable, `AuthRepository.kt` | MEDIUM | YES |
| A06 | P0 | TURN server is public/unreliable | `WebRtcManager.kt:127-138` | Provision a private TURN server (Coturn or Twilio/Metered); store credentials securely (not hardcoded) | `WebRtcManager.kt` | MEDIUM | YES |
| A07 | P0 | No crash reporting | No Crashlytics dependency | Add `firebase-crashlytics` to `build.gradle.kts`; configure in Application class | `build.gradle.kts`, `FitAIApplication.kt` | LOW | YES |

---

### P1 — REQUIRED BEFORE PUBLIC RELEASE

| ID | Priority | Problem | Evidence | Proposed Solution | Files Affected | Complexity | Mandatory |
|---|---|---|---|---|---|---|---|
| B01 | P1 | No ProGuard rules | `rules.keep` empty | Add keep rules for MediaPipe, WebRTC, Room entities, Firebase models | `proguard-rules.pro`, `rules.keep` | LOW | YES |
| B02 | P1 | Backup rules expose data | `backup_rules.xml` empty | Exclude Room DB and DataStore from cloud backup | `backup_rules.xml`, `data_extraction_rules.xml` | LOW | YES |
| B03 | P1 | No privacy policy | Missing | Publish privacy policy; add URL to app sign-up screen and Play listing | `AuthScreen.kt`, Play Console | LOW | YES |
| B04 | P1 | No custom app icon | Default icon | Design and add custom icon assets for all densities + adaptive icon | `res/mipmap-*/`, `res/drawable/` | LOW | YES |
| B05 | P1 | Create and store release keystore | No `fitai-release.jks` | Generate keystore; store securely (not in source); configure CI env vars | `build.gradle.kts` | LOW | YES |
| B06 | P1 | No message pagination | `FirestoreCommunityDataSource.kt:76` | Add `.limit(50)` and cursor pagination to `getRecentMessages()` | `FirestoreCommunityDataSource.kt`, `ChannelChatScreen.kt` | MEDIUM | YES |
| B07 | P1 | No server search optimization | `FirestoreCommunityDataSource.kt:34` | Add `.limit(30)` to public server queries; implement Firestore `orderBy+startAt+endAt` for name search | `FirestoreCommunityDataSource.kt` | LOW | YES |
| B08 | P1 | `sessionScoreHistory` unbounded | `WorkoutViewModel.kt:83` | Cap the list at N=500 entries using a circular buffer | `WorkoutViewModel.kt` | LOW | YES |
| B09 | P1 | No email verification | `AuthRepository.kt:55` | Call `user.sendEmailVerification()` after sign-up | `AuthRepository.kt`, `AuthScreen.kt` | LOW | RECOMMENDED |
| B10 | P1 | Zero real tests | Test files | Write unit tests for `ConsistencyTracker`, `RepCounter`, `WorkoutScoreEngine`, `AuthRepository` error mapping | New test files | MEDIUM | YES |
| B11 | P1 | Firebase App Check | No implementation | Add Firebase App Check (Play Integrity) to prevent abuse | `build.gradle.kts`, `FitAIApplication.kt` | LOW | RECOMMENDED |
| B12 | P1 | Guest users can access Firestore | `AuthRepository.kt:84` | Gate all Firestore calls on `FirebaseAuth.currentUser != null`, not just `FirebaseConfig.isConfigured` | All data sources | MEDIUM | YES |
| B13 | P1 | `memberCount` not decremented on leave | `FirestoreCommunityDataSource.kt:120` | Use Firestore `FieldValue.increment(-1)` when setting isActive=false | `FirestoreCommunityDataSource.kt` | LOW | YES |
| B14 | P1 | Community posts are local-only | `CommunityRepository.kt:65` | Move post storage from Room to Firestore | `CommunityRepository.kt`, `PostDao`, new Firestore collection | HIGH | YES |

---

### P2 — POST-LAUNCH IMPROVEMENTS

| ID | Priority | Problem | Evidence | Proposed Solution | Files Affected | Complexity | Mandatory |
|---|---|---|---|---|---|---|---|
| C01 | P2 | No push notifications (FCM) | No FCM dependency | Add FCM; implement Cloud Functions to trigger missed workout alerts | `build.gradle.kts`, Cloud Functions | HIGH | NO |
| C02 | P2 | No profile photo upload | `avatarUrl` field unused | Integrate Firebase Storage for profile photos | New Firebase Storage integration | MEDIUM | NO |
| C03 | P2 | Meal tracking UI missing | MealPost model exists | Build meal log screen with photo picker (Firebase Storage) | New screen + viewmodel | HIGH | NO |
| C04 | P2 | Routines not synced to Firestore | `CommunityRepository.kt:46` | Move routine storage to Firestore for cross-device/cross-user access | `CommunityRepository.kt`, new Firestore collection | MEDIUM | NO |
| C05 | P2 | Google Sign-In disabled | `AuthScreen.kt:376-388` | Implement Firebase Google Sign-In (requires SHA-1 fingerprint in Firebase Console) | `AuthRepository.kt`, `AuthScreen.kt` | MEDIUM | NO |
| C06 | P2 | WebRTC only 1:1 | `WebRtcManager.kt` | Design multi-party session architecture (SFU vs mesh) | Architecture decision needed | HIGH | NO |
| C07 | P2 | No DI framework | Manual everywhere | Introduce Hilt for ViewModels and repositories | All ViewModels, all repositories | HIGH | NO |
| C08 | P2 | Avatar screen inaccessible | No nav trigger | Add Avatar screen navigation from Profile | `MainActivity.kt`, `ProfileScreen.kt` | LOW | NO |
| C09 | P2 | Dead Room tables | servers/channels/messages in Room | Remove or repurpose the local Room tables for server/channel/message entities | `FitnessDatabase.kt`, migration 3→4 | MEDIUM | NO |

---

### P3 — FUTURE SCALE

| ID | Priority | Problem | Evidence | Proposed Solution | Files Affected | Complexity | Mandatory |
|---|---|---|---|---|---|---|---|
| D01 | P3 | No Firestore offline persistence | No config | Enable `FirebaseFirestore.getInstance().firestoreSettings` with `isPersistenceEnabled = true` | `FitAIApplication.kt` or data source init | LOW | NO |
| D02 | P3 | No search service | Client-side filtering | Integrate Algolia or Typesense for full-text user/server search | Cloud Functions + search SDK | HIGH | NO |
| D03 | P3 | Trainer platform features | Not implemented | Design trainer dashboard, client roster, workout assignment | Major new feature set | HIGH | NO |
| D04 | P3 | Subscription/payment | Not implemented | Integrate Google Play Billing or Stripe | New payment module | HIGH | NO |
| D05 | P3 | AI food analysis | Not implemented | Integrate Google Vision AI or OpenAI for meal photo analysis | Cloud Function + Storage trigger | HIGH | NO |
| D06 | P3 | Cloud Functions for validation | Not implemented | Server-side message validation, rate limiting, banned words | Cloud Functions | MEDIUM | NO |
| D07 | P3 | Stale live_calls cleanup | Accumulates forever | Cloud Function scheduled job to delete ended calls older than 24h | Cloud Functions | LOW | NO |

---

## A. CTO-LEVEL VERDICT

**NO — NOT READY FOR PUBLIC LAUNCH.**

The application has a genuinely impressive AI workout tracking core and a functional community platform. However, it has **critical security vulnerabilities** (exposed API key, open Firestore, public TURN server, insecure keystore), is **missing compliance requirements** (account deletion, privacy policy), has **virtually no tests**, and is branded as "Pravin" rather than "FitAI". These are not minor polish issues — several are Play Store rejection criteria or serious security risks.

**The app is approximately 4–6 weeks of focused work away from a minimal viable public launch**, assuming the P0 and P1 items are addressed in order.

---

## B. TOP 10 MOST IMPORTANT ISSUES

1. **Firebase API key hardcoded in source code** (SEC-01) — rotate immediately
2. **No Firestore Security Rules** (SEC-03) — open database is a data breach waiting to happen
3. **App name and package ID are "Pravin"** (PB-02, PB-03) — cannot publish to Play Store
4. **Release keystore does not exist / fallback password "changeme"** (PB-05, PB-06, SEC-02)
5. **Account deletion missing** (PB-08) — Play Store policy compliance blocker
6. **Public untrusted TURN server** (SEC-04) — unreliable for users, privacy violation
7. **No crash reporting** (PB-10) — impossible to monitor production health
8. **Community posts are local-only** (B14) — community feature is fundamentally broken
9. **No ProGuard rules** (B01) — release build may crash due to stripped classes
10. **Zero real tests** (B10) — no confidence in correctness

---

## C. EXACT NEXT STEPS IN IMPLEMENTATION ORDER

1. **Rotate Firebase API key** in Firebase Console → update `google-services.json` → add to `.gitignore` → delete `FirebaseConfig.kt` hardcoded constants
2. **Rename app**: change `applicationId`, `namespace`, `app_name` string, theme name from "Pravin" to "FitAI"
3. **Generate release keystore** (`keytool -genkey`) → store securely → configure env vars in build
4. **Fix keystore fallback**: remove `?: "changeme"` from `build.gradle.kts`
5. **Write and deploy Firestore Security Rules** (`firestore.rules`) — start with: authenticated users can read public servers, write only to their own documents
6. **Add Firebase Crashlytics** dependency and initialize in `FitAIApplication`
7. **Design and add custom app icon** assets for all densities
8. **Add ProGuard keep rules** for MediaPipe, WebRTC, Room, Firebase in `proguard-rules.pro`
9. **Fix backup rules** to exclude Room database and DataStore files
10. **Implement account deletion** (Profile screen → Firebase Auth delete → Firestore cleanup → Room clear → DataStore clear)
11. **Add privacy policy** URL to sign-up screen
12. **Provision private TURN server** and update `WebRtcManager.kt:122` ICE servers
13. **Paginate messages** in `FirestoreCommunityDataSource.getRecentMessages()` with `.limit(50)` and cursor
14. **Gate Firestore calls** on `FirebaseAuth.currentUser != null`, not just on `isConfigured`
15. **Write minimum test suite**: `ConsistencyTracker`, `RepCounter`, `AuthRepository` error mapping
16. **Move community posts to Firestore** (breaking change, requires migration)
17. **Enable Firebase App Check** in `FitAIApplication`
18. **Prepare Play Store listing**: screenshots, description, Data Safety form, privacy policy link

---

## D. QUESTIONS REQUIRING YOUR CONFIRMATION BEFORE IMPLEMENTATION

1. **App Identity**: What should the final application ID and brand name be? (`com.fitai.app`? `com.jsf.fitai`?) Changing the app ID after first publish is impossible.
2. **Trainer Feature Scope**: Is the trainer management platform part of the MVP, or a post-launch add-on?
3. **Subscription Model**: Will the app be free, freemium, or paid? This affects Play Store category and billing integration.
4. **Data Residency**: Are there any geographic constraints on where Firebase data is stored? (EU GDPR, India DPDP?)
5. **TURN Server**: Do you have a preference for TURN provider — self-hosted (Coturn on a VPS), Twilio, or Metered.ca paid tier?
6. **Post Visibility**: Should community posts be visible to all server members (Firestore) or remain device-local?
7. **Guest Mode**: Should guest/local users be able to join community servers, or should community require a Firebase account?
8. **Multi-account**: Should a single device support multiple FitAI accounts?
9. **AI Food Analysis**: Which AI provider to use for food recognition — Google Vision AI, OpenAI GPT-4V, or a custom model?
10. **Firebase Security Rules**: Should users be able to see other members' workout history within a server, or is it private?

---

## E. ITEMS THAT COULD NOT BE VERIFIED

1. **Firestore Security Rules** — Cannot verify deployed rules without Firebase Console access. The source repository contains no `firestore.rules` file.
2. **Firebase project quotas and billing status** — Blaze plan or Spark plan not visible from source code.
3. **Firebase Storage rules** — Storage is not integrated, but existing rules (if any) cannot be verified.
4. **Release keystore file** (`fitai-release.jks`) — Not present in the project. Cannot verify if it exists elsewhere.
5. **MediaPipe model file integrity** — `app/src/main/assets/pose_landmarker_full.task` is present (filename visible) but binary content cannot be verified for version correctness.
6. **Actual Play Store account status** — Cannot verify if a developer account exists or if the app has any previous submissions.
7. **Firebase Auth configuration** — Whether email verification, reCAPTCHA, and anonymous auth are enabled cannot be verified from source code.
8. **Firebase App Check enforcement state** — Cannot verify without Firebase Console.
9. **TURN server availability** — The `openrelayproject` public TURN server availability and reliability cannot be verified without live testing.
10. **Google Play Billing API setup** — Payment features are not implemented, so integration requirements cannot be scoped without your chosen monetization model.

---

*End of Audit — FitAI Production Readiness Report v1.0 — 2026-11-03*
