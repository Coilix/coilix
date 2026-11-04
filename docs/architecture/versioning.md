# Wersjonowanie i wydania

> [ADR 0011](../adr/0011-release-groups-and-changelog-files.md) zmienia ten standard: grupy wydań (`core`, moduły funkcji, plugin IntelliJ) z osobnymi wersjami i tagami `<grupa>-vX.Y.Z`, changelog w plikach `changelog/<grupa>/<wersja>.md` i publikacja strony przy tagu. Do wdrożenia w COILIX-00009 obowiązuje opis poniżej.

## Standard

- **[Semantic Versioning 2.0.0](https://semver.org/)**: `MAJOR.MINOR.PATCH`, wydania przedpremierowe z sufiksem `-M1`, `-RC1`.
- **Jedna wersja dla całego frameworka.** Wszystkie publikowane moduły (`common`, `annotations`, `slf4s-plugin`, `config`, `context`, `boot`, `data`, `web`, `security`) wychodzą razem z tym samym numerem, nawet jeśli któryś się nie zmienił. Użytkownik nie musi sprawdzać, które wersje modułów do siebie pasują. Tak robią Spring Boot, Akka/Pekko i http4s.
- **`versionScheme := Some("early-semver")`** w `build.sbt`. sbt i Coursier wykrywają wtedy konflikty wersji zgodnie z tym schematem.
- **Wersja pochodzi z tagu gita** `vX.Y.Z` (np. sbt-dynver, który ma build dla sbt 2). Commity po tagu dają wersję snapshotu. Wersja nie jest wpisywana ręcznie w `build.sbt`.
- Plugin IntelliJ ma ten sam numer co framework. W `plugin.xml` deklaruje zakres IDE (`since-build`/`until-build`), a nie wersję frameworka.

## Co oznacza zmiana numeru

### Faza `0.x` (teraz)

API nie jest stabilne. Zgodnie z `early-semver`:

| Zmiana | Wersja |
|---|---|
| niezgodna zmiana API lub binarna, usunięcie elementu, zmiana klucza configu | `0.MINOR` (np. `0.3.0` → `0.4.0`) |
| nowa funkcja zgodna wstecz, poprawka | `0.x.PATCH` (np. `0.3.0` → `0.3.1`) |

### Od `1.0.0`

| Zmiana | Wersja |
|---|---|
| niezgodna zmiana API publicznego, binarna (bytecode) albo TASTy, usunięcie elementu oznaczonego `@deprecated`, zmiana znaczenia lub usunięcie klucza configu, podniesienie minimalnej wersji Scali lub JDK | `MAJOR` |
| nowe API, nowy moduł, nowy klucz configu, oznaczenie `@deprecated` | `MINOR` |
| poprawka bez zmiany API | `PATCH` |

**Publiczne API** to wszystko, co jest publiczne w `src/main` publikowanych modułów, klucze configu `coilix.*`, format `banner.txt` i `application.*` oraz zachowanie opisane w Scaladoc. Klasy implementacji (`Default*`, `Reflective*`) są publiczne, bo można je wskazać kluczem `class`, więc też podlegają tym zasadom.

## Kompatybilność

- **Binarna:** [MiMa](https://github.com/lightbend-labs/mima) (`sbt mimaReportBinaryIssues`, wtyczka dla sbt 2) porównuje bytecode z poprzednim wydaniem tej samej linii `MAJOR.MINOR`. Wydanie odniesienia wybiera się automatycznie z ostatniego tagu (`previousStableVersion` z sbt-dynver), więc przed pierwszym wydaniem i po podniesieniu `MINOR` sprawdzenie nic nie robi. CI uruchamia je w każdym PR.
- **TASTy:** [TASTy-MiMa](https://github.com/scalacenter/tasty-mima) sprawdza zgodność dla kodu Scali 3 (inline, typy, `given`), której MiMa nie widzi. Nie ma jeszcze wtyczki dla sbt 2 (stan na 2026-10-29), więc do czasu jej wydania trzeba ją uruchamiać ręcznie przed wydaniem patch albo przez API biblioteki w osobnym kroku CI.
- W fazie `0.x` sprawdzenia działają tylko między wydaniami patch tej samej linii (`0.3.0` ↔ `0.3.1`). Od `1.0.0` obejmują całą linię major.
- **Wycofywanie API:** element najpierw dostaje `@deprecated("use X instead", since = "0.4.0")`, a znika najwcześniej w następnej wersji niezgodnej (`0.5.0`, a od 1.0 w następnym major).

## Wersja Scali i JDK

Scala 3 jest kompatybilna tylko wstecz: biblioteka skompilowana Scalą 3.N działa tylko w projektach na 3.N i nowszych. Publikując wersję skompilowaną Scalą 3.10, wymuszamy na użytkownikach 3.10.

- **Publikowane moduły kompilujemy Scalą z aktualnej linii LTS.** 3 września 2026 wyszła **Scala 3.9.0 LTS**, która zastępuje 3.3 LTS jako zalecana baza dla bibliotek. Build używa `scalaVersion := 3.9.0` (zmienione z 3.10.0 w COILIX-00009, kod nie korzystał z funkcji 3.10). Plugin IntelliJ zostaje na Scali 2.13.
- Wyjątek od zasady „najnowsza stabilna wersja” z `CLAUDE.md`: dla Scali obowiązuje najnowsza wersja **linii LTS**.
- **JDK:** Scala 3.9 wymaga JDK 17, więc bytecode generujemy dla 17 (`-java-output-version 17` w scalac, `--release 17` w javac), choć build działa na nowszym JDK. CI testuje na JDK 17 i najnowszym LTS (teraz 25).
- Podniesienie minimalnej wersji Scali lub JDK od `1.0.0` to zmiana `MAJOR`.

## Changelog

Changelog to katalog `changelog/` z podkatalogiem dla każdej grupy wydań (`core`, `intellij`, moduły funkcji, [ADR 0011](../adr/0011-release-groups-and-changelog-files.md)). Grupa ma plik `unreleased.md` ze zmianami po ostatnim wydaniu i plik `<wersja>.md` dla każdego wydania, z nagłówkiem `version`, `date` i (dla modułu funkcji) `requires-core`. Treść jest w formacie [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) z sekcjami `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`. Wpis zawiera ID taska (`COILIX-00004`). Zmiana niezgodna jest oznaczona **BREAKING** i opisuje migrację.

Pełny opis formatu jest w [changelog/README.md](../../changelog/README.md). Format wszystkich plików sprawdza job `Documentation site` w każdym PR (`npm run check`), a obecność wpisu sprawdza przegląd PR.

## Proces wydania

Kroki dla opiekuna są w [docs/process/development.md](../process/development.md#wydanie). Automatyzacja:

- `.github/workflows/ci.yml`: przy każdym pushu na `master` i `series/**` oraz w każdym PR `checkLicenses`, `testFull`, `mimaReportBinaryIssues` i `doc` (bez zepsutych linków) na JDK 17 i 25.
- `.github/workflows/release.yml`: na tag `v*` te same sprawdzenia, notka wydania z `changelog/core/X.Y.Z.md` (brak pliku albo błąd formatu zatrzymuje wydanie przed testami), `sbt ci-release` (podpis PGP i publikacja na Maven Central przez Central Portal) i GitHub Release z notatkami. Tag z sufiksem (`-M1`, `-RC1`) daje pre-release.
- Wersja pochodzi z tagu przez sbt-dynver (część sbt-ci-release): commit z tagiem `v0.1.0` ma wersję `0.1.0`, kolejne commity `0.1.0+N-<hash>-SNAPSHOT`, a przed pierwszym tagiem `0.0.0+N-<hash>-SNAPSHOT`.
- Do zrobienia później: publikacja pluginu IntelliJ do Marketplace i dokumentacji na GitHub Pages (COILIX-00007).

Sekrety repozytorium wymagane przez `release.yml`: `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` (token z Central Portal).
