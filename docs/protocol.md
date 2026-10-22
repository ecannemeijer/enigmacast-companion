# Companion-protocol v1

Dit document is de bron van waarheid voor de communicatie tussen de EnigmaCast TV-app
(de **brug**) en de Companion-app op de telefoon. De DTO's staan in
`shared/core-protocol`; de gouden voorbeelden in `protocol/v1`.

> **Status:** v1 is bevroren voor de berichten die hier staan.
>
> **Implementatiestatus (21-10-2026):** de TV-brug implementeert §2–§7, §8, §9 (PUT) en
> §10. De catalogus in §7 bevat **24** gebruikersinstellingen in vijf categorieën, met
> `category`/`dangerous`/`requiresRestart`. Profielbeheer (`CREATE`/`RENAME`/`SET_AVATAR`/
> `DELETE`, profiel-PIN) is geïmplementeerd. Bij een Settings-PIN blijven de entries
> zichtbaar (`locked: true`); een gevorderd pad vraagt de pincode mee te sturen en de TV
> weigert een lege/onjuiste PIN met `SETTINGS_LOCKED`. De gedeelde IPTV-bibliotheek
> (`state.library`/`cmd.library`) en foto-`GET`/`DELETE` zijn geïmplementeerd.
> De telefoon spreekt het protocol tegen de **echte** brug (NSD of handmatig adres,
> TLS-pin, getekende WSS). De mock blijft voor de demo.
> Zie `docs/tv-bridge.md`.

**Buiten het protocol (fase 5–7).** Live-tv, IPTV en de bibliotheek gaan niet over deze
WebSocket: de telefoon leest OpenWebif/M3U/Xtream rechtstreeks via HTTP(S) en speelt de
stream met Media3. Zie `docs/playback.md`. De brug blijft de enige route voor
afstandsbediening, formulieren, instellingen, profielen en foto's.

## 1. Uitgangspunten

| Keuze | Waarde |
|---|---|
| Versie | `v: 1` in elk WS-bericht; `api: "v1"` in `hello`, `/health` en `/pair` |
| Transport | Eén TLS-poort (standaard **8750**): HTTPS voor request/response, WSS op `/ws` voor realtime |
| Ontdekking | mDNS `_enigmacast._tcp.` + subnet-scan + handmatig IP + QR |
| Serialisatie | JSON, UTF-8, eigenschappen in camelCase |
| Enums | **UPPERCASE** op de draad (`DPAD_DOWN`, `GLASS_EARTH`, `PASSWORD`) |
| Vrije velden | `source`, `route`, `status`, `mode`, `screen.app` zijn lowercase strings |
| Onbekende velden | Negeren (forward-compatible) |
| Onbekende berichttypen | Negeren, geen fout |
| Tijd | Unix-epoch in milliseconden |
| Limieten | Zie §10 |

## 2. Ontdekking

De TV registreert bij het starten van de brug:

```text
Service type: _enigmacast._tcp.
Poort:        8750 (TLS)
TXT:
  v=1
  id=6f1c2a9e-0000-4000-8000-000000000001
  name=Woonkamer TV
  app=1.0.0-beta.72
  fp=<base64url(SHA-256(SPKI))>
```

De telefoon:

1. browst mDNS (met Wi-Fi multicast lock, zoals `NsdReceiverDiscovery` in EnigmaCast);
2. resolve't één service tegelijk;
3. doet `GET /api/v1/health` en accepteert alleen een certificaat waarvan de SPKI-hash
   gelijk is aan `fp` (de TXT-waarde is een **hint**; de pin wordt bij het koppelen
   definitief vastgelegd);
4. valt terug op een scan van de eigen /24 op poort 8750, en daarna op handmatige invoer.

`GET /api/v1/health` (geen auth):

```json
{
  "ok": true,
  "service": "enigmacast-companion",
  "api": "v1",
  "tvId": "6f1c2a9e-0000-4000-8000-000000000001",
  "name": "Woonkamer TV",
  "pairing": true
}
```

