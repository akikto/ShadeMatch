---
name: Android Gradle toolchains on Replit
description: JVM discovery mismatch when running Android 36 Robolectric tests in this Nix environment
---

When running Robolectric tests targeting Android 36 on Replit, use a JDK 21 Gradle runtime, but avoid assuming a separately installed Nix JDK 17 will be discovered automatically as a Gradle toolchain.

**Why:** Android 36's Robolectric sandbox rejected Java 17. Switching Gradle to Java 21 then made a hard `jvmToolchain(17)` requirement fail despite JDK 17 being installed; Gradle's toolchain discovery listed only the current Java 21 runtime. Compiling Java/Kotlin bytecode to target 17 without demanding a separate toolchain avoided that environment-specific mismatch.

**How to apply:** For Android 36 tests in this workspace, run Gradle with JDK 21 and keep Java/Kotlin target compatibility explicit. If a future change truly needs a different compiler JDK, configure and verify Gradle toolchain discovery rather than relying on Nix executable presence alone.