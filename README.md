# JioRemote

A native Kotlin and Jetpack Compose remote for an Android TV on your local Wi-Fi. It uses the TV's ADB connection to start JioTV+ 6.0.8, tune numbered channels, and control volume.

## Build and install

Open the project in Android Studio with Android SDK 36 and JDK 17 or newer, or run the checked-in Gradle wrapper.

```sh
./gradlew clean testDebugUnitTest assembleDebug lint
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 9 or newer phone. Kadb remains pinned to 2.1.1 for the project's API 36 and Gradle 8.14.3 toolchain.

## Connect and tune

1. Put the TV and phone on the same trusted private network. Enable ADB debugging on the TV.
2. In JioRemote settings, enter the TV IP and ADB connection port. For classic ADB, the port is commonly 5555. Approve the phone's RSA key on the TV when prompted.
3. If using Android 11+ wireless debugging, use the temporary pairing port and six-digit code under **Wireless pairing**. After pairing, use the separate connection port shown on the TV's Wireless debugging screen.
4. Tap **Connect**, then **Open JioTV+**. With JioTV+ open on the TV, tap a configured channel to send its digits. A single ADB command sends the digits with 150 ms pauses between them.
5. Tap **Add channel** or long-press a row to enter a channel number or choose a logo. Drag the handle to reorder. TalkBack offers **Move up** and **Move down** on the handle.

TV settings, channel order, and custom logos stay on the phone. The app copies chosen logos into private storage. Keep ADB on your trusted LAN and avoid port-forwarding it.

## Default channels

A fresh install includes 69 defaults from the supplied catalogue: 38 Malayalam, 7 English movies, 7 wildlife/documentary, and 17 Tamil. The app supports 200 saved rows. Numbers are stored as digit strings so leading zeros are preserved.

The upgrade retains saved order, custom rows, edited names and numbers, and logo associations. It skips additions already represented by saved names or numbers and leaves previously deleted defaults alone. Sun TV's historical playback ID `304189` stays separate from channel numbers. The fresh catalogue suggests `1503` for the original Sun TV entry and `1504` for Sun TV (SD).

Asianet Movies HD `1827` was confirmed when tuning from the phone. Targeted checks with the TV's ADB interface also identified Flowers `555`, Reporter TV `1855`, and Discovery Tamil `1525`. The TV reported “No channels found” for Janam TV `1492` and Zee Keralam `1812` during spot checks. Kite Victers `580` also remains unconfirmed. These and other unverified entries retain the supplied numbers as editable suggestions.

Bundled artwork comes from [JioStar's media toolkit](https://www.jiostar.com/mediatoolkit/all-logos/), the [Flowers broadcaster site](https://www.flowerstv.in/about-us/), and provider assets for [Mazhavil Manorama HD](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_MAZHAVIL_MANORAMA_HD/images/LOGO_HD/image.png) and [Sun TV](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_SUN_TV/images/LOGO_HD/image.png).

## Validation

- The clean build, Android lint, and all 29 unit tests passed. Coverage includes catalogue size and uniqueness, migration and duplicate handling, the 200-row limit, serialized tuning, and the 150 ms digit sequence.
- The A063 phone upgraded from catalogue version 2 to 3. All seven existing rows retained their positions and saved values. A cold restart preserved the migrated list without reinserting rows.
- On the connected TV, the updated app opened JioTV+, tuned to Asianet Movies HD, and exercised Volume Up, Volume Down, Mute, and reconnect.
- The expanded list scrolled to the final row above the fixed volume bar. A temporary channel was added, reordered, edited by long-press, and deleted after confirmation. Its system-picker photo survived a cold restart in app-private storage and was removed on deletion.