`pairing` is `true` zolang de TV een koppelcode toont.

## 3. Koppelen

### 3.1 QR

De TV toont een QR met:

```text
enigmacast://pair?v=1&h=192.168.1.50&p=8750&id=<tvId>&fp=<spki-hash>&c=<6 cijfers>
```

De QR bevat dus host, poort, TV-id, **certificaatpin** en code. Scannen is voldoende;
de telefoon hoeft dan niet te zoeken.

### 3.2 Pincode

Zonder QR toont de TV een 6-cijferige code (120 s geldig, eenmalig, 5 pogingen per
5 minuten per IP). De telefoon zoekt de TV via §2 en vraagt de gebruiker de code.

### 3.3 Uitwisseling

```http
POST /api/v1/pair
Content-Type: application/json

{
  "code": "483920",
  "name": "Pixel 8",
  "platform": "android",
  "publicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE…",
  "nonce": "5b0e2f5e-6a1f-4f0e-9b1a-2f6f0f2a9c11"
}
```

```json
{
  "deviceId": "d41d8cd98f00b204e9800998ecf8427e",
  "token": "9tK… (32 random bytes, base64url)",
  "role": "member",
  "tv": {"id": "6f1c…", "name": "Woonkamer TV", "app": "1.0.0-beta.72"},
  "api": "v1",
  "capabilities": ["key", "nav", "zap", "volume", "text.form", "settings", "profiles", "photo", "state", "power"]
}
```

- `publicKey` is de Base64 van de **SPKI** van een ECDSA P-256-sleutel die in de Android
  Keystore van de telefoon is gemaakt en niet exporteerbaar is.
- De TV bewaart alleen `SHA-256(token)` en de publieke sleutel.
- De TV controleert dat het aangeboden TLS-certificaat bij zijn eigen sleutel hoort
  (pin = eigen SPKI-hash), dat de code ongebruikt en niet verlopen is, en rate-limit.
- Het eerste gekoppelde toestel krijgt `admin` (mag andere toestellen intrekken); de TV
  zelf is altijd beheerder via zijn eigen scherm.

## 4. Authenticatie

Elk request na het koppelen bevat:

```http
Authorization: Bearer <token>
X-Device-Id: <deviceId>
X-Timestamp: 1761100000123
X-Nonce: 5b0e2f5e-6a1f-4f0e-9b1a-2f6f0f2a9c11
X-Signature: <base64(ECDSA-SHA256)>
```

Te ondertekenen bytes (`AuthSignature.canonical`):

```text
<timestamp> \n <nonce> \n <METHOD> \n <path?query> \n <32 ruwe bytes van SHA-256(body)>
```

Voorbeeld (vector uit `ProtocolContractTest`):

```text
1761100000123
5b0e2f5e-6a1f-4f0e-9b1a-2f6f0f2a9c11
GET
/api/v1/state?since=41
<SHA-256("{}") = 44136fa3…ff8a, als bytes>
```

Regels:

| Controle | Waarde |
|---|---|
| Token | `SHA-256(token)` moet bij een niet-ingetrokken toestel horen |
| Klok | `abs(now - X-Timestamp) <= 120 s` |
| Nonce | Nog niet gezien binnen het venster; daarna opslaan tot het venster voorbij is |
| Handtekening | ECDSA-SHA256 met de opgeslagen publieke sleutel |
| Query | De query maakt deel uit van het ondertekende pad, in dezelfde volgorde |
| Body | De hash gaat over de **ruwe** body-bytes, ook bij een lege body |

De WebSocket-upgrade is een gewoon getekend `GET /ws` (zonder query). Na de upgrade
erft de verbinding de identiteit; berichten hoeven niet opnieuw ondertekend te worden,
omdat TLS de verbinding al beveiligt en de handshake geauthenticeerd is.

## 5. WebSocket

### 5.1 Envelope

