# ADR 0015 — Gedeelde IPTV-bibliotheek tussen TV en telefoon

- **Status:** aangenomen en geïmplementeerd (fase 7.1)
- **Datum:** 2026-10-22

## Context

De telefoon hield "verder kijken" en favorieten lokaal bij; de TV bewaart zijn IPTV-
favorieten en kijkposities per profiel in `IptvLibrary`/Room en synchroniseert die al met
de EnigmaCast-syncserver. De volgende stap was dat de telefoon dezelfde bibliotheek ziet
en wijzigingen terugschrijft.

## Beslissing

1. De brug stuurt bij het verbinden en na elke wijziging `state.library` met de
   **favorieten** en **kijkposities** van het actieve TV-profiel (`IptvLibrary.favorites`/
   `resumes`).
2. De telefoon voegt die samen met zijn `LibraryStore` (versleuteld in `SecureStore`) op
   basis van URL of `<source>/<kind>/<id>`.
3. De telefoon schrijft een ster of positie terug met `cmd.library`
   (`FAVORITE`/`PROGRESS`); de TV gebruikt `IptvLibrary.setFavorite`/`saveResume`, dezelfde
   route als zijn eigen Library, inclusief de cloud-outbox.
4. De sync is **IPTV-only** en volgt het actieve TV-profiel. Live/Enigma2-geschiedenis
   blijft lokaal; een telefoon-item zonder TV-record wordt alleen lokaal bewaard.
5. Alles gaat over de bestaande getekende WSS; er komen geen nieuwe poorten of servers.

## Gevolgen

- **Positief:** "verder kijken" en favorieten van IPTV zijn nu echt gedeeld; de TV-Library
  en de telefoon gebruiken dezelfde records en dezelfde cloud-outbox.
- **Negatief:** live/Enigma2 en vrije M3U-items zonder TV-record synchroniseren niet; de
  TV moet op de voorgrond draaien (ADR 0006).
- **Veiligheid:** alleen een gekoppeld toestel; de entries bevatten IPTV-URL's en staan
  lokaal versleuteld op de telefoon.
- **Bewijs:** Testfilm vanaf de telefoon → `iptv_resume` (18 s/60 s) en `iptv_library` op
  de TV; `state.library` bij verbinden.
