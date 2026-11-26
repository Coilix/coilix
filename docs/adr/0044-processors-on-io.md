# 0044: Procesory tła na IO i fiberach

- Status: proponowana, uzupełniona przez [0048](0048-stream-processors-with-draining.md) (styl strumieniowy i `drain-timeout`)
- Data: 2026-11-26
- Task: COILIX-00032 (krok 4, część 1), COILIX-00031 (krok 3)

## Kontekst

ADR 0037 dał procesorom dwa style na wątkach: `BlockingProcessor` (pętla na wątku wirtualnym, zatrzymanie flagą i przerwaniem wątku) i `CustomProcessor` (praca na własnych wątkach z `Execution.completion: Future[Unit]`). Menedżer wykonywał operacje na wątku `coilix-processor-manager`, stan trzymał w `AtomicReference`, a restarty planował przez `TaskScheduler`. ADR 0042 i 0043 przenoszą runtime na `IO`, `Resource` i fs2, więc procesory przechodzą na ten sam model. Model nadzoru (`ShardSupervision`, `RestartPolicy`, sloty i przydział shardów) jest czystymi danymi i zostaje bez zmian.

## Decyzja

- **Jeden styl na `IO` zamiast dwóch na wątkach.** `IOProcessor.run(shard): IO[Unit]` zastępuje `BlockingProcessor`, `CustomProcessor` i `Execution`. `prepare`, `onAcquired` i `onReleased` zwracają `IO[Unit]`. Kod blokujący idzie przez `IO.blocking` albo `IO.onBlockingExecutor`, a praca na własnych wątkach przez `IO.async` albo `IO.fromFuture`. Styl strumieniowy z opróżnianiem (ADR 0043) dochodzi w drugiej części kroku jako osobny styl na tym samym silniku.
- **`Shard` z sygnałem zatrzymania.** `Shard` jest case class z `Deferred[IO, Unit]`. `stopRequested: IO[Boolean]` sprawdza go między krokami, a `stopped: IO[Unit]` czeka na niego, np. jako sygnał dla `interruptWhen` w strumieniu. Każdy start workera dostaje nowy `Shard` (`Shard.create`).
- **`FiberEngine` zamiast `VirtualThreadEngine`.** Bean `fiberEngine` (z `Resource`) uruchamia każdy shard jako fiber pod `Supervisor`. Zatrzymanie: sygnał, połowa limitu na samodzielne zakończenie, anulowanie, druga połowa na finalizery, potem porzucenie z ostrzeżeniem. Anulowanie czeka w osobnym fiberze, więc praca w obszarze nieanulowalnym nie blokuje menedżera. SPI `ProcessorEngine`, `RunningWorker` i `WorkerEvents` zwraca `IO`.
- **Menedżer na prymitywach cats-effect.** `DefaultProcessorManager` jest beanem z `Resource`: procesory tworzy przy akwizycji i zamyka przy zwolnieniu. Operacje idą po kolei przez `ControlLoop` (kolejka `IO` przetwarzana przez jeden fiber), stan shardów jest w `Ref`, a restart po opóźnieniu to fiber pod `Supervisor` zamiast zadania `TaskScheduler`. Wywołanie z samej pętli, np. z `onReleased`, wykonuje się od razu; pętla rozpoznaje je po `IOLocal`, a fibery startowane z pętli (workery, restarty) dostają je wyzerowane.
- **API `ProcessorManager` w `IO`.** `status`, `start`, `stop`, `restart` i `stopAll` zwracają `IO`. Nieznany identyfikator i cudzy shard to błąd w `IO` (`NoSuchProcessorException`, `ShardNotOwnedException`). `processors` i `topology` zostają wartościami, bo nie zmieniają się po starcie.
- **Granica z kontenerem.** Start procesorów po starcie kontekstu i zatrzymanie przy zamykaniu są metodami w `IO` (`startEnabled`, `stopForShutdown`). Dopóki `ContextLifecycleListener` jest synchroniczny, menedżer uruchamia je przez `unsafeRunSync` na `IORuntime` aplikacji. Kolejny krok przenosi start aplikacji na `IOApp` i odbiorców cyklu życia na `IO`, co usuwa tę granicę.

## Konsekwencje

- Częściowo zastąpione: ADR 0037 w punktach o stylach kroku 4, silniku `VirtualThreadEngine` i wątku menedżera.
- Nazwy wątków `coilix-processor-<nazwa>-<shard>` i MDC `processor`/`shard` znikają; shard jest w logach przez swoją nazwę (`invoice-export#2`). Przenoszenie MDC na fibery czeka na most kontekstu dla `IO` (COILIX-00020).
- Błąd startu menedżera (np. dwa procesory z jednym identyfikatorem) wychodzi z kontekstu bez opakowania w `BeanCreationException`, tak jak błędy innych beanów z efektem.
- Flow z bulldozera przenosi się jako `IOProcessor` od razu, a z opróżnianiem strumienia po drugiej części kroku.
