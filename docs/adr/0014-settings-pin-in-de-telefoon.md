# ADR 0014 — Settings-PIN in de telefoon

- **Status:** aangenomen en geïmplementeerd
- **Datum:** 2026-10-22

## Context

De TV kan zijn instellingen met een 4-cijferige PIN vergrendelen. De Companion-brug
verstopte eerder alle gevorderde instellingen als er een PIN was; daarmee kon de telefoon
ze niet wijzigen, maar was er ook geen echte PIN-flow.

## Beslissing

1. `SettingsCatalogPayload` krijgt `locked: Boolean`; de brug stuurt bij een PIN de
   **volledige** catalogus met `locked: true` in plaats van de gevorderde entries te
   verbergen.
2. `SettingsCommand` krijgt een optionele `pin`. Bij een gevorderd pad en een
   vergrendelde TV controleert `CompanionSettingsHost` de PIN met
   `SettingsLock.unlock(pin)`. Een lege/onjuiste PIN geeft `SETTINGS_LOCKED`; een juiste
   PIN ontgrendelt en de catalogus gaat naar `locked: false`.
3. De telefoon toont een vergrendelingsbanner en vraagt bij een gevorderde wijziging om de
   pincode; niet-gevoelige instellingen blijven zonder PIN.
4. De PIN wordt nooit gelogd en gaat alleen over de getekende WSS.

## Gevolgen

- **Positief:** de telefoon kan dezelfde gevorderde instellingen als de TV, met dezelfde
  PIN en pogingenteller; geen verborgen functionaliteit meer.
- **Negatief:** de TV-PIN-flow zelf is op de emulator niet end-to-end getest (TV-navigatie
  naar de lockpagina was te beperkt); de brugcode is gecompileerd en de unit-suite slaagt.
- **Veiligheid:** server-side afgedwongen; de telefoon kan de PIN niet omzeilen.
- **Vervolg:** een echte TV-PIN-E2E op hardware.
