# COILIX-00030: Diagnostyka i błędy fatalne

> Decyzje opiekuna z 2026-11-20:
>
> - aplikacja wykrywa zakleszczenia wątków i braki pamięci, a przed brakiem pamięci broni się zrzucaniem obciążenia,
> - po błędzie fatalnym (OOM, `StackOverflowError`, `LinkageError`) aplikacja się wyłącza, a nie działa dalej z okaleczonymi pulami,
> - wykryte zakleszczenie też wyłącza aplikację, bo zakleszczone wątki same się nie odblokują,
> - pomysły pochodzą z inspektorów wątków i zasobów bulldozera (`ThreadInspector`, `ResourceInspector`) i z obsługi błędów puli w `OptimusRuntime` z `org.optimus:implicits`; kod piszemy od nowa w formie Coilix (zasada z CLAUDE.md).

## Cel

- Wyjątek fatalny w dowolnym wątku nie zostawia aplikacji w stanie „żyje, ale nie działa”. Coilix loguje go, zamyka kontekst z krótkim limitem czasu i kończy proces, a orkiestrator stawia nową instancję.
- Aplikacja widzi, że brakuje jej pamięci, zanim poleci `OutOfMemoryError`, i przestaje przyjmować nową pracę, dopóki pamięć nie wróci.
- Zakleszczenie, nadmiar wątków i wątki długo czekające na blokadę są wykrywane i opisane w logu ze stosami.
- Stan zdrowia aplikacji (`Up`, `Degraded`, `Down`) jest dostępny jako API i jako zdarzenia, żeby procesory (COILIX-00026), HTTP (COILIX-00023) i sondy Kubernetesa mogły na niego reagować.

## Stan obecny

- `TaskExecutor`, `TaskScheduler` i odbiorcy zdarzeń łapią tylko `NonFatal`. Wyjątek fatalny zabija wątek, JVM wypisuje go na stderr, a aplikacja działa dalej.
- `boot` rejestruje shutdown hook, który zamyka kontekst (`RuntimeShutdownHookRegistrar`). Nic nie wywołuje zamknięcia po błędzie fatalnym.
- Nie ma monitorowania wątków, pamięci ani modelu zdrowia.

## Wzorce źródłowe

- bulldozer: osobny wątek diagnostyki, zrzut stanu co interwał, lista wymiennych detektorów (zakleszczenie, za dużo wątków, długie `BLOCKED`/`WAITING`, przyrost liczby wątków w oknie), raport z ograniczeniem częstotliwości logów, progi heapu, RAM-u i CPU.
- implicits: handler nieobsłużonych wyjątków puli, który rozróżnia `NonFatal` od błędu fatalnego i woła wymienny callback. W Coilix callback domyślnie wyłącza aplikację, zamiast nic nie robić.
- Zmiany względem wzorców: pamięć oceniamy po odśmiecaniu (progi `collectionUsageThreshold`), a nie z bieżącej zajętości heapu, bo heap tuż przed GC zawsze jest pełny. Wątki wirtualne nie są widoczne w `ThreadMXBean`, więc monitor wątków ma dla nich osobną drogę (krok 2).

## Model

### Błędy fatalne

- Fatalne są `VirtualMachineError` (w tym `OutOfMemoryError`, `StackOverflowError`, `InternalError`) i `LinkageError`. Klasyfikację robi jedna funkcja w rdzeniu, zgodna z `scala.util.control.NonFatal`.
- `FatalErrorHandler` (trait w `concurrency`, komponent z `ConfigPath` `coilix.fatal-errors`) dostaje wątek i wyjątek. Domyślny `ShutdownFatalErrorHandler`:
  1. loguje krótki komunikat na `ERROR`, bez ciężkiego formatowania, bo przy OOM samo budowanie tekstu może się nie udać,
  2. uruchamia zamknięcie z nowego wątku `coilix-fatal-shutdown`, a nie z wątku, który padł. `System.exit` odpala shutdown hook, który zamyka kontekst i czeka na wykonawcę, a w nim siedzi właśnie ten wątek,
  3. uruchamia wątek-strażnika (demona), który po `coilix.fatal-errors.shutdown-timeout` (domyślnie 15 s) wywołuje `Runtime.halt`. `halt` nie odpala hooków, więc nie da się go zakleszczyć,
  4. drugi i kolejne błędy fatalne podczas zamykania tylko loguje.
