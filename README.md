# FinanceAI Android

FinanceAI is an offline-first Android personal finance application with multi-device synchronization for income and expense tracking, budgets, scheduled reminders, and AI-assisted financial analysis. Users can manage local financial records offline, with pending changes synchronized across devices when connectivity is restored. It uses Room for local financial data, Firebase for account synchronization and notification coordination, and Firebase AI Logic for the financial assistant.

Local financial records remain available offline for a prepared account. Authentication, cloud synchronization, remote media, Maps services, and AI requests require connectivity.

## Key Features

- Email/password and Google sign-in, email verification, and password recovery
- Income and expense records with categories, dates, optional photos and locations
- Transaction history filtered by date, type, and category
- General monthly budgets, category limits, percentage allocations, and spending summaries
- Scheduled transactions with confirmation, snooze, and account-scoped multi-device coordination
- Current-month financial analysis through quick questions and free-text AI chat

## Architecture

The Android client follows feature-first organization with MVVM and separate domain, data, and presentation responsibilities.

```text
app/                   # Application wiring, navigation, startup, backup, cross-feature coordination
core/                  # Database, session, synchronization, money, time, media, shared UI
feature/
├── auth/
├── transaction/
├── schedule/
├── budget/
├── aichat/
├── home/
└── location/
fcm/                   # Push parsing, token registration, messaging service and work
notification/          # Android notification presentation, actions and work
photo/                 # Photo upload workers and retained worker entry points

functions/
├── index.js            # Firebase entry points
├── src/auth/           # Account lookup and authentication-related callable logic
├── src/schedule/       # Command processing, shared reminder state and reconciliation
└── test/               # Backend unit, rules and emulator integration tests
```

Within a feature, `domain` owns models, repository contracts, and business rules; `data` owns Room/SDK access, mappers, and repository implementations; `presentation` owns Compose UI, ViewModels, and UI models. Features add these packages and `di` bindings where needed. DAO and entity files remain with the feature or core capability that owns their tables.

Presentation uses domain contracts, while data implements them. Hilt bindings and the application composition layer connect the implementations. Cross-feature completion coordination lives in `app`; shared reconciliation and account isolation live in `core`.

### UI and Navigation

Routes obtain ViewModels and connect lifecycle-aware state, provider launchers, and feature navigation. Screens and their components receive the state and callbacks they need. ViewModels manage form/business state and asynchronous operations; dialog visibility, dropdown expansion, and similar display state stay in Compose.

Navigation Compose destinations use Kotlin Serialization. External notification/password-reset URI identities are defined independently in `core/deeplink`. A single `MainActivity` hosts the UI, while `FinanceApplication` configures application-level dependencies and WorkManager.

## Technical Decisions

### Offline-First Synchronization and Account Isolation

Repositories commit financial changes and their pending synchronization payloads in the same Room transaction. `AccountSyncEngine` pushes pending intent before pulling server records; Firestore listeners also update local data. Network-constrained WorkManager jobs retain retryable work.

Synchronization compares the last accepted baseline, the local change, and the remote version. Independent field edits can merge; overlapping changes retain both versions for an explicit user choice. Mutation identities and revisions prevent an old acknowledgement from clearing a newer local edit. Deletion uses tombstones rather than treating every missing local row as a successful remote deletion.

Account-owned queries, session generation checks, and work tags isolate records and background operations. Account switches cancel the previous account's work and notifications. Pending financial intent remains stored for its owner and is restored when that account becomes active again.

### Multi-Device Reminders

Android persists notification actions as commands; Functions processes them through Firestore transactions and publishes shared state revisions. FCM events prompt devices to synchronize that state rather than serving as the authoritative reminder record.

- Automatic reminder slots are 09:00 and 18:00 in the account's time zone.
- Each explicit snooze requests one hour from its original request time; it does not create an automatic hourly loop.
- Completion uses a stable `completed_<planId>` financial identity, making duplicate completion requests idempotent.
- Completed-plan conflicts update the existing financial record while preserving its completion date, instead of reopening the plan or duplicating income/expense.
- After server acceptance, other connected devices update or cancel their local reminders. Offline actions propagate after reconnection.
- The overdue plan's expiration retention starts at the first server acceptance of an `expiration_shown` receipt and lasts 24 hours. Android queues that receipt only after successfully posting the expiration notification; FCM delivery alone does not count as display. Completed financial records are outside this expiration cleanup.

WorkManager wake-ups and FCM delivery are not exact-time guarantees. Permission/channel denial keeps reminder intent pending. Android and Functions share contract fixtures for command types, fields, reminder policy, and completion identities.

### Financial Values and Media

