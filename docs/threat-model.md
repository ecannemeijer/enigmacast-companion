# Dreigingsmodel — Companion-koppeling

Versie 1, 21-10-2026. Hoort bij `docs/protocol.md`.

## 1. Scope

Binnen scope: de verbinding tussen de Companion-app en de EnigmaCast TV-app op hetzelfde
lokale netwerk, inclusief koppelen, bediening, tekstinvoer, instellingen, profielen en
avatar-upload. Buiten scope: de bestaande koppeling tussen EnigmaCast en de Enigma2-ontvanger
(OpenWebif), de accountserver (`api.velixatv.com`), en de torrentfunctie.

## 2. Assets

| Asset | Waar | Impact bij compromise |
|---|---|---|
| Bediening van de TV-app | brug | Iemand anders zapt, opent instellingen of start een opname |
| Wachtwoorden van de ontvanger | TV (Keystore) / formulier | Volledige toegang tot OpenWebif en de streamserver |
| IPTV-login (M3U/Xtream) | TV (Keystore) / syncserver | Kosten, providerblokkade, privacyschade |
| Profielfoto's | TV-bestand | Privacyschade (gezichten) |
| Kijkgedrag/voortgang | TV + syncserver | Privacyschade |
| Koppeltokens | telefoon (Keystore), TV (hash) | Bediening tot het token wordt ingetrokken |
| TLS-serversleutel | TV (PKCS#12 of Keystore) | Actieve MITM bij nieuwe koppelingen |

## 3. Vertrouwensgrenzen

```text
[telefoon] ──TLS+pin──► [TV-brug] ──OpenWebif (HTTP/HTTPS)──► [Enigma2]
     │                       │
     │                       └── Keystore (secrets), DataStore (instellingen)
     └── Keystore (token, apparaatsleutel)
```

Het LAN wordt **niet** vertrouwd: elk toestel op hetzelfde netwerk kan meeluisteren of
verbindingen proberen. De cloud wordt niet gebruikt voor de koppeling.

## 4. Aanvallers en maatregelen

| # | Aanvaller | Scenario | Maatregel |
|---|---|---|---|
| T1 | Passieve luisteraar | Wi-Fi-sniffer leest verkeer | TLS 1.2/1.3; geen plaintext-fallback voor de brug |
| T2 | Actieve MITM | Valse TV met eigen certificaat | SPKI-pinning; QR bevat de pin (out-of-band); pincode-pad vraagt bevestiging op de TV |
| T3 | Willekeurig LAN-toestel | Poort 8750 open, probeert commando's | Bearer-token + ECDSA-handtekening per request; nonce/timestamp; geen anonieme commando's |
| T4 | Replay | Opgenomen request opnieuw sturen | Nonce-tabel + 120 s klokvenster |
| T5 | Gestolen telefoon | Token in Keystore | Token intrekken op de TV; 90 dagen TTL; toestellijst met laatste gebruik |
| T6 | Kwaadaardige webpagina (DNS-rebinding) | Browser op het LAN bezoekt `https://192.168.1.50:8750` | API eist custom headers en een handtekening die een browser niet kan maken; geen CORS; `Content-Type: application/json` verplicht bij POST |
| T7 | Brute force op de pincode | 6 cijfers = 1 miljoen | 5 pogingen per 5 min per IP; 120 s TTL; eenmalig; mislukte pogingen loggen zonder code |
| T8 | Kwaadwillige medegebruiker | Kent de TV en het netwerk | Koppelen vereist toegang tot het TV-scherm (QR/code) of bevestiging op de TV |
| T9 | Malware op de telefoon | Leest app-data | Token en apparaatsleutel in Android Keystore (niet exporteerbaar); geen tokens in logs |
| T10 | Malware op de TV | Leest app-data | Buiten scope (het toestel is gecompromitteerd); wel: server-sleutel waar mogelijk in Keystore |
| T11 | Verloren/gelekte logs | Wachtwoorden of tokens in logcat | Geen secrets loggen; `sanitizeUrl`; formulierwaarden nooit loggen; test hierop |
| T12 | Screenshot/recents | Wachtwoord zichtbaar in de app-switcher | `FLAG_SECURE` op wachtwoordformulieren (fase 4) |

## 5. Koppelen: MITM-analyse

**QR-pad (sterk).** De QR bevat host, poort, TV-id, codenummer én de SPKI-pin. De telefoon
pint het certificaat vóór het de code verstuurt. Een MITM met een eigen certificaat wordt
geweigerd vóór er iets geheims over de lijn gaat.

**Pincode-pad (zwakker).** De telefoon leert de pin uit de mDNS-TXT of van het eerste
certificaat dat hij ziet. Een actieve MITM kan zich voordoen als de TV en de code
doorgeven. Maatregelen:

1. De TV toont na ontvangst van een koppelverzoek: *"Pixel 8 wil koppelen — Toestaan /
   Weigeren"* (30 s). Zonder toestemming op het TV-scherm geen token. Dit is de
   belangrijkste maatregel: de aanvaller ziet dat verzoek niet op de TV van het slachtoffer.
2. Korte TTL (120 s), eenmalige code, 5 pogingen.
3. Na het koppelen toont de TV de pin in de toestellenlijst; de telefoon toont dezelfde pin.
   De gebruiker kan vergelijken (TOFU met handmatige verificatie).
4. De telefoon waarschuwt als de pin bij een latere verbinding verandert.

**Waarom geen PAKE?** Een PAKE (bijv. SPAKE2) zou het pincode-pad sterker maken, maar
vereist extra crypto in beide apps en is voor een LAN-koppeling met TV-bevestiging niet
nodig. Wel genoteerd als toekomstige verbetering.

## 6. TLS

| Aspect | Keuze |
|---|---|
| Certificaat | Zelfondertekend, ECDSA P-256 (voorkeur) of RSA-2048, CN `enigmacast-companion` |
| Vertrouwen | Alleen SPKI-pin; hostname-verificatie vervangen door `hostname == pinned host` |
| Protocollen | TLS 1.2 en 1.3; geen SSLv3/TLS 1.0/1.1 |
| Ciphers | ECDHE-ECDSA/RSA met AES-GCM of CHACHA20 |
| Rotatie | Nieuw cert → alle telefoons opnieuw koppelen; de TV waarschuwt eerst |
| Pin tonen | QR, TV-scherm, telefooninstellingen |
| Geen pin | Verbinding weigeren (zoals `EnigmaSyncClient` al doet) |

Zie `docs/tls-spike.md` voor de implementatiekeuze en validatie.

## 7. Sleutel- en tokenbeheer

| Item | Opslag | Bescherming |
|---|---|---|
| Telefoon-apparaatsleutel | Android Keystore, alias `enigmacast_companion_device` | Niet exporteerbaar, alleen signeren |
| Token | Telefoon: Keystore-encrypted store; TV: `SHA-256(token)` | Token nooit in logs of backup |
| TV-server sleutel | PKCS#12 in app-sandbox, wachtwoord in Keystore-store (optie B) of Keystore-key (optie C) | Niet in backup (`allowBackup=false`) |
| Herstelcode | TV toont eenmalig | Na gebruik vernieuwd |
| Koppellijst | TV (`devices.json`) | Alleen via TV-scherm of admin-telefoon |

**Intrekken:** `POST /api/v1/devices/{id}/revoke` (admin). De TV sluit de WebSocket van dat
toestel en weigert het token. **Noodgeval:** "Alle toestellen vergeten" op de TV, daarna
opnieuw koppelen.

## 8. Geheimen in de app

- Wachtwoorden gaan alleen via een formulier (één keer) en worden door de TV direct in de
  bestaande `SecretStore` gezet (AES-256-GCM, Keystore).
- De descriptor bevat voor een `sensitive` veld nooit een waarde.
- `state.*`-events bevatten nooit wachtwoorden, tokens of volledige URL's met credentials.
- Logregels gebruiken `sanitizeUrl` en noemen alleen pad, status en duur.
- De Settings-PIN en profiel-PIN worden nooit meegestuurd in `state`; alleen `hasPin`.

## 9. Privacy

- Profielfoto's worden op de TV **hercodeerd**, waardoor EXIF/GPS verdwijnt.
- De telefoon bewaart de foto niet buiten de app-cache; na upload wordt het tijdelijke
  bestand verwijderd.
- Kijkgedrag verlaat het LAN niet; de enige cloudkoppeling is de bestaande accountserver
  (los van de Companion-koppeling).

## 10. Restrisico's

| Risico | Restrisico | Acceptatie |
|---|---|---|
| Pincode-MITM | Aanvaller die het TV-scherm ziet of de gebruiker misleidt | Geaccepteerd; QR is de aanbevolen route; TV-bevestiging blijft |
| Compromitteerde TV | Alle secrets op de TV | Buiten scope; TV is het vertrouwde toestel |
| Verouderde TLS-stack op oude TV-boxen | Alleen TLS 1.2 | Geaccepteerd; testen in de spike |
| Gebruiker koppelt een verkeerd toestel | Bediening door derde | Intrekken + toestellijst met namen en laatste gebruik |
| LAN met kwaadwillige DNS | Verkeerde host voor mDNS-naam | Pin in QR/TXT en waarschuwing bij pinwijziging |

## 11. Verificatie-checklist (fase 2 en 8)

- [ ] Zonder token: elk endpoint weigert (behalve `/health` en `/pair`).
- [ ] Verkeerde pin: handshake faalt vóór er een request wordt gedaan.
- [ ] Replay van een geldig request: `AUTH_REPLAY`.
- [ ] Timestamp 3 minuten oud: `AUTH_SKEW`.
- [ ] Ingetrokken token: `DEVICE_REVOKED` en WS gesloten.
- [ ] 6 foute codes: `PAIR_RATE_LIMITED`.
- [ ] `logcat` na een volledige koppeling + instellingenwijziging: geen wachtwoord, token
      of volledige URL met credentials.
- [ ] Foto met EXIF-GPS: na upload bevat het TV-bestand geen EXIF.
- [ ] Browser op het LAN kan geen commando uitvoeren (geen handtekening).

## 12. Implementatiestatus (21-10-2026)

Geïmplementeerd in de TV-brug:

| Maatregel | Status |
|---|---|
| TLS 1.2/1.3 met SPKI-pinning | ja, Keystore-sleutel (ADR 0005) |
| Eenmalige code, 120 s, 5 pogingen/5 min | ja |
| ECDSA-getekende requests + nonce + 120 s klokvenster | ja |
| Tokenhash in de registry, intrekken per toestel | ja |
| Geen secrets in logs/state | ja (code en token worden niet gelogd) |
| Alleen op de voorgrond | ja (ADR 0006) |
| Bevestiging op het TV-scherm bij pincode-koppeling | **nee**, de code staat alleen op de TV; QR/bevestiging volgt |
| Instellingen-PIN / profiel-PIN bij remote wijzigingen | nog niet van toepassing (fase 3/4) |
| Foto's/EXIF-strip | nog niet (fase 4) |

Aan de telefoonkant (fase 2, geïmplementeerd):

| Maatregel | Status |
|---|---|
| Private apparaatsleutel in de Android Keystore | ja |
| Koppeltoken AES-GCM versleuteld (`SecureStore`) | ja |
| SPKI-pinning; handmatig adres = TOFU, daarna vastgehouden | ja |
| Pin-fout is fataal; netwerkreset geeft reconnect met backoff | ja |
| Geen token/pin in logs | ja |
| Formuliertekst alleen over de versleutelde WSS-verbinding | ja |
| Wachtwoordvelden (`masked`/`sensitive`) nooit in een descriptor of log | ja |
| Formuliersessie alleen voor het geauthenticeerde, gekoppelde toestel | ja |
| Instellingen alleen via de getekende WSS van een gekoppeld toestel | ja |
| 24 gebruikersinstellingen; gevorderde paden vragen de Settings-PIN | ja (TV controleert) |
| Settings-PIN met pogingenteller/cooldown; geen PIN in logs | ja |
| Niet-gevoelige instellingen blijven zonder PIN beschikbaar | beperking |
| Fotoupload: getekend, max. 10 MB, magic bytes/afmetingen, EXIF/GPS gestript | ja |
| Foto's alleen per profiel op de TV; geen `GET`/`DELETE` van buiten | ja (nu alleen PUT) |
| Profiel-PIN voor kiezen, hernoemen, avatar en verwijderen | ja |
| Foto-upload naar een PIN-profiel geblokkeerd | ja |
| IPTV: M3U/Xtream, films en series | ja |
| Settings-PIN end-to-end op TV-emulator getest | nee (code + unit-suite) |
| Gedeelde IPTV-bibliotheek alleen via de getekende WSS; actief TV-profiel | ja |
| Foto-`GET`/`DELETE` alleen voor een gekoppeld, getekend toestel | ja |
| Een kapot/onbekend state-bericht laat de app niet crashen | ja (event-guard) |
| Receiver- en Xtream-wachtwoorden in de Android Keystore (AES-GCM) | ja |
| Live/IPTV praat rechtstreeks met ontvanger/provider, niet via de TV | ja (bewuste keuze) |
| LAN-HTTP toegestaan; HTTPS waar de ontvanger/provider dat biedt | ja (risico op LAN) |
| Bibliotheek-JSON versleuteld; IPTV-URL's kunnen inloggegevens bevatten | ja (SecureStore) |
| Geen auth-headers in de bibliotheek bewaard | ja (beperking) |

Bewezen met de rooktest in `docs/tv-bridge.md`: verkeerde pin faalt, goede pin + code geven
een token, de WS-upgrade is 101 en een toets krijgt een ack. De registry staat in
`filesDir/companion/devices.json` en bevat alleen publieke sleutels en tokenhashes.