- `coilix.fatal-errors.action`: `shutdown` (domyślnie) albo `log`. Kod wyjścia w `coilix.fatal-errors.exit-code` (domyślnie 70).
- Wyjście z procesu idzie przez `ProcessTerminator` (`exit`, `halt`), żeby testy mogły go podmienić.
- Handler dostają wątki z fabryk Coilix (`ThreadFactories`, `NamedThreadFactory`), a zadania wykonawcy, harmonogramu i odbiorców zdarzeń przepuszczają błąd fatalny do handlera, zamiast go połykać. `boot` ustawia go też jako domyślny handler procesu (`Thread.setDefaultUncaughtExceptionHandler`), a testkit nie, żeby test nie wyłączył JVM.
- Przy starcie `boot` sprawdza flagi JVM i ostrzega na `WARN`, gdy brakuje `-XX:+ExitOnOutOfMemoryError` albo `-XX:+CrashOnOutOfMemoryError`, oraz gdy maksymalny heap to mniej niż połowa pamięci dostępnej dla kontenera (domyślne 25% z `MaxRAMPercentage`). Przewodnik podaje zalecane flagi, także `-XX:+HeapDumpOnOutOfMemoryError`.

### Zdrowie

- `HealthStatus` to `Up`, `Degraded` (działa, ale nie powinna dostawać nowej pracy) albo `Down` (nie działa poprawnie).
- `HealthIndicator` (trait) to bean z nazwą, rodzajem (`Liveness` albo `Readiness`) i bieżącym stanem z powodem. Moduły i aplikacja dodają własne wskaźniki.
- `Health` (bean) zbiera wskaźniki: liveness to najgorszy stan wskaźników `Liveness`, readiness to najgorszy stan wszystkich. Zmiana stanu publikuje zdarzenie `HealthChanged` przez `EventPublisher`.
- Wystawienie przez HTTP (`/health/live`, `/health/ready`) przychodzi z COILIX-00023. Do tego czasu stan jest dostępny przez API i log.

### Monitor wątków

- Działa na osobnym wątku platformowym `coilix-diagnostics`, poza pulami aplikacji, co `coilix.diagnostics.threads.interval` (domyślnie 5 s).
- Kontrole (trait `ThreadCheck`, lista wymienna):
  - **zakleszczenie**: `ThreadMXBean.findDeadlockedThreads` (monitory i blokady `java.util.concurrent`),
  - **za dużo wątków**: progi ostrzeżenia i błędu liczby wątków platformowych,
  - **długie czekanie na blokadę**: wątek w `BLOCKED` dłużej niż próg,
  - **przyrost wątków**: wzrost liczby wątków w oknie czasu ponad próg (podejrzenie wycieku),
  - **zablokowane zadania wirtualne**: `TaskExecutor` liczy zadania działające dłużej niż próg, bo wątków wirtualnych nie widać w `ThreadMXBean`. Na JDK 25 i nowszych długie zadania wyzwalają zrzut `dumpThreads` JSON i szukanie w nim cykli (ADR 0039).
- Wynik kontroli to `Finding` z wagą (`Info`, `Warning`, `Critical`), rodzajem, opisem i wątkami. `FindingReporter` loguje je ze stosami (pierwsze ramki) i ogranicza powtórzenia tego samego rodzaju (np. raz na 30 s).
- `coilix.diagnostics.threads.on-deadlock`: `shutdown` (domyślnie) loguje zrzut wątków i woła `FatalErrorHandler`. `report` tylko loguje i ustawia wskaźnik `threads` (liveness) na `Down`. Przy zakleszczeniu łagodne zamknięcie może się nie udać, więc strażnik z `halt` jest tu szczególnie potrzebny.

