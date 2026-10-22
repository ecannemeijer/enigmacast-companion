# ADR 0016 — Meerveldsformulieren

- **Status:** aangenomen en geïmplementeerd
- **Datum:** 2026-10-22

## Context

De Companion kon één `TvTextField` tegelijk overnemen. De ontvanger-setup vraagt echter
om adres, gebruikersnaam en wachtwoord; die in één sessie invoeren is veel handiger dan
drie losse velden.

## Beslissing

1. `CompanionFormHost.openForm(title, fields)` opent een sessie met meerdere
   `FieldSpec`s; `open(...)` blijft de single-field variant voor `TvTextField`.
2. `CompanionFormState` bevat `fields: List<CompanionFormField>`; `formJson` stuurt alle
   velden met `id`, `label`, `type`, `masked` en (niet-sensitive) `value`.
3. `cmd.form` verwerkt een waardemap: `TEXT` werkt alleen de niet-sensitive velden bij,
   `SUBMIT` past alle waarden toe, `CANCEL` sluit zonder wijzigingen. Een leeg wachtwoord
   betekent "niet gewijzigd".
4. De ontvanger-setup heeft **"Vul via telefoon"** (zowel de wizard als **Ontvanger
   bewerken**), dat host/username/password opent en de waarden bij submit in de
   `ConnectionViewModel` zet.
5. De telefoon-UI (`FormSheet`) rendert de velden al generiek; geen nieuw protocoltype.

## Gevolgen

- **Positief:** één telefoonformulier voor de hele login; dezelfde pijplijn als
  `TvTextField`, zonder tweede protocol.
- **Negatief:** de setup-pagina was op de emulator niet bereikbaar, dus de E2E is nog open;
  de code is gecompileerd en de bestaande single-field-tests blijven groen.
- **Veiligheid:** wachtwoorden gaan niet mee in de descriptor of `TEXT`; alleen bij
  `SUBMIT` en nooit in logs.
- **Bewijs:** de host/DTO/telefoon zijn gecompileerd; de TV- en Companion-suites zijn groen.
