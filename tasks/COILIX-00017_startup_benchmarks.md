# COILIX-00017: Benchmarki startu

## Cel

Mierzyć czas startu i koszt skanowania, żeby regresje wydajności były widoczne przed wydaniem, a nie po zgłoszeniu użytkownika. Kontener oparty na refleksji ([ADR 0002](../docs/adr/0002-runtime-reflection-and-java-annotations.md)) jest z tego oceniany najczęściej.

## Stan obecny

- Jedyny pomiar to `<app> is up after <n> s` w logu startu.
- Nie ma danych, ile kosztuje skanowanie (`ReflectionsClassPathScanner`), rozwiązywanie zależności (`TopologicalDependencyResolver`, które jest kwadratowe względem liczby beanów) i tworzenie instancji.

## Zakres

1. Moduł `benchmarks` z `sbt-jmh`, niepublikowany (`.settings(unpublished)`).
2. Generowane aplikacje testowe: 10, 100 i 1000 beanów z losowym grafem zależności o stałym ziarnie.
3. Benchmarki:
   - pełny `Coilix.run` (czas do gotowego kontekstu),
   - samo skanowanie pakietu,
   - samo `DependencyResolver.resolve`,
   - samo tworzenie instancji.
4. Komenda `sbt benchmarks/jmh:run` opisana w `CLAUDE.md`.
5. Wyniki pierwszego pomiaru zapisane w `docs/architecture/performance.md` z opisem maszyny.

## Proponowany projekt

- Generator źródeł w `project/` tworzy klasy aplikacji testowych w `sourceManaged`, więc nie trzeba ich trzymać w repo.
- Benchmarki nie idą do CI przy każdym PR (są wolne i niestabilne na współdzielonych runnerach). Osobny workflow uruchamiany ręcznie (`workflow_dispatch`) zapisuje wyniki jako artefakt.

## Kryteria akceptacji

1. `sbt benchmarks/jmh:run` działa dla trzech rozmiarów aplikacji.
2. `docs/architecture/performance.md` zawiera pierwsze wyniki.
3. Wiadomo, czy `TopologicalDependencyResolver` przy 1000 beanach wymaga zmiany algorytmu.

## Poza zakresem

- Optymalizacje. Ten task tylko mierzy. Poprawki dostają własne taski.
- Porównanie z innymi frameworkami.

## Ryzyka i otwarte pytania

- `sbt-jmh` musi wspierać sbt 2. Jeśli nie wspiera, potrzebny jest osobny build JMH w Mavenie albo ręczne uruchamianie JMH przez `run`.
- Numer katalogu modułu trzeba ustalić razem z przenumerowaniem z COILIX-00010 i COILIX-00013.
