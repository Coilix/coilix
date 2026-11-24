# 0034: CoilixIOApp z runtime'em IOApp jako beanem

- Status: proponowana
- Data: 2026-11-24
- Task: COILIX-00020 (krok 5c, część 1)

## Kontekst

Decyzje opiekuna z 2026-11-23: oba tryby startu, `CoilixIOApp` (`IOApp`, anulowanie zamyka kontekst) obok `Coilix.run`; `CoilixIOApp` po runnerach czeka na anulowanie, a `coilix.cats-effect.exit-after-runners = true` kończy aplikację po runnerach. Specyfikacja mówi, że w tym trybie runtime należy do `IOApp`. Z ADR 0031 wynika, że pula obliczeniowa cats-effect ma być jedyną pulą obliczeniową aplikacji, a dotąd moduł tworzył własny runtime w beanie `coilixIORuntime`.

`IOApp.main` jest `final` i wybiera runtime, zanim wywoła `run(args)`, więc runtime powstaje przed wczytaniem konfiguracji z argumentami wiersza poleceń.

## Decyzja

- **Runtime należy do `IOApp`.** `CoilixIOApp` nadpisuje `computeWorkerThreadCount` wartością `coilix.concurrency.compute.threads` z konfiguracji wczytanej bez argumentów (pliki, właściwości systemowe, zmienne środowiskowe). Runtime `IOApp` trafia do kontekstu jako gotowy bean `ioRuntime`. `coilixIORuntime` i `ioRuntime` w `CatsEffectConfiguration` mają `@ConditionalOnMissingBean(IORuntime)`, a `ioTaskExecutor` i `ioRunnerAdapter` biorą `IORuntime`, nie właściciela, więc w aplikacji jest nadal jedna pula.
- **Start w `boot` bez runnerów.** `CoilixApplicationRunner.start(args, singletons)` robi to samo co `run` aż do zalogowania czasu startu i zwraca `StartedApplication(context, arguments)`, bez hooka zamknięcia, runnerów i ich zdarzeń. `OrderedRunner.all(context)` daje uporządkowaną listę runnerów (używa jej też `OrderedRunnerInvoker`), a `StartupEvents.ready`/`started`/`failure` publikują zdarzenia startu pojedynczo.
- **`CoilixIOStartup`** w module: `application[T](args, runtime)` to `Resource[IO, StartedApplication]` (budowa w `IO.blocking`, zamknięcie kontekstu w finalizerze), `runners(application)` uruchamia runnery między `ContextReady` i `ApplicationStarted` (`IOApplicationRunner` natywnie, pozostałe w `IO.blocking`, błąd publikowany jako `ApplicationFailed`), `run[T](args, runtime)` składa to z oczekiwaniem (`IO.never` albo koniec przy `exit-after-runners`). `CoilixIOApp.run` deleguje do `CoilixIOStartup.run` z runtime'em `IOApp`.
- **Bez hooka Coilix.** Zamknięcie przy `SIGTERM` robi hook `IOApp`, który anuluje główny fiber, co zwalnia `Resource` kontekstu. Kod wyjścia po błędzie ustala `IOApp` (1).

## Konsekwencje

- `--coilix.concurrency.compute.threads` z wiersza poleceń nie zmienia liczby wątków w `CoilixIOApp`. Opisane w przewodniku i Scaladoc.
- Budowa kontekstu jest nadal synchroniczna w `IO.blocking`, a interpreter beanów efektowych otwiera je jednym `unsafeRunSync` na tym samym runtime (z wątku blokującego). Przeniesienie interpretera bezpośrednio do `Resource` aplikacji wymagałoby buildera w `IO`; do zrobienia, gdy pojawi się potrzeba anulowania w trakcie startu.
- W trybie `CoilixIOApp` nie działa domyślny handler wyjątków procesu z `RuntimeShutdownHookRegistrar`; błędy fatalne w fiberach obsługuje `IOApp`, a w zadaniach `TaskExecutor` nadal `FatalErrorHandler`.
- Demo startuje jako `CoilixIOApp` z `exit-after-runners: true`.
- Krok 5c, część 2: moduł `testkit-cats-effect` (`useIO`, `CoilixIOSuite`).