```json
{
  "v": 1,
  "id": "c-101",
  "seq": 57,
  "type": "cmd.key",
  "ts": 1761100000123,
  "payload": { }
}
```

| Veld | Verplicht | Betekenis |
|---|---|---|
| `v` | ja | Protocolversie (1) |
| `type` | ja | Berichttype |
| `id` | client-commando | Uniek per verbinding; de ack gebruikt hetzelfde id |
| `seq` | server-event | Monotoon; `hello` is 0 |
| `ts` | nee | Tijdstempel van de afzender |
| `payload` | nee | Type-specifieke inhoud |

### 5.2 Verbindingslevenscyclus

1. Telefoon opent `wss://host:8750/ws` met de headers uit §4.
2. TV stuurt direct `hello` (seq 0) met de volledige toestand.
3. Telefoon stuurt commando's; TV antwoordt met `ack` (zelfde `id`).
4. TV stuurt `state.*`-events met oplopende `seq`.
5. Hartslag: telefoon stuurt elke 10 s `ping`; TV antwoordt `pong`. Geen `pong` binnen
   5 s → sluiten en opnieuw verbinden.
6. Na een reconnect stuurt de TV opnieuw `hello` (nieuwe snapshot). De telefoon mag met
   `GET /api/v1/state?since=<seq>` bijwerken als hij alleen delta's wil.

### 5.3 Reconnect

- Backoff: 250 ms, 500 ms, 1 s, 2 s, 4 s, 8 s (max), ±20 % jitter.
- Reset na 30 s stabiel.
- Bij een netwerkverandering (`ConnectivityManager`) direct opnieuw.
- Bij een nieuwe mDNS-resolve: nieuw adres proberen zonder de oude verbinding te sluiten
  voordat de nieuwe `hello` binnen is.

## 6. Berichten

### 6.1 Client → server

| Type | Payload | Doel |
|---|---|---|
| `cmd.key` | `KeyCommand` | D-pad, OK, Back, media, kleur, cijfers |
| `cmd.nav` | `NavCommand` | Naar Home, dashboard, settings, gids, zoeken, My Items, IPTV, speler |
| `cmd.zap` | `ZapCommand` | Zappen op service-referentie of zendernummer |
| `cmd.volume` | `VolumeCommand` | Volume/mute op TV, ontvanger of telefoon |
| `cmd.player` | `PlayerCommand` | Play, pause, stop, seek, snelheid |
| `cmd.form` | `FormSubmitCommand` | Formulier in één keer verzenden (incl. wachtwoorden) |
| `cmd.settings` | `SettingsCommand` | Instellingen patchen met `baseRevision` |
| `cmd.profile` | `ProfileCommand` | Profiel kiezen, maken, hernoemen, avatar, verwijderen |
| `cmd.library` | `LibraryCommand` | IPTV-favoriet of kijkpositie terugschrijven |
| `cmd.recent` | `RecentCommand` | Zelf bekeken Enigma2-zender terugmelden |
| `cmd.power` | `PowerCommand` | Ontvanger standby/wake/reboot/GUI/herstart |
| `cmd.sync` | `SyncCommand` | Sync met de ontvanger nu uitvoeren |
| `ping` | — | Hartslag |

`cmd.key`:

```json
{"v":1,"id":"c-101","type":"cmd.key","ts":1761100000123,
 "payload":{"key":"DPAD_DOWN","phase":"DOWN","repeat":0,"long":false}}
```

- `phase`: `DOWN`, `UP` of `REPEAT`.
- `repeat` telt op bij herhaalde richting-/kanaaltoetsen (0 bij de eerste).
- `long` wordt gezet wanneer de gebruiker langer dan `longPressMs` (standaard 500 ms) houdt.
- De TV vertaalt dit naar een `KeyEvent` met dezelfde `keyCode`, `repeatCount` en
  `eventTime`, en levert die aan `Activity.dispatchKeyEvent`. Daardoor gelden exact
  dezelfde regels als op de afstandsbediening (KeyRepeatGate, Back-hold, toewijzingen).

