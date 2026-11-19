# 0018: Zakresy numerów katalogów modułów

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00000 (odłożone z COILIX-00010, COILIX-00013 i COILIX-00017)

## Kontekst

Katalogi modułów mają prefiks numeryczny, a numer odpowiada poziomowi zależności: moduł zależy tylko od modułów o niższym numerze. Numery były kolejne (`01`–`12`), więc każdy nowy moduł rdzenia albo funkcji, który miał trafić pod `boot`, `demo` albo `docs`, wymagał przesunięcia wszystkich wyższych katalogów. Przenumerowanie odkładano od COILIX-00010, a szkice COILIX-00005 (`concurrency`), COILIX-00027 (`cluster`), COILIX-00023 (`web-*`), COILIX-00026 (`processing*`) i COILIX-00017 (benchmarki) dodają kilkanaście modułów. `99_docs` i demo zależą od wszystkich publikowanych modułów, więc zawsze muszą mieć najwyższe numery.

## Decyzja

Numery idą zakresami z lukami:

| Zakres | Moduły |
|---|---|
| `01`–`14` | biblioteki rdzenia (`common`, `annotations`, `slf4s-plugin`, `config`, `context`, potem `cluster`, `concurrency` itd.) |
| `15`–`19` | `15_boot`, `16_testkit` i kolejne moduły rdzenia nad `boot` |
| `20`–`89` | moduły funkcji i integracji w kolejności powstawania; integracja ma numer wyższy niż funkcje, które łączy |
| `90`–`98` | niepublikowane aplikacje (`90_demo`, benchmarki) |
| `99` | `99_docs` |

Jednorazowe przeniesienie: `06_data` → `20_data`, `07_web` → `21_web`, `08_security` → `22_security`, `09_boot` → `15_boot`, `10_testkit` → `16_testkit`, `11_demo` → `90_demo`, `12_docs` → `99_docs`. Nazwy projektów sbt, artefaktów i pakietów się nie zmieniają.

## Konsekwencje

- Nowy moduł dostaje kolejny wolny numer w swoim zakresie, bez przenoszenia istniejących katalogów. Pierwszy to `06_concurrency` (COILIX-00005, ADR 0020), a po nim `07_cluster` (COILIX-00027).
- Zasada „niższy numer” nadal działa, a `checkModuleLayers` dalej sprawdza warstwy z grafu zależności, a nie z numerów.
- Ścieżki w workflowach, skryptach strony i dokumentach zmieniły się raz. Starsze ADR-y zostają z dawnymi ścieżkami jako zapis decyzji z tamtego czasu.
- IntelliJ trzeba raz przeładować z sbt, bo pliki modułów w `.idea/` nie są w gicie.
