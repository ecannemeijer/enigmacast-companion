# ADR 0008 — TV-brug als geïsoleerd package in `:app`, met raw TLS/WS

- **Status:** aangenomen en geïmplementeerd (fase 2)
- **Datum:** 2026-10-21

## Context

De Companion moet de TV-app kunnen bedienen. Dat vereist code in het EnigmaCast-proces,
maar de TV-app mag functioneel niet veranderen en de build moet offline blijven werken.
Een losse `:companion-bridge`-module was het plan, maar de plugin-marker
`com.android.library` staat niet in de offline Gradle-cache; `nanohttpd-websocket` ook niet.

## Beslissing

1. **Package in `:app`** (`com.enigmacast.companionbridge`) in plaats van een Gradle-module.
   Isolatie via package + de BuildConfig-flag `COMPANION_BRIDGE`. Verwijderen is het package
   wissen en vier regels terugdraaien.
2. **Geen nieuwe dependency.** TLS via de Android Keystore (ADR 0005), HTTP en WebSocket
   met raw `SSLServerSocket` en een kleine RFC 6455-implementatie.
3. **Alleen op de voorgrond** (ADR 0006): start in `onStart`, stop 30 s na `onStop`.
4. **Toetsen als echte `KeyEvent`s** naar `Activity.dispatchKeyEvent`, zodat de bestaande
   focus-, herhaal- en long-presslogica ongewijzigd blijft. Alleen `HOME` en `NAV` gaan via
   de hooks, omdat een systeem-HOME niet is toegestaan.
5. **Twee sturingsvlakken**: de brug bestuurt de app; OpenWebif blijft voor de ontvanger.
6. **De telefoon gebruikt nu de echte client** (`WebSocketConnection`, `PairingClient`,
   `NsdDiscovery`); `MockBridge` blijft voor de demo. Het protocol is aan beide kanten
   bewezen: JVM-rooktest en een koppeling tussen twee emulators.

## Alternatieven

| Alternatief | Waarom niet |
|---|---|
| Losse Android-library-module | Plugin-marker niet offline beschikbaar; buildrisico |
| NanoHTTPD + nanohttpd-websocket | Websocket-artifact niet in de cache |
| Ktor/Netty-server | Zware dependency, groot in de TV-app |
| Accessibility Service of globale key-injectie | Vereist rechten die de app niet heeft en is een play-store-risico |

## Gevolgen

- **Positief:** geen dependency, geen manifestwijziging, geen service; de TV-app blijft
  functioneel gelijk en de brug is met één flag uit te zetten.
- **Negatief:** de brug deelt het `:app`-proces en de build; een fout in de brug kan de TV-app
  raken. Daarom: weinig code, eigen coroutine-scope, try/catch rond elke verbinding, en
  unittests voor de protocolbouwstenen.
- **Onderhoud:** de protocol-DTO's staan nu twee keer (TV en Companion). De contracttests
  en de gouden bestanden in de Companion-repo bewaken de veldnamen; een gedeelde module
  kan later alsnog.
- **Let op:** de brug start automatisch als de gebruiker hem één keer heeft aangezet
  (persistente voorkeur), maar alleen op de voorgrond.
