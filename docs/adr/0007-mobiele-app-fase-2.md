# ADR 0007 — Mobiele app fase 2: mock-first, handmatige DI, geen navigatiebibliotheek

- **Status:** aangenomen (fase 2)
- **Datum:** 2026-10-21

## Context

De afstandsbediening, het koppelen en de schermstructuur moeten nu gebouwd en getest worden,
terwijl de TV-brug pas later in de EnigmaCast-repo komt. De build moet bovendien offline
blijven werken en de app moet visueel aansluiten bij de TV-app.

## Beslissing

1. **Mock-first.** `MockBridge` implementeert `CompanionConnection` en spreekt het echte
   protocol (`Envelope`, `KeyCommand`, `StatePlayerPayload`, `ack`, `error`). De UI, de
   ViewModel en de protocolmodule zijn daardoor nu al definitief; fase 2 vervangt alleen de
   implementatie achter de interface.
2. **Handmatige DI** (`AppContainer`) in plaats van Hilt. Reden: geen extra Gradle-plugins
   of KSP in deze fase, en de app heeft één verbinding en één store. Hilt kan later worden
   toegevoegd zonder de UI te wijzigen.
3. **Geen Navigation-Compose.** `AppScreen` + `when` in `CompanionApp`, net als de TV-app.
   Minder afhankelijkheden en dezelfde navigatiestijl.
4. **Geen Room/DataStore in fase 2.** `LocalStore` (SharedPreferences) bewaart de
   gekoppelde TV en de trilinstelling. Fase 4 brengt DataStore, Keystore en de echte
   tokenopslag.
5. **Material 3 met de Glass Earth-tokens** uit `ui/theme/Skins.kt` (canvas `#03060F`,
   accent `#5AA9FF`, highlight `#9ADCFF`, Inter). Donker thema; dynamic color uit.
6. **Telefoon-first, minSdk 26.** De TV-app blijft `minSdk 28`; de Companion ondersteunt
   iets oudere telefoons. `compileSdk`/`targetSdk` 37 volgt de TV-app en Compose 1.12.
7. **Landschap scrollt.** De remote gebruikt in landschap twee scrollende kolommen; de
   videospeler (fase 5) krijgt een eigen, niet-scrollende layout.

## Alternatieven

| Alternatief | Waarom niet |
|---|---|
| Wachten op de TV-brug | De app en het protocol kunnen nu al bewezen worden; de brug is de enige afhankelijkheid |
| Hilt vanaf dag één | Extra plugin/KSP, meer buildrisico, geen voordeel bij één container |
| Navigation-Compose | Extra dependency voor vijf tabs en een handvol schermen |
| De TV-UI hergebruiken | `androidx.tv.material3` en D-pad-focus zijn ongeschikt voor touch |

## Gevolgen

- **Positief:** de app is nu demo-baar en getest (5 mock-tests); de protocolcode is dezelfde
  als straks; de TV-repo blijft ongemoeid.
- **Negatief:** de mock kan gedrag simpel houden dat de echte brug complex maakt (tuners,
  rechten, lifecycle). Die gevallen komen in fase 2/5 met echte integratietests.
- **Let op:** `MockBridge` mag nooit in een releasebuild als enige verbinding overblijven;
  fase 2 voegt de WebSocket-client toe en de mock wordt debug-only.