### Monitor pamięci

- Na pulach heapu, które to obsługują (stara generacja), ustawia `collectionUsageThreshold`. JVM powiadamia, gdy po odśmiecaniu zajętość nadal jest ponad progiem, czyli przy prawdziwym ciśnieniu pamięci.
- Wskaźnik `memory` (readiness):
  - `Degraded`, gdy zajętość po GC przekroczy `degraded-percent` (domyślnie 85%) albo czas GC w oknie przekroczy `gc-time-percent` (domyślnie 25%),
  - powrót do `Up` dopiero poniżej `recovered-percent` (domyślnie 75%), żeby stan nie migał.
- `Degraded` nie wyłącza aplikacji. Źródła pracy reagują na `HealthChanged`: procesory (COILIX-00026) wstrzymują pobieranie, HTTP (COILIX-00023) odpowiada 503 na nowe żądania, a readiness wyłącza ruch z Kubernetesa. Ten moduł daje tylko sygnał i API, a reakcje dokładają tamte moduły.
- Okresowy wpis `INFO` ze statystykami (heap, non-heap, GC, liczba wątków) co `coilix.diagnostics.memory.summary-interval` (domyślnie 5 min, `0` wyłącza).

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `concurrency` | rdzeń (zmiana) | — | `FatalErrors` (klasyfikacja), `FatalErrorHandler`, `ShutdownFatalErrorHandler`, `ProcessTerminator`, handler w fabrykach wątków, licznik długich zadań w `TaskExecutor` |
| `events` | rdzeń (zmiana) | — | odbiorcy asynchroniczni przepuszczają błąd fatalny do handlera |
| `boot` | rdzeń (zmiana) | — | domyślny handler procesu, ostrzeżenia o flagach JVM |
| `diagnostics` (`09_diagnostics`) | rdzeń | `concurrency`, `events` | `HealthStatus`, `HealthIndicator`, `Health`, `HealthChanged`, monitor wątków z kontrolami, monitor pamięci, `FindingReporter`, ustawienia `coilix.diagnostics` |

`diagnostics` jest w rdzeniu, bo moduły funkcji (procesory, HTTP) muszą znać model zdrowia, a nie mogą zależeć od siebie nawzajem. Nie ma zależności zewnętrznych, korzysta tylko z `java.lang.management`.

## Kroki (osobne PR-y)

1. **Błędy fatalne** (zrobione, ADR 0027). Klasyfikacja, `FatalErrorHandler` z wyłączeniem, strażnik z `halt`, `ProcessTerminator`, handler w wątkach `concurrency` i `events`, domyślny handler w `boot`, ostrzeżenia o flagach JVM. ADR, przewodnik w obu językach, demo (opcja uruchomienia, która wywołuje błąd fatalny w zadaniu).
2. **Moduł `09_diagnostics`: zdrowie i monitor wątków.** Najpierw eksperyment na JDK 17 i 25, co z zakleszczeń widać przy wątkach wirtualnych (`synchronized` i `ReentrantLock`), i wynik w ADR (zrobione: ADR 0039, uruchomione też na JDK 21 i 26). Część 1 zrobiona (ADR 0040): moduł `09_diagnostics` ze zdrowiem, kontrolami wątków platformowych, `FindingReporter` i `on-deadlock`, przewodnik i demo. Część 2: wątki wirtualne (długie zadania wykonawców, zrzut `dumpThreads` na JDK 25+). Potem `HealthStatus`, `HealthIndicator`, `Health`, `HealthChanged`, kontrole wątków, `FindingReporter`, `on-deadlock`. Przewodnik, demo.
3. **Monitor pamięci.** Progi po GC, czas GC, wskaźnik `memory` z histerezą, okresowe statystyki. Przewodnik z zaleceniami flag JVM dla kontenera, demo.

## Kryteria akceptacji

