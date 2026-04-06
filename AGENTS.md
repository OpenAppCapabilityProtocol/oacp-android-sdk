# AGENTS.md — OACP Android SDK

This file is the authoritative context document for any AI agent working in this repository. Read it before making changes.

---

## What Is This

`oacp-android` is the official Kotlin SDK for the [Open App Capability Protocol](https://github.com/OpenAppCapabilityProtocol/oacp). It provides base classes that handle all OACP plumbing — ContentProvider registration, intent handling, parameter extraction, result broadcasting — so app developers only write their business logic.

**Maven coordinates:** `org.oacp:oacp-android:0.3.0`
**Min SDK:** 21 | **Compile SDK:** 35 | **Kotlin:** 2.0.21 | **Java:** 17

---

## Repository Structure

```
oacp-android-sdk/
├── build.gradle.kts                    # Root build config (AGP 8.5.2, Kotlin 2.0.21)
├── settings.gradle.kts                 # Includes :oacp-android module
├── gradle.properties
├── gradlew + gradle/                   # Gradle wrapper
├── oacp-android/                       # SDK library module
│   ├── build.gradle.kts                # Library config (namespace: org.oacp.android)
│   └── src/
│       ├── main/
│       │   ├── kotlin/org/oacp/android/
│       │   │   ├── Oacp.kt            # Protocol constants (actions, extras, statuses, errors)
│       │   │   ├── OacpReceiver.kt    # Abstract BroadcastReceiver with goAsync() threading
│       │   │   ├── OacpActivity.kt    # Abstract Activity for foreground actions
│       │   │   ├── OacpProvider.kt    # ContentProvider serving oacp.json + OACP.md
│       │   │   ├── OacpParams.kt      # Type-safe parameter accessor with suffix resolution
│       │   │   ├── OacpResult.kt      # Result builder + broadcaster
│       │   │   └── OacpEntitySource.kt # Interface for dynamic entity data
│       │   └── AndroidManifest.xml     # Auto-registers OacpProvider via manifest merger
│       └── test/
│           └── kotlin/org/oacp/android/
│               ├── OacpTest.kt
│               ├── OacpReceiverTest.kt
│               ├── OacpActivityTest.kt
│               ├── OacpProviderTest.kt
│               ├── OacpParamsTest.kt
│               ├── OacpResultTest.kt
│               └── OacpEntityTest.kt
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
└── LICENSE                             # Apache 2.0
```

---

## Core API

### `Oacp` — Protocol Constants

- `ACTION_RESULT = "org.oacp.ACTION_RESULT"` — broadcast action for async results
- `EXTRA_REQUEST_ID`, `EXTRA_STATUS`, `EXTRA_CAPABILITY_ID`, `EXTRA_MESSAGE`, `EXTRA_RESULT`, `EXTRA_ERROR_CODE`
- Status values: `STATUS_ACCEPTED`, `STATUS_STARTED`, `STATUS_COMPLETED`, `STATUS_FAILED`, `STATUS_CANCELLED`
- Error codes: `ERROR_INVALID_PARAMETERS`, `ERROR_NOT_FOUND`, `ERROR_NOT_AUTHENTICATED`, `ERROR_INTERNAL`, etc.
- Provider: `PROVIDER_AUTHORITY_SUFFIX = ".oacp"`, paths: `/manifest`, `/context`, `/entities`

### `OacpReceiver` — Background Actions

Abstract BroadcastReceiver. Uses `goAsync()` + thread pool internally (no ANR risk).

```kotlin
abstract fun onAction(context: Context, action: String, params: OacpParams, requestId: String?): OacpResult?
```

### `OacpActivity` — Foreground Actions

Abstract Activity. Auto-extracts `requestId`, `capabilityId`, `params` from intent.

```kotlin
protected abstract fun onOacpAction(action: String, params: OacpParams)
protected fun sendResult(result: OacpResult, targetPackage: String? = null)
```

### `OacpProvider` — Discovery

ContentProvider auto-registered via manifest merger at `${applicationId}.oacp`.
- `/manifest` → serves `assets/oacp.json` with `__APPLICATION_ID__` substitution
- `/context` → serves `assets/OACP.md`
- `/entities/<type>?q=query` → dynamic entity data via `OacpEntitySource`

### `OacpParams` — Parameter Access

Type-safe accessor with suffix-based key resolution for build variant safety.

```kotlin
params.getString("query")    // finds "query", "EXTRA_QUERY", ".EXTRA_QUERY", etc.
params.getInt("amount")      // handles int/long/string/double coercion
params.getBoolean("enabled") // handles bool/string/int coercion
```

### `OacpResult` — Result Broadcasting

```kotlin
OacpResult.success("Battery is at 85%")
OacpResult.error(Oacp.ERROR_NOT_FOUND, "No alarms set")
OacpResult.cancelled("User cancelled")
```

### `OacpEntitySource` — Dynamic Entities

```kotlin
interface OacpEntitySource {
    fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity>
}
```

---

## Development

```bash
./gradlew :oacp-android:build      # Build
./gradlew :oacp-android:test       # Run tests (Robolectric)
./gradlew :oacp-android:lint       # Lint
```

### Test framework

Robolectric + JUnit 4. Tests use `CountDownLatch` for async verification of `goAsync()` completion.

---

## Working Rules

1. **Explicit API mode is ON.** Every public symbol needs explicit `public` visibility. Internal helpers must be `internal` or `private`.
2. **Backward compatibility matters.** This is a library consumed by third-party apps. Do not break public method signatures without version bump.
3. **`__APPLICATION_ID__` substitution** in OacpProvider is critical for build variant support (debug/release). Do not change this behavior.
4. **Suffix-based key resolution** in OacpParams is intentional — it handles the case where intent extras are prefixed with the app's package name. Do not simplify to exact-match only.
5. **Feature branches only.** Never commit directly to `main`.
6. **Stage specific files.** Never `git add -A`.

---

## Related Repos

| Repo | What |
|------|------|
| [OpenAppCapabilityProtocol/oacp](https://github.com/OpenAppCapabilityProtocol/oacp) | Protocol spec, docs, example app |
| [OpenAppCapabilityProtocol/hark](https://github.com/OpenAppCapabilityProtocol/hark) | Reference voice assistant that consumes OACP |

---

## Git

- **Remote:** `https://github.com/OpenAppCapabilityProtocol/oacp-android-sdk.git`
- **Default branch:** `main`
