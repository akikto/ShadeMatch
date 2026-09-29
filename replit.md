# DentaShade project notes

This checkout is a native Android/Kotlin Jetpack Compose project with an additional standalone Kotlin/JVM `color-engine` module. It is **not** a web app, so no browser preview workflow is configured.

## Build and tests

The original repository and supplied ZIP omitted root Gradle settings/build files, a version catalog and a wrapper. They have been reconstructed without replacing the Android or color-science modules. Use **JDK 21** to run Gradle and Robolectric's Android 36 tests; compiled Java/Kotlin bytecode still targets Java 17. Install Android SDK platform **36.1** and Build Tools **35.0.0** (the AGP 8.13 default). Set `ANDROID_HOME` to the SDK directory, then run:

```sh
./gradlew :color-engine:test :app:testDebugUnitTest :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. No web preview workflow applies to a native Android app. An Android emulator/device is still needed for interactive camera and UI testing.

The pinned dependency versions live in `gradle/libs.versions.toml`, not in individual source files:

| Selection | Reason |
| --- | --- |
| Android Gradle Plugin **8.13.2**, Gradle wrapper **8.14.2** | [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes#compatibility) explicitly supports API 36.1 and Gradle 8.13+ with JDK 17 minimum. Wrapper SHA-256 is pinned. |
| Kotlin/Compose compiler plugin **2.2.20**, KSP **2.2.20-2.0.4** | Corresponding stable plugin artifacts published for the same Kotlin release in [Maven Central](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/2.2.20/) and [KSP releases](https://github.com/google/ksp/releases). |
| Google Services **4.4.4**, Maps Secrets plugin **2.0.1** | Published plugin versions; [Google's Secrets plugin instructions](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin) document 2.0.1. |
| Compose BOM **2025.09.00**, Firebase BOM **34.19.0** | Published BOMs in [Google Maven](https://maven.google.com/web/index.html); align each family rather than manually pinning every Compose/Firebase artifact. |
| AndroidX Activity **1.12.4**, CameraX **1.5.3**, Core **1.17.0**, Lifecycle **2.9.4**, Navigation **2.9.7**, Room **2.8.4**, Compose icons **1.7.8** | Published stable artifacts in Google Maven, pinned together in the catalog; avoid alpha/pre-release versions. |
| Coil **2.7.0**, Coroutines **1.10.2**, Moshi **1.15.2**, OkHttp **4.12.0**, Retrofit **2.11.0** | Published stable artifacts in [Maven Central](https://central.sonatype.com/); retain existing APIs rather than moving to newer major versions. |
| JUnit **4.13.2**, AndroidX Test **1.2.1**, Runner **1.6.2**, Espresso **3.6.1**, Robolectric **4.16.1** | Published stable test artifacts; Robolectric's Android 36 sandbox needs JDK 21 at runtime. |

The debug build does not require a Google Services JSON file; the plugin warns that it is absent. Firebase integration requires a real configuration if used. A release build also requires the project's own release signing credentials; none are generated here.

## Scientific and data safety

The bundled VITA Classical coordinates have **no verified measurement provenance**. They are marked `REQUIRES_VALIDATED_DATA`, not clinical reference values. The bundled device matrices have no validation evidence. Precise shade analysis and new PDFs are intentionally blocked until both a validated device/camera configuration and a validated reference dataset are supplied and tested. The sRGB conversion matrix is not camera calibration. Do not bypass these gates merely to show results.

The database is local Room SQLite, not encrypted storage. Keep patient data off debug logs and do not introduce automatic uploads or destructive Room migrations. Export to public Downloads requires an explicit user action and exposes the report to other apps/users of the device.