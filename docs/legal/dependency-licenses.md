# Licencje zależności

Źródło: `sbt <moduł>/dependencyLicenseInfo` i sekcje `<licenses>` w POM-ach z cache Coursiera, stan na 2026-10-29. Pełny audyt (z narzędziem w CI) jest w zakresie [SCALING-00008](../../tasks/SCALING-00008_license_and_publication_setup.md).

## Zależności runtime publikowanych modułów

| Zależność | Wersja | Licencja | Używa | Zgodna z Apache-2.0 |
|---|---|---|---|---|
| `org.scala-lang:scala3-library_3` | 3.10.0 | Apache-2.0 | wszystkie moduły Scali | tak |
| `org.slf4j:slf4j-api` | 2.0.20 | MIT | moduły z `@Slf4s` | tak |
| `com.typesafe:config` | 1.4.9 | Apache-2.0 | `config` | tak |
| `org.snakeyaml:snakeyaml-engine` | 3.2 | Apache-2.0 | `config` | tak |
| `org.reflections:reflections` | 0.10.2 | WTFPL **lub** Apache-2.0 (do wyboru) | `context` | tak, korzystamy z Apache-2.0 |
| `org.javassist:javassist` (przez reflections) | 3.28.0-GA | MPL-1.1 **lub** LGPL-2.1 **lub** Apache-2.0 (do wyboru) | `context` | tak, korzystamy z Apache-2.0 |
| `com.google.code.findbugs:jsr305` (przez reflections) | 3.0.2 | Apache-2.0 | `context` | tak |

`sbt dependencyLicenseInfo` pokazuje tylko pierwszą licencję z POM-a (dla javassist MPL 1.1, dla reflections WTFPL). Pełne listy z POM-ów potwierdzają, że obie biblioteki dają też Apache-2.0.

## Zależności pluginu kompilatora `@Slf4s` (`slf4s-plugin`)

| Zależność | Licencja |
|---|---|
| `scala3-compiler_3`, `tasty-core_3`, `scala3-interfaces` | Apache-2.0 |
| `org.scala-sbt:compiler-interface`, `util-interface` | Apache-2.0 |
| `org.ow2.asm:*` | BSD-3-Clause |

`scala3-compiler` jest teraz zwykłą zależnością. Przy publikacji ma mieć zakres `Provided`, bo dostarcza go kompilator użytkownika. Inaczej trafi do jego classpath.

## Zależności testowe i narzędzia builda

Nie są dystrybuowane z naszymi artefaktami, więc nie nakładają obowiązków na użytkowników.

| Zależność | Licencja |
|---|---|
| `org.scalameta:munit` | Apache-2.0 |
| `org.slf4j:slf4j-simple` (testy, demo) | MIT |
| sbt, sbt-wartremover, sbt-scalafmt, sbt-ide-settings | Apache-2.0 |

## Plugin IntelliJ (`11_intellij_plugin`)

- Kompiluje się na jarach IntelliJ Platform i pluginu Scala z lokalnej instalacji IDE. Oba są na Apache-2.0 (intellij-community, intellij-scala).
- Jarów platformy nie dystrybuujemy. Plugin w Marketplace zawiera tylko nasze klasy i deklaruje zależność od pluginu Scala w `plugin.xml`.

## Obowiązki

- Publikujemy osobne jary bez wbudowanych cudzych klas (bez fat-jara i shadingu), więc nie musimy kopiować licencji ani plików `NOTICE` zależności. Użytkownik pobiera je z Maven Central z ich własnymi licencjami.
- Gdyby kiedyś powstała dystrybucja z zależnościami w środku (fat-jar demo, plugin z bibliotekami), musi zawierać licencje i `NOTICE` każdej z nich.
- Nowa zależność może mieć tylko licencję zgodną z Apache-2.0: Apache-2.0, MIT, BSD-2/3-Clause, EPL-2.0 (tylko jako zależność binarna), MPL-2.0 (tylko jako zależność binarna) albo licencję z opcją jednej z nich. Zakazane są GPL i AGPL oraz licencje bez opcji komercyjnej.

## Ryzyka techniczne

- **reflections 0.10.2** to ostatnie wydanie biblioteki (2021). Według repozytorium projekt nie jest aktywnie rozwijany, co trzeba potwierdzić. Licencyjnie nic się nie zmienia, ale w razie problemów z nowym JDK alternatywą jest ClassGraph (MIT).
- **jsr305** zawiera pakiet `javax.annotation`, który koliduje z innymi bibliotekami przy JPMS (split package). To problem techniczny, nie prawny.
