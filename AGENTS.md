# JioRemote — Codex project instructions

## Goal
Build a reliable **native Android** phone app (Kotlin, Jetpack Compose) that controls one Android TV over local Wi-Fi ADB and opens JioTV+ channels using deep links. No Flutter, no backend, no Firebase, no subscriptions.

## Verified facts
- TV CPU ABI: `armeabi-v7a,armeabi` (not directly relevant to this phone APK).
- TV JioTV+ package `com.jio.media.jiotvplus`, version `6.0.8` (build `6008`).
- Command verified on user's TV: `adb shell am start -W -a android.intent.action.VIEW -d 'jiotvplus://304189?resume=true&source=watchnext' -p com.jio.media.jiotvplus`.
- That command **actually opened and played Sun TV** from the user's Mac ADB.
- Other channel IDs are not confirmed.

## Current code
- App skeleton, settings, channel list, command builders, Kotlin/Compose UI, pair/connection attempt using `com.flyfishxu:kadb:2.1.5`.
- **Not built, run or phone/TV tested here** because the authoring environment has no Android SDK/TV.
- Gradle wrapper JAR/script are not bundled. Generate a standard wrapper using `gradle wrapper --gradle-version 8.14.3` or Android Studio/Gradle, and commit files.
- Do not mark phone-to-TV connectivity or channel playback complete without physical test.

## Required first Codex task
1. Open in Android Studio and make Gradle sync succeed. Fetch Kadb source/API if needed; resolve any compile/dependency problems with minimal changes.
2. Generate/check in Gradle wrapper JAR and scripts. Run `./gradlew testDebugUnitTest`, `assembleDebug`, and Android lint.
3. Install on Android phone, connect to same network as TV, test TV IP/ADB TCP port. TV must authorize the phone's new RSA key or be paired on supported Android 11+ wireless debugging.
4. Verify app's Connect, Open JioTV+, Sun TV, Back/OK controls. Record actual logs and behavior.
5. Fix errors. Add reconnection or mDNS only after basic connection succeeds. Preserve the tested URI pattern and do not invent additional channel IDs.

## Safety / engineering
- Restrict access to trusted LAN; never expose TCP/5555 publicly.
- Never accept free-form shell commands in the UI. Validate channel IDs as numeric and allow-list remote keycodes.
- Keep private ADB identity in app-internal storage, do not commit real credentials or enable backup.
- Maintain clear busy and error states; no hardcoded claims of playback success.
- Avoid claiming tests passed without evidence; add short validation summary after every task.
