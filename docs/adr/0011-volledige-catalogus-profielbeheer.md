# ADR 0011 — Volledige instellingencatalogus en profielbeheer

- **Status:** aangenomen en geïmplementeerd (fase 4.1)
- **Datum:** 2026-10-21

## Context

Na de eerste fase 4 had de telefoon zes instellingen en kon hij alleen foto's uploaden.
De wens was de **volledige gebruikersinstellingen** en **profielbeheer**: aanmaken,
hernoemen, avatar, verwijderen en foto's wissen, met dezelfde regels als de TV.

## Beslissing

1. **Catalogus van 24 gebruikersinstellingen** in vijf categorieën (Weergave, Afspelen,
   Opstarten, Bediening, Debug). Elke entry heeft `category`, `type`, `value`, opties,
   `min`/`max`/`step`, en de markeringen `dangerous`/`requiresRestart`.
2. **Dezelfde schrijfroute**: `SettingsRepository.update`; de TV-UI en de telefoon zien
   dezelfde waarde. Een verouderde `baseRevision` geeft `SETTINGS_CONFLICT`.
3. **Settings-PIN**: als de TV een PIN heeft, filtert de brug de `dangerous`-entries uit
   de catalogus en weigert hij die paden server-side (`SETTINGS_LOCKED`). De telefoon vraagt
   nog geen PIN; de veilige instellingen blijven beschikbaar.
4. **Profiel-CRUD** via `cmd.profile`: `CREATE`, `RENAME`, `SET_AVATAR`, `DELETE`, met
   `clearPhoto`. Maximaal 6 profielen, 16 avatars, naam 1–20 tekens; het actieve/laatste
   profiel kan niet weg.
5. **Profiel-PIN**: `SELECT` (niet-actief), `RENAME`, `SET_AVATAR` en `DELETE` vragen de
   pincode; de telefoon toont een PIN-dialoog. Foto-upload naar een PIN-profiel is
   geblokkeerd; de avatar/foto wijzigen gaat via `SET_AVATAR` met PIN.
6. **Opruimen**: `DELETE` gebruikt `ProfileCleanup.purge` (geschiedenis, favorieten, foto,
   PIN), zodat er geen gegevens achterblijven.
7. **Telefoon-UI**: gegroepeerde catalogus, bevestiging voor gevaren, avatar-raster,
   verwijderbevestiging en PIN-dialoog.

## Gevolgen

- **Positief:** de telefoon dekt nu de gebruikersinstellingen en profielbeheer zonder
  nieuwe server of dependency; dezelfde opslag en opruiming als de TV.
- **Negatief:** de Settings-PIN vraagt nog geen pincode in de telefoon (gevorderde
  instellingen worden verborgen); `GET`/`DELETE` van foto's ontbreken; de labels komen uit
  de TV-taal.
- **Beveiliging:** alleen een gekoppeld, getekend toestel kan wijzigen; gevarenpaden zijn
  server-side beschermd; profiel-PIN en foto-blokkade zijn aan de TV-kant afgedwongen.
- **Bewijs:** profiel aanmaken → hernoemen → avatar → verwijderen, foto wissen en de
  gevarenbevestiging, op twee emulators.
