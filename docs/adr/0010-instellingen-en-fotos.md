# ADR 0010 — TV-instellingen en profielfoto's via de bestaande brug

- **Status:** aangenomen en geïmplementeerd (fase 4)
- **Datum:** 2026-10-21

## Context

Fase 4 moest instellingen op afstand en avatars mogelijk maken. Het plan voorzag een
volledige instellingencatalogus via HTTP (`GET /api/v1/catalog/settings`) en foto's via
`PUT /api/v1/profiles/{uuid}/photo`. De TV heeft al een `SettingsRepository` (één JSON in
DataStore, met dezelfde schrijfroute als de UI) en een bewezen `AvatarPhotos`-pijplijn
(magic bytes, EXIF-rotatie, vierkante crop, WebP 512).

## Beslissing

1. **Instellingen over de bestaande WebSocket**, niet via een nieuwe HTTP-catalogus:
   `state.settings` met `SettingsCatalogPayload` (revisie + entries) en `cmd.settings` met
   `SettingsPatchOp`s. De TV schrijft met `SettingsRepository.update`.
2. **Eerste catalogus is een subset van zes niet-gevoelige paden**: `osdSkin`,
   `motionQuality`, `navigationSounds`, `osd.transparency`, `playback.subtitlesEnabled`,
   `osd.debugBorders`. De telefoon rendert `BOOL`, `CHOICE` en `NUMBER` generiek.
3. **Revisieconflict**: een verouderde `baseRevision` geeft `SETTINGS_CONFLICT`; de
   telefoon krijgt daarna de nieuwe catalogus.
4. **Foto's via dezelfde brug**: een getekende `PUT /api/v1/profiles/{id}/photo` met de
   bestaande auth (ECDSA, timestamp, nonce, body-hash), max. 10 MB. De TV hergebruikt
   `AvatarPhotos`, werkt `ProfileStore.photo` bij en stuurt `state.profiles`.
5. **Telefoon**: "Foto kiezen" via de systeemfotokiezer en "Testfoto" (gegenereerde
   EC-afbeelding) voor diagnose; de foto zelf blijft op de TV, de telefoon toont "Eigen foto".
6. **Robuustheid**: de telefoon vangt fouten in de eventverwerking af, zodat een nieuw of
   kapot `state`-bericht de app niet meer laat crashen (dit gebeurde bij de eerste
   `state.profiles`-uitzending zonder `profile`-wrapper).

## Waarom niet de volledige catalogus

De TV heeft honderden instellingen, waarvan sommige een herstart, de speler of de
Settings-PIN vereisen. Een volledige catalogus vraagt per instelling een type, validatie,
scope en risico-inschatting. De zes gekozen paden zijn zichtbaar, veilig en dezelfde die de
TV-UI al kent; de rest volgt met dezelfde vorm.

## Gevolgen

- **Positief:** geen nieuwe server of dependency; dezelfde schrijf- en fotopijplijn als de
  TV; de telefoon hoeft niets over de TV te weten behalve de catalogus.
- **Negatief:** de labels komen uit de TV-strings (taal van de TV, niet van de telefoon);
  de Settings-PIN wordt voor deze zes niet gevraagd; `GET`/`DELETE` van foto's en
  profielbeheer ontbreken nog.
- **Beveiliging:** alleen een gekoppeld, getekend toestel kan wijzigen/uploaden; foto's
  worden gestript en per profiel opgeslagen; er is geen nieuwe anonieme route.
- **Bewijs:** TV-instelling aan/uit en de EC-testfoto op Enrico, op twee emulators.
