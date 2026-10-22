# ADR 0012 — Directe Enigma2- en IPTV-weergave op de telefoon

- **Status:** aangenomen en geïmplementeerd (fase 5/6)
- **Datum:** 2026-10-21

## Context

De telefoon moest live tv kunnen kijken zonder dat de EnigmaCast-TV aan staat. De
TV-brug is ontworpen voor afstandsbediening en status, niet voor het doorsturen van
videobytes; proxying via de TV zou de TV belasten en de streams vertragen.

## Beslissing

1. De telefoon leest de Enigma2-zenderlijst en EPG **rechtstreeks** via OpenWebif
   (`OpenWebifEndpoints`/`OpenWebifJson` uit `shared:core-openwebif`, dezelfde code als de
   TV) en speelt de stream rechtstreeks van de ontvanger met **Media3 ExoPlayer**.
2. Receiverconfig (host, poorten, HTTPS, optionele login) staat in
   `SharedPreferences`; het wachtwoord in `SecureStore` (AES-GCM/Keystore). De
   streamauthenticatie gebruikt dezelfde Basic-header als de TV.
3. IPTV: M3U via de gedeelde `M3uParser`; Xtream via `player_api.php` met
   `get_live_categories`/`get_live_streams`, `get_vod_categories`/`get_vod_streams` en
   `get_series_categories`/`get_series`/`get_series_info` (films en series, inclusief
   afleveringen), met de bestaande `XtreamUrls`/`XtreamJson`.
4. Eén speler voor Live, IPTV en later VOD/series; HLS en MPEG-TS via Media3.
5. LAN-HTTP is toegestaan (`usesCleartextTraffic`), met HTTPS als de ontvanger dat biedt.

## Gevolgen

- **Positief:** de TV hoeft niet aan; geen extra server; dezelfde parsers/URL's als de TV;
  de telefoon is onafhankelijk van de Companion-brug voor video.
- **Negatief:** de telefoon heeft de receivergegevens nodig (eenmalige setup); HTTP is
  cleartext op het LAN; video op fysieke hardware is nog niet getest.
- **Veiligheid:** credentials versleuteld; geen auth-headers in de bibliotheek; de
  Companion-brug blijft de enige remote-control-route.
- **Bewijs:** Live (lijst/EPG/speler), M3U en Xtream op de emulator met een fake
  OpenWebif/Xtream en een audiostream.
