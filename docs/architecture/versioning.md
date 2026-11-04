# Wersjonowanie i wydania

> Model grup wydań pochodzi z [ADR 0011](../adr/0011-release-groups-and-changelog-files.md). Wydanie modułów funkcji i pluginu IntelliJ przy ich tagu oraz publikacja strony przy tagu dojdą w kolejnych krokach COILIX-00009, a do tego czasu tag wydaje tylko grupę `core`.

## Standard

- **[Semantic Versioning 2.0.0](https://semver.org/)**: `MAJOR.MINOR.PATCH`, wydania przedpremierowe z sufiksem `-M1`, `-RC1`.
- **Grupy wydań z osobnymi wersjami:**

  | Grupa | Moduły | Tag |
  |---|---|---|
  | `core` | `common`, `annotations`, `slf4s-plugin`, `config`, `context`, `boot` | `core-vX.Y.Z` |
  | moduł funkcji, np. `web` | jeden moduł funkcji (`data`, `web`, `security`) | `web-vX.Y.Z` |
  | `intellij` | `intellij-plugin` | `intellij-vX.Y.Z` |

  Moduły rdzenia mają zawsze tę samą wersję i wychodzą razem, nawet jeśli któryś się nie zmienił, więc użytkownik nie sprawdza, które wersje rdzenia do siebie pasują. Moduł funkcji wychodzi niezależnie od rdzenia i od innych modułów funkcji.
- **Przynależność do grupy jest tylko w `build.sbt`:** moduł rdzenia dostaje `.settings(coreGroup)`, moduł funkcji `.settings(featureGroup("<id>"))`, a plugin IntelliJ grupę `intellij`. Lista grup jest w `ReleaseGroups.buildSettings(...)` w `build.sbt`, a logika w `project/ReleaseGroups.scala`. `sbt "show releaseGroup"` pokazuje grupę każdego modułu. Moduły niepublikowane bez grupy (`demo`, `docs`) i root mają wersję grupy `core`.
- **`versionScheme := Some("early-semver")`** w `build.sbt`. sbt i Coursier wykrywają wtedy konflikty wersji zgodnie z tym schematem.
- **Wersja modułu pochodzi z ostatniego tagu jego grupy** (`git describe --match '<grupa>-v[0-9]*'` przez sbt-dynver). Commit z tagiem `core-v0.1.0` daje modułom rdzenia wersję `0.1.0`, kolejne commity `0.1.0+N-<hash>-SNAPSHOT`, a grupa bez tagu ma `0.0.0+N-<hash>-SNAPSHOT`. Stan gita wszystkich grup jest czytany raz przy wczytaniu buildu, bo równoległe `git describe --dirty` blokują się na indeksie. Wersji nie wpisuje się ręcznie.
- **Moduł funkcji zależy od wydanego rdzenia.** Gdy moduł funkcji ma wersję wydania (commit z jego tagiem), jego zależności od modułów rdzenia dostają wersję ostatniego tagu `core-v*` zamiast bieżącej wersji rdzenia, więc POM nigdy nie wskazuje snapshotu. `sbt <moduł>/checkReleasedCore` przerywa wydanie, gdy nie ma żadnego tagu `core-v*` albo kod modułów rdzenia zmienił się od ostatniego z nich. Wtedy najpierw wychodzi rdzeń.
- Plugin IntelliJ ma własną wersję z tagów `intellij-v*`. W `plugin.xml` deklaruje zakres IDE (`since-build`/`until-build`), a nie wersję frameworka. Wersja w `plugin.xml` jest jeszcze wpisana ręcznie, do czasu publikacji w Marketplace.

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

- **Binarna:** [MiMa](https://github.com/lightbend-labs/mima) (`sbt mimaReportBinaryIssues`, wtyczka dla sbt 2) porównuje bytecode z poprzednim wydaniem tej samej linii `MAJOR.MINOR` tej samej grupy. Wydanie odniesienia wybiera się automatycznie z ostatniego tagu grupy przed bieżącym commitem (`previousStableVersion`), więc przed pierwszym wydaniem i po podniesieniu `MINOR` sprawdzenie nic nie robi. CI uruchamia je w każdym PR.
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
- `.github/workflows/release.yml`: na tag `core-v*` te same sprawdzenia, notka wydania z `changelog/core/X.Y.Z.md` (brak pliku albo błąd formatu zatrzymuje wydanie przed testami), `sbt ci-release` (podpis PGP i publikacja na Maven Central przez Central Portal) i GitHub Release z notatkami. Tag z sufiksem (`-M1`, `-RC1`) daje pre-release.
- sbt-ci-release publikuje wszystkie moduły bez `publish / skip`, a o wydaniu decyduje wersją buildu, czyli grupy `core`. Do czasu publikacji per grupa (krok 4 COILIX-00009) moduły funkcji i plugin IntelliJ mają `unpublished`.
- Do zrobienia później: wydanie modułów funkcji przy tagu `<moduł>-v*` z `checkReleasedCore`, workflow „Prepare release”, publikacja pluginu IntelliJ do Marketplace i dokumentacji na GitHub Pages (COILIX-00009, COILIX-00007).

Sekrety repozytorium wymagane przez `release.yml`: `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` (token z Central Portal).
