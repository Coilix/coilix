# Licencje zależności

Źródło: `sbt <moduł>/dependencyLicenseInfo` i sekcje `<licenses>` w POM-ach z cache Coursiera, stan na 2026-10-29.

## Audyt w buildzie

`sbt checkLicenses` (`project/LicenseAudit.scala`) sprawdza zależności `compile` i `runtime` każdego modułu i przerywa build, gdy któraś ma licencję spoza listy z sekcji [Obowiązki](#obowiązki) albo nie ma licencji w POM-ie. Zależność z kilkoma licencjami do wyboru, z których jedna jest dozwolona, trzeba dopisać do `chosenLicenses` w `LicenseAudit.scala` z wybraną licencją (teraz: reflections i javassist). Zależności `test` i `provided` nie są sprawdzane, bo nie trafiają do użytkowników.

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

`scala3-compiler` ma zakres `Provided`, bo dostarcza go kompilator użytkownika.

## Zależności testowe i narzędzia builda

Nie są dystrybuowane z naszymi artefaktami, więc nie nakładają obowiązków na użytkowników.

| Zależność | Licencja |
|---|---|
| `org.scalameta:munit` | Apache-2.0 |
| `org.slf4j:slf4j-simple` (testy, demo) | MIT |
| sbt, sbt-wartremover, sbt-scalafmt, sbt-ide-settings, sbt-scoverage, sbt-java-formatter, google-java-format | Apache-2.0 |

## Strona dokumentacji (`12_docs/website`)

Zależności npm (Docusaurus i jego zależności przechodnie) służą tylko do zbudowania statycznej strony `docs.coilix.dev`. Nie trafiają do artefaktów Coilix, więc nie nakładają obowiązków na użytkowników. Na stronę trafia tylko JavaScript i CSS wygenerowany przez Docusaurusa (MIT) i React (MIT).

`npm run check` (`scripts/check-licenses.mjs`) sprawdza licencję każdego pakietu z `package-lock.json` i przerywa build, gdy któraś jest spoza `scripts/allowed-licenses.json`. Wyrażenie z `OR` przechodzi, gdy jedna z opcji jest dozwolona. Pakiet bez pola `license` dopisuje się do sekcji `packages` tego pliku z licencją sprawdzoną ręcznie w jego pliku licencji (teraz: `require-like`, MIT).

Dozwolone licencje narzędzi budowania strony to licencje z sekcji [Obowiązki](#obowiązki) oraz licencje permisywne typowe dla npm: ISC, 0BSD, MIT-0, BlueOak-1.0.0, CC0-1.0, CC-BY-4.0 (dane `caniuse-lite`) i Python-2.0 (`argparse`). GPL, AGPL i LGPL są zakazane tak samo jak w kodzie.

Stan na 2026-11-03: 1281 pakietów, większość na MIT.

Podatne wersje zależności przechodnich, których Docusaurus jeszcze nie podbił, nadpisuje sekcja `overrides` w `package.json` (teraz `serialize-javascript`, `tinypool`, `uuid`). Nadpisanie usuwa się, gdy Docusaurus sam wymaga poprawionej wersji.

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
