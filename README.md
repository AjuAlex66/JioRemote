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

## TV power controls

**Reboot TV** and **Power Off TV** are below the volume controls. Each requires confirmation. After a request is sent, JioRemote disconnects and lets you reconnect when the TV is ready. Power Off uses the TV's shutdown option, so you may need the physical remote or TV power button to turn the TV back on.

## Default channels

| Default channel | Number |
| --- | --- |
| Asianet HD | `1806` |
| Flowers | `555` |
| Mazhavil Manorama HD | `1818` |
| Asianet Movies HD | `1827` |
| Asianet Plus | `1809` |
| Sun TV | `1503` |

Catalogue version 4 removes the 63 extra v3 defaults by their saved keys. The upgrade keeps manually added channels and preserves the names, numbers, logos, and relative order of the remaining rows. Previously deleted defaults stay deleted. You can still save up to 200 rows. Sun TV's legacy playback ID `304189` stays separate from channel numbers.

Bundled artwork comes from [JioStar's media toolkit](https://www.jiostar.com/mediatoolkit/all-logos/), the [Flowers broadcaster site](https://www.flowerstv.in/about-us/), and provider assets for [Mazhavil Manorama HD](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_MAZHAVIL_MANORAMA_HD/images/LOGO_HD/image.png) and [Sun TV](https://xstreamcp-assets-msp.streamready.in/assets/LIVETV/LIVECHANNEL/LIVETV_LIVETVCHANNEL_SUN_TV/images/LOGO_HD/image.png).

## Validation

- The clean Gradle build and lint completed with 26 passing unit tests and zero lint errors. Tests cover all 63 retired keys, retained rows and numbers, earlier catalogue versions, logo cleanup, and the existing ADB digit sequence.
- An in-place upgrade on the A063 phone reduced 69 saved rows to seven. The six original defaults and one manually added channel remained in their saved order. A cold restart preserved the result. The saved TV endpoint and ADB identity remained unchanged.
- The upgraded app opened JioTV+ and tuned the physical TV to Asianet Movies HD `1827`. Volume Up, Volume Down, and Mute responded on the TV. Adding, long-press editing, drag reordering across restart, and confirmed deletion worked on the phone using a temporary channel.
- The power-control update passed 37 unit tests in a clean build. On the A063 phone, both confirmation dialogs opened and canceled while the TV stayed connected with the same boot ID. The two rows fit above the navigation bar in a compact viewport. The TV's reboot binary advertises `-p`, and live reboot and shutdown remain unverified.
