# 0047: Słuchacze cyklu życia kontekstu w IO

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00031 (krok 7)

## Kontekst

`ContextLifecycleListener` (ADR 0024) miał metody `onContextStarted(context): Unit` i `onContextClosing(): Unit`, wołane przez synchroniczny kontener: `DefaultApplicationContextBuilder` po utworzeniu beanów i `DefaultApplicationContext.close()` przed `@PreDestroy`. Od ADR 0042 kontrakty, które runtime woła w kodzie aplikacji po starcie, zwracają `IO`, a po ADR 0045 każda aplikacja startuje jako `CoilixIOApp`. Słuchacz, którego praca jest w `IO`, musiał przechodzić do `IO` przez `unsafeRunSync` (`DefaultProcessorManager`, ADR 0044), a zatrzymanie pracy w fazie zamykania blokowało wątek kontenera.

## Decyzja

- **Kontrakt w `IO`.** `onContextStarted(context: ApplicationContext): IO[Unit]` i `onContextClosing: IO[Unit]` (domyślnie `IO.unit`). Fazy zamykania (`ClosingPhase`) i kolejność zostają: start w kolejności tworzenia, zamykanie faza po fazie w odwrotnej kolejności tworzenia, jeden słuchacz po drugim. Błąd startu przerywa start, błąd zamykania jest logowany.
- **Kontener tylko tworzy beany.** `ApplicationContextBuilder.create` tworzy beany bez powiadamiania słuchaczy, a `CoilixBootstrap.start` używa `create`. Kontekst dostaje `ConfigurableApplicationContext.start: IO[Unit]` i `shutdown: IO[Unit]`.
- **Start aplikacji w `Resource`.** `CoilixIOStartup.application` buduje kontekst w `IO.blocking`, uruchamia `start` po nabyciu zasobu i `shutdown` przy zwolnieniu, także po błędzie startu słuchacza i po anulowaniu. Ścieżka aplikacji nie ma `unsafeRunSync`.
- **Jedna granica dla kodu poza `IO`.** `ApplicationContextBuilder.build` (create + start) i `close()` zostają dla testów synchronicznych i testkitu. Uruchamiają `IO` słuchaczy przez `SynchronousLifecycle` na beanie `IORuntime` kontekstu, a bez niego na globalnym runtime cats-effect. `close()` niszczy beany w wątku wołającym, więc runtime z beana nie jest zamykany, zanim skończą się słuchacze.
- **Wewnętrzna logika bez zmian.** Szyna zdarzeń, `@Scheduled` i diagnostyka opakowują swoją obecną logikę w `IO` (blokujące części w `IO.blocking`). Przepisanie ich stanu na `Ref`, `Queue` i fs2 to osobne kroki COILIX-00031 (4–6).

## Konsekwencje

- Zmiana niezgodna API publicznego (`0.x`: `MINOR`): własne słuchacze zwracają `IO`, a własne implementacje `ApplicationContextBuilder` nadpisują `create` zamiast `build`, a `ConfigurableApplicationContext` dodają `start` i `shutdown`.
- `DefaultProcessorManager` uruchamia procesory i zatrzymuje shardy bez `unsafeRunSync` i bez wstrzykiwania `IORuntime`.
- Testkit w `IO` (`CoilixIOTestSetup.resource`) zamyka kontekst przez `shutdown`. Start w testkicie zostaje synchroniczny, bo `StartupEvents` i runnery testów działają synchronicznie.
- Częściowo zastępuje ADR 0024 (sygnatury metod słuchacza).