`cmd.zap`:

```json
{"type":"cmd.zap","payload":{"serviceRef":"1:0:19:4B6F:2E:19:C00000:0:0:0:","bouquetRef":"1:7:1:0:0:0:0:0:0:0:FROM BOUQUET \"userbouquet.favourites.tv\" ORDER BY bouquet"}}
```

`cmd.settings`:

```json
{"v":1,"id":"c-110","type":"cmd.settings",
 "payload":{"baseRevision":42,
            "ops":[{"path":"osdSkin","value":"GLASS_EARTH"},
                   {"path":"playback.hdmiPassthrough","value":false}],
            "confirm":false,
            "pin":"1234"}}
```

### 6.2 Server → client

| Type | Payload | Wanneer |
|---|---|---|
| `hello` | `HelloPayload` | Direct na de upgrade en na elke reconnect |
| `ack` | `AckPayload` | Antwoord op een commando (`id` gelijk) |
| `pong` | — | Antwoord op `ping` |
| `state.player` | `StatePlayerPayload` | Zender/speler/tuner verandert |
| `state.screen` | `StateScreenPayload` | De TV wisselt van scherm of dialoog |
| `state.settings` | `SettingsCatalogPayload` | Na een patch of externe wijziging |
| `state.profiles` | `StateProfilesPayload` | Profielen of avatar wijzigen |
| `state.library` | `LibraryStatePayload` | IPTV-favorieten/kijkposities van het actieve profiel |
| `state.recent` | `RecentStatePayload` | Recente Enigma2-zenders van de actieve ontvanger |
| `state.iptv` | `IptvSourcePayload` | M3U/Xtream-bron uit de TV-playliststore of sync-server |
| `state.tuner` | `StatePlayerPayload` (tuner-deel) | Tunerbezetting wijzigt |
| `form` | `FormDescriptor` | Een formulier wordt geopend/bijgewerkt |
| `toast` | `ToastPayload` | Korte melding op de telefoon |
| `error` | `ProtocolError` | Afwijzing van een commando |

`hello`:
```json
{"v":1,"seq":0,"type":"hello","ts":1761100000000,
 "payload":{
   "tv":{"id":"6f1c…","name":"Woonkamer TV","app":"1.0.0-beta.72"},
   "api":"v1",
   "capabilities":["key","nav","zap","volume","text.form","settings","profiles","photo","state","power"],
   "screen":{"app":"dashboard","section":"home"},
   "profile":{"activeId":"default","profiles":[{"id":"default","name":"Thuis","avatar":3,"photoStamp":0,"hasPin":false}]},
   "receiver":{"id":"sf8008","name":"SF8008","online":true,"mode":"own_tuner"},
   "settingsRevision":42}}
```

`state.player`:

```json
{"v":1,"seq":57,"type":"state.player","ts":1761100001000,
 "payload":{
   "source":"enigma2",
   "channel":{"ref":"1:0:19:4B6F:2E:19:C00000:0:0:0:","name":"NPO 1 HD","number":1,"picon":"/picon/1_0_19_….png"},
   "now":{"title":"NOS Journaal","startMs":1761099600000,"endMs":1761101400000,"progress":0.42,
          "nextTitle":"EenVandaag","nextStartMs":1761101400000},
   "player":{"playing":true,"paused":false,"positionMs":0,"durationMs":0,"bufferedMs":0},
   "tuner":{"used":1,"total":2,"status":"PLAYING"},
   "route":"home"}}
```

`ack` en `error`:

```json
{"v":1,"id":"c-101","type":"ack","payload":{"ok":true,"latencyMs":11}}
{"v":1,"id":"c-102","type":"error","payload":{"code":"AUTH_REPLAY","message":"Nonce already used","retryable":false}}
```

`NowInfo.nextTitle` en `nextStartMs` zijn optioneel; de telefoon toont "Straks: …" als ze
aanwezig zijn. Ze zijn backward-compatible toegevoegd (ontbreken = geen volgende regel).

