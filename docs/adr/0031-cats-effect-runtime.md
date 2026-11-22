# 0031: Runtime cats-effect jako jedyna pula obliczeniowa

- Status: proponowana
- Data: 2026-11-23
- Task: COILIX-00020 (krok 5a)

## Kontekst

ADR 0028 ustalił, że aplikacja ma jedną pulę obliczeniową, a z cats-effect jest nią pula work-stealing `IORuntime`, której nie wolno zastępować obcą pulą. Moduł cats-effect ma więc zbudować `IORuntime`, podłączyć go jako `TaskExecutor` i zamknąć razem z kontekstem. To pierwszy krok modułu; beany `IO` i `Resource`, `CoilixIOApp` i testkit przychodzą w krokach 5b i 5c.

## Eksperymenty (2026-11-23, cats-effect 3.7.1, JDK 25)

- `IORuntime.createWorkStealingComputeThreadPool(threads = n)` zwraca pulę, API odpytywania I/O i funkcję zamknięcia. `IORuntime.builder().setCompute(pool, shutdown).addPoller(poller, ...)` daje runtime, w którym ta sama `WorkStealingThreadPool` jest pulą i schedulerem, `IO.sleep` działa, `IO.blocking` blokuje w miejscu (`io-compute-blocker-*`), a pracuje dokładnie `n` wątków. Builder nie ma osobnego ustawienia liczby wątków.
- Błąd fatalny w fiberze zamyka wszystkie runtime'y JVM (`IORuntime.allRuntimes`) i woła ich hooki z `addShutdownHook`. Testy innych suite'ów w tej samej JVM wiszą wtedy na `unsafeRunSync` martwego runtime'u.

## Decyzja

- Nowy moduł funkcji `23_cats_effect` (`coilix-cats-effect`, pakiet `dev.coilix.catseffect`, grupa wydań `cats-effect`), zależny od `concurrency`. Rdzeń nie zależy od cats-effect.
- Bean `coilixIORuntime` (`CoilixIORuntime`) tworzy runtime jak w eksperymencie, z `coilix.concurrency.compute.threads` wątkami `io-compute-*`, i zamyka go w `@PreDestroy`. Bean `ioRuntime` wystawia sam `IORuntime` do wstrzykiwania i `using`.
- Bean `ioTaskExecutor` (`IORuntimeTaskExecutor`) to `TaskExecutor` na `runtime.compute`, z propagatorami kontekstu (ADR 0029) i `FatalErrors.guarded` dla `execute`. Jako dostawca `TaskExecutor` wyłącza domyślny `taskExecutor` z `@ConditionalOnMissingBean` (ADR 0028), więc wątki `coilix-compute-*` nie powstają. Wykonawca nie zamyka puli, bo należy do runtime'u.
- Zamknięcie runtime'u, którego nie zlecił kontekst, czyli zamknięcie po błędzie fatalnym w fiberze, trafia do `FatalErrorHandler` jako `IORuntimeStoppedException` (`VirtualMachineError`, więc fatalny). Zamknięcie przez kontekst ustawia znacznik przed `shutdown`, więc nie jest zgłaszane.
- `CoilixIO.onBlockingExecutor` (`IO.blocking(...).evalOn(blockingExecutor.executionContext)`) dla masowego czekania na wątkach wirtualnych. `IO.blocking` zostaje mechanizmem cats-effect.
- Testy modułu nie biegną równolegle (`Test / parallelExecution := false`), bo test błędu fatalnego w fiberze zatrzymuje wszystkie runtime'y JVM.

## Konsekwencje

- Aplikacja z modułem ma jedną pulę CPU `io-compute-*` dla `IO`, `@Async`, odbiorców zdarzeń i zadań. Pekko (COILIX-00029) weźmie ją przez `TaskExecutor.executionContext`.
- Wątki fiberów nie dziedziczą MDC; propagatory działają dla zadań Coilix. Most MDC z `IOLocal` to osobny temat.
- `IO` uruchamia się na razie na brzegu kodu (`unsafeRunSync` w runnerze albo metodzie). Beany zwracające `IO` i `Resource` i start przez `IOApp` dojdą w krokach 5b i 5c.
