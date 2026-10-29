# SCALING-00011: Proces developmentu i model gałęzi

## Cel

Ustalić, jak zmiany przechodzą od taska do wydania: model gałęzi, pull requesty, sprawdzenia przed scaleniem, wydania i utrzymanie starszych linii.

## Stan obecny

- Cała praca idzie bezpośrednio na `master`, bez gałęzi i bez PR.
- Brak zdalnego repozytorium i CI.
- Standard wersji jest opisany w [versioning.md](../docs/architecture/versioning.md), ale nie wiadomo, z której gałęzi wychodzą wydania i jak łata się starsze linie.

## Zakres

1. Opis procesu w [docs/process/development.md](../docs/process/development.md): trunk-based z `master`, gałęzie `feature/*`, `fix/*`, `release/*`, `series/X.Y`, cykl taska, definicja ukończenia, wydanie, utrzymanie linii, ochrona gałęzi.
2. Odnośnik do procesu i zasada DCO (`git commit -s`) w `CLAUDE.md`.
3. Repozytorium na GitHubie z ustawieniami z sekcji „Ochrona gałęzi” (po założeniu organizacji `coilix` i upublicznieniu).

## Kryteria akceptacji

1. `docs/process/development.md` istnieje i jest podlinkowany z `CLAUDE.md`.
2. Zmiany od SCALING-00008 trafiają na `master` przez gałęzie według procesu.
3. Repozytorium na GitHubie ma ustawione metody scalania i usuwanie gałęzi po scaleniu.

## Poza zakresem

- Workflowy CI i wydań (SCALING-00009).
- `CONTRIBUTING.md` dla kontrybutorów z zewnątrz (SCALING-00008), który streszcza ten proces po angielsku.

## Ryzyka i otwarte pytania

- Reguły ochrony gałęzi na darmowym planie GitHuba działają tylko dla repozytoriów publicznych. Do upublicznienia proces opiera się na dyscyplinie.
