# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0]
- Add cross-platform app directory helpers for cache and documents paths.
- Add app configuration reading from Android manifest meta-data and iOS Info.plist values.
- Add CPU architecture and core count information to device metadata.
- Fix Android manifest provider merging conflicts when host apps declare their own `FileProvider`.

## [0.1.0]
- Initial extracted release of the toolkit library.
- Support Android and iOS app, device, clipboard, network, keyboard, haptics, sensors, sharing, navigation, and file APIs.
- Include a Compose Multiplatform demo app.
- Remove toolkit build metadata from `AppInfo` and make optional app metadata nullable.
- Remove deprecated manual network monitor start and stop APIs.
- Simplify keyboard monitoring to lifecycle-aware observation and snapshot access.
