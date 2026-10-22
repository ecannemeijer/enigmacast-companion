# Versies, update-JSON en sideload

## Companion-versie

| Veld | Waarde |
|---|---|
| `versionName` | `1.0.0-beta.14` |
| `versionCode` / build | `113` |
| Package | `com.enigmacast.companion` |
| Sideload-APK | `dist/enigmacast-companion.apk` |

De app toont dit in **Meer** onder de titel en in **Over deze app**.

De release-APK is voor sideload ondertekend met de **debug-key** (dezelfde sleutel die de
TV-release gebruikt). Daardoor kun je hem over een eerdere debug-installatie heen
installeren. Voor een Play Store-release is een eigen upload-key nodig; wie daarna
overstapt, moet de app eenmalig verwijderen.

## Update-JSON

Er zijn twee bestanden:

1. **`dist/enigmacast-companion.json`** â€” alleen de Companion. De app leest dit als
   terugvaloptie:

   ```json
   {
     "versionCode": 113,
     "versionName": "1.0.0-beta.14",
     "apkUrl": "https://update.velixatv.com/enigmacast-companion.apk",
     "notes": "â€¦",
     "sideloadFile": "enigmacast-companion.apk"
   }
   ```

2. **`enigmacast-v2.json`** (TV) â€” gecombineerd. De TV-velden blijven bovenaan staan,
   zodat de bestaande `AppUpdateClient` het bestand ongewijzigd kan lezen. Het extra
   `companion`-object wordt door de TV genegeerd (`ignoreUnknownKeys`):

   ```json
   {
     "versionCode": 87,
     "versionName": "1.0.0-beta.84",
     "apkUrl": "https://update.velixatv.com/enigmacast.apk",
     "notes": "â€¦",
     "companion": {
       "versionCode": 113,
       "versionName": "1.0.0-beta.14",
       "apkUrl": "https://update.velixatv.com/enigmacast-companion.apk",
       "notes": "â€¦",
       "sideloadFile": "enigmacast-companion.apk"
     }
   }
   ```

   Een kopie staat als `dist/enigmacast-updates.json` in deze repository.

## Publiceren

1. Upload de APK naar `https://update.velixatv.com/enigmacast-companion.apk`.
2. Upload `enigmacast-companion.json` (of de gecombineerde `enigmacast-v2.json`) naar de
   update-server.
3. Verhoog bij elke release **zowel** `versionName` als `versionCode` in
   `mobile/app/build.gradle.kts`; de update-JSON moet dezelfde waarden krijgen.
4. De TV-app leest `enigmacast-v2.json` (de bovenste velden). De Companion leest
   hetzelfde bestand bij het opstarten en via **Meer â€º Controleer op updates**, en gebruikt
   het `companion`-object. Bij een hogere `versionCode` verschijnt de update-melding met
   **Downloaden** en **Installeren**.

Lokaal klaargezet in `dist/`:

| Bestand | Doel |
|---|---|
| `enigmacast-companion.apk` | het te installeren bestand |
| `enigmacast-companion.apk` | dezelfde APK onder de naam uit de update-JSON |
| `enigmacast-companion.json` | mobiel-only manifest |
| `enigmacast-updates.json` | gecombineerd manifest (kopie van de TV-versie) |

De TV-distributie bevat daarnaast `enigmacast.apk` (beta.80) en
`enigmacast-v2.json` met het `companion`-blok.

## Sideload installeren

```powershell
adb install -r "dist\enigmacast-companion.apk"
```

Of open de APK op de telefoon en sta installatie uit onbekende bronnen toe.


























