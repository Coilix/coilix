# COILIX-00031: Runtime na `IO`, komponenty aplikacji w tagless final

> Decyzje opiekuna z 2026-11-25: kontrakty zwracające `Unit` i implementacja na `synchronized`, `wait`/`notify` i `Atomic*` wyglądają jak Java. Każda aplikacja pracuje na jakimś efekcie, a w aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu. Wybrany wariant: aplikacja i framework na `IO`, komponenty aplikacji mogą być w tagless final. Kierunek zapisuje ADR 0042.

## Cel

- Każdy kontrakt, który runtime Coilix woła w kodzie aplikacji po starcie, zwraca `IO` i istnieje w jednej wersji.
- Komponent aplikacji napisany w `F[_]` (`final class OrderService[F[_]: Async](repo: OrderRepository[F])`) jest beanem `OrderService[IO]` bez dodatkowej konfiguracji, a instancje klas typów (`Async[IO]`) są beanami.
- Runtime jest napisany idiomatycznie: `Ref`, `Deferred`, `Queue`, `Supervisor`, `Resource` zamiast `synchronized`, `wait`/`notify`, `Atomic*` i `Thread.sleep`.
- Kontener (skanowanie, plan, tworzenie beanów) zostaje synchroniczny.

## Zakres

| Moduł | Dziś | Po migracji |
|---|---|---|
| `context`, `boot` | niezwiązane `F[_]` w zależności to błąd | `F[_]` komponentów i metod `@Bean` wiązane z `IO`, `Async[IO]` jako bean, dopasowanie klas typów po nadtypach |
| `processing` | `process: Unit`, `poll: List`, `run: Unit`, `ShardMetrics` na `Atomic*`, `InMemoryStream` na `wait`/`notify`, wątki workerów | `process: IO[Unit]`, `poll: IO[List[...]]`, worker jako fiber, statystyki w `Ref`, `InMemoryStream` na `Queue`; menedżer jako fiber z kolejką poleceń zamiast wątku sterującego |
| `events` | odbiorca `Unit`, szyna na `synchronized` i `Atomic*` | odbiorca `IO[Unit]` (metoda `@EventListener` zwraca `IO[Unit]`), `publish: IO[Unit]`, odbiorcy asynchroniczni jako fibery pod `Supervisor`, ponowienia przez `IO.sleep` |
| `concurrency` | `TaskExecutor`, `BlockingExecutor`, `TaskScheduler` na `ExecutorService` | `IORuntime` jako jedyny runtime, `@Scheduled` i `@Async` zwracające `IO`, harmonogram jako fibery; `TaskExecutor` zostaje tylko jako most dla bibliotek Javy |
| `diagnostics` | `HealthIndicator.state: HealthState`, `Health` na `AtomicReference` | `state: IO[HealthState]`, `Health` w `Ref`; wątek diagnostyki zostaje platformowy |
| `cats-effect`, `testkit-cats-effect` | moduły funkcji | połączone z `concurrency`, `boot` i `testkit` |

## Kroki (osobne PR-y)