### 6.3 Profielbeheer

`cmd.profile` met `ProfileCommand`:

| `action` | Velden | Betekenis |
|---|---|---|
| `SELECT` | `uuid`, `pin?` | Profiel kiezen; PIN nodig als het profiel er een heeft |
| `CREATE` | `name`, `avatar` | Nieuw profiel (max. 6) |
| `RENAME` | `uuid`, `name`, `pin?`, `clearPhoto?` | Naam wijzigen; optioneel de foto wissen |
| `SET_AVATAR` | `uuid`, `avatar`, `pin?`, `clearPhoto?` | Avatar wijzigen; optioneel de foto wissen |
| `DELETE` | `uuid`, `pin?` | Profiel verwijderen (niet het actieve/laatste) |

Regels:

- Naam: 1–20 tekens; avatar: 0–15; maximaal 6 profielen.
- Het actieve of laatste profiel kan niet verwijderd worden.
- Bij een profiel-PIN is de pincode nodig voor `SELECT` (niet-actief), `RENAME`,
  `SET_AVATAR` en `DELETE`. Fouten: `VALIDATION` met message `PROFILE_PIN_WRONG` of
  `PROFILE_PIN_COOLDOWN`.
- `DELETE` ruimt ook de kijkgeschiedenis, favorieten, foto en PIN van dat profiel op.
- Foto-upload (`PUT /photo`) is voor een PIN-profiel geblokkeerd; de avatar/foto wijzigen
  gaat via `SET_AVATAR`/`clearPhoto` met PIN.

### 6.4 Gedeelde bibliotheek

De TV stuurt bij het verbinden en na elke wijziging `state.library`:

```json
{"favorites":[{"title":"Testfilm","url":"http://…/movie/demo/demo/201.mp4",
               "source":"…","kind":"m","id":"201","favorite":true,"updatedMs":…}],
 "resume":[{"title":"Testfilm","url":"http://…","source":"…","kind":"m","id":"201",
            "positionMs":18423,"durationMs":60000,"updatedMs":…}]}
```

De telefoon schrijft een favoriet of positie terug:

```json
{"action":"PROGRESS",
 "entry":{"title":"Testfilm","url":"http://…","source":"…","kind":"m","id":"201",
          "positionMs":18423,"durationMs":60000,"favorite":false,"updatedMs":…}}
```

`source`/`kind`/`id` zijn dezelfde sleutel als de TV-bibliotheek (`<source>/<kind>/<id>`);
`kind` is `m` (film), `s` (serie) of `e` (aflevering). De TV schrijft via de bestaande
`IptvLibrary` (inclusief cloud-outbox). Live/Enigma2-geschiedenis valt buiten dit contract.

### 6.5 Recente zenders

De TV stuurt bij het verbinden en na elke wijziging `state.recent`:

```json
{"receiverId":"…","receiverName":"Woonkamer",
 "channels":[{"reference":"1:0:19:…","name":"NPO 1 HD","bouquet":"1:7:1:…"}]}
```

De lijst komt uit `ChannelMemory.recentFor` van de actieve ontvanger. De telefoon toont
hem als "Recent op de tv" en speelt een zender via zijn eigen OpenWebif-stream; de
referenties moeten op dezelfde ontvanger geldig zijn.

Bekijkt de telefoon zelf een zender op **dezelfde** ontvanger, dan meldt hij die terug:

```json
{"receiverHost":"192.168.1.209","reference":"1:0:19:…","name":"NPO 1 HD","bouquet":"1:7:1:…"}
```

De TV controleert `receiverHost` tegen de actieve ontvanger en werkt anders niets bij.

### 6.6 IPTV-bron

De TV stuurt `state.iptv` met de M3U/Xtream-login die hij lokaal heeft, of die hij bij de
sync-server heeft opgehaald:

```json
{"configured":true,"kind":"XTREAM","server":"http://provider:8080",
 "username":"user","password":"…","m3uUrl":"","updatedMs":…,"fromSyncServer":true}
```

