# 0029: Przekazywanie kontekstu wątku do zadań

- Status: proponowana
- Data: 2026-11-22
- Task: COILIX-00005 (krok 4)

## Kontekst

Zadanie przekazane wykonawcy działa na innym wątku, więc traci wartości związane z wątkiem wywołującym: MDC logowania (identyfikator żądania, zlecenia), a w przyszłości tenant, kontekst bezpieczeństwa albo śledzenie. Po ADR 0028 zadania trafiają do dwóch wykonawców i harmonogramu, a także do `@Async`, asynchronicznych odbiorców zdarzeń i kombinatorów `Future` na `executionContext`. Przekazywanie trzeba zrobić raz, w miejscu, przez które przechodzi każda z tych dróg.

## Decyzja

- Punkt rozszerzeń `ContextPropagator` z `capture(): ContextSnapshot`, a `ContextSnapshot.runWith(task)` przywraca kontekst wokół zadania i oddaje wątkowi poprzedni, także po błędzie. Propagatory to beany: wykonawcy i harmonogram dostają `List[ContextPropagator]` i składają je w kolejności rejestracji, pierwszy najbardziej na zewnątrz.
- `MdcContextPropagator` jest beanem `mdcContextPropagator` modułu `concurrency`, wyłączanym przez `coilix.concurrency.propagate-mdc = false` (`@ConditionalOnProperty`). Kopiuje mapę MDC przy przekazaniu i po zadaniu przywraca poprzednią albo czyści MDC, więc wątek z puli nie przenosi MDC do następnego zadania.
- W wykonawcach przekazywanie robi `PropagatingExecutorService`, dekorator `ExecutorService` pod `execute`, `submit` i `executionContext`. Bez propagatorów dekorator nie powstaje. Dekorator woła `execute` pod spodem z tego samego wątku, więc zadanie przekazane przez wątek roboczy `ForkJoinPool` nadal trafia do jego kolejki lokalnej (ADR 0028). Harmonogram pobiera kontekst przy zaplanowaniu i przywraca go przy każdym uruchomieniu.
- Demo i testy `concurrency` używają Log4j 2 zamiast `slf4j-simple`, bo `slf4j-simple` ma `NOPMDCAdapter` i MDC niczego w nim nie przechowuje. Log4j ma `shutdownHook="disable"`, bo jego własny hook zamykania wyłączał logowanie, gdy Coilix jeszcze zamykał aplikację.

## Konsekwencje

- Każda droga zadania (oba wykonawcy, harmonogram, `@Async`, odbiorcy asynchroniczni, `Future` na `executionContext`) zachowuje MDC bez kodu w aplikacji.
- Moduły integracji, które dostarczają własny `TaskExecutor` (cats-effect na `runtime.compute`), powinny przyjąć te same propagatory. Dla fiberów `IO` właściwym mechanizmem jest `IOLocal`, a most z MDC opisze COILIX-00020.
- Przekazanie kosztuje kopię mapy MDC na zadanie. Kto tego nie chce, wyłącza propagację kluczem.
- W testach z Log4j SLF4J czasem wypisuje ostrzeżenie o loggerach zastępczych, gdy równoległe suite'y logują w trakcie startu Log4j. Gubi się wtedy tylko kilka pierwszych wpisów testów.
