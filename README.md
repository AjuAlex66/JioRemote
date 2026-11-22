# JioRemote — Android TV + JioTV+ remote

A native **Kotlin + Jetpack Compose** personal Android phone app. It sends ADB commands over your local Wi-Fi to an Android TV running JioTV+ 6.0.8.

### What is included
- Material 3 remote with light and dark themes, a TV connection card, channel grid, and large remote keys
- TV IP and ADB connection port saved locally with DataStore, including migration from earlier builds
- Explicit Connect and Disconnect controls. The app starts disconnected and enables remote actions after the authenticated shell check succeeds.
- Private/link-local IPv4 validation so commands cannot be sent to a public Internet address
- ADB connectivity check (a real shell command)
- Android 11+ six-digit wireless pairing UI (pairing and connection port are different)
- JioTV+ launch
- Sun TV direct-play channel card using the **user-verified** deep link
- Add/remove custom numeric JioTV+ channel IDs, saved locally
- Home, Back, volume, OK, D-pad controls
- Error/busy states; no server, cloud, or Firebase

### Open and build
1. Open this existing project in Android Studio and let Gradle sync. The standard Gradle 8.14.3 wrapper is checked in.
2. Ensure Android SDK 36 and JDK 17 or newer are available. From the project root run:
   ```sh
   ./gradlew testDebugUnitTest assembleDebug lint
   ```
3. Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 9+ **phone**. The TV's 32-bit ABI does not determine the phone app's ABI.

Kadb 2.1.1 is intentionally pinned: it is the newest Kadb 2.1.x AAR whose metadata supports this project's API 36 / Gradle 8.14.3 toolchain. Kadb 2.1.2–2.1.5 require compile SDK 37. The required JitPack repository is restricted to Kadb's SPAKE2 dependency group. Conscrypt is included so six-digit pairing also has a TLS provider on an Android 9 phone.

### Physical phone + TV validation
1. Put the phone and TV on the same trusted private LAN. Never expose an ADB port to the Internet or port-forward TCP/5555.
2. On the TV, enable Developer options and ADB debugging. Choose one path:
   - **Classic TCP ADB:** enable network debugging and use its connection port (commonly 5555). The TV must approve the phone app's new RSA identity when prompted.
   - **Android 11+ Wireless debugging:** choose **Pair device with pairing code**. Enter the displayed pairing port and six-digit code in the app, then tap **Pair phone with TV**. After pairing, return to the TV's Wireless debugging screen and enter its separate **IP address & port** connection port in the main connection field.
3. Tap **Save settings** to remember the valid address and connection port, or tap **Connect** to save and verify them in one step. The connection card shows **Connected** and the endpoint after an authenticated shell probe. Tap **Disconnect** to disable remote actions on the phone.
4. Tap **Open JioTV+**, then **Sun TV**. Confirm on the TV that JioTV+ 6.0.8 opens and Sun TV actually plays.
5. Verify **Back**, **OK**, D-pad, Home, and volume buttons individually; some TV firmware handles Home/volume differently.
6. Record the phone model/Android version, TV IP and connection mode (not pairing code or private key), UI status/errors, and relevant phone logs. From a Mac attached to the phone for debugging, a useful log command is `adb logcat --pid=$(adb shell pidof com.jioremote.app)`.

### Validation

- Gradle 8.14.3 completed `testDebugUnitTest assembleDebug lint` with 13 passing JVM tests and zero lint errors. The tests cover the exact Sun TV command, endpoint validation, DataStore persistence, and ViewModel operation transitions.
- A physical Android 15 phone (A063) received an in-place update. Its previous TV IP and connection port migrated to DataStore and survived a cold reopen. The existing app-private ADB identity remained in place.
- On the ACP75P TV running JioTV+ 6.0.8, the phone connected over classic TCP ADB. **Open JioTV+** brought the JioTV+ activity to the foreground, and the TV activity log recorded the **Sun TV** VIEW intent for channel `304189`. **Back** returned the TV to the previous app, and **Disconnect** returned the phone to editable settings.
- The TV started its secure video decoder after the phone’s Sun TV request, and the user confirmed visible playback. Six-digit wireless pairing and first-time RSA authorization still need separate physical tests.

### Notes
- The pairing port (shown with a temporary six-digit code) is NOT the ADB connection port; Android 11+ wireless debugging normally chooses dynamic ports.
- Android devices may disable ADB or change ports after reboot. Automatic reconnection and mDNS are future enhancements.
- The ADB private identity is stored only in app-internal storage. Cloud backup and device transfer are disabled for app data.
- On some Jio TV devices, standard home and volume key events are ignored or handled differently.
- Other channel IDs must be confirmed on your specific TV. Only Sun TV is seeded.
- JioTV+ is a third-party app; this is not an official Jio product.
- Protect the TV's ADB settings. ADB provides broad device control and is not intended to be exposed to untrusted networks.

### References
- Kadb: https://github.com/flyfishxu/Kadb (Apache-2.0)
- Official ADB docs: https://developer.android.com/tools/adb
