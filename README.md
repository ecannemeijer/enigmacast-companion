# EnigmaCast Companion

A standalone Android phone app that works alongside the EnigmaCast Android TV app:
remote control, phone keyboard, TV settings, profiles and avatars, and direct Enigma2
and IPTV playback on the phone.

**Status: phases 0-6 and the local part of phase 7 are delivered and tested.** The app
pairs with the **real** TV bridge (NSD or manual address, TLS pinning, `/pair`, signed
WSS, live status, commands, text entry in every TV text field, the full settings catalog
with PIN unlock, profile management and photos). The phone also plays Enigma2 live TV and
IPTV directly (M3U and Xtream Codes, including films and series) with Media3, and keeps
Continue watching and favourites. The mock bridge is only used for the demo.

The TV bridge is not required for Live or IPTV: the phone reads the channel list through
OpenWebif and plays the stream directly from the receiver or provider. The bridge remains
the route for remote control, forms, settings, profiles and photos.

## Contents

- [Modules](#modules)
- [Features](#features)
- [Real connection](#real-connection)
- [Build and test](#build-and-test)
- [Run the app](#run-the-app)
- [Offline builds](#offline-builds)
- [Shared code provenance](#shared-code-provenance)
- [Documentation](#documentation)
- [Screenshots](#screenshots)
- [Version, update JSON and sideload](#version-update-json-and-sideload)
- [Next steps](#next-steps)

## Modules

| Module | Type | Contents |
|---|---|---|
| `mobile/app` | Android app | Compose Material 3: onboarding, pairing, devices, remote control, forms and keyboard, settings, profiles. Talks to the real bridge through `WebSocketConnection` (NSD or manual address, TLS pin); `MockBridge` only for the demo. |
| TV bridge (in EnigmaCast) | Kotlin/Android | `app/src/main/java/com/enigmacast/companionbridge/`: HTTPS and WSS on port 8750, pairing, authentication, state and commands. See [`docs/tv-bridge.md`](docs/tv-bridge.md). |
| `shared/core-model` | Kotlin/JVM | Most of `com.enigmacast.domain` from EnigmaCast (133 files), plus the user-library entities, `TmdbJson` and JVM shims (`UiText`, `EnigmaLog`, `CoreKey`, `CoreStrings`). |
| `shared/core-openwebif` | Kotlin/JVM | OpenWebif endpoints and all JSON parsers (`/api/about`, bouquets, EPG, timers, tuners, signal). |
| `shared/core-protocol` | Kotlin/JVM | Companion protocol v1: envelope, commands, events, pairing DTOs and the canonical request signature. |
| `protocol/v1` | JSON | Golden examples used by both sides in tests. |
| `tools/tls-spike` | Kotlin/JVM | Proof for the certificate, PKCS#12 and SPKI pinning. |
| `docs` | Markdown | Protocol, threat model, TLS spike, ADRs and screenshots. |
| `SYNC.md` | Markdown | Provenance per copied file (source path and SHA-256). |

Not yet covered: video playback on a physical Enigma2 receiver; the emulator has been
proven with an audio stream.

## Features

- **Live TV (phase 5):** the phone reads channel lists and now/next EPG directly from the
  Enigma2 receiver through OpenWebif and plays the MPEG-TS/HLS stream with Media3.
  Settings cover host, webif port, stream port, optional username/password and HTTPS; the
  credentials are stored encrypted in the Android Keystore. The TV does not need to be on.
  **Recent on the TV** shows the TV's recent channels (through `state.recent`) and plays
  them with one tap. When the phone watches a channel on the same receiver, it reports it
  back with `cmd.recent`. The player opens **automatically in landscape/fullscreen** and
  preserves the stream aspect ratio.
- **IPTV (phase 6):** M3U playlists and Xtream Codes with **Live, Films and Series**. Once
  a source exists (manually or through `state.iptv` from the TV/sync server), the app opens
  the content directly; the M3U URL and Xtream login fields only appear under **Edit**.
  Channels have logos, a source line and category chips. Films and series get a **TMDB
  information page** with poster, year, score and description; a series opens its episode
  list from there. For a live channel with an archive, **Catch-up** opens an EPG list of
  programmes available in the archive.
- **Player:** opens automatically in **landscape/fullscreen** and preserves the stream
  aspect ratio. On live IPTV a **channel list** with logos appears on the left for direct
  zapping; the menu button hides it. The OSD hides automatically after five seconds and
  returns when the screen is tapped. VOD has a bottom timeline with the current position,
  duration, **-10 s**, **+30 s** and a seek bar. Live playback has no seek controls.
- **Library (phase 7):** **My Items** shows only films and series: **Continue watching**,
  **Favourites** and **Recordings on the receiver**. Live channels and loose recent
  streams are deliberately excluded. **The TV is authoritative for IPTV favourites and
  playback positions** (`state.library`/`cmd.library`); old demo/loopback entries are
  removed during synchronisation and real TV entries replace them. The phone also keeps
  only the newest episode per series in Continue watching. Receiver recordings come from
  OpenWebif and are sent to the phone through the same bridge.
- **Settings PIN:** when the TV has a PIN, advanced settings remain visible but the phone
  asks for the PIN; the TV verifies it server-side.
- **Profile photos:** upload, delete and retrieve (API) through `PUT`/`GET`/`DELETE`; the
  profile PIN and photo lock remain enforced.
- **Playback progress:** the phone sends progress on pause, completion, closing the player
  and reconnecting. The TV stores progress on pause and stop and forwards it to the sync
  server. Older series episodes are removed from the sync history as well.

## Real connection

- **Identity:** ECDSA P-256 in the Android Keystore; the public key is sent during pairing.
  The pairing token is stored AES-GCM-encrypted in `SecureStore`.
- **Discovery:** `_enigmacast._tcp.` through `NsdManager`, with TXT records (`id`, `name`,
  `app`, `fp`); a manual IP address on port 8750 is also supported.
- **Pairing:** `POST /api/v1/pair` with the code and public key. The pin comes from TXT; for
  a manual address the pin is recorded on the first handshake (TOFU) and pinned afterwards.
- **Connecting:** signed `GET /ws` upgrade (ECDSA, timestamp, nonce), followed by `hello`,
  `state.*` and `ack`. A dropped connection reconnects automatically with backoff; a real
  pin mismatch is fatal and requires pairing again.
- **Forms:** every `TvTextField` on the TV opens a `form` session when edited; the phone
  shows a keyboard sheet and sends `cmd.form` (`TEXT`/`SUBMIT`/`CANCEL`). **Multi-field
  forms** work too: receiver setup offers **Fill from phone** for host, username and
  password in one go. Passwords never appear in the descriptor or the logs.
- **Settings:** the TV sends `state.settings` with a catalog of **24 user settings** in five
  categories (BOOL/CHOICE/NUMBER), including danger and restart flags. The phone changes
  them through `cmd.settings`; the TV writes with the same `SettingsRepository` as its own
  UI. With a Settings PIN the entries remain visible but the phone asks for the PIN; the
  TV verifies it server-side.
- **Profile management:** create (maximum 6), rename, choose an avatar, delete (including
  viewing history/favourites cleanup) and clear the profile photo. PIN-protected profiles
  ask for the PIN; photo upload to a PIN-protected profile is blocked.
- **Profile photos:** the phone uploads a signed `PUT /api/v1/profiles/{id}/photo`; the TV
  crops to a square, re-encodes to WebP at 512 px (EXIF removed) and updates
  `state.profiles`. The TV displays the photo on the profile.
- **Proven on two emulators:** pairing with the TV code, WSS connected, `CHANNEL_UP` with a
  **0 ms** ack, the TV showing "1 phone(s) connected", automatic reconnecting, live text
  in the TV search field, toggling settings, the "EC" test photo, creating, renaming,
  changing the avatar of, deleting a profile and clearing its photo, Live with channel
  list/EPG and a playing stream, M3U and Xtream IPTV, and Continue watching plus
  favourites.

## Build and test

JDK 21 and the Android Studio JDK (or a compatible JDK) are required. On the development
machine:

```powershell
$env:JAVA_HOME = "C:\Users\user3\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat test
.\gradlew.bat :mobile:app:assembleDebug --offline
.\gradlew.bat :mobile:app:testDebugUnitTest --offline
```

Or use the Gradle distribution already in the cache (when `~/.gradle` is writable):

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.7.0-bin\*\gradle-9.7.0\bin\gradle.bat" test --offline
```

Result on 22 October 2026: **671 tests, 0 failures, 1 skipped**. The EnigmaCast TV suite
(including the bridge) is at **929 tests, 0 failures**.

### Run the app

```powershell
adb install -r mobile\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.enigmacast.companion/.MainActivity
```

Demo: **Start demo**, or **Find TV -> Living room TV -> 123456**. See
[`mobile/README.md`](mobile/README.md) for the screens and the mock.

## Offline builds

The build is deliberately configured to work **without network access**, using the Gradle
cache already populated by the EnigmaCast build:

- Kotlin plugins are applied through the `buildscript` classpath; the plugin markers are
  not in the cache.
- `error_prone_annotations` is forced to an already available version.
- `kotlin-scripting-compiler-embeddable` is excluded; it is only needed for Kotlin scripts.
- `core-model` does not use OkHttp (the only OkHttp call in the domain was removed).

See `docs/adr/0003-offline-build.md`. With network access this can return to the normal
`plugins { }` DSL.

## Shared code provenance

`shared/**` is a **vendored copy** of EnigmaCast at HEAD
`40db96d3c47414112ff61138694b6405a2a4f445` (branch `feature/torrents`). 131 files are
byte-identical, 7 have a small shim and 1 file is new. `SYNC.md` contains the complete
table with SHA-256 per file.

When a TV change touches the shared code:

1. Copy the file from EnigmaCast again.
2. Re-apply the shim (see `SYNC.md`, section "Applied shims").
3. Update `SYNC.md`.
4. Run `gradlew test`.

The TV repository itself is **not modified** by this process.

## Documentation

- [`docs/tv-bridge.md`](docs/tv-bridge.md) - the TV bridge: architecture, endpoints, evidence and limitations.
- [`docs/protocol.md`](docs/protocol.md) - the complete protocol v1.
- [`docs/threat-model.md`](docs/threat-model.md) - threats and mitigations.
- [`docs/tls-spike.md`](docs/tls-spike.md) - TLS design and validation.
- [`docs/adr/`](docs/adr/) - architecture decisions.
- [`mobile/README.md`](mobile/README.md) - the app, the mock and the screens.

## Screenshots

| Welcome | Remote | Settings |
|---|---|---|
| ![Welcome](docs/screenshots/01-welcome.png) | ![Remote](docs/screenshots/38-phone-remote-tuner.png) | ![Settings](docs/screenshots/05-settings.png) |

| Scan | Pairing | Profiles |
|---|---|---|
| ![Scan](docs/screenshots/02-scan.png) | ![Pairing](docs/screenshots/03-pair.png) | ![Profiles](docs/screenshots/06-profiles.png) |

| Real TV paired | TV sees the phone |
|---|---|
| ![Phone paired](docs/screenshots/08-phone-paired.png) | ![TV connected](docs/screenshots/09-tv-connected.png) |

| Form on the phone | Live on the TV |
|---|---|
| ![Phone form](docs/screenshots/10-phone-form.png) | ![TV form](docs/screenshots/11-tv-form-live.png) |

| TV settings | Profile photo on the TV |
|---|---|
| ![Phone settings](docs/screenshots/12-phone-settings.png) | ![TV photo](docs/screenshots/14-tv-photo.png) |

| Full catalog | Advanced confirmation |
|---|---|
| ![Settings full](docs/screenshots/16-phone-settings-full.png) | ![Danger confirm](docs/screenshots/17-phone-danger-confirm.png) |

| Profile management | Edit profile |
|---|---|
| ![Profiles](docs/screenshots/18-phone-profiles.png) | ![Editor](docs/screenshots/19-phone-profile-editor.png) |

| Live with EPG | Player |
|---|---|
| ![Live](docs/screenshots/20-phone-live-channels.png) | ![Player](docs/screenshots/21-phone-player.png) |

| IPTV (M3U) | Xtream Codes |
|---|---|
| ![IPTV](docs/screenshots/22-phone-iptv.png) | ![Xtream](docs/screenshots/25-phone-xtream.png) |

| Film from Xtream | Series episodes |
|---|---|
| ![Film](docs/screenshots/26-phone-film.png) | ![Series](docs/screenshots/27-phone-series.png) |

| Library |
|---|
| ![Library](docs/screenshots/24-phone-library.png) |

| Photo from the TV | Catch-up programme picker |
|---|---|
| ![Photo view](docs/screenshots/28-phone-photo-view.png) | ![Catch-up picker](docs/screenshots/30-phone-catchup-picker.png) |

| Catch-up playing | Recent on the TV |
|---|---|
| ![Catch-up play](docs/screenshots/31-phone-catchup-play.png) | ![Recent TV](docs/screenshots/32-phone-recent-tv.png) |

| IPTV content with logos | Player channel list |
|---|---|
| ![IPTV content](docs/screenshots/35-phone-iptv-content.png) | ![Player channels](docs/screenshots/36-phone-player-channels.png) |

## Version, update JSON and sideload

- **Version:** `1.0.0-beta.12`, build `111` (`versionCode`).
- The app shows this in **More** under the title and in **About this app**.
- **Sideload APK:** `dist/enigmacast-companion.apk`.
- **In-app update:** the app checks the combined manifest on startup and **More -> Check
  for updates**; a new version shows a prompt with **Download** and **Install**.
- **Launcher icon:** the same EnigmaCast mark as the Android TV app. The app is signed with
  the debug key for sideloading, not for the Play Store.
- **Companion update JSON:** `dist/enigmacast-companion.json`.
- **Combined with the TV:** `dist/enigmacast-updates.json`; the TV version lives in
  `EnigmaCast/dist/enigmacast-v2.json`. The TV reads the top-level fields and ignores the
  `companion` object, so one manifest serves both apps.
- See [docs/updates.md](docs/updates.md) for the schema and upload steps.

![Version in More](docs/screenshots/33-phone-version.png)

![App icon](docs/screenshots/34-phone-icon.png)

## Next steps

1. Test video playback on a physical Enigma2 receiver and IPTV provider; the emulator has
   been proven with an audio stream.
2. Run a full multi-field end-to-end test on receiver setup and the TV PIN on hardware.
3. Test NSD on a real home network and the TLS route on physical TV boxes.
