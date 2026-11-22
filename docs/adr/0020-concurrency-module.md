# 0020: Moduł `concurrency` z wykonawcą na wątkach wirtualnych

- Status: proponowana, wykonawca domyślny zastąpiony przez ADR 0028
- Data: 2026-11-19
- Task: COILIX-00005 (kroki 1 i 3), COILIX-00020 (krok 4)

## Kontekst

Beany aplikacji tworzyły własne pule wątków (`Executors.newFixedThreadPool` w `@PostConstruct`) i same musiały je zamykać. COILIX-00005 przewiduje pule i harmonogram jako beany zamykane z kontekstem, a COILIX-00020 potrzebuje puli blokującej na wątkach wirtualnych i schedulera dla `IORuntime`. Klaster (COILIX-00027) potrzebuje schedulera do heartbeatu, a `@Scheduled` z COILIX-00005 ma korzystać z klastra przy zadaniach na jednej replice.

Artefakty są kompilowane dla JDK 17 (COILIX-00009), a wątki wirtualne są w JDK od wersji 21.

## Decyzja

- Nowy moduł rdzenia `concurrency` (`06_concurrency`, pakiet `dev.coilix.concurrency`, artefakt `coilix-concurrency`) zależy od `context`. `boot` zależy od niego, więc każda aplikacja go ma. Moduł podłącza się przez `CoilixModule` (`ConcurrencyModule`), więc można go wyłączyć kluczem `coilix.modules.concurrency.enabled = false`.
- `concurrency` dostaje `06`, a `cluster` przesuwa się na `07`. Klaster korzysta z harmonogramu, więc wątki są niżej. Zadania `@Scheduled` na jednej replice wpina moduł klastra, a nie odwrotnie.
- `TaskExecutor` (`execute`, `submit` z `Future`, `executionContext`) i `TaskScheduler` (`schedule`, `scheduleAtFixedRate`, `scheduleWithFixedDelay`, uchwyt `ScheduledTask`) to beany, a nie komponenty frameworka z `ConfigPath`. Aplikacja podmienia wykonawcę własnym beanem `@Primary`, a rozmiary i nazwy wątków ustawia w `coilix.concurrency`.
- Domyślny `taskExecutor` uruchamia każde zadanie na nowym wątku wirtualnym (`coilix-task-<n>`). Wątki wirtualne są tworzone przez refleksję (`Thread.ofVirtual`), więc kod kompiluje się dla JDK 17, a na JDK 17 działa na nowych wątkach platformowych. `virtual-threads = false` daje stałą pulę wątków platformowych.
- `taskScheduler` ma pulę wątków platformowych (`coilix-scheduler-<n>`, domyślnie jeden). Błąd jednego uruchomienia zadania okresowego jest logowany i nie zatrzymuje kolejnych.
- Zamknięcie kontekstu zamyka oba beany przez `@PreDestroy`, po beanach, które z nich korzystają. Zamykanie czeka do `shutdown-timeout` na trwające zadania, potem je przerywa i loguje liczbę niedokończonych. Harmonogram porzuca zadania, które jeszcze nie wystartowały.
- `@Scheduled(fixedRate | fixedDelay, initialDelay)` z `02_annotations` przyjmuje czasy jako tekst, rozwiązywany jak `@Value` (`"10s"`, `"${shop.refresh:1m}"`). Metody znajduje `ScheduledMethodRegistrar` (bean modułu), także prywatne i z nadklas, a błędna metoda to `InvalidScheduledMethodException` przy starcie.
- Rejestrator potrzebuje wszystkich beanów, więc rdzeń kontekstu dostaje `ContextLifecycleListener`: `onContextStarted` po utworzeniu wszystkich beanów (wyjątek zamyka kontekst i przerywa start) i `onContextClosing` przed pierwszym `@PreDestroy`. Dzięki temu zadania `@Scheduled` są anulowane, zanim zniszczone zostaną beany, których metody wywołują.
- Ustawienia (`ConcurrencySettings`) są czytane z `Option[Environment]`, więc moduł działa też w kontekście bez środowiska, z wartościami domyślnymi.

## Konsekwencje

- Rdzeń ma nowy publikowany artefakt w grupie `core`.
- Każdy kontekst z `boot` ma beany `concurrencySettings`, `taskExecutor` i `taskScheduler`. Testy, które sprawdzają pełną listę beanów, muszą je uwzględnić.
- `@Async` jest opisane w ADR 0022, a przekazywanie MDC to ostatni krok COILIX-00005.
- Bean `TaskExecutor` z aplikacji bez `@Primary` jest drugim kandydatem obok domyślnego, więc wstrzyknięcie `TaskExecutor` kończy się `AmbiguousBeanException`. Komunikat podpowiada `@Primary`.
