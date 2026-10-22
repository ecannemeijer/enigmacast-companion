# ADR 0003 — Bouwen zonder netwerk (buildscript-classpath en twee uitsluitingen)

- **Status:** tijdelijk, aangenomen voor de ontwikkelmachine
- **Datum:** 2026-10-21

## Context

De ontwikkelmachine heeft geen bereikbare Maven-repository en `%USERPROFILE%\.gradle` is
onder de sandbox alleen leesbaar. De Gradle-cache is gevuld door de EnigmaCast-build, maar
bevat niet alles wat een vers `kotlin("jvm")`-project nodig heeft:

- de plugin-markers `org.jetbrains.kotlin.jvm.gradle.plugin` ontbreken;
- `kotlin-scripting-compiler-embeddable` ontbreekt;
- `com.squareup.okhttp3:okhttp-jvm` ontbreekt (alleen de Android-variant is gecacht);
- `error_prone_annotations:2.27.0` ontbreekt (2.36.0 wel).

## Beslissing

1. De Kotlin-plugins worden via `buildscript { classpath(...) }` toegepast in plaats van
   via de `plugins { }`-DSL.
2. `error_prone_annotations` wordt geforceerd op 2.36.0.
3. `kotlin-scripting-compiler-embeddable` wordt uitgesloten van de compiler-plugin-classpath.
4. `core-model` gebruikt geen OkHttp; de enige OkHttp-aanroep in het domein
   (`StremioManifest.httpUrl`) is verwijderd. De Companion speelt geen torrents.
5. Er is een minimale `javax.inject:1`-dependency voor de `@Singleton`/`@Inject`-annotaties
   in `OpenWebifEndpoints`.

## Gevolgen

- **Positief:** `gradlew test` werkt offline; 652 tests groen.
- **Negatief:** de build ziet er anders uit dan een standaard Kotlin-project.
- **Wanneer terugdraaien:** zodra er netwerk is, kan de `plugins { }`-DSL terug; de
  uitsluitingen zijn dan niet meer nodig. De functionele inhoud verandert niet.
- **Let op:** de sandbox moet schrijftoegang tot `%USERPROFILE%\.gradle` hebben, anders
  kan Gradle zijn locks en caches niet bijwerken.

## Update 21-10-2026 — de Android-app

De mobiele module (`mobile:app`) vroeg om extra werkarounds:

1. **AGP 9 heeft ingebouwde Kotlin.** `org.jetbrains.kotlin.android` mag niet meer worden
   toegepast; de app gebruikt alleen `com.android.application`, `org.jetbrains.kotlin.plugin.compose`
   en AGP's eigen Kotlin.
2. **`compileSdk = 37` is verplicht**, omdat Compose UI 1.12.1 `minCompileSdk=37` heeft.
   De offline SDK heeft geen build-tools 37; daarom staat `buildToolsVersion = "36.1.0"`
   expliciet (AGP accepteert dat).
3. **`profileinstaller` is uitgesloten.** Die trekt `androidx.concurrent:concurrent-futures`
   en `ListenableFuture` mee; de cache heeft daar geen volledige set van. Profielinstallatie
   is optioneel (baseline-profielen) en de app werkt zonder.
4. **`listenablefuture` is geforceerd** op de lege placeholder, zoals de Guava-ecosystem
   dat bedoelt bij afwezigheid van Guava.
5. **Afhankelijkheden via string-invoke** (`"implementation"(...)`) omdat de Kotlin-DSL-
   accessors alleen bij de `plugins { }`-DSL worden gegenereerd.
6. **Gedeelde modules targeten JVM 17** (met JDK 21 als toolchain), zodat de Android-app
   (JVM 17) de klassen kan inlinen.

Al deze punten zijn puur omgevingsgebonden. Met netwerk en een volledige SDK kunnen ze
terug naar de standaardconfiguratie zonder de app-code te raken.
