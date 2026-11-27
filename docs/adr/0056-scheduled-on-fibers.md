# 0056: `@Scheduled` na fiberach, bez `TaskScheduler`

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 5, część 1)
- Zastępuje część: 0020 i 0022 (harmonogram na `ScheduledThreadPoolExecutor`, bean `taskScheduler`, klucze `coilix.concurrency.scheduler`)

## Kontekst

Metody `@Scheduled` działały na puli `coilix-scheduler-*` (`ScheduledExecutorTaskScheduler`), zwracały `Unit`, a ich zadania trzymał `AtomicReference`. Kod aplikacji jest w `IO`, więc metoda, która chciała wykonać efekt, musiała go uruchamiać poza fiberem albo przekazywać pracę dalej. Każda metoda zajmowała też wątek puli na czas działania, a pula liczyła jeden wątek domyślnie. Bean `TaskScheduler` służył jeszcze do harmonogramu z kodu, ale w `IO` to samo daje `IO.sleep` albo strumień fs2 (`Stream.fixedDelay`, `Stream.awakeEvery`) na fiberze.

## Decyzja

- **Metoda `@Scheduled` zwraca `IO[Unit]` i działa na własnym fiberze.** `ScheduledMethodRegistrar` uruchamia dla każdej metody pętlę: `fixedDelay` to uruchomienie i `IO.sleep(delay)`, `fixedRate` liczy następny start od poprzedniego (`IO.monotonic`), a spóźnione uruchomienie idzie od razu, bez nadrabiania serii. Uruchomienia jednej metody nie nachodzą na siebie, a nieudane jest zapisywane w logu i harmonogram trwa dalej.
- **Stan w `Ref`, bean bez efektu.** Rejestrator trzyma fibery w `Ref.unsafe`, który tylko alokuje komórkę i nie uruchamia efektu, więc zostaje zwykłym beanem. Dzięki temu kontekst z `ConcurrencyModule` nie wymaga interpretera efektów. Fibery anuluje faza `Producers`, przed zniszczeniem beanów.
- **`TaskScheduler` usunięty.** Znikają `TaskScheduler`, `ScheduledTask`, `ScheduledExecutorTaskScheduler`, `FutureScheduledTask`, `SchedulerSettings` i klucze `coilix.concurrency.scheduler.*`. Harmonogram z kodu to strumień fs2 na fiberze uruchamianym przez `ContextLifecycleListener` (przykład `StockRefresher`, w demo `ClaimSweeper`), a ponowienie z `RetryPolicy` to `IO.sleep(delay)`.
- **`TaskExecutor` i `BlockingExecutor` zostają** jako most dla bibliotek Javy i kodu, który potrzebuje `Executor` albo `Future`. `@Async` przechodzi na `IO` w osobnym kroku.

## Konsekwencje

- Metoda `@Scheduled` nie zajmuje wątku między uruchomieniami i nie konkuruje z innymi metodami o jeden wątek harmonogramu.
- Zmiana niezgodna: metoda `@Scheduled` zwraca `IO[Unit]` (wywołanie, które czeka, w `IO.blocking`), a kod korzystający z `TaskScheduler` przechodzi na fiber albo strumień fs2. Konfiguracja `coilix.concurrency.scheduler` nie jest już czytana.
- Błąd fatalny w metodzie `@Scheduled` obsługuje runtime cats-effect, tak jak w każdym fiberze.
