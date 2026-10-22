# ADR 0013 — Lokale bibliotheek voor verder kijken en favorieten

- **Status:** aangenomen en geïmplementeerd (fase 7)
- **Datum:** 2026-10-21

## Context

De bibliotheek moest "verder kijken" en favorieten tonen. De TV bewaart zijn geschiedenis
per profiel in Room; de telefoon heeft geen toegang tot die database en de Companion-brug
stuurt alleen profielen/instellingen. Een cloud-sync is een groter project.

## Beslissing

1. `LibraryStore` bewaart per stream-URL: titel, positie, duur, bron, favoriet en tijdstip
   (max. 100, JSON in `SecureStore`/AES-GCM, omdat IPTV-URL's inloggegevens kunnen
   bevatten).
2. De speler schrijft elke 5 s en bij sluiten de positie; de ster in de speler zet een
   favoriet.
3. "Verder kijken" toont items met een positie tussen 5 s en het einde; daarnaast
   favorieten en recente streams.
4. Profielen en instellingen lopen via de Companion-brug; de bibliotheek is **lokaal**.
   De "Sync met de ontvanger"-knop stuurt `cmd.sync` voor de TV/ontvanger.

## Gevolgen

- **Positief:** direct bruikbaar, geen account/cloud, geen nieuwe server; dezelfde
  speler voor live en IPTV.
- **Negatief:** live/Enigma2-geschiedenis deelt niet automatisch met de TV of een tweede
  telefoon; auth-headers worden niet bewaard, dus een met auth beveiligde stream kan na
  herstart niet hervatten.
- **Bijgewerkt (fase 7.1):** IPTV-favorieten en -posities worden nu via de brug gedeeld
  (ADR 0015); live/Enigma2 blijft lokaal.
