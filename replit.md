# DentaShade project notes

This checkout is a native Android/Kotlin Jetpack Compose project with an additional standalone Kotlin/JVM `color-engine` module. It is **not** a web app, so no browser preview workflow is configured.

## Current build status

The imported repository and the supplied ZIP both omit root Gradle settings/build files, a version catalog, a Gradle wrapper, and several Android resources. `app/build.gradle.kts` references `libs.*` aliases from that missing catalog. An Android APK or Android test run cannot be verified from this checkout as-is. Do not replace the existing dependency declarations with guessed versions or claim that Android builds.

The standalone Kotlin sources can be checked with:

```sh
kotlinc $(find color-engine/src/main/kotlin -name '*.kt' | sort) -d /tmp/dentashade-color-engine.jar
kotlinc $(find app/src/main/java/com/example/colorengine -name '*.kt' | sort) app/src/main/java/com/example/calibration/CalibrationModels.kt -d /tmp/dentashade-app-science.jar
```

These commands only compile pure Kotlin code, not the Android application or JUnit tests. Once a complete Gradle project and Android SDK are available, run the appropriate Gradle unit tests and Android build before using the app.

## Scientific and data safety

The bundled VITA Classical coordinates have **no verified measurement provenance**. They are marked `REQUIRES_VALIDATED_DATA`, not clinical reference values. The bundled device matrices have no validation evidence. Precise shade analysis and new PDFs are intentionally blocked until both a validated device/camera configuration and a validated reference dataset are supplied and tested. The sRGB conversion matrix is not camera calibration. Do not bypass these gates merely to show results.

The database is local Room SQLite, not encrypted storage. Keep patient data off debug logs and do not introduce automatic uploads or destructive Room migrations. Export to public Downloads requires an explicit user action and exposes the report to other apps/users of the device.