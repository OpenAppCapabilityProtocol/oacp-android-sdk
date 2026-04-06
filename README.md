# oacp-android

**Make any Android app voice-controllable in 5 minutes.**

`oacp-android` is the official Android SDK for the [Open App Capability Protocol (OACP)](https://github.com/OpenAppCapabilityProtocol/oacp) — the open standard that lets any voice assistant understand and control any Android app.

> MCP gave AI agents tools on servers.
> OACP gives AI assistants capabilities inside mobile apps.

---

## Quick Start

### 1. Add the dependency

**Option A: Local AAR** (available now)

Download `oacp-android-release.aar` from the [latest release](https://github.com/OpenAppCapabilityProtocol/oacp-android-sdk/releases), copy it to `app/libs/`, then:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(files("libs/oacp-android-release.aar"))
    implementation("androidx.annotation:annotation:1.7.1")
}
```

**Option B: Maven Central** (coming soon)

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("org.oacp:oacp-android:0.3.0")
}
```

### 2. Create `assets/oacp.json`

Declare what your app can do:

```json
{
  "oacpVersion": "0.3",
  "appId": "__APPLICATION_ID__",
  "displayName": "My App",
  "capabilities": [
    {
      "id": "do_thing",
      "description": "Does the thing when the user asks",
      "parameters": [],
      "confirmation": "never",
      "visibility": "public",
      "invoke": {
        "android": {
          "type": "broadcast",
          "action": "__APPLICATION_ID__.ACTION_DO_THING"
        }
      }
    }
  ]
}
```

### 3. Create `assets/OACP.md`

Add semantic context for AI assistants:

```markdown
# My App — OACP Context

## What this app does
My App does the thing. Users typically say "do the thing" or "make it happen."

## Disambiguation
- "do the thing" → do_thing
- "make it happen" → do_thing
```

### 4. Handle the action

```kotlin
class MyReceiver : OacpReceiver() {
    override fun onAction(
        context: Context,
        action: String,
        params: OacpParams,
        requestId: String?
    ): OacpResult? {
        return when {
            action.endsWith(".ACTION_DO_THING") -> {
                doTheThing()
                OacpResult.success("Done!")
            }
            else -> null
        }
    }
}
```

### 5. Register the receiver in `AndroidManifest.xml`

```xml
<receiver android:name=".MyReceiver" android:exported="true">
    <intent-filter>
        <action android:name="${applicationId}.ACTION_DO_THING" />
    </intent-filter>
</receiver>
```

**That's it.** The SDK auto-registers the ContentProvider via manifest merger. Your app is now discoverable and controllable by Hark and any OACP-compatible assistant.

---

## What You Get

| Component | What it does |
|-----------|-------------|
| **`OacpProvider`** | ContentProvider auto-registered via manifest merger. Serves `oacp.json` and `OACP.md` at `${applicationId}.oacp`. Handles `__APPLICATION_ID__` substitution for build variants. |
| **`OacpReceiver`** | Abstract BroadcastReceiver base class. Parses parameters, extracts request IDs, sends async results. You implement one method. |
| **`OacpParams`** | Type-safe parameter access. `params.getString("query")`, `params.getInt("volume")`. Matches by suffix for build-variant safety. |
| **`OacpResult`** | Result builder. `OacpResult.success("Done!")`, `OacpResult.error("not_found", "...")`. Auto-broadcasts to the assistant. |
| **`Oacp`** | Protocol constants — standard extra keys, status values, error codes. |

---

## Parameters

Assistants pass parameters as intent extras. Use `OacpParams` for type-safe access:

```kotlin
override fun onAction(context: Context, action: String, params: OacpParams, requestId: String?): OacpResult? {
    val query = params.getString("query") ?: return OacpResult.error("missing_parameters", "query is required")
    val limit = params.getInt("limit") ?: 10
    val verbose = params.getBoolean("verbose") ?: false

    val results = search(query, limit, verbose)
    return OacpResult.success("Found ${results.size} results", mapOf("count" to results.size))
}
```

`OacpParams` matches by suffix — if the intent has `com.example.app.EXTRA_QUERY`, calling `getString("query")` finds it. This means your receiver works across debug/release build variants without changes.

---

## Async Results

For long-running operations, return results asynchronously:

**In `oacp.json`:**
```json
{
  "id": "get_weather",
  "completionMode": "async_result",
  "resultTransport": {
    "android": { "type": "broadcast", "action": "org.oacp.ACTION_RESULT" }
  }
}
```

**In your receiver:**
```kotlin
override fun onAction(context: Context, action: String, params: OacpParams, requestId: String?): OacpResult? {
    // Start background work
    fetchWeatherAsync { weather ->
        OacpResult.success(
            "Currently ${weather.temp}°C and ${weather.condition}",
            mapOf("temperature" to weather.temp, "condition" to weather.condition)
        ).send(context, requestId!!, "get_weather")
    }

    // Acknowledge immediately
    return OacpResult.accepted("Checking the weather...")
}
```

The assistant receives both the acknowledgment and the final result, keeping the user informed.

---

## Build Variant Safety

OACP is designed for real Android projects with multiple build variants:

- **`oacp.json`**: Use `__APPLICATION_ID__` — replaced at runtime by `OacpProvider`
- **`AndroidManifest.xml`**: Use `${applicationId}` — resolved by Gradle
- **Receiver code**: Match with `action.endsWith(".ACTION_X")` — works for `.debug`, `.staging`, etc.
- **`OacpParams`**: Resolves parameters by suffix — `EXTRA_QUERY` matches regardless of prefix

---

## Foreground Actions

For actions that need UI (camera, editor, etc.), use an Activity instead of a BroadcastReceiver:

**In `oacp.json`:**
```json
{
  "invoke": {
    "android": { "type": "activity", "action": "__APPLICATION_ID__.ACTION_OPEN_CAMERA" }
  }
}
```

**In `AndroidManifest.xml`:**
```xml
<activity android:name=".CameraActivity" android:exported="true">
    <intent-filter>
        <action android:name="${applicationId}.ACTION_OPEN_CAMERA" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

Parse parameters from the launching intent in your Activity's `onCreate`.

---

## Testing with adb

Verify your integration without installing an assistant:

```bash
# Check discovery
adb shell content query --uri content://com.example.myapp.oacp/manifest

# Test a broadcast action
adb shell am broadcast -a com.example.myapp.ACTION_DO_THING \
  --es org.oacp.extra.REQUEST_ID "test-123"

# Test with parameters
adb shell am broadcast -a com.example.myapp.ACTION_SEARCH \
  --es com.example.myapp.EXTRA_QUERY "hello world" \
  --es org.oacp.extra.REQUEST_ID "test-456"
```

---

## API Reference

### `Oacp` — Protocol constants

| Constant | Value |
|----------|-------|
| `Oacp.ACTION_RESULT` | `"org.oacp.ACTION_RESULT"` |
| `Oacp.EXTRA_REQUEST_ID` | `"org.oacp.extra.REQUEST_ID"` |
| `Oacp.EXTRA_STATUS` | `"org.oacp.extra.STATUS"` |
| `Oacp.EXTRA_MESSAGE` | `"org.oacp.extra.MESSAGE"` |
| `Oacp.EXTRA_RESULT` | `"org.oacp.extra.RESULT"` |
| `Oacp.STATUS_COMPLETED` | `"completed"` |
| `Oacp.STATUS_FAILED` | `"failed"` |
| `Oacp.ERROR_NOT_FOUND` | `"not_found"` |
| `Oacp.ERROR_MISSING_PARAMETERS` | `"missing_parameters"` |

See [`Oacp.kt`](oacp-android/src/main/kotlin/org/oacp/android/Oacp.kt) for the full list.

### `OacpResult` — Factory methods

| Method | Description |
|--------|-------------|
| `OacpResult.success(message, data?)` | Action completed |
| `OacpResult.accepted(message?)` | Action acknowledged, processing |
| `OacpResult.started(message?)` | Processing began |
| `OacpResult.error(code, message, retryable?)` | Action failed |
| `OacpResult.cancelled(message?)` | Action cancelled |

---

## Requirements

- Android minSdk 21+
- Kotlin or Java
- Works with any framework: native Android, Flutter, React Native, Compose

---

## License

Apache 2.0 — see [LICENSE](LICENSE)