De telefoon bewaart de M3U-URL en het Xtream-wachtwoord versleuteld en logt automatisch in.
`configured:false` betekent: geen bron gevonden; de handmatige invoer blijft dan werken.
Deze state gaat alleen over de gekoppelde WSS.

## 7. Instellingen

### 7.1 Catalogus

`GET /api/v1/catalog/settings` (auth) — **v1-vorm, implementatie in fase 4**:

```json
{
  "revision": 42,
  "entries": [
    {
      "path": "osdSkin",
      "type": "ENUM",
      "scope": "PROFILE",
      "options": ["STANDARD", "OPENPLI", "MIDNIGHT", "METRIX", "AURORA", "GLASS_EARTH", "GLASS_WAVE"],
      "requiresRestart": false,
      "dangerous": false,
      "requiresSettingsUnlock": false
    },
    {
      "path": "playback.videoDecoder",
      "type": "ENUM",
      "scope": "DEVICE",
      "options": ["AUTO", "HARDWARE", "SOFTWARE"],
      "requiresRestart": true,
      "dangerous": false,
      "requiresSettingsUnlock": false
    }
  ]
}
```

De telefoon rendert hieruit generiek een formulier. Nieuwe TV-instellingen verschijnen
dan zonder app-update.

**Implementatie (fase 4).** De brug gebruikt de WebSocket-vorm: `state.settings` bevat een
`SettingsCatalogPayload` met `revision`, `locked` en `entries`. Elke entry heeft `path`,
`label`, `description`, `category`, `type` (`BOOL`, `CHOICE`, `NUMBER`), `value`, `options`
(met label), optioneel `min`/`max`/`step`, en de markeringen `dangerous` en
`requiresRestart`. De catalogus bevat 24 gebruikersinstellingen in vijf categorieën:

| Categorie | Paden |
|---|---|
| Weergave | `osdSkin`, `motionQuality`, `navigationSounds`, `osd.transparency`, `osd.fastPolling`, `osd.hideTimeoutMs`, `osd.debugBorders` |
| Afspelen | `playback.subtitlesEnabled`, `playback.lowLatency`, `playback.hardwareDecoder`, `playback.autoReconnect`, `playback.hdmiPassthrough`, `playback.audioDelayPcmMs`, `playback.audioDelayPassthroughMs`, `playback.subtitleDelayMs`, `playback.videoDecoder`, `playback.videoCompatibility`, `playback.recordingTarget` |
| Opstarten | `startupLive`, `tvFallback` |
| Bediening | `remote.keyRepeatIntervalMs`, `remote.longPressMs` |
| Debug | `debug.overlay`, `debug.verboseLogging` |

De TV schrijft via `SettingsRepository.update` (dezelfde route als zijn eigen UI) en
stuurt daarna een nieuwe catalogus. `cmd.settings` met een verouderde `baseRevision` geeft
`SETTINGS_CONFLICT` (409). De HTTP-vorm `GET /api/v1/catalog/settings` is gereserveerd.

Heeft de TV een **Settings-PIN**, dan blijft de catalogus volledig zichtbaar met
`locked: true`. Bij een `dangerous`-pad stuurt de telefoon de pincode als `pin` mee; de TV
controleert die met `SettingsLock` en weigert een lege/onjuiste PIN met
`SETTINGS_LOCKED` (423). Na een juiste PIN wordt de catalogus ontgrendeld
(`locked: false`). Niet-gevoelige instellingen blijven zonder PIN beschikbaar.

### 7.2 Patch

`PATCH /api/v1/settings` (auth) of `cmd.settings` over de WS:

```json
{"baseRevision": 42, "ops": [{"path": "osdSkin", "value": "GLASS_EARTH"}], "confirm": false, "pin": "1234"}
```

