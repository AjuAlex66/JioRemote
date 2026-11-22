# JioRemote — Android TV + JioTV+ remote

A native **Kotlin + Jetpack Compose** personal Android phone app. It sends ADB commands over your local Wi-Fi to an Android TV running JioTV+ 6.0.8.

### What is included
- TV IP and ADB connection port input, remembered locally
- ADB connectivity check (a real shell command)
- Android 11+ six-digit wireless pairing UI (pairing and connection port are different)
- JioTV+ launch
- Sun TV direct-play channel card using the **user-verified** deep link
- Add/remove custom numeric JioTV+ channel IDs, saved locally
- Home, Back, volume, OK, D-pad controls
- Error/busy states; no server, cloud, or Firebase

### Open and build
1. Unzip into a folder on your Mac; open it in Android Studio.
2. The Gradle wrapper JAR and scripts are **not included** in this initial ZIP. If you have Gradle installed, run `gradle wrapper --gradle-version 8.14.3` in the project root, or ask Codex to generate the standard wrapper. Android Studio can assist with Gradle setup.
3. Sync dependencies (requires network). Build/install the `app` debug variant on an Android 9+ **phone**. The TV's 32-bit ABI does not determine the phone app's ABI.
4. Put TV + phone on same trusted LAN. On TV enable ADB debugging and its TCP connection, or use Android 11+ pairing if supported.
5. Enter TV IP and **ADB connection port** (5555 if using classic TCP ADB); tap Connect / Test TV. Approve the prompt on TV for the new phone.
6. Tap Open JioTV+ or Sun TV.

### What is verified / not verified
**Verified externally by user:** From Mac, the deep link to Sun TV `jiotvplus://304189?resume=true&source=watchnext` opened and played on JioTV+ 6.0.8.

**Not yet verified:** This app has not been compiled or run on a phone or paired with the TV in this environment. Kadb 2.1.5 API was referenced from its upstream documentation; device-specific authentication may still need adjustments. The app reports when a shell command succeeds, **not** whether streaming media has actually played.

### Notes
- The pairing port (shown with a temporary six-digit code) is NOT the ADB connection port; Android 11+ wireless debugging normally chooses dynamic ports.
- Android devices may disable ADB or change ports after reboot. Automatic reconnection and mDNS are future enhancements.
- On some Jio TV devices, standard home and volume key events are ignored or handled differently.
- Other channel IDs must be confirmed on your specific TV. Only Sun TV is seeded.
- JioTV+ is a third-party app; this is not an official Jio product.
- Protect the TV's ADB settings. ADB provides broad device control and is not intended to be exposed to untrusted networks.

### References
- Kadb: https://github.com/flyfishxu/Kadb (Apache-2.0)
- Official ADB docs: https://developer.android.com/tools/adb
