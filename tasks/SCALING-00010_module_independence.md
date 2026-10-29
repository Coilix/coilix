# SCALING-00010: Niezależność modułów opcjonalnych

## Cel

Sprawić, żeby `data`, `web` i `security` były opcjonalne: aplikacja działa bez nich, a dodanie modułu do zależności włącza go bez zmiany kodu. Zasady i projekt są w [docs/architecture/modularity.md](../docs/architecture/modularity.md).

## Stan obecny

- `boot` zależy tylko od `context`, więc rdzeń już dziś nie wymaga modułów funkcji.
- `security` zależy od `web`.
- Brak mechanizmu, przez który moduł z classpath dodaje swoje beany. `DefaultCoilixBootstrap.start` przyjmuje `extraDefinitions`, ale nikt ich nie dostarcza z modułów.
- Moduły `data`, `web`, `security` są puste.

## Zakres

1. `CoilixModule` i `ModuleLoader` (trait, `given ConfigPath`, `default`) w `context`, implementacja `ServiceLoaderModuleLoader` z wyłączaniem przez `coilix.modules.<nazwa>.enabled`.
2. Podpięcie modułów w `DefaultCoilixBootstrap`: definicje z modułów razem z definicjami ze skanowania.
3. Podział `security` na `security` (bez `web`) i `security-web` (integracja). Przenumerowanie katalogów zgodnie z zasadą poziomów z `CLAUDE.md`.
4. Sprawdzenie w `build.sbt`, że rdzeń i `boot` nie zależą od modułów funkcji, a moduły funkcji nie zależą od siebie. Uruchamiane w `sbt testFull`.
5. Test, że `boot` startuje bez modułów funkcji, i test modułu wykrywanego przez `ServiceLoader` (moduł testowy w `src/test` z plikiem `META-INF/services`).
6. Aktualizacja tabeli modułów w `CLAUDE.md` (nowe moduły, warstwy).

## Kryteria akceptacji

1. Dodanie zależności od modułu z `CoilixModule` w `META-INF/services` dodaje jego beany do kontekstu bez zmian w kodzie aplikacji.
2. `coilix.modules.<nazwa>.enabled = false` wyłącza moduł.
3. `security` nie zależy od `web`.
4. Build przerywa się, gdy rdzeń albo `boot` dostanie zależność od modułu funkcji.
5. `sbt testFull` i `sbt demo/run` przechodzą.

## Poza zakresem

- Implementacja samych funkcji `data`, `web`, `security`.
- Wydzielenie `config-yaml` z `config` (opcjonalne, osobny task, jeśli będzie potrzebne).

## Ryzyka i otwarte pytania

- Kolejność modułów: czy moduł może wymagać innego (np. `security-web` wymaga obu)? Proponowane: zależności między modułami tylko przez `build.sbt`, a `ModuleLoader` nie sortuje, bo konflikty rozstrzyga `@Primary`.
- `ServiceLoader` w aplikacjach z niestandardowym classloaderem (np. testy, fat-jar) musi dostać właściwy `ClassLoader`, ten sam co skaner classpath.
