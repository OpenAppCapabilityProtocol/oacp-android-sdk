# OACP Android SDK

Make your Android app voice-controllable. Apps expose capabilities via a JSON manifest and handle voice commands through standard Android components (Activities and BroadcastReceivers).

## Quick Start

### 1. Add the SDK

Copy `oacp-android-release.aar` to `app/libs/`, then in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(files("libs/oacp-android-release.aar"))
    implementation("androidx.annotation:annotation:1.7.1")
}
```

### 2. Create `app/src/main/assets/oacp.json`

```json
{
  "oacpVersion": "0.3",
  "appId": "__APPLICATION_ID__",
  "displayName": "My App",
  "capabilities": [
    {
      "id": "do_thing",
      "description": "Does the thing",
      "parameters": [],
      "confirmation": "never",
      "visibility": "public",
      "invoke": {
        "android": {
          "type": "broadcast",
          "action": "__APPLICATION_ID__.oacp.ACTION_DO_THING"
        }
      }
    }
  ]
}
```

`__APPLICATION_ID__` is auto-replaced with your package name at runtime.

### 3. Create `app/src/main/assets/OACP.md`

```markdown
# My App — OACP Context
## What this app does
Describe your app for the voice assistant's LLM.
## Disambiguation
- "do the thing" → do_thing
```

### 4. Handle the action

See the two patterns below.

### 5. Done

The SDK auto-registers a ContentProvider via manifest merger. The voice assistant discovers your app at `content://${applicationId}.oacp/manifest`.

---

## Two Dispatch Patterns

Every OACP capability uses one of two dispatch types. Choose based on whether the action needs your app's UI.

| | Activity (foreground) | Broadcast (background) |
|---|---|---|
| **When to use** | Action needs app UI (camera, recorder, scanner, navigation) | Action works silently (check data, toggle setting, media controls) |
| **oacp.json type** | `"type": "activity"` | `"type": "broadcast"` |
| **How assistant dispatches** | `startActivity()` | `sendBroadcast()` |
| **What handles it** | Your Activity (intent filter) | `OacpReceiver` subclass |
| **Opens your app?** | Yes | No |
| **Android BAL safe?** | Yes (assistant is foreground) | N/A (no activity launch) |
| **SDK class** | `OacpActivity` or existing Activity | `OacpReceiver` |

---

## Pattern 1: Activity Dispatch (Foreground)

For actions that open your app. The voice assistant calls `startActivity()` directly — no BAL restrictions because the assistant is a foreground `VoiceInteractionService`.

### Step 1: Add intent filter to your Activity

```xml
<!-- AndroidManifest.xml -->
<activity android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <!-- your existing intent filters -->
    <intent-filter>
        <action android:name="${applicationId}.oacp.ACTION_START_RECORDING" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### Step 2: Handle the intent

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleOacpIntent()
    }

    // Critical for singleTask — called when activity already exists
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOacpIntent()
    }

    private fun handleOacpIntent() {
        if (intent?.action?.endsWith(".oacp.ACTION_START_RECORDING") != true) return
        intent?.action = null  // consume so it doesn't re-trigger
        startRecording()       // your app logic
    }
}
```

**Important:** If your Activity uses `singleTask` launch mode, you MUST override `onNewIntent()`. Without it, the OACP intent is silently dropped when the activity is already in memory.

### Step 3: oacp.json

```json
{
  "id": "start_recording",
  "description": "Start a new audio recording",
  "parameters": [],
  "confirmation": "never",
  "visibility": "public",
  "requiresForeground": true,
  "invoke": {
    "android": {
      "type": "activity",
      "action": "__APPLICATION_ID__.oacp.ACTION_START_RECORDING"
    }
  }
}
```

### Alternative: OacpActivity

For dedicated OACP activities (not your main Activity):

```kotlin
class CaptureActivity : OacpActivity() {
    override fun onOacpAction(action: String, params: OacpParams) {
        // Set up camera UI using params
        val mode = params.getString("mode") // "photo" or "video"
    }

    private fun onCaptureDone(path: String) {
        sendResult(OacpResult.success("Captured", mapOf("path" to path)))
        finish()
    }
}
```

`OacpActivity` provides `requestId`, `capabilityId`, `params`, and `sendResult()`.

---

## Pattern 2: Broadcast Dispatch (Background)

For actions that don't need your app's UI. The voice assistant sends a broadcast, your receiver handles it silently and returns data.

### Step 1: Create a receiver

```kotlin
class MyReceiver : OacpReceiver() {
    override fun onAction(
        context: Context,
        action: String,
        params: OacpParams,
        requestId: String?
    ): OacpResult? {
        return when {
            action.endsWith(".oacp.ACTION_CHECK_WEATHER") -> {
                val temp = getTemperature() // your app logic
                OacpResult.success("It's ${temp}° and sunny")
            }
            action.endsWith(".oacp.ACTION_PAUSE_MUSIC") -> {
                pausePlayback() // your app logic
                null // fire-and-forget, no response needed
            }
            else -> null
        }
    }
}
```

`onAction()` runs on a background thread (via `goAsync()`). Safe for I/O. Must complete within ~10 seconds.

### Step 2: Register in manifest

