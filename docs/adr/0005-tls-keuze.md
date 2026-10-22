# ADR 0005 — TLS: Android Keystore (optie C)

- **Status:** aangenomen en geïmplementeerd (21-10-2026)
- **Datum:** 2026-10-21

## Context

De brug moet HTTPS én WSS op één poort aanbieden, met een zelfondertekend certificaat dat
de telefoon via SPKI-pinning vertrouwt. Drie routes waren mogelijk (zie `docs/tls-spike.md`):

- **A** geen TLS, alleen getekende requests;
- **B** een PKCS#12-certificaat uit BouncyCastle;
- **C** een niet-exporteerbare sleutel in de Android Keystore.

## Beslissing

**Optie C.** De Keystore genereert de EC P-256-sleutel én het zelfondertekende certificaat
(`setCertificateSubject`, `setCertificateSerialNumber`, `setCertificateNotBefore/After`).
Een eigen `X509ExtendedKeyManager` levert de sleutel en de certificate chain aan
`SSLContext`. De telefoon vertrouwt alleen `base64url(SHA-256(SPKI))`.

De sleutel staat `DIGEST_NONE`, `SHA-256`, `SHA-384` en `SHA-512` toe. Zonder
`DIGEST_NONE` faalt de handshake met `KeyStoreException: Incompatible digest`, omdat
Conscrypt de `CertificateVerify` met een raw digest ondertekent.

## Waarom niet B

B werkt, maar vraagt een BouncyCastle-dependency (~4 MB) en zet een exporteerbare sleutel
op schijf. C is kleiner, veiliger en bleek op de API 36-emulator direct te werken. De
eerdere zorg dat `KeyManagerFactory` de Keystore-sleutel niet kan gebruiken, is opgelost
met een eigen `X509ExtendedKeyManager` in plaats van de standaardfactory.

## Gevolgen

- **Positief:** geen nieuwe dependency, sleutel niet exporteerbaar, certificaatrotatie is
  een nieuwe alias.
- **Negatief:** de TLS-servercombinatie blijft toestelafhankelijk; daarom staat de
  testmatrix in `docs/tls-spike.md` nog open voor fysieke boxen.
- **Rotatie:** de alias is `enigmacast_companion_tls_v2`; een nieuwe alias betekent dat
  gekoppelde telefoons opnieuw moeten koppelen (de pin verandert).
- **Fallback:** als een toestel geen TLS-server ondersteunt, is A (HTTP + getekende
  requests) de gedocumenteerde noodoplossing; dat is nu niet geactiveerd.

## Bewijs

Zie `docs/tls-spike.md` §10: handshake, pin, WebSocket 101 en een `cmd.key`-ack in 2 ms
tegen de echte TV-brug op de emulator.
