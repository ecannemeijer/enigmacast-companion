# ADR 0002 — Eén `core-model`-module in fase 0

- **Status:** aangenomen (fase 0), te herzien zodra een module te groot wordt
- **Datum:** 2026-10-21

## Context

Het plan noemt aparte modules voor model, OpenWebif, IPTV, EPG en sync. De werkelijkheid
is dat het domein onderling verweven is: `domain/sync` gebruikt de IPTV-entiteiten,
`domain/resume` gebruikt een TMDB-type uit `domain/torrent`, en `domain/series` gebruikt
`TmdbJson`. Een fijne splitsing in fase 0 kost veel tijd en levert nog geen voordeel.

## Beslissing

- Fase 0 levert drie modules: `core-model`, `core-openwebif`, `core-protocol`.
- `core-model` bevat het volledige domein (133 bestanden) plus de gebruikersentiteiten,
  `TmdbJson` en de JVM-shims.
- Splitsen gebeurt pas wanneer een module aantoonbaar te groot wordt of een client maar
  een deel nodig heeft (bijvoorbeeld een toekomstige `core-iptv` voor de telefoon).

## Gevolgen

- **Positief:** snel, weinig risico, alle 581 domeintests draaien mee.
- **Negatief:** `core-model` is groter dan ideaal en bevat code die de Companion niet
  gebruikt (soccer, torrent-domein).
- **Mitigatie:** de module heeft geen Android-afhankelijkheden en kan later gesplitst
  worden zonder de packagenamen te wijzigen.