| Uitkomst | Betekenis |
|---|---|
| `200` + `SettingsResultPayload` | Toegepast; `restart` noemt paden die de speler/sessie herbouwen |
| `409 SETTINGS_CONFLICT` | `baseRevision` is verouderd; de respons bevat de actuele `revision` en waarden |
| `423 SETTINGS_LOCKED` | De TV heeft een Settings-PIN; opnieuw met `X-Settings-Pin` |
| `422 VALIDATION` | Minstens één pad is ongeldig; `rejected` noemt pad en code |

De TV schrijft via dezelfde `SettingsRepository.update` als zijn eigen UI, zodat
geheimen in de Keystore belanden en per-profielvelden in de profielscope.

## 8. Formulieren

De TV opent een formsessie zodra een `TvTextField` in bewerken gaat, of wanneer de
ontvanger-setup **"Vul via telefoon"** kiest. Het `form`-event (of
`GET /api/v1/forms/{formId}`) geeft een `FormDescriptor` met één of meer velden:

```json
{
  "formId": "field-1",
  "title": "Zoeken",
  "revision": 3,
  "closed": false,
  "fields": [
    {"id":"value","label":"Zoeken","type":"TEXT","value":"test","masked":false,"sensitive":false,"required":false}
  ],
  "errors": {}
}
```

Een meerveldsvoorbeeld (ontvanger-setup):

```json
{"formId":"field-2","title":"Ontvanger instellen","revision":1,"closed":false,
 "fields":[
   {"id":"host","label":"Adres","type":"HOST","value":"192.168.1.20","masked":false},
   {"id":"username","label":"Gebruikersnaam","type":"TEXT","value":"root","masked":false},
   {"id":"password","label":"Wachtwoord","type":"PASSWORD","masked":true}
 ],
 "errors":{}}
```

Live typen en verzenden via `cmd.form`:

```json
{"formId":"field-1","revision":3,"action":"TEXT","values":{"value":"test"}}
```

| `action` | Betekenis |
|---|---|
| `TEXT` | Waarde live bijwerken; de TV past het veld meteen aan |
| `SUBMIT` | Klaar: laatste waarde toepassen en de sessie sluiten |
| `CANCEL` | Sluiten zonder de laatste waarde toe te passen |

Regels:

- Een `sensitive`/`masked` veld heeft **nooit** een `value` in de descriptor; de telefoon
  begint leeg. Wachtwoorden gaan één keer mee, worden nooit geëchood, gelogd of in een
  `state`-event opgenomen.
- De TV valideert met dezelfde regels als zijn eigen scherm (`ReceiverConfigValidator`
  voor ontvangers) en antwoordt met `errors` per veld.
- De TV sluit de sessie ook als de focus van het veld verdwijnt; de laatste `form` heeft
  dan `closed: true` en geen velden.
- Een leeg `SUBMIT` op een `sensitive` veld betekent "niet gewijzigd": de bestaande waarde
  blijft staan. Live `TEXT` gaat alleen voor niet-gevoelige velden.
- Elke `cmd.form` krijgt een `ack`; een onbekend `formId` of een oude `revision` wordt
  genegeerd (de telefoon stuurt bij de volgende sessie opnieuw).
- De HTTP-vorm (`POST /api/v1/forms/{formId}`) is gereserveerd; de TV-brug gebruikt in
  fase 3 de WebSocket-variant.
- De telefoon rendert elk veld generiek (tekst, wachtwoord, host, nummer, meerregelig);
  `SELECT`/`TOGGLE` zijn gereserveerd.
- **Meerveldsformulieren:** `CompanionFormHost.openForm` opent meerdere velden; `TEXT`
  stuurt alleen de niet-sensitive velden, `SUBMIT` stuurt alle waarden als map. Een leeg
  wachtwoord betekent "niet gewijzigd". De ontvanger-setup gebruikt dit via
  **"Vul via telefoon"**.

## 9. Avatars

