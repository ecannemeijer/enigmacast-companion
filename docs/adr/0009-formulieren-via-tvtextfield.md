# ADR 0009 — Formulieren via één haak in `TvTextField`

- **Status:** aangenomen en geïmplementeerd (fase 3)
- **Datum:** 2026-10-21

## Context

De TV-app heeft tientallen tekstvelden (`TvTextField`) op verschillende schermen. De
Companion moet daar kunnen typen zonder elk scherm apart aan te passen en zonder de
bestaande remote- en toetsenbordlogica te breken. Het protocol heeft al een
`FormDescriptor`/`FormSubmitCommand`; fase 3 moest dat echt laten werken.

## Beslissing

1. **Eén haak in `TvTextField`**: bij `editing = true` opent het veld een sessie bij
   `CompanionFormHost`; bij stoppen sluit het die. Elk tekstveld in de app doet daarmee
   automatisch mee, zonder schermaanpassingen.
2. **Sessie per veld**: de TV stuurt één veld (`id = value`) met label, type, waarde en
   `masked`. De DTO en de telefoon-UI ondersteunen meerdere velden; de TV opent er nu één.
3. **Live tekst**: `cmd.form` heeft een `action` (`TEXT`, `SUBMIT`, `CANCEL`). `TEXT` past
   de waarde direct toe; `SUBMIT`/`CANCEL` sluiten de sessie. De TV stuurt een laatste
   `form` met `closed: true`.
4. **Main-thread-callback**: de brug verwerkt commando's op IO; `CompanionFormHost` post
   `onValueChange` naar de main-thread, zodat Compose-state veilig blijft.
5. **Sensitive velden**: `masked`/`sensitive` velden hebben nooit een `value` in de
   descriptor en komen niet in de logs. De telefoon begint leeg en stuurt de tekst één keer
   mee bij `SUBMIT`/`TEXT`.
6. **Sluiten op focusverlies**: de TV sluit de sessie als het veld de bewerking verlaat,
   ook als de telefoon nog open staat; de telefoon ruimt de sheet dan op.

## Alternatieven

| Alternatief | Waarom niet |
|---|---|
| Elk scherm apart aansluiten | Tientallen aanpassingen, groot regressierisico |
| Alleen het TV-toetsenbord vervangen | De telefoon moet ook zonder TV-IME kunnen typen |
| Accessibility-service / toetsinjectie | Rechten, play-store-risico, geen echte tekstwaarden |
| Alleen HTTP `POST /forms` | Geen live feedback en geen sessie/focusmodel |

## Gevolgen

- **Positief:** elk tekstveld werkt meteen; geen schermwijzigingen; wachtwoorden kunnen
  veilig op de telefoon worden getypt.
- **Negatief:** de TV-IME blijft ook open (de telefoon is een tweede invoerweg); een
  meerveldsformulier in één sessie volgt later.
- **Debug:** `Log.d("CompanionForm", …)` logt alleen formId/label/actie, nooit tekst of
  wachtwoorden.
- **Test:** de end-to-endroute is het TV-zoekveld: telefoon typt "test", de TV zoekt live;
  `SUBMIT` sluit. Daarnaast is er een Testveld in Settings › Phone.