1. ADR 0042 i ten task.
2. Kontener: wiązanie niezwiązanych `F[_]` klas komponentów i metod `@Bean` z `IO`, `Async[IO]` jako bean, dopasowanie klas typów po nadtypach (`Sync[IO]` znajduje `Async[IO]`), czytelny błąd dla parametrów innego kształtu (`F[_, _]`) albo kilku `F`. Przewodnik o komponentach w tagless final.
3. `processing` na `IO`: style procesorów, SPI źródeł i wyjść, worker jako fiber, menedżer na kolejce poleceń, statystyki w `Ref`, `InMemoryStream` na `Queue`. ADR o `fs2.Stream` jako źródle. Zastępuje otwarty PR kroku 5 z COILIX-00026.
4. `events` na `IO`.
5. `concurrency`: cats-effect jako zależność rdzenia, `@Scheduled` i `@Async` w `IO`, harmonogram na fiberach.
6. `diagnostics` na `IO`.
7. Połączenie modułu `cats-effect` z rdzeniem (interpreter planu, `CoilixIOApp`, adapter runnerów) i `testkit-cats-effect` z `testkit`. `Coilix.run` przez ten sam runtime. Pomysły z `OptimusIOApp` bulldozera (koncepcje, nie kod): wykrywanie zablokowanych wątków `IORuntime` (`blockedThreadDetectionEnabled`), okresowy log wątków nie-demonów, czytelny log głodzenia CPU (`onCpuStarvationWarn`) i `reportFailure` przez logger Coilix. Połączenie zrobione przed krokiem 3, bo `processing` (moduł funkcji) nie może zależeć od modułu `cats-effect`: runtime, interpreter, `Async[IO]` i efekt `IO` w `concurrency` (`CatsEffectConfiguration`), `CoilixIOApp`, `CoilixIOStartup`, `IOApplicationRunner` i `ioRunnerAdapter` (`BootModule`) w `boot`, `CoilixIOSuite` i `CoilixIOTestSetup` w `testkit`; `ioTaskExecutor` jest jedynym `TaskExecutor` (domyślny `ForkJoinPool` usunięty), klucze `coilix.cats-effect.*` zostają. `Coilix.run` usunięte na rzecz jednego wejścia `CoilixIOApp` (ADR 0045). Pomysły z `OptimusIOApp` zrobione osobno. `ContextLifecycleListener` w `IO`, start i zamykanie kontekstu w `Resource` aplikacji bez `unsafeRunSync` (ADR 0047). Beany z efektów, builder, bootstrap, runnery i testkit w `Resource` i `IO`, a `unsafeRunSync` tylko w `SynchronousLifecycle` dla kodu synchronicznego; demo i przykłady bez `unsafeRun*` (ADR 0049).
8. Wart, który zabrania `synchronized`, `wait`/`notify` i `Atomic*` w modułach runtime'u (COILIX-00028), z wyjątkami dla wątków niezależnych od puli.

9. Moduły kontenera, których migracja nie przepisuje (`common`, `config`, `context`, `boot`, `testkit`): zamiana klas (także `final case class`) na `case class` z konstruktorem szczegółowym i `apply` w obiekcie towarzyszącym według zasady z `CLAUDE.md`.

10. Błędy typowane: przewidywalne błędy jako typ warstwy w `Either` i `EitherT`, rzucenie albo kod wyjścia w warstwie najwyższej. Kontener (`ContextError`) i start (`StartupError`) zrobione (ADR 0050), konfiguracja (`ConfigError`) zrobiona (ADR 0051), procesory tła (`ProcessingSetupError`, `ProcessorError`) i ustawienia logowania zrobione (ADR 0052); dalej `events`.

Każdy krok aktualizuje przewodniki w obu językach, demo i ADR-y, które zastępuje, a przepisywane klasy od razu są `case class` według zasady z `CLAUDE.md`.

## Kryteria akceptacji

1. Żaden publiczny kontrakt runtime'u wołany w kodzie aplikacji nie zwraca `Unit` ani `Future`; zwraca `IO`.
2. Komponent `final class OrderService[F[_]: Async](repo: OrderRepository[F])` jest beanem `OrderService[IO]` bez dodatkowej konfiguracji, a procesor może go wstrzyknąć jako `OrderService[IO]`.
3. W modułach runtime'u nie ma `synchronized`, `wait`, `notify`, `Atomic*` ani `Thread.sleep` poza udokumentowanymi wyjątkami.
4. Demo i przykłady z przewodników nie wołają `unsafeRun*`.
5. Aplikacja z kodem na `Future` działa przez `IO.fromFuture`, co pokazuje przewodnik.

## Ryzyka i otwarte pytania

1. Pierwsza bariera dla użytkownika bez doświadczenia z cats-effect. Przewodnik „Pierwsza aplikacja” musi pokazać `IO` od pierwszej strony, a osobny przewodnik komponenty w tagless final.
2. Wiązanie dotyczy parametrów typu wyższego rzędu (`F[_]`). Komponent z kilkoma takimi parametrami albo z parametrem innego kształtu (`F[_, _]`) to błąd startu z wyjaśnieniem.
3. `fs2.Stream` jako źródło procesorów daje backpressure i gotowe konektory, ale dokłada zależność. Decyzja w kroku 3.
4. Biblioteki Javy, które wymagają `Executor` albo wołają callbacki na własnych wątkach, potrzebują mostu (`IO.async`, `Dispatcher`).