Room and remote financial payloads use integer `amountMinor` values with an ISO currency code. `MoneyAmounts` uses decimal conversion at form/storage boundaries, validates supported precision, and sums in minor units. Domain/UI values still use validated major-unit `Double` representations.

Camera capture uses `ActivityResultContracts.TakePicture` and `FileProvider`. Photo preparation corrects orientation and bounds image dimensions before JPEG storage. Account-owned, versioned uploads prevent an older photo operation from replacing a newer attachment.

### AI Financial Context

Firebase AI Logic currently configures `gemini-3.5-flash-lite` with `LOW` thinking. A request receives the active account's financial report for the current calendar month, including income, expenses, category spending, and budget usage. Older financial records remain stored but are not automatically included in that report.

The prompt carries month/year and ISO date boundaries without English month names. The quick question's localized XML text or the manual question determines the response language; financial values and currency remain unchanged. The system instruction handles empty-month data, brief greetings, and unrelated questions without inventing spending.

Requests use bounded timeouts and retries. Expected failures are presented as errors rather than stored as AI replies, and retry preserves the existing user-message identity. Model availability and provider quotas still apply. Financial context is sent to the AI provider for generation; use synthetic records for portfolio demos.

### Errors, Time Zones, Backup and Compatibility

- Expected errors use shared or feature-owned typed exceptions and presentation mappings to XML messages. Cancellation remains cancellation; programming preconditions and unknown causes are preserved.
- The account's first accepted time zone is persisted for reminders and reminder date selection. Home, budget, history, and AI calendar periods use the device time zone. This deliberate distinction can place a boundary timestamp in different months on devices in different zones.
- The shared near-budget warning threshold is 80%; calculations and warning priority stay separate.
- Backup retains a sanitized Room snapshot and retained photos, while excluding active session/device token state and SDK credentials. Restored users sign in again; account work is rebuilt from retained records.
- Existing wire values, legacy Functions adapters, retained Worker identities, and old PendingIntent actions remain where compatibility requires them. For example, the historical `ACTION_CANCEL` identity still maps to snooze.

## Firebase Projects

| Firebase app | Responsibilities |
|---|---|
| Main `[DEFAULT]` app | Authentication, Firestore, Storage, FCM and Functions; user profiles, financial records and persisted chat history |
| Named `finance-ai` app | Firebase AI Logic model access and its own App Check instance |

The main backend uses Blaze for deployed Functions. A separate AI project allows demo model access to use eligible Spark/free-tier access independently of the main project's billing configuration. Linking billing to the AI project changes Gemini Developer API usage to paid-tier pricing. Free-tier access is limited and depends on model availability and provider conditions; this split does not remove quotas. See [Firebase AI Logic pricing](https://firebase.google.com/docs/ai-logic/pricing).

`@AiFirebaseApp` qualifies the secondary client. `AiFirebaseConfig.of(...)` converts the `BuildConfig.AI_FIREBASE_*` values to FirebaseOptions, and the code requires an AI project ID different from the main project ID. The AI-only app disables automatic collection for unrelated SDKs. No second user account, database, Storage, or FCM client is configured. The AI project receives report payloads for generation; persistent account data and chat history stay in the main project.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3, Navigation Compose and Kotlin Serialization
- Hilt, KSP, Coroutines, Flow and StateFlow
- Room, WorkManager, Gson and Coil
- Firebase Authentication, Firestore, Storage, Cloud Messaging, Functions, AI Logic and App Check
- Android Credential Manager, Google ID, Maps SDK/Maps Compose and Play services location
- Node.js 22, Firebase Admin SDK, Firebase Functions SDK and Luxon
- JUnit, Mockito, Coroutines Test, Compose UI Test, WorkManager testing and Firebase Rules Unit Testing

