# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in OACP, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, email: **security@oacp.dev** (or contact [@0xharkirat](https://github.com/0xharkirat) directly)

Please include:

- Description of the vulnerability
- Steps to reproduce
- Affected versions
- Any potential impact

We will acknowledge receipt within 48 hours and aim to provide a fix or mitigation within 7 days for critical issues.

## Scope

This policy covers:

- The OACP protocol specification
- The OACP Android SDK (`org.oacp:oacp-android`)
- Example apps in this repository

## Security Model

OACP uses Android's standard security mechanisms:

- **ContentProvider**: Exported read-only, no write operations, no URI permission grants
- **BroadcastReceiver**: Apps must explicitly declare `exported="true"` on receivers
- **Intent extras**: Parameters are passed via standard Android Intent extras; apps are responsible for validating and sanitizing input values
- **No network**: The protocol itself requires no network access; all discovery and invocation happens on-device via Android IPC
