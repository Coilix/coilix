# COILIX-00019: Przestrzeń nazw `dev.coilix`

## Cel

`groupId` i pakiety oparte na domenie projektu `coilix.dev` zamiast na organizacji GitHub, przed pierwszym wydaniem.

## Stan obecny

- `groupId` `io.github.coilix` i pakiety `io.github.coilix.*`, decyzja z COILIX-00008.
- Domena `coilix.dev` kupiona 2026-11-02, poczta projektu działa, organizacja GitHub zweryfikowana domeną.
- Nic nie zostało wydane.

## Zakres

1. `organization` i `idePackagePrefix` w `build.sbt`.
2. Deklaracje pakietów, importy i pełne nazwy w kodzie, testach, configach, Scaladoc i Javadoc.
3. Identyfikator i klasy w `plugin.xml` pluginu IntelliJ, klasa główna w `.run/Demo.run.xml`.
4. `CLAUDE.md`, `README.md`, dokumenty w `docs/` i taski, które opisują bieżący stan.
5. ADR 0009.

## Proponowany projekt

Podmiana `io.github.coilix` na `dev.coilix` w całym repozytorium. Pliki źródłowe leżą w płaskich katalogach modułów (`idePackagePrefix`), więc żaden plik nie zmienia położenia.

## Kryteria akceptacji

1. `git grep io.github.coilix` zwraca tylko zapisy historyczne (decyzja z COILIX-00008).
2. `sbt testFull`, `sbt doc` i `sbt demo/run` przechodzą.

## Poza zakresem

- Weryfikacja przestrzeni nazw w Central Portal (rekord TXT dodaje autor, część COILIX-00009).
- Strona projektu pod `coilix.dev` (COILIX-00007).

## Ryzyka i otwarte pytania

- Lokalne konfiguracje IDE i cache sbt mogą trzymać stare nazwy. Wystarczy przeładować projekt sbt w IntelliJ.
