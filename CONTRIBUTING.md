# Contributing to OACP Android SDK

Thank you for your interest in contributing!

## How to Contribute

- **Bug reports**: Open an issue describing the bug, steps to reproduce, and expected behavior.
- **Feature requests**: Open an issue describing the use case and proposed API.
- **Pull requests**: Fork the repo, make your changes, and submit a PR.

## Development Setup

1. Clone the repo
2. Open in Android Studio or run from command line
3. Run tests: `./gradlew :oacp-android:test`
4. Build AAR: `./gradlew :oacp-android:assembleRelease`

## Guidelines

- Keep the public API surface minimal
- Add tests for new functionality
- Follow existing Kotlin conventions and KDoc patterns
- Ensure all tests pass before submitting

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
