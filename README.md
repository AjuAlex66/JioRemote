# JioRemote

A native Kotlin and Jetpack Compose remote for an Android TV on your local Wi-Fi. It uses the TV's ADB connection to start JioTV+ 6.0.8, tune numbered channels, and control volume.

## Build and install

Open the project in Android Studio with Android SDK 36 and JDK 17 or newer, or run the checked-in Gradle wrapper.

```sh
./gradlew testDebugUnitTest assembleDebug lint
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

| Channel | Verified TV channel number |
| --- | --- |
| Asianet HD | Needs configuration |
| Flowers | Needs configuration |
| Mazhavil Manorama HD | Needs configuration |
| Asianet Movies HD | `1827` |
| Asianet Plus | Needs configuration |
| Sun TV | Needs configuration |

The upgrade preserves saved channels, order, names, and logo associations. Historical JioTV+ playback IDs are kept separately and are never used for numeric tuning. Sun TV's historical ID `304189` has no verified channel-number equivalent.

Bundled artwork comes from [JioStar's media toolkit](https://www.jiostar.com/mediatoolkit/all-logos/), the [Flowers broadcaster site](https://www.flowerstv.in/about-us/), and provider assets for [Mazhavil Manorama HD](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_MAZHAVIL_MANORAMA_HD/images/LOGO_HD/image.png) and [Sun TV](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_SUN_TV/images/LOGO_HD/image.png).

## Validation

- Gradle build, lint, and all 23 unit tests passed. The tests cover tuning order and timing, input validation, catalogue migration, channel editing and reordering, and private logo file persistence and cleanup.
- On an Android 15 phone, the updated app connected to the existing TV and tuned JioTV+ to **1827 Asianet Movies HD**. The TV reported the expected changes from Volume Up, Volume Down, and Mute. Disconnecting and reconnecting also worked.
- Phone checks covered both themes, the fixed volume bar while scrolling, drag auto-scroll, channel order after a cold restart, Add and confirmed Delete, and opening the system photo picker.