```xml
<receiver android:name=".oacp.MyReceiver" android:exported="true">
    <intent-filter>
        <action android:name="${applicationId}.oacp.ACTION_CHECK_WEATHER" />
        <action android:name="${applicationId}.oacp.ACTION_PAUSE_MUSIC" />
    </intent-filter>
</receiver>
```

### Step 3: oacp.json

```json
{
  "id": "check_weather",
  "description": "Check current weather conditions",
  "parameters": [
    {"name": "location", "type": "string", "required": false, "description": "City name"}
  ],
  "confirmation": "never",
  "visibility": "public",
  "invoke": {
    "android": {
      "type": "broadcast",
      "action": "__APPLICATION_ID__.oacp.ACTION_CHECK_WEATHER"
    }
  }
}
```

---

## Mixed Pattern

Most apps use both. Example from Fossify Voice Recorder:

| Capability | Type | Why |
|-----------|------|-----|
| `start_recording` | activity | Needs to open app UI |
| `pause_recording` | broadcast | Background, no UI |
| `resume_recording` | broadcast | Background, no UI |
| `stop_recording` | broadcast | Background, no UI |

---

## oacp.json Reference

### Required fields per capability

| Field | Description |
|-------|-------------|
| `id` | Unique capability identifier |
| `description` | Plain English description (used for embedding matching) |
| `parameters` | Array of parameter objects (can be empty `[]`) |
| `confirmation` | `"never"`, `"always"`, or `"if_destructive"` |
| `visibility` | `"public"` or `"trusted_only"` |
| `invoke` | Dispatch configuration (see below) |

### Embedding-critical fields (for voice matching)

| Field | Description |
|-------|-------------|
| `aliases` | Alternate phrases: `["start recording", "record audio"]` |
| `examples` | Example utterances: `["record my voice", "take a memo"]` |
| `keywords` | Ranking terms: `["record", "audio", "voice", "memo"]` |
| `disambiguationHints` | Cross-app hints: `["Do NOT use for video recording"]` |

App-level: `appDomains`, `appKeywords`, `appAliases` help match the app itself.

### `__APPLICATION_ID__` placeholder

Always use `__APPLICATION_ID__` instead of hardcoding your package name. The SDK replaces it at runtime with the actual `applicationId`, which may include `.debug` suffix in debug builds.

---

## SDK Classes

| Class | Purpose |
|-------|---------|
| `OacpReceiver` | Abstract BroadcastReceiver. Extend and implement `onAction()`. Handles `goAsync()` and threading. |
| `OacpActivity` | Abstract Activity for dedicated OACP screens. Provides `params`, `requestId`, `sendResult()`. |
| `OacpParams` | Type-safe intent extra accessor. `getString()`, `getInt()`, `getBoolean()`, `getDouble()`. |
| `OacpResult` | Result builder. `success(message)`, `error(code, message)`, `accepted()`, `cancelled()`. |
| `OacpProvider` | ContentProvider (auto-registered). Serves `oacp.json` and `OACP.md`. No setup needed. |
| `Oacp` | Protocol constants: `VERSION`, `ACTION_RESULT`, `EXTRA_*`, `STATUS_*`, `ERROR_*`. |

### OacpParams

```kotlin
val name = params.getString("name")       // String?
val count = params.getInt("count")         // Int?
val enabled = params.getBoolean("enabled") // Boolean?
val price = params.getDouble("price")      // Double?
val raw = params.getRaw("key")             // Any? (raw extra)
```

Uses suffix matching for build-variant safety — works across `com.example.app` and `com.example.app.debug`.

### OacpResult

```kotlin
OacpResult.success("Done!")
OacpResult.success("Photo saved", mapOf("path" to "/sdcard/photo.jpg"))  // with JSON data
OacpResult.error(Oacp.ERROR_MISSING_PARAMETERS, "Location is required")
OacpResult.accepted("Processing...")
OacpResult.cancelled()
```

---

## Testing with adb

```bash
# Verify your manifest is served
adb shell content read --uri "content://<your.package>.oacp/manifest"

# Test activity dispatch
adb shell am start -a <your.package>.oacp.ACTION_DO_THING \
  -n <your.package>/.MainActivity

# Test broadcast dispatch
adb shell am broadcast -a <your.package>.oacp.ACTION_DO_THING \
  -p <your.package>

# Test with extras (parameters)
adb shell am broadcast -a <your.package>.oacp.ACTION_DO_THING \
  -p <your.package> \
  --es org.oacp.extra.REQUEST_ID "test-123" \
  --es "query" "hello world"
```

---

## Common Pitfalls

### Background Activity Launch (BAL) blocked
**Symptom:** `startActivity()` from a BroadcastReceiver silently fails on Android 14+.
**Fix:** Use `type=activity` in oacp.json. The assistant calls `startActivity()` from the foreground — not your receiver.

### onNewIntent not handled
**Symptom:** Activity opens but doesn't do anything on subsequent voice commands.
**Fix:** Override `onNewIntent()` and call your OACP handler. Critical for `singleTask` activities.

### Build variant suffix
**Symptom:** Works in release but not debug (or vice versa).
**Fix:** Use `action.endsWith(".oacp.ACTION_DO_THING")` instead of exact matching. Debug builds append `.debug` to the package name.

### ContentProvider not found
**Symptom:** `adb shell content read` returns "Error while accessing provider".
**Fix:** The provider authority includes the build variant suffix. Use `<package>.debug.oacp` for debug builds.
