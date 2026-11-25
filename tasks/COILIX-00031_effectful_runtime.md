# COILIX-00031: Runtime na efektach w stylu tagless final

> Decyzja opiekuna z 2026-11-25: kontrakty zwracające `Unit` i implementacja na `synchronized`, `wait`/`notify` i `Atomic*` wyglądają jak Java. Każda aplikacja pracuje na jakimś efekcie, a w aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu. Opiekun chce „Springa na `IO`” w stylu tagless final. Kierunek zapisuje ADR 0042.

## Cel

- Każdy kontrakt, który runtime Coilix woła w kodzie aplikacji po starcie, zwraca `F[_]` z wymaganiami w klasach typów cats-effect i istnieje w jednej wersji.
- Aplikacja wybiera efekt raz (`object Shop extends CoilixApp[IO]`), a kontener wiąże z nim każde niezwiązane `F[_]` komponentów i dostarcza instancje klas typów (`Async[IO]`) jako beany.
- Runtime jest napisany idiomatycznie i polimorficznie w `F`: `Ref`, `Deferred`, `Queue`, `Supervisor`, `Resource` zamiast `synchronized`, `wait`/`notify`, `Atomic*` i `Thread.sleep`.
- Kontener (skanowanie, plan, tworzenie beanów) zostaje synchroniczny.

## Zakres

| Moduł | Dziś | Po migracji |
|---|---|---|
| `context`, `boot` | niezwiązane `F[_]` w zależności to błąd | `F[_]` komponentów i metod `@Bean` wiązane z efektem aplikacji, `Async[F]` efektu jako bean, `CoilixApp[F]` |
| `processing` | `process: Unit`, `poll: List`, `run: Unit`, `ShardMetrics` na `Atomic*`, `InMemoryStream` na `wait`/`notify`, wątki workerów | `StatelessProcessor[F, T]` z `process: F[Unit]`, `poll: F[List[...]]`, worker jako fiber, statystyki w `Ref[F, _]`, `InMemoryStream` na `Queue`; menedżer jako fiber z kolejką poleceń zamiast wątku sterującego |
| `events` | odbiorca `Unit`, szyna na `synchronized` i `Atomic*` | odbiorca `F[Unit]` (metoda `@EventListener` zwraca `F[Unit]`), `EventPublisher[F]` z `publish: F[Unit]`, odbiorcy asynchroniczni jako fibery pod `Supervisor`, ponowienia przez `Temporal[F].sleep` |
| `concurrency` | `TaskExecutor`, `BlockingExecutor`, `TaskScheduler` na `ExecutorService` | `IORuntime` jako jedyny runtime, `@Scheduled` i `@Async` zwracające `F`, harmonogram jako fibery; `TaskExecutor` zostaje tylko jako most dla bibliotek Javy |
| `diagnostics` | `HealthIndicator.state: HealthState`, `Health` na `AtomicReference` | `HealthIndicator[F]` ze `state: F[HealthState]`, `Health[F]` w `Ref`; wątek diagnostyki zostaje platformowy |
| `cats-effect`, `testkit-cats-effect` | moduły funkcji | połączone z `concurrency`, `boot` i `testkit` |

## Kroki (osobne PR-y)

1. ADR 0042 i ten task.
2. Kontener: `CoilixApp[F]` z efektem aplikacji czytanym z typu obiektu aplikacji, wiązanie niezwiązanych `F[_]` klas komponentów i metod `@Bean` z tym efektem, instancja `Async[F]` jako bean, dopasowanie klas typów po nadtypach (`Sync[IO]` znajduje `Async[IO]`). Przewodnik o pisaniu komponentów w `F`.
3. `processing` w `F`: style procesorów, SPI źródeł i wyjść, worker jako fiber, menedżer na kolejce poleceń, statystyki w `Ref`, `InMemoryStream` na `Queue`. ADR o `fs2.Stream` jako źródle. Zastępuje otwarty PR kroku 5 z COILIX-00026.
4. `events` w `F`.
5. `concurrency`: cats-effect jako zależność rdzenia, `@Scheduled` i `@Async` w `F`, harmonogram na fiberach.
6. `diagnostics` w `F`.
7. Połączenie modułu `cats-effect` z rdzeniem (interpreter planu, `CoilixApp`, adapter runnerów) i `testkit-cats-effect` z `testkit`. `Coilix.run` przez ten sam runtime.
8. Wart, który zabrania `synchronized`, `wait`/`notify` i `Atomic*` w modułach runtime'u (COILIX-00028), z wyjątkami dla wątków niezależnych od puli.

Każdy krok aktualizuje przewodniki w obu językach, demo i ADR-y, które zastępuje.

## Kryteria akceptacji

1. Żaden publiczny kontrakt runtime'u wołany w kodzie aplikacji nie zwraca `Unit`, `Future` ani konkretnego `IO`; zwraca `F[_]`.
2. Komponent `final class OrderService[F[_]: Async](repo: OrderRepository[F])` jest beanem `OrderService[IO]` w aplikacji `CoilixApp[IO]` bez dodatkowej konfiguracji.
3. W modułach runtime'u nie ma `synchronized`, `wait`, `notify`, `Atomic*` ani `Thread.sleep` poza udokumentowanymi wyjątkami.
4. Demo i przykłady z przewodników nie wołają `unsafeRun*`.
5. Aplikacja z kodem na `Future` działa przez `Async[F].fromFuture`, co pokazuje przewodnik.

## Ryzyka i otwarte pytania

1. Pierwsza bariera dla użytkownika bez doświadczenia z cats-effect i tagless final. Przewodnik „Pierwsza aplikacja” musi pokazać `F[_]: Async` i `CoilixApp[IO]` od pierwszej strony.
2. Wiązanie `F` dotyczy parametrów typu wyższego rzędu (`F[_]`). Komponent z kilkoma takimi parametrami albo z `F` o innym kształcie (`F[_, _]`) to błąd startu z wyjaśnieniem.
3. `fs2.Stream` jako źródło procesorów daje backpressure i gotowe konektory, ale dokłada zależność. Decyzja w kroku 2.
4. Biblioteki Javy, które wymagają `Executor` albo wołają callbacki na własnych wątkach, potrzebują mostu (`IO.async`, `Dispatcher`).
