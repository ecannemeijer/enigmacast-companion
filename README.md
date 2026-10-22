# EnigmaCast Companion

Zelfstandige Android-app (telefoon) naast de EnigmaCast Android TV-app: afstandsbediening,
telefoontoetsenbord, instellingen, profielen/avatars, Enigma2- en IPTV-streaming op de telefoon.

**Status: fase 0ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“6 en de lokale kant van fase 7 zijn opgeleverd en getest.** De app
koppelt en werkt tegen de **echte** TV-brug (NSD/handmatig adres, TLS-pinning, `/pair`,
getekende WSS, live status, commando's, typen in elk tekstveld, de volledige instellingen
met PIN-ontgrendeling, profielbeheer en foto's). Daarnaast speelt de telefoon
**rechtstreeks** Enigma2-live-tv en IPTV af (M3U en Xtream Codes, incl. films en series)
met Media3, en houdt hij **verder kijken** en favorieten bij. De mock blijft alleen voor de
demo.

De TV-brug is niet nodig voor Live/IPTV: de telefoon leest de zenderlijst via OpenWebif en
speelt de stream rechtstreeks van de ontvanger of provider. De brug blijft de route voor
afstandsbediening, formulieren, instellingen, profielen en foto's.

## Wat hier staat

| Module | Type | Inhoud |
|---|---|---|
| `mobile/app` | Android-app | Compose Material 3: onboarding, koppelen, apparaten, afstandsbediening, formulieren/toetsenbord, instellingen, profielen. Spreekt de echte brug via `WebSocketConnection` (NSD of handmatig adres, TLS-pin); `MockBridge` alleen voor de demo. |
| TV-brug (in EnigmaCast) | Kotlin/Android | `app/src/main/java/com/enigmacast/companionbridge/`: HTTPS + WSS op poort 8750, koppelen, auth, status en commando's. Zie [`docs/tv-bridge.md`](docs/tv-bridge.md). |
| `shared/core-model` | Kotlin/JVM | Het grootste deel van `com.enigmacast.domain` uit EnigmaCast (133 bestanden), plus de gebruikersentiteiten, `TmdbJson` en JVM-shims (`UiText`, `EnigmaLog`, `CoreKey`, `CoreStrings`). |
| `shared/core-openwebif` | Kotlin/JVM | OpenWebif-endpoints en alle JSON-parsers (`/api/about`, bouquets, EPG, timers, tuners, signaal). |
| `shared/core-protocol` | Kotlin/JVM | Het Companion-protocol v1: envelope, commando's, events, pairing-DTO's en de canonieke request-handtekening. |
| `protocol/v1` | JSON | Gouden voorbeelden waar beide kanten tegen worden getest. |
| `tools/tls-spike` | Kotlin/JVM | Bewijs voor certificaat, PKCS#12 en SPKI-pinning. |
| `docs` | Markdown | Protocol, dreigingsmodel, TLS-spike, ADR's en schermafbeeldingen. |
| `SYNC.md` | Markdown | Herkomst per gekopieerd bestand (bronpad + SHA-256). |

Nog **niet** aanwezig: video op een fysieke ontvanger (de emulator is met een audiostream
bewezen).

- **Live tv (fase 5):** de telefoon leest rechtstreeks via OpenWebif de zenderlijsten en
  EPG (now/next) van de Enigma2-ontvanger en speelt de MPEG-TS/HLS-stream met Media3.
  Instellingen: host, webif-poort, streampoort, optionele gebruikersnaam/wachtwoord en
  HTTPS; de inloggegevens gaan versleuteld de Keystore in. De TV hoeft niet aan te staan.
  **"Recent op de tv"** toont de recente zenders van de TV (via `state.recent`) en speelt
  ze met ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©n tik. Bekijkt de telefoon zelf een zender op dezelfde ontvanger, dan meldt hij
  die met `cmd.recent` terug. De speler opent **automatisch in landscape/fullscreen** en
  houdt de beeldverhouding van de stream vast.
- **IPTV (fase 6):** M3U-playlist en Xtream Codes, met **Live, Films en Series**. Zodra er
  een bron is (handmatig of via `state.iptv` van de tv/sync-server), opent de app **direct
  de inhoud**; de M3U-URL- en Xtream-loginvelden verschijnen alleen nog onder
  **Wijzigen**. Zenders hebben logo's, een bronregel en categoriechips. Films en series
  krijgen een **TMDB-infopagina** met poster, jaar, score en beschrijving; bij een serie
  opent vandaar de afleveringenlijst. Bij een live-zender met archief opent
  **"Terugkijken"** een EPG-lijst met de programma's uit het archief.
- **Speler:** opent automatisch in **landscape/fullscreen** en houdt de beeldverhouding
  vast. Bij live-IPTV staat links een **zenderlijst** met logo's waarin je direct kunt
  zappen; met het menu-icoon verberg je hem. De onderbalk blijft rechts van de lijst.
- **Bibliotheek (fase 7):** **Mijn** toont alleen films en series: **Verder kijken**,
  **Favorieten** en **Opnames op de ontvanger**. Live-zenders en losse recente streams
  staan er bewust niet in. **De TV is leidend voor IPTV-favorieten en -kijkposities**
  (`state.library`/`cmd.library`); oude demo-/loopback-items worden bij het synchroniseren
  verwijderd en echte TV-items komen ervoor in de plaats. Receiver-opnames komen uit
  OpenWebif en worden via dezelfde brug naar de telefoon gestuurd.
- **Settings-PIN:** heeft de TV een PIN, dan blijven de gevorderde instellingen zichtbaar
  maar vraagt de telefoon om de pincode; de TV controleert die server-side.
- **Profielfoto's:** uploaden, wissen en (API) ophalen via `PUT`/`GET`/`DELETE`; de
  profiel-PIN en de foto-blokkade blijven van kracht.

## Echte verbinding

- **Identiteit:** ECDSA P-256 in de Android Keystore; de publieke sleutel gaat mee bij het
  koppelen. Het koppeltoken staat AES-GCM-versleuteld in `SecureStore`.
- **Discovery:** `_enigmacast._tcp.` via `NsdManager`, met TXT (`id`, `name`, `app`, `fp`);
  daarnaast een handmatig IP-adres met poort 8750.
- **Koppelen:** `POST /api/v1/pair` met code + publieke sleutel. De pin komt uit TXT; bij
  een handmatig adres wordt de pin bij de eerste handshake vastgelegd (TOFU) en daarna
  vastgehouden.
- **Verbinden:** getekende `GET /ws`-upgrade (ECDSA, timestamp, nonce), daarna `hello`,
  `state.*` en `ack`. Bij wegvallen volgt automatische reconnect met backoff; een echte
  pinfout is fataal en vraagt om opnieuw koppelen.
- **Formulieren:** elk `TvTextField` op de TV opent bij bewerken een `form`-sessie; de
  telefoon toont een toetsenbord-sheet en stuurt `cmd.form` (`TEXT`/`SUBMIT`/`CANCEL`).
  **Meerveldsformulieren** werken ook: de ontvanger-setup heeft **"Vul via telefoon"** voor
  host, gebruikersnaam en wachtwoord in ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©n keer. Wachtwoorden gaan nooit mee in de
  descriptor of de logs.
- **Instellingen:** de TV stuurt `state.settings` met een catalogus van **24 gebruikers-
  instellingen** in vijf categorieÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â«n (BOOL/CHOICE/NUMBER), inclusief gevaren- en
  herstartmarkering. De telefoon wijzigt via `cmd.settings`; de TV schrijft met dezelfde
  `SettingsRepository` als zijn eigen UI. Bij een Settings-PIN blijven de entries zichtbaar
  maar vraagt de telefoon om de pincode; de TV controleert die server-side.
- **Profielbeheer:** aanmaken (max. 6), hernoemen, avatar kiezen, verwijderen (met opruimen
  van kijkgeschiedenis/favorieten) en de eigen foto wissen. Profielen met een PIN vragen om
  de pincode; foto-upload naar een PIN-profiel is geblokkeerd.
- **Profielfoto's:** de telefoon uploadt een getekende `PUT /api/v1/profiles/{id}/photo`;
  de TV cropt vierkant, hercodeert naar WebP 512 px (EXIF weg) en werkt `state.profiles`
  bij. De TV toont de foto op het profiel.
- **Bewezen op twee emulators:** koppelen met de TV-code, WSS verbonden, `CHANNEL_UP` met
  **0 ms** ack, de TV toonde "1 telefoon(s) verbonden", automatisch herverbinden, live
  tekst in het TV-zoekveld, instellingen aan/uit, de "EC"-testfoto, profiel
  aanmaken ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ hernoemen ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ avatar wijzigen ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ verwijderen ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ foto wissen, Live met
  zenderlijst/EPG en een spelende stream, M3U- en Xtream-IPTV, en "Verder kijken" +
  favoriet.

## Bouwen en testen

Vereist JDK 21 en de Android Studio JDK of een vergelijkbare JDK. Op deze machine:

```powershell
$env:JAVA_HOME = "C:\Users\user3\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat test
.\gradlew.bat :mobile:app:assembleDebug --offline
.\gradlew.bat :mobile:app:testDebugUnitTest --offline
```

Of met de Gradle-distributie die al in de cache staat (wanneer `~/.gradle` schrijfbaar is):

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.7.0-bin\*\gradle-9.7.0\bin\gradle.bat" test --offline
```

Uitkomst op 21-10-2026: **125 testklassen, 667 tests, 0 failures, 1 overgeslagen** ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â 581 in
`core-model`, 65 in `core-openwebif`, 7 in `core-protocol`, 4 in de TLS-spike (waarvan 1
optionele rooktest tegen een draaiende TV-brug) en 10 in de mobiele app.
De EnigmaCast TV-suite (inclusief de brug) staat op **189 klassen, 929 tests, 0 failures**.

### De app draaien

```powershell
adb install -r mobile\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.enigmacast.companion/.MainActivity
```

Demo: **Demo starten**, of **TV zoeken ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Woonkamer TV ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ 123456**. Zie
[`mobile/README.md`](mobile/README.md) voor de schermen en de mock.

### Offline

De build is bewust zo ingericht dat hij ook **zonder netwerk** werkt met de Gradle-cache die
de EnigmaCast-build al had gevuld:

- De Kotlin-plugins worden via de `buildscript`-classpath toegepast; de plugin-markers staan
  niet in de cache.
- `error_prone_annotations` wordt geforceerd op een wÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©l aanwezige versie.
- `kotlin-scripting-compiler-embeddable` is uitgesloten; die is alleen voor Kotlin-scripts nodig.
- `core-model` gebruikt geen OkHttp (de enige OkHttp-aanroep in het domein is verwijderd).

Zie `docs/adr/0003-offline-build.md`. Met netwerk mag dit weer naar de gewone `plugins { }`-DSL.

## Herkomst en onderhoud

`shared/**` is een **vendored kopie** van EnigmaCast op HEAD `40db96d3c47414112ff61138694b6405a2a4f445`
(branch `feature/torrents`). 131 bestanden zijn byte-identiek; 7 hebben een kleine shim en
1 bestand is nieuw. `SYNC.md` bevat de volledige tabel met SHA-256 per bestand.

Bij een wijziging in de TV-code die de gedeelde delen raakt:

1. Kopieer het bestand opnieuw uit EnigmaCast.
2. Breng de shim opnieuw aan (zie `SYNC.md`, sectie "Aangebrachte shims").
3. Werk `SYNC.md` bij.
4. Draai `gradlew test`.

De TV-repository wordt hierbij **niet** gewijzigd.

## Documentatie

- [`docs/tv-bridge.md`](docs/tv-bridge.md) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â de TV-brug: architectuur, endpoints, bewijs, beperkingen.
- [`docs/protocol.md`](docs/protocol.md) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â het volledige protocol v1.
- [`docs/threat-model.md`](docs/threat-model.md) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â dreigingen en maatregelen.
- [`docs/tls-spike.md`](docs/tls-spike.md) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â TLS-ontwerp en validatie.
- [`docs/adr/`](docs/adr/) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â architectuurbesluiten.
- [`mobile/README.md`](mobile/README.md) ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â de app, de mock en de schermen.

## Schermen

| Welkom | Afstandsbediening | Instellingen |
|---|---|---|
| ![Welkom](docs/screenshots/01-welcome.png) | ![Remote](docs/screenshots/38-phone-remote-tuner.png) | ![Instellingen](docs/screenshots/05-settings.png) |

| Zoeken | Koppelen | Profielen |
|---|---|---|
| ![Zoeken](docs/screenshots/02-scan.png) | ![Koppelen](docs/screenshots/03-pair.png) | ![Profielen](docs/screenshots/06-profiles.png) |

| Echte TV gekoppeld | TV ziet de telefoon |
|---|---|
| ![Phone paired](docs/screenshots/08-phone-paired.png) | ![TV connected](docs/screenshots/09-tv-connected.png) |

| Formulier op de telefoon | Live op de TV |
|---|---|
| ![Phone form](docs/screenshots/10-phone-form.png) | ![TV form](docs/screenshots/11-tv-form-live.png) |

| TV-instellingen | Profielfoto op de TV |
|---|---|
| ![Phone settings](docs/screenshots/12-phone-settings.png) | ![TV photo](docs/screenshots/14-tv-photo.png) |

| Volledige catalogus | Gevorderde bevestiging |
|---|---|
| ![Settings full](docs/screenshots/16-phone-settings-full.png) | ![Danger confirm](docs/screenshots/17-phone-danger-confirm.png) |

| Profielbeheer | Profiel bewerken |
|---|---|
| ![Profiles](docs/screenshots/18-phone-profiles.png) | ![Editor](docs/screenshots/19-phone-profile-editor.png) |

| Live met EPG | Speler |
|---|---|
| ![Live](docs/screenshots/20-phone-live-channels.png) | ![Player](docs/screenshots/21-phone-player.png) |

| IPTV (M3U) | Xtream Codes |
|---|---|
| ![IPTV](docs/screenshots/22-phone-iptv.png) | ![Xtream](docs/screenshots/25-phone-xtream.png) |

| Film uit Xtream | Serie-afleveringen |
|---|---|
| ![Film](docs/screenshots/26-phone-film.png) | ![Series](docs/screenshots/27-phone-series.png) |

| Bibliotheek |
|---|
| ![Library](docs/screenshots/24-phone-library.png) |

| Foto van de tv | Terugkijken: programma kiezen |
|---|---|
| ![Photo view](docs/screenshots/28-phone-photo-view.png) | ![Catch-up picker](docs/screenshots/30-phone-catchup-picker.png) |

| Terugkijken speelt | Recent op de tv |
|---|---|
| ![Catch-up play](docs/screenshots/31-phone-catchup-play.png) | ![Recent TV](docs/screenshots/32-phone-recent-tv.png) |

| IPTV-inhoud met logo's | Zenderlijst in de speler |
|---|---|
| ![IPTV content](docs/screenshots/35-phone-iptv-content.png) | ![Player channels](docs/screenshots/36-phone-player-channels.png) |


## Versie, update-JSON en sideload

- **Versie:** `1.0.0-beta.12` ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· **build 111** (`versionCode`).
- De app toont dit in **Meer** onder de titel en in **Over deze app**.
- **Sideload-APK:** `dist/enigmacast-companion.apk`.
- **In-app update:** de app controleert bij het opstarten het gecombineerde manifest en
  heeft **Meer ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âº Controleer op updates**; bij een nieuwe versie verschijnt een melding met
  **Downloaden** en **Installeren**.
- **Launcher-icoon:** hetzelfde EnigmaCast-mark als de Android TV-app. De app is met de
  debug-key ondertekend (sideload), niet voor de Play Store.
- **Companion-update-JSON:** `dist/enigmacast-companion.json`.
- **Gecombineerd met de TV:** `dist/enigmacast-updates.json`; de TV-versie staat in
  `EnigmaCast/dist/enigmacast-v2.json`. De TV leest de bovenste velden en negeert het
  `companion`-object, dus ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©n manifest bedient beide apps.
- Zie [docs/updates.md](docs/updates.md) voor het schema en de uploadstappen.

![Versie in Meer](docs/screenshots/33-phone-version.png)

![App-icoon](docs/screenshots/34-phone-icon.png)

## Volgende stap

1. Video op een fysieke Enigma2-ontvanger en IPTV-provider testen; de emulator is met een
   audiostream bewezen.
2. Een volledige meervelds-E2E op de ontvanger-setup en de TV-PIN op hardware.
3. NSD op een echt thuisnetwerk en de TLS-route op fysieke TV-boxen testen.











"# enigmacast-companion" 
