# ADR 0004 — Protocol v1: enums in uppercase, envelope met JsonElement, getekende HTTP-requests

- **Status:** aangenomen (fase 1)
- **Datum:** 2026-10-21

## Context

De TV-brug en de telefoon moeten hetzelfde protocol implementeren, in twee repositories.
Elke afwijking in veldnamen, enumwaarden of de handtekeningstring is een interoperability-bug
die pas op een echt toestel zichtbaar wordt.

## Beslissing

1. **Enums uppercase** op de draad (`DPAD_DOWN`, `GLASS_EARTH`, `PASSWORD`). Vrije velden
   (`source`, `route`, `status`, `mode`, `screen.app`) blijven lowercase strings.
2. **Envelope met `payload: JsonElement`** in plaats van een gesloten polymorphische
   hiërarchie. Voordeel: onbekende berichttypen kunnen worden genegeerd zonder de hele
   verbinding te breken; typedecodering gebeurt per bericht.
3. **Getekende requests** volgens het bestaande EnigmaCast-schema:
   `timestamp \n nonce \n METHOD \n path?query \n SHA-256(body)` (32 ruwe bytes), ECDSA
   P-256, nonce-tabel, 120 s klokvenster.
4. **Gouden bestanden** in `protocol/v1` en een contracttest die heen en terug decodeert.
5. Wijzigingen die oude clients breken verhogen `Protocol.VERSION` en krijgen
   `protocol/v2`; de TV ondersteunt tijdelijk beide.

## Alternatieven

| Alternatief | Waarom niet |
|---|---|
| `kotlinx.serialization` polymorphisch met `@SerialName` per type | Elke onbekende subtype laat de decode klappen |
| Protobuf/FlatBuffers | Moeilijker te debuggingen, geen voordeel op LAN |
| Handtekening over de JSON-string | Whitespace/ordening verschillen tussen talen; nu alleen over de bytes die echt verstuurd worden |

## Gevolgen

- **Positief:** één test bewijst dat de DTO's en het contract niet uiteenlopen.
- **Negatief:** `payload` is niet statisch getypeerd in de envelope; de typedecodering
  moet per bericht worden aangeroepen (`ProtocolJson.decodePayload<T>()`).
- **Let op:** de query maakt deel uit van het ondertekende pad; verander de volgorde niet
  tussen client en server.
