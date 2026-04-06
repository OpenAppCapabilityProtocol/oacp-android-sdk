# Changelog

All notable changes to the OACP Android SDK will be documented in this file.

This project follows [Semantic Versioning](https://semver.org/).

---

## [0.3.0] - 2026-04-06

Initial public release.

### Added

- **OacpReceiver** — Abstract BroadcastReceiver with `goAsync()` threading, automatic error wrapping, and result broadcasting
- **OacpActivity** — Abstract Activity for foreground actions with automatic request ID and parameter extraction
- **OacpProvider** — ContentProvider with `__APPLICATION_ID__` substitution, entity serving, and manifest merger auto-registration
- **OacpParams** — Type-safe parameter accessor with suffix-based key resolution and type coercion
- **OacpResult** — Builder for result objects with factory methods for all lifecycle states (`success`, `accepted`, `started`, `error`, `cancelled`)
- **OacpEntitySource** — Interface for serving dynamic entity data (alarms, timers, playlists)
- **Oacp** — Protocol constants: intent actions, extra keys, status values, error codes
