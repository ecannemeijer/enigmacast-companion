# ADR 0001 — Aparte Companion-repo met gedeelde JVM-modules en een geïsoleerde TV-brug

- **Status:** aangenomen (fase 0)
- **Datum:** 2026-10-21

## Context

De Companion moet bestaande EnigmaCast-logica hergebruiken (OpenWebif-parsers, Xtream,
EPG, tunerregels, sync-conflicten), maar de TV-app is één Gradle-module van 64k regels.
Een grote refactor van de TV-app is expliciet ongewenst: elk regressierisico raakt een
productie-app die al in gebruik is.

## Beslissing

1. De Companion komt in een **eigen repository** met eigen Gradle-modules.
2. De gedeelde code wordt **gekopieerd** uit EnigmaCast op een vastgepinde commit, met
   **dezelfde packagenamen**, en per bestand vastgelegd in `SYNC.md` (bronpad + SHA-256).
3. De enige wijziging in de TV-repo is later een **aparte module** `:companion-bridge`
   met vijf aansluitpunten, uitschakelbaar met één dependency-regel.4. Omdat de packagenamen gelijk blijven, kan de TV-app de modules later zonder
   importwijzigingen overnemen (aandachtspunten: `internal`-zichtbaarheid en `R`).

## Alternatieven

| Alternatief | Waarom niet |
|---|---|
| Alles dupliceren zonder afspraken | Drift; twee keer dezelfde parserfix |
| De TV-app eerst opsplitsen in modules | Groot regressierisico, veel tijd, geen directe waarde |
| Eén gedeelde multi-module codebase vanaf dag één | Vereist dat de TV-app meeverhuist; strijdig met "functioneel ongewijzigd" |

## Gevolgen

- **Positief:** de TV-app blijft ongewijzigd; de Companion krijgt echte code; later is
  adoptie mechanisch.
- **Negatief:** duplicates tot de TV-app de modules overneemt; daarom `SYNC.md` en een
  kwartaalreview.
- **Neutraal:** de Companion heeft een eigen versionCode/versionName en releasecyclus.

## Update 21-10-2026 — brug werd een package, geen module

De TV-brug is géén losse Gradle-module geworden maar een geïsoleerd package in `:app`
(`com.enigmacast.companionbridge`), omdat de plugin-marker `com.android.library` niet in de
offline cache staat. De scheiding blijft: eigen package, eigen tests, één BuildConfig-flag.
Zie ADR 0008. De rest van deze ADR (aparte repo, vendoring, zelfde packagenamen) staat.
