# ADR 0006 — Brug alleen actief zolang de TV-app op de voorgrond is

- **Status:** aangenomen en geïmplementeerd (fase 2)
- **Datum:** 2026-10-21

## Context

De TV-app stopt bij `onStop` alle weergave (`AppViewModel.stopForBackground`) en heeft één
activity (`singleTask`). Android beperkt background-activity-starts en achtergronddiensten.
Een brug die altijd bereikbaar is, vraagt een foreground service, een bootreceiver en
extra permissies — en vergroot het aanvalsoppervlak.

## Beslissing

- **Standaard (v1): ForegroundOnly.** De brug start in `MainActivity.onStart`, stopt 30 s
  na `onStop` (grace voor korte app-wissels) en direct bij `onDestroy`.
- **Later (optioneel): AlwaysOn.** Een foreground service van het type `connectedDevice`
  met melding, alleen voor ontvangeracties; app-commando's blijven geweigerd met
  `APP_NOT_FOREGROUND`.
- Als de TV-app niet actief is, kan de telefoon altijd nog de **ontvanger** bedienen via
  OpenWebif en zelf streamen.

## Gevolgen

- **Positief:** geen nieuwe permissies, geen bootgedrag, geen batterijgebruik, en de
  TV-app blijft functioneel identiek.
- **Negatief:** "TV-app bedienen" kan alleen als EnigmaCast zichtbaar is. Dat is ook het
  enige moment waarop de gebruiker het resultaat ziet.
- **UI:** de telefoon toont duidelijk "TV-app niet actief — open EnigmaCast op de TV" en
  biedt de ontvangerbesturing aan.
