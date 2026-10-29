# SCALING-00009: Wersjonowanie, kompatybilność i proces wydań

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

- Dokumentacja na GitHub Pages (SCALING-00007, korzysta z wersji z tego taska).

## Ryzyka i otwarte pytania

- Wsparcie MiMa, TASTy-MiMa i sbt-ci-release dla sbt 2 trzeba potwierdzić przy wdrożeniu. Awaryjnie: narzędzia CLI w CI.
- Kod może już korzystać z funkcji Scali 3.10. Wtedy trzeba je zastąpić albo świadomie zostać przy 3.10 i opisać to w README.
- Wtyczki builda (`sbt-scalafmt`, `sbt-wartremover`) muszą obsługiwać wybraną wersję Scali.