1. `OutOfMemoryError` rzucony w zadaniu `TaskExecutor`, w zadaniu `TaskScheduler` i w odbiorcy asynchronicznym wywołuje `FatalErrorHandler` (test z podmienionym `ProcessTerminator`).
2. Domyślny handler zamyka kontekst (`@PreDestroy` beanów się wykonuje) i woła `exit` z kodem 70. Gdy zamykanie wisi dłużej niż `shutdown-timeout`, woła `halt`.
3. Drugi błąd fatalny w trakcie zamykania jest tylko logowany i nie uruchamia drugiego zamykania.
4. `action = log` tylko loguje i nie kończy procesu.
5. `NonFatal` w zadaniu działa jak dotąd: log i dalsza praca.
6. Testkit nie ustawia domyślnego handlera procesu.
7. Brak `-XX:+ExitOnOutOfMemoryError` daje ostrzeżenie przy starcie, a z flagą ostrzeżenia nie ma.
8. Dwa wątki platformowe zakleszczone na monitorach dają `Finding` `Critical` ze stosami obu wątków, a przy `on-deadlock = shutdown` wywołanie `FatalErrorHandler`. Przy `report` wskaźnik `threads` przechodzi na `Down`.
9. Przekroczenie progu liczby wątków i długie `BLOCKED` dają `Warning`, logowany najwyżej raz na okres ograniczenia.
10. Zadanie `TaskExecutor` działające dłużej niż próg jest zgłaszane z nazwą wątku.
11. Zajętość starej generacji po GC ponad `degraded-percent` przestawia wskaźnik `memory` na `Degraded` i publikuje `HealthChanged`. Powrót do `Up` dopiero poniżej `recovered-percent`.
12. `Health` liczy liveness tylko ze wskaźników `Liveness`, a readiness ze wszystkich.
13. `diagnostics` nie ma zależności zewnętrznych, co sprawdza `checkModuleLayers`.

## Poza zakresem

- Endpointy HTTP zdrowia i metryk. Przyjdą z COILIX-00023 (zdrowie) i z osobnym taskiem metryk (Micrometer albo OpenTelemetry).
- Automatyczne naprawianie zakleszczeń przez przerywanie wątków. Przerwanie losowego wątku zostawia niespójny stan, więc jedyną reakcją jest wyłączenie albo zgłoszenie.
- Łapanie `OutOfMemoryError` i dalsza praca, także trik z rezerwą pamięci zwalnianą przy OOM. Po OOM stan programu jest niewiarygodny.
- Monitor CPU. Wysokie CPU nie jest samo w sobie błędem aplikacji. Wrócimy do niego razem z metrykami.

## Ryzyka i otwarte pytania

1. Wątki wirtualne nie są widoczne w `ThreadMXBean`, a od JDK 24 nie przypinają się do wątku nośnego przy `synchronized`. Wykrycie zakleszczeń między zadaniami wirtualnymi może być niemożliwe przez MXBean. Wtedy zostaje licznik długich zadań w `TaskExecutor` i zrzut przez `jcmd Thread.dump_to_file` w przewodniku. Eksperyment w kroku 2 to rozstrzygnie. Rozstrzygnięte w ADR 0039: MXBean nie widzi wątków wirtualnych na żadnej wersji, więc sygnałem jest licznik długich zadań, a potwierdzeniem zrzut `dumpThreads` JSON z informacją o blokadach (monitory od JDK 25, `java.util.concurrent` od JDK 26).
2. `System.exit` w bibliotece jest kontrowersyjny. Dlatego domyślny handler procesu ustawia tylko `boot` (aplikacja startowana przez Coilix), a `action = log` wyłącza wyłączanie.
3. Progi pamięci zależą od GC (G1, ZGC, Parallel). ZGC generacyjny ma inne pule. Test kontraktowy trzeba uruchomić na G1 i ZGC.
4. Po wyłączeniu przez błąd fatalny Kubernetes restartuje pod. Przy błędzie powtarzalnym pod wpada w `CrashLoopBackOff`, co jest poprawnym, widocznym sygnałem. Przewodnik ma to opisać.
