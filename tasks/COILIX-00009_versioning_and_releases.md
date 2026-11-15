# COILIX-00009: Wersjonowanie, kompatybilność i proces wydań

## Cel

Wdrożyć standard z [docs/architecture/versioning.md](../docs/architecture/versioning.md): wersję z tagu gita, `early-semver`, wersję Scali z linii LTS, sprawdzanie kompatybilności i automatyczne wydania na Maven Central i JetBrains Marketplace.

## Stan obecny

- `ThisBuild / version := "0.1.0-SNAPSHOT"` wpisane ręcznie, brak `versionScheme`.
- `scalaVersion := "3.10.0"`, co wymusza na użytkownikach Scalę 3.10+. Scala 3.9.0 LTS wyszła 3 września 2026.
- Bytecode generowany dla JDK, na którym działa build (teraz 25).
- Brak CI, `CHANGELOG.md` i sprawdzania kompatybilności.

## Zakres

1. Wersja z tagu `vX.Y.Z` (sbt-dynver dla sbt 2 albo funkcja w `project/` oparta na `git describe`), `versionScheme := Some("early-semver")`.
2. `scalaVersion` publikowanych modułów na 3.9.x LTS, sprawdzenie kodu pod kątem funkcji z 3.10, aktualizacja zasady o wersjach w `CLAUDE.md` (zrobione).
3. `-java-output-version 17` dla scalac i `--release 17` dla javac.
4. `CHANGELOG.md` w formacie Keep a Changelog z sekcją `Unreleased`.
5. GitHub Actions:
   - `ci.yml`: `sbt testFull` i `sbt doc` na JDK 17 i 25 dla każdego pusha i PR,
   - `release.yml`: na tag `v*` publikacja na Maven Central (sbt-ci-release albo odpowiednik zgodny z sbt 2) i GitHub Release z sekcją z changelogu,
   - plugin IntelliJ: budowa, Plugin Verifier, podpis, publikacja do Marketplace.
6. MiMa i TASTy-MiMa w CI względem poprzedniego wydania (od pierwszego wydania po `0.1.0`).
7. Opis procesu wydania w `CONTRIBUTING.md`.

## Kryteria akceptacji

1. `sbt version` na commicie z tagiem `v0.1.0` zwraca `0.1.0`, a po kolejnym commicie zwraca wersję snapshotu.
2. Publikowane jary mają bytecode w wersji 61 (JDK 17), a TASTy z wersji Scali 3.9.
3. Tag `v*` w GitHubie publikuje artefakty na Maven Central i plugin w Marketplace bez ręcznych kroków.
4. CI odrzuca PR, który psuje kompatybilność binarną lub TASTy w obrębie linii patch.
5. `CHANGELOG.md` istnieje i jest opisany w `CONTRIBUTING.md`.

## Poza zakresem

- Dokumentacja na GitHub Pages (COILIX-00007, korzysta z wersji z tego taska).

## Ryzyka i otwarte pytania

- Wsparcie MiMa, TASTy-MiMa i sbt-ci-release dla sbt 2 trzeba potwierdzić przy wdrożeniu. Awaryjnie: narzędzia CLI w CI.
- Kod może już korzystać z funkcji Scali 3.10. Wtedy trzeba je zastąpić albo świadomie zostać przy 3.10 i opisać to w README.
- Wtyczki builda (`sbt-scalafmt`, `sbt-wartremover`) muszą obsługiwać wybraną wersję Scali.

## Stan realizacji (2026-10-29)

Zrobione: punkty 1–5 bez pluginu IntelliJ, MiMa z punktu 6 i opis wydania w `docs/process/development.md` (punkt 7, `CONTRIBUTING.md` do niego odsyła).

- wersja z tagu przez sbt-ci-release 1.12.1 (zawiera sbt-dynver), `versionScheme := early-semver`,
- Scala 3.9.0, bytecode 61 (JDK 17), TASTy 28.9,
- jar pluginu `@Slf4s` ma stałą nazwę `coilix-slf4s-plugin.jar`, bo wersja z gita zmienia się z każdym commitem, a IntelliJ trzyma ścieżkę do jara,
- `CHANGELOG.md`, `.github/workflows/ci.yml` i `.github/workflows/release.yml`,
- MiMa z wydaniem odniesienia wybieranym z ostatniego tagu tej samej linii.

Zostaje:

- sekrety `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` w repozytorium i weryfikacja przestrzeni nazw `dev.coilix` w Central Portal rekordem DNS TXT w domenie `coilix.dev` (wymaga konta autora),
- TASTy-MiMa: brak wtyczki dla sbt 2,
- budowa, podpis i publikacja pluginu IntelliJ do Marketplace (z wersją z tagu w `plugin.xml`).

## Grupy wydań (ADR 0011)

[ADR 0011](../docs/adr/0011-release-groups-and-changelog-files.md) zmienia model wydań. Kolejne PR-y, każdy zostawia `master` w stanie gotowym do wydania:

1. **Decyzja:** ADR 0011 i ten plan (zrobione, ADR przyjęta).
2. **Changelog w plikach:** katalog `changelog/<grupa>/` z `unreleased.md`, przeniesienie treści z `CHANGELOG.md`, sprawdzanie formatu w CI, strona `/changelog` z tych plików, zasady w `CLAUDE.md` i `docs/process/development.md` (zrobione: format opisuje `changelog/README.md`, sprawdza `npm run check`, a do czasu kroku 4 tag `v*` bierze notkę z `changelog/core/X.Y.Z.md`).
3. **Grupy w buildzie:** przypisanie modułów do grup w `build.sbt`, wersja z tagu `<grupa>-v*` dla każdego modułu, MiMa względem poprzedniego wydania grupy, sprawdzenie, że moduł funkcji zależy od wydanego rdzenia, nowy opis w `docs/architecture/versioning.md` (zrobione: `project/ReleaseGroups.scala`, `checkReleasedCore`; przy okazji tag `core-v*` w `release.yml` i rulesety tagów `*-v*`, żeby rdzeń dało się wydać bez czekania na krok 4).
4. **Wydanie przy tagu:** `release.yml` dla tagów `*-v*` (sprawdzenia z ADR 0011, publikacja tylko grupy, GitHub Release z pliku changelogu), skrypt i workflow „Prepare release”, ruleset tagów `*-v*` (zrobione: `release.yml` na `*-v*` z `release-tag.mjs` i `releaseVerify`, publikacja tylko grupy przez `RELEASE_GROUP`, zamiast workflow lokalny skrypt `npm run prepare-release`; zostaje publikacja pluginu IntelliJ w Marketplace).
5. **Publikacja strony** (razem z krokiem 7 COILIX-00007): workflow „Publish docs” na tag i ręcznie, sekcje i wersje per grupa, API pod `/api/<grupa>/<linia>/`, lychee przed publikacją, `gh-pages` i domena `docs.coilix.dev`.

Kryteria akceptacji grup wydań:

1. Tag `core-v0.1.0` publikuje tylko moduły rdzenia z wersją `0.1.0`, a tag `web-v0.1.0` tylko `coilix-web` zależny od wydanej wersji rdzenia.
2. Tag bez pliku `changelog/<grupa>/<wersja>.md` albo z błędnym plikiem nie publikuje niczego.
3. GitHub Release i strona `/changelog` pokazują treść pliku wydania.
4. Strona jest publikowana tylko przy tagu albo ręcznie i przechodzi link checker przed publikacją.