| Methode | Pad | Body |
|---|---|---|
| `PUT` | `/api/v1/profiles/{uuid}/photo?x=0.1&y=0.05&s=0.8` | WebP/JPEG/PNG, max. 10 MB |
| `GET` | `/api/v1/profiles/{uuid}/photo?v=<photoStamp>` | — |
| `DELETE` | `/api/v1/profiles/{uuid}/photo` | — |

- `x`, `y` en `s` zijn fracties van het getoonde (EXIF-geroteerde) beeld, zoals
  `PhotoImage.square` ze verwacht.
- De TV controleert magic bytes, formaat, afmetingen (≤ 50 MP) en hercodeert naar
  WebP 512 px. Daarmee verdwijnt EXIF/GPS.
- Na opslaan: `photoStamp` in `ProfileStore` bijwerken en `state.profiles` sturen.
- `GET` is geauthenticeerd en gebruikt `photoStamp` als cache-buster.

**Implementatie (fase 4).** `PUT` is geïmplementeerd: de telefoon stuurt de geselecteerde
foto (max. 10 MB) met een getekend request; de TV controleert magic bytes en afmetingen,
cropt vierkant (zonder `x`/`y`/`s` uit het midden), hercodeert naar WebP 512 px en werkt
`photoStamp` + `state.profiles` bij. `GET` (WebP) en `DELETE` (bestand + stempel) zijn
geïmplementeerd; de telefoonclient heeft `fetch`/`delete`. De telefoon toont na de upload
"Eigen foto"; de foto zelf blijft op de TV. Voor een profiel met een PIN is de upload
geblokkeerd (de avatar/foto wijzigt dan via `SET_AVATAR` met PIN).

## 10. Foutcodes en limieten

| Code | HTTP | Betekenis |
|---|---|---|
| `PAIR_CODE_INVALID` | 401 | Code fout, verlopen of gebruikt |
| `PAIR_RATE_LIMITED` | 429 | Te veel pogingen |
| `AUTH_MISSING` | 401 | Geen token |
| `AUTH_REPLAY` | 401 | Nonce al gezien |
| `AUTH_SKEW` | 401 | Timestamp buiten ±120 s |
| `AUTH_SIGNATURE` | 401 | Handtekening ongeldig |
| `DEVICE_REVOKED` | 403 | Toestel ingetrokken |
| `APP_NOT_FOREGROUND` | 409 | TV-app niet actief; alleen ontvangeracties mogelijk |
| `SETTINGS_CONFLICT` | 409 | `baseRevision` verouderd |
| `SETTINGS_LOCKED` | 423 | Settings-PIN vereist |
| `VALIDATION` | 422 | Veldfouten |
| `PAYLOAD_TOO_LARGE` | 413 | Body boven de limiet |
| `NOT_FOUND` | 404 | Onbekend pad of formulier |
| `INTERNAL` | 500 | Onverwacht |

| Limiet | Waarde |
|---|---|
| Foto | 10 MB, 50 MP, uitvoer 512 px |
| WS-frame | 64 KB |
| Settings-patch | 256 KB |
| Koppelcode | 120 s, 5 pogingen per 5 min per IP |
| Klokverschil | 120 s |
| Token | 90 dagen, intrekbaar |

## 11. Compatibiliteit

- Een oudere telefoon met een nieuwere TV: onbekende velden en berichttypen negeren;
  `capabilities` bepaalt welke UI wordt getoond.
- Een nieuwere telefoon met een oudere TV: idem; de TV negeert onbekende commando's en
  antwoordt met `ack` of `error` (`NOT_FOUND`).
- Een wijziging die oude clients zou breken, verhoogt `Protocol.VERSION` en krijgt een
  nieuwe map `protocol/v2`; de TV ondersteunt dan tijdelijk beide.

## 12. Tests

`ProtocolContractTest` leest elk bestand in `protocol/v1`, decodeert het naar `Envelope`,
schrijft het opnieuw en controleert dat geen veld verandert. Daarnaast staan de
handtekeningvector en de query-encoding vast. Een wijziging aan een DTO zonder de
gouden bestanden bij te werken, laat de test falen.
