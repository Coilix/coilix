# COILIX-00031: Runtime na efektach, kontrakty w `IO`

> Decyzja opiekuna z 2026-11-25: kontrakty zwracające `Unit` i implementacja na `synchronized`, `wait`/`notify` i `Atomic*` wyglądają jak Java. Każda aplikacja pracuje na jakimś efekcie, a w aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu. Kierunek zapisuje ADR 0042.

## Cel

- Każdy kontrakt, który runtime Coilix woła w kodzie aplikacji po starcie, zwraca `IO` i istnieje w jednej wersji.
- Runtime jest napisany idiomatycznie: `Ref`, `Deferred`, `Queue`, `Supervisor`, `Resource` zamiast `synchronized`, `wait`/`notify`, `Atomic*` i `Thread.sleep`.
- Kontener (skanowanie, plan, tworzenie beanów) zostaje synchroniczny.

## Zakres

| Moduł | Dziś | Po migracji |
|---|---|---|
| `processing` | `process: Unit`, `poll: List`, `run: Unit`, `ShardMetrics` na `Atomic*`, `InMemoryStream` na `wait`/`notify`, wątki workerów | `process: IO[Unit]`, `poll: IO[List[...]]`, worker jako fiber, statystyki w `Ref`, `InMemoryStream` na `Queue`; menedżer jako fiber z kolejką poleceń zamiast wątku sterującego |
| `events` | odbiorca `Unit`, szyna na `synchronized` i `Atomic*` | odbiorca `IO[Unit]` (metoda `@EventListener` może zwracać `IO`), `publish: IO[Unit]`, odbiorcy asynchroniczni jako fibery pod `Supervisor`, ponowienia przez `IO.sleep` |
| `concurrency` | `TaskExecutor`, `BlockingExecutor`, `TaskScheduler` na `ExecutorService` | `IORuntime` jako jedyny runtime, `@Scheduled` i `@Async` zwracające `IO`, harmonogram jako fibery; `TaskExecutor` zostaje tylko jako most dla bibliotek Javy |
| `diagnostics` | `HealthIndicator.state: HealthState`, `Health` na `AtomicReference` | `state: IO[HealthState]`, `Health` w `Ref`; wątek diagnostyki zostaje platformowy |
| `cats-effect`, `testkit-cats-effect` | moduły funkcji | połączone z `concurrency`, `boot` i `testkit` |

## Kroki (osobne PR-y)

1. ADR 0042 i ten task.
2. `processing` na `IO`: style procesorów, SPI źródeł i wyjść, worker jako fiber, menedżer na kolejce poleceń, statystyki w `Ref`, `InMemoryStream` na `Queue`. ADR o `fs2.Stream` jako źródle. Zastępuje otwarty PR kroku 5 z COILIX-00026.
3. `events` na `IO`.
4. `concurrency`: cats-effect jako zależność rdzenia, `@Scheduled` i `@Async` w `IO`, harmonogram na fiberach.
5. `diagnostics` na `IO`.
6. Połączenie modułu `cats-effect` z rdzeniem (interpreter planu, `CoilixIOApp`, adapter runnerów) i `testkit-cats-effect` z `testkit`. `Coilix.run` przez ten sam runtime.
7. Wart, który zabrania `synchronized`, `wait`/`notify` i `Atomic*` w modułach runtime'u (COILIX-00028), z wyjątkami dla wątków niezależnych od puli.

Każdy krok aktualizuje przewodniki w obu językach, demo i ADR-y, które zastępuje.

## Kryteria akceptacji

1. Żaden publiczny kontrakt runtime'u wołany w kodzie aplikacji nie zwraca `Unit` ani `Future`.
2. W modułach runtime'u nie ma `synchronized`, `wait`, `notify`, `Atomic*` ani `Thread.sleep` poza udokumentowanymi wyjątkami.
3. Demo i przykłady z przewodników nie wołają `unsafeRun*`.
4. Aplikacja na `Future` działa przez `IO.fromFuture`, co pokazuje przewodnik.

## Ryzyka i otwarte pytania

1. Pierwsza bariera dla użytkownika bez doświadczenia z cats-effect. Przewodnik „Pierwsza aplikacja” musi pokazać `IO` od pierwszej strony.
2. `fs2.Stream` jako źródło procesorów daje backpressure i gotowe konektory, ale dokłada zależność. Decyzja w kroku 2.
3. Biblioteki Javy, które wymagają `Executor` albo wołają callbacki na własnych wątkach, potrzebują mostu (`IO.async`, `Dispatcher`).