Dependencies and plugins are managed in [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Requirements

- Android Studio, JDK 21, and Android SDK 36
- Android API 30 or newer on the device
- Your own main Firebase project and a separate AI Firebase project
- A Maps-enabled Google Cloud project and a restricted Android Maps API key
- Node.js 22 and pnpm for Functions development; Firebase CLI is included in its development dependencies
- Billing configured for deployed backend services; [Functions deployment requires Blaze](https://firebase.google.com/docs/functions/get-started)

## Local Setup

### 1. Clone and Register the Firebase Apps

```bash
git clone https://github.com/Ahmetkaragunlu/FinanceAI.git
cd FinanceAI
```

Register an Android app with package name `com.ahmetkaragunlu.financeai` in each Firebase project.

In the **main project**, enable Email/Password and Google authentication and configure Firestore and Storage. Download its Android `google-services.json` to `app/google-services.json`. The Google Services plugin generates Android resources and the Google web OAuth client ID from this file.

In the **AI project**, set up Firebase AI Logic with the Gemini Developer API. Its downloaded Android configuration supplies the three AI values below; do not replace the main JSON with the AI JSON or put the AI JSON in `res/raw`. No additional Android `GEMINI_API_KEY` is needed.

[app/google-services.json.example](app/google-services.json.example) documents the shape only. Its placeholders and empty OAuth list are not a working configuration. Google sign-in requires the real downloaded JSON with a web OAuth client entry.

### 2. Supply Local Configuration

Enable Maps SDK for Android in your Google Cloud project and create a separate Maps key. Copy [local.properties.example](local.properties.example) to the ignored `local.properties` file and fill in your values:

```properties
sdk.dir=/absolute/path/to/Android/sdk
MAPS_API_KEY=your-restricted-android-maps-key
AI_FIREBASE_PROJECT_ID=your-ai-firebase-project-id
AI_FIREBASE_APP_ID=your-ai-firebase-android-app-id
AI_FIREBASE_API_KEY=your-restricted-ai-firebase-client-key
```

| Setting | Source |
|---|---|
| `sdk.dir` | Your local Android SDK path |
| `MAPS_API_KEY` | Google Cloud Console → APIs & Services → Credentials |
| `AI_FIREBASE_PROJECT_ID` | AI project's JSON → `project_info.project_id` |
| `AI_FIREBASE_APP_ID` | Matching Android client → `client_info.mobilesdk_app_id` |
| `AI_FIREBASE_API_KEY` | Matching Android client → `api_key[].current_key`; this is a Firebase client key |

Maps/AI settings resolve in **environment variable → Gradle property → local.properties** order. Missing required fields fail during Gradle configuration, including Android Studio sync, with the setting name. Project/app IDs are identifiers; they are not passwords or signing keys.

### 3. Configure Google Sign-In and Certificates

With local configuration in place, obtain this machine's certificate fingerprints:

```bash
./gradlew :app:signingReport
```

Register the appropriate SHA-1/SHA-256 certificates in the Firebase Android app settings. Google sign-in uses the main project's OAuth configuration. Restrict the Maps key to the Android package and matching SHA-1 signing certificate, and to the Maps APIs used by the app. Follow the [Maps SDK setup guide](https://developers.google.com/maps/documentation/android-sdk/get-api-key) for project/API setup.

Re-download the **main** `google-services.json` after changing Google provider/OAuth or certificate configuration. A different developer's debug certificate is not automatically authorized.

### 4. Configure Your Main Backend

The repository includes Functions in [functions/](functions/), [firestore.rules](firestore.rules), and [storage.rules](storage.rules). Install the locked Functions dependencies and authenticate Firebase CLI with your own account. Deploy only to your own main project:

```bash
pnpm --dir functions install --frozen-lockfile
pnpm --dir functions exec firebase deploy --config ../firebase.json --project YOUR_MAIN_FIREBASE_PROJECT_ID --only functions,firestore:rules,storage
```

Callables are configured in `us-central1`; Android uses the same region. Pre-login account lookups enforce App Check, while protected account operations validate authentication/ownership. Shared reminder state and delivery events are server-owned.

The deployed callable endpoints must allow Firebase SDK invocation at the HTTP/IAM layer; access checks remain in callable logic and Security Rules. The maintainer repair script in `scripts/ensure-firebase-callable-access.cjs` is pinned to the original project and is not a clone setup command.

## App Check: Debug and Release

`app/src/debug` installs the Debug App Check provider, and `app/src/release` installs Play Integrity. App Check is initialized separately for the main and named AI apps.

For local development:

1. Run the debug app on your emulator or test device and trigger a protected main-project request.
2. Find the generated value under `DebugAppCheckProvider` in Android Studio Logcat. In the **main project's** Firebase Console, open **App Check → Apps → Android app overflow menu → Manage debug tokens**, add the token, and save.
3. After signing in, open the AI feature and trigger its request to obtain the **AI app's** token. Register that token through the same menu in the **AI project's** Console.
4. Verify App Check configuration/enforcement for the main services you use and keep it enforced for Firebase AI Logic. See [AI Logic App Check setup](https://firebase.google.com/docs/ai-logic/app-check).

Tokens authorize development installations; they are not Google OAuth client IDs or API keys. Another emulator/device may generate different tokens. Clearing app data or reinstalling can regenerate them; ordinary in-place updates that retain data usually retain them.

Optional local notes can use the ignored `appcheck-debug-token.local.txt` and `appcheck-ai-debug-token.local.txt` filenames. Never put debug tokens in Git, BuildConfig, or release builds. See the [Android debug provider guide](https://firebase.google.com/docs/app-check/android/debug-provider).

For release, configure Play Integrity and the release/Google Play signing certificates in **both** Firebase projects, plus the corresponding main-project OAuth and Maps settings. End users do not register debug tokens individually. Release signing is not preconfigured in this repository; keep the keystore/private key outside Git and validate the intended distribution's attestation settings.

## Build, Tests and CI

After service configuration:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

After changing client keys, use a clean build to avoid stale inlined BuildConfig values:

```bash
./gradlew clean :app:assembleDebug
```

| Suite | Location and responsibility |
|---|---|
| Android JVM | `app/src/test`: pure policies, calculations, reconciliation, ViewModels and isolated mocks |
| Android device/Compose | `app/src/androidTest`: UI callbacks/state restoration, Room/migrations, media, Workers and SDK boundaries |
| Functions unit/syntax | `functions/test`: handlers, auth, schedule policy, idempotency and contracts |
| Rules/integration | `functions/test/rules`, `functions/test/integration` and Android SDK integration fixtures: explicit local Firebase emulators |

Tests mirror production ownership. A simple rename does not require a new test; behavior, cancellation, account isolation, concurrency, and data integrity determine test scope.

Functions checks:

```bash
pnpm --dir functions run check
pnpm --dir functions test
```

For local Firestore/Storage suites, start emulators in one terminal:

```bash
pnpm --dir functions exec firebase emulators:start --config ../firebase.test.json --project demo-financeai --only firestore,storage
```

Then run the backend emulator suites from another terminal:

```bash
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 FIREBASE_STORAGE_EMULATOR_HOST=127.0.0.1:9199 pnpm --dir functions run test:rules
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 pnpm --dir functions run test:integration
```

Android device tests require a running emulator/device. Firestore-backed classes additionally require explicit local host/port arguments; the integration fixture uses the isolated `demo-financeai-integration` project:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firestoreHost=10.0.2.2 -Pandroid.testInstrumentationRunnerArguments.firestorePort=8080
```

`10.0.2.2` addresses the development host from an Android emulator; a physical device needs localhost port forwarding. Keep production background FCM delivery inactive during isolated instrumentation runs. SDK routing checks do not generate a live Gemini reply.

[GitHub Actions CI](.github/workflows/ci.yml) runs Android JVM tests and Functions unit/syntax checks with JDK 21, Node 22 and pnpm. It uses synthetic Maps/AI values and a generated synthetic main JSON through [prepare-android-config.cjs](scripts/ci/prepare-android-config.cjs), not real service credentials. The script refuses to overwrite existing local configuration and is for CI, not interactive app setup.

Rules, device/Compose and SDK integration suites are separate local-emulator runs. CI does not deploy, send live AI/FCM requests, or distribute APKs; its actions are commit-pinned and permissions are limited to repository read access.

## Security Notes

Ignore rules cover `app/google-services.json`, the former `app/src/main/res/raw/ai_google_services.json` location, `local.properties`, `.env` files, keystores, supported server-credential filename patterns, debug token notes and logs. `node_modules` is also ignored. Keep downloaded AI JSON and any other server credential files outside the checkout; the app only needs the three AI client values in local configuration. Do not force-add private files or print their values in build logs. `AiFirebaseConfig.toString()` also redacts the client API key.

Firebase/Maps client keys may be extracted from an APK even when supplied through local properties or BuildConfig. Git exclusion keeps the repository clean; authorization depends on Authentication, Security Rules, App Check, and suitable API/application restrictions. Firebase client keys should allow only required Firebase APIs; never add `generativelanguage.googleapis.com` to their allowlist or include the server-side Gemini Developer API key in Android. See [Firebase API key guidance](https://firebase.google.com/docs/projects/api-keys).

Choose Firebase application restrictions compatible with the SDKs in use and verify access after applying them; Maps package/certificate restrictions are a separate configuration.

An ignore rule does not remove previously tracked values from Git history. If credentials are exposed, update restricted replacements locally, verify them, revoke the old credentials, and handle tracked/history copies separately. Preserve the existing Firebase project/app identities when rotating a client key.

Functions deployment uses your Firebase CLI account/project permissions. Server private keys do not belong in the Android application, and public SHA fingerprints are not keystore/private keys.

## Screenshots

### Authentication

![Auth Screen](https://github.com/Ahmetkaragunlu/FinanceAI/blob/master/auth.jpg?raw=true)

### Home and Financial Overview

![Home Screen](https://github.com/Ahmetkaragunlu/FinanceAI/blob/master/home.png?raw=true)

### AI Financial Assistant

![AI Screen](https://github.com/Ahmetkaragunlu/FinanceAI/blob/master/ai.png?raw=true)
