# Gradle Warning Audit

Last refreshed: **2026-07-27**

This records the warning baseline for the Android build so that intentional
compatibility debt is distinguishable from warnings that can be removed mechanically.
It is not a warning-suppression list.

## Verification

Run from the repository root with the Android Studio JBR and local Android SDK:

```shell
./gradlew help --no-configuration-cache --warning-mode all
./gradlew testDebugUnitTest --rerun-tasks --no-configuration-cache --warning-mode all
./gradlew :app:assembleDebug --rerun-tasks --no-configuration-cache --warning-mode all
```

All three commands passed on Linux. The complete Android unit-test run executed 420
tasks; the forced debug APK build executed 377 tasks. No install task or `adb` command
was run.

Before this cleanup, a forced APK build emitted 128 source warning lines. The equivalent
build now emits 14:

- 13 references to the deprecated AndroidX Security Crypto
  `EncryptedSharedPreferences`/`MasterKey` API in `TokenDataStore`;
- one deprecated Compose `BackHandler` call in `PlatformBackHandler`.

The remaining warnings below are intentional debt rather than safe mechanical edits.

## Deferred migrations

### AGP built-in Kotlin and the new KMP Android plugin

The project temporarily opts out of AGP built-in Kotlin and the new DSL with
`android.builtInKotlin=false` and `android.newDsl=false`. The Kotlin Multiplatform
modules also still combine `org.jetbrains.kotlin.multiplatform` with
`com.android.library`.

These warnings share one migration boundary: move the Android app to AGP built-in
Kotlin, move every KMP Android library to
`com.android.kotlin.multiplatform.library`, update the convention plugins, and remove
the compatibility flags. Removing only the flags breaks the current build. Treat this
as a dedicated, cross-module migration before AGP 10, following the
[Android built-in Kotlin migration guide](https://developer.android.com/build/migrate-to-built-in-kotlin)
and [Kotlin AGP 9 KMP migration guide](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).

The obsolete `applicationVariants`, `testVariants`, `unitTestVariants`, and
`libraryVariants` warnings are downstream symptoms of the legacy Kotlin/Android plugin
combination, not calls made by application code.

### Detekt Gradle 10 compatibility

Detekt 1.23.8 calls Gradle's deprecated `ReportingExtension.file(String)`. The next
Detekt line changes plugin IDs, coordinates, configuration, and custom-rule APIs, while
this repository has a custom `:detekt-rules` module. Do not upgrade merely to silence
the warning while Detekt 2 remains pre-release; use its
[migration guide](https://detekt.dev/docs/introduction/migration/) in a separately
verified upgrade.

### Android credential storage

AndroidX deprecated all APIs in `security-crypto`, including the credential store used
by `TokenDataStore`. Replacing it changes persisted sensitive-data behavior and must
include migration and rollback tests for existing logins. Do not substitute plain
`SharedPreferences` as a warning-only cleanup.

### Predictive back handling

Compose now recommends `NavigationEventHandler` instead of `BackHandler`.
`PlatformBackHandler` sits on the navigation behavior boundary, so this should be
changed with back-stack and predictive-back device coverage rather than as a mechanical
rename.

## Host and packaging notices

- Linux cannot compile the `nwparams_defaults` cinterop for iOS. The disabled iOS
  targets and cinterop-commonization messages are expected on this host; validate iOS
  on macOS instead of hiding the warnings globally.
- The debug packager cannot strip three dependency-owned native libraries:
  `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`, and
  `libsqliteJni.so`. AGP packages them unchanged, and the build succeeds.
