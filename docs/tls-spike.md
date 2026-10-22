# TLS-spike — ontwerp en validatieplan

Status: **gevalideerd op de Android TV-emulator (API 36); optie C gekozen en geïmplementeerd.**
De JVM-bouwstenen zijn groen; de echte TV-brug draait met een Keystore-sleutel.

## 1. Vraag

Kan de TV-app op Android TV (API 28–34, verschillende SoC's) een TLS-server draaien met
een zelfondertekend certificaat, waarvan de telefoon alleen de **SPKI-pin** vertrouwt —
zonder nieuwe zware dependency, zonder cloud en zonder de bestaande app te raken?

## 2. Opties

| Optie | Werking | Voor | Tegen | Grootte |
|---|---|---|---|---|
| **A** | Geen TLS; HTTP met ECDSA-getekende requests | Eenvoudig, geen cert | Inhoud leesbaar op Wi-Fi (wachtwoorden, foto's) | 0 |
| **B** | Zelfondertekend cert uit PKCS#12, gegenereerd met BouncyCastle; wachtwoord in Keystore | Deterministisch, werkt op elk toestel, eenvoudig te roteren | Nieuwe dependency; sleutel staat op schijf (app-sandbox) | ~4 MB vóór R8 |
| **C** | Sleutel in Android Keystore + custom `X509ExtendedKeyManager` | Sleutel niet exporteerbaar | Keystore/TLS-servercombinatie is toestelafhankelijk; meer code | 0 dependency |

**Voorstel:** **B** als primair (voorspelbaar, snel te bouwen), **C** als hardening na de
spike. **A** alleen als noodoplossing, en dan met een duidelijke waarschuwing in de UI.

## 3. Ontwerp B

```text
Eerste start van de brug
  ├─ genereer EC P-256 sleutelpaar (JCA)
  ├─ maak self-signed X.509 (JcaX509v3CertificateBuilder, CN=enigmacast-companion,
  │  SAN=IP:<lan-ip>, geldig 10 jaar)
  ├─ schrijf PKCS#12 naar filesDir/companion/tls.p12 (wachtwoord: 32 random bytes)
  ├─ bewaar het wachtwoord in de bestaande SecretStore (AES-GCM, Keystore)
  └─ pin = base64url(SHA-256(cert.publicKey.encoded))

Per verbinding
  ├─ KeyStore.getInstance("PKCS12").load(file, password)
  ├─ KeyManagerFactory.init(keystore, password)
  ├─ SSLContext.getInstance("TLS").init(keyManagers, null, null)
  └─ NanoHTTPD.makeSecure(context.socketFactory, null) → HTTPS + WSS op één poort
```

- BouncyCastle: `org.bouncycastle:bcpkix-jdk18on:1.80.2` (+ `bcprov`, `bcutil`), al
  aanwezig in de Gradle-cache van deze machine.
- Het PKCS#12-wachtwoord staat nooit in de JSON-instellingen en nooit in een log.
- De pin wordt bij elke start uit het certificaat herberekend; verandert hij, dan sluit de
  TV bestaande koppelingen en toont hij "opnieuw koppelen".
- `SAN=IP:<lan-ip>` is niet nodig voor de pin-controle (de telefoon vervangt
  hostname-verificatie door de pin), maar maakt debuggen met een browser makkelijker.

## 4. Ontwerp C (hardening)

```kotlin
KeyGenParameterSpec.Builder("enigmacast_companion_tls", PURPOSE_SIGN)
    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
    .setDigests(DIGEST_SHA256)
    .setCertificateSubject(X500Principal("CN=enigmacast-companion"))
    .setCertificateSerialNumber(BigInteger.ONE)
    .setCertificateNotBefore(date)
    .setCertificateNotAfter(datePlus10Years)
```

Daarna een `X509ExtendedKeyManager` die `getPrivateKey(alias)` uit de AndroidKeyStore
haalt en `getCertificateChain(alias)` uit het bijbehorende certificaat. Reden: de standaard
`KeyManagerFactory` probeert de sleutel te exporteren en dat lukt niet met een
Keystore-sleutel.

**Risico:** niet elke Android-versie/SoC accepteert dit voor TLS 1.3. Daarom eerst B.

## 5. Testmatrix

| # | Omgeving | Wat |
|---|---|---|
| 1 | Emulator API 28 (x86) | Handshake, pin, HTTPS + WSS tegelijk |
| 2 | Emulator API 30 (TV) | Idem |
| 3 | Emulator API 34 | Idem, TLS 1.3 verplicht proberen |
| 4 | Fysieke box A (AmLogic/OpenATV) | Idem + herstart + cert-rotatie |
| 5 | Fysieke box B (HiSilicon) | Idem |
| 6 | OkHttp-client met pin | Goede pin accepteert, foute pin weigert vóór request |
| 7 | 10 parallelle verbindingen | Geen accept-loop-blokkade |
| 8 | Certificaat verwijderd | TV genereert een nieuwe en vraagt opnieuw koppelen |
| 9 | Klok 10 minuten fout | `AUTH_SKEW`, geen crash |
| 10 | App naar achtergrond | Brug stopt, poort dicht, geen lek |

## 6. Acceptatiecriteria

- Handshake < 300 ms op LAN.
- Zowel HTTPS (`/api/v1/health`) als WSS (`/ws`) op dezelfde poort.
- Een client zonder de juiste pin krijgt **geen** HTTP-antwoord (TLS faalt eerst).
- Certificaatrotatie is een expliciete gebruikersactie met waarschuwing.
- Geen private key of wachtwoord in logs, `state`-events of de settings-JSON.
- Op alle vijf omgevingen geen crash en geen ANR.

## 7. Fallback

Als B of C op een doeltoestel niet werkt:

1. Terug naar **A** (HTTP + signed requests), maar alleen na een expliciete waarschuwing
   dat wachtwoorden en foto's op het LAN leesbaar zijn.
2. Of: de brug alleen aanbieden op toestellen waar de spike slaagt (capability `tls`).

## 8. Wat al vaststaat

### 8.1 JVM-bewijs (21-10-2026, groen)

`tools/tls-spike` bevat drie tests die de bouwstenen van optie B bewijzen:

1. een EC P-256-sleutel en zelfondertekend certificaat genereren, als PKCS#12 opslaan en
   opnieuw laden;
2. een TLS-server opzetten met die sleutel en een client laten verbinden die **alleen de
   SPKI-pin** vertrouwt — de payload komt aan;
3. een verkeerde pin laat de handshake falen (`SSLHandshakeException`) vóór er data gaat;
4. de pin blijft gelijk na het herladen van het PKCS#12-bestand.

Niet gedekt door deze tests (en dus nog te valideren op een toestel): Android Keystore,
`NanoHTTPD.makeSecure`, TLS op oude TV-SoC's, en het gedrag bij app-herstart of
certificaatrotatie.

### 8.2 Afspraken die al vastliggen

- Het poortnummer (8750), het pinformaat (`base64url(SHA-256(SPKI))`) en de
  pin-verificatie zijn identiek aan het bewezen `EnigmaSyncClient` in EnigmaCast.
- De telefoon gebruikt dezelfde `X509TrustManager`-aanpak: cert nemen, SPKI-hashen,
  vergelijken, en `hostnameVerifier { hostname, _ -> hostname == host }`.
- De servercertificaat-rotatie is een beheeractie op de TV, niet iets wat de telefoon
  automatisch accepteert.

## 9. Openstaande punten

- [x] B of C kiezen: **C** (Android Keystore) is geïmplementeerd, zonder nieuwe dependency.
- [x] BouncyCastle: **niet nodig**; de Keystore maakt het certificaat.
- [x] `SSLServerSocket` met een eigen `SSLContext` werkt op de emulator.
- [x] Certificaat in de app-backup uitsluiten (`allowBackup=false` staat al aan).
- [ ] Herhalen op fysieke TV-toestellen (AmLogic/HiSilicon) uit de matrix.

## 10. Resultaat in de TV-brug (21-10-2026)

De eerste handshake faalde met:

```text
java.security.InvalidKeyException: Keystore operation failed
Caused by: android.security.KeyStoreException: Incompatible digest
```

Oorzaak: Conscrypt ondertekent de TLS-handshake met een **raw digest**
(`NONEwithECDSA`), terwijl de sleutel alleen `DIGEST_SHA256` toestond. Oplossing:

```kotlin
.setDigests(
    KeyProperties.DIGEST_NONE,
    KeyProperties.DIGEST_SHA256,
    KeyProperties.DIGEST_SHA384,
    KeyProperties.DIGEST_SHA512,
)
```

Daarna: TLS 1.2/1.3-handshake, SPKI-pin gelijk aan de TV, WebSocket-upgrade 101 en een
`cmd.key` met ack in 2 ms. De sleutel is geroteerd naar alias
`enigmacast_companion_tls_v2`, zodat toestellen met de oude sleutel opnieuw koppelen.

**Conclusie:** optie C is de productiekeuze; de PKCS#12-route (B) is niet nodig gebleken.
Alleen de fysieke toestellen uit de matrix staan nog open.
