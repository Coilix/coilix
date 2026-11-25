# 0036: Moduł pekko z ActorSystem na pulach Coilix

- Status: zastąpiona przez 0043
- Data: 2026-11-24
- Task: COILIX-00029 (krok 1)

## Kontekst

Decyzje opiekuna (task COILIX-00029, ADR 0028): Pekko Streams to podstawowy silnik strumieni, jedna pula obliczeniowa na aplikację (domyślny dispatcher na `TaskExecutor`, `internal-dispatcher` jako alias), integracje z cats-effect i Pekko działają osobno i razem. Opiekun zapowiedział 2026-11-24, że bulldozer będzie przepisany na Coilix, więc wzorce bulldozera są wymaganiem.

W bulldozerze (`OptimusRuntime` z biblioteki implicits) jest jeden klasyczny `ActorSystem` z `defaultExecutionContext` na puli obliczeniowej opakowanej tak, żeby Pekko jej nie zamykało, `Materializer.matFromSystem`, a serwisy dostają `ActorSystem` i `Materializer` jako `given` (`case class MyService(config)(using OptimusRuntime)`, `OptimusRuntimeHolder`). Zamykanie to `terminate()` i czekanie na `whenTerminated`.

## Decyzja

- **Moduł funkcji `25_pekko`** (`dev.coilix.pekko`, artefakt `coilix-pekko`, grupa wydań `pekko`), zależny od `boot` i Pekko 2.0.0 (`pekko-actor`, `pekko-stream`, `pekko-slf4j`). Nie zależy od `cats-effect`: bierze tylko `TaskExecutor`, który z modułem cats-effect jest pulą `IORuntime`.
- **Beany** w `PekkoConfiguration`: `pekkoSettings`, `coilixActorSystem` (właściciel, wzorem `CoilixIORuntime`), `actorSystem` i `materializer`. Bean aplikacji tego typu wyłącza bean modułu (`@ConditionalOnMissingBean`), tak samo jak przy `TaskExecutor`; nie ma osobnego `ActorSystemFactory` z `ConfigPath`, bo `ActorSystem` potrzebuje beanów (wykonawców, handlera błędów), a nie tylko konfiguracji.
- **`using` zamiast holdera.** Konstruktor `(...)(using Materializer)` działa jak zwykła zależność, więc serwis z bulldozera przenosi się bez `OptimusRuntime`: `using OptimusRuntime` zamienia się na `using ActorSystem` / `using Materializer`.
- **Pule.** Domyślny dispatcher przez `BootstrapSetup.defaultExecutionContext` = `TaskExecutor.executionContext` opakowany w `ExecutionContext.fromExecutor` (nie `ExecutorService`, więc Pekko go nie zamyka). `pekko.actor.internal-dispatcher = "pekko.actor.default-dispatcher"`. `default-blocking-io-dispatcher` ma `executor = BlockingExecutorConfigurator`, który bierze `BlockingExecutor` z `CoilixExecutorsSetup` (własny `Setup` w `ActorSystemSetup`, bo Pekko tworzy konfiguratory po nazwie klasy) i daje `SharedExecutorService`, którego zamknięcie nie zamyka wykonawcy Coilix.
- **Konfiguracja** z `coilix.pekko` (`system-name`, `shutdown-timeout`, `config`), bez `application.conf`. Kolejność: klucze wymagane (`coordinated-shutdown.run-by-jvm-shutdown-hook = off`, `exit-jvm = off`, `jvm-exit-on-fatal-error = off`) > `coilix.pekko.config` > domyślne Coilix (SLF4J, dispatchery) > `reference.conf`.
- **Zamykanie.** `@PreDestroy` właściciela uruchamia `CoordinatedShutdown(system).run(CoilixShutdownReason)` i czeka najwyżej `shutdown-timeout`; zależności sprawiają, że dzieje się to po beanach używających `ActorSystem` i przed `TaskExecutor`/`BlockingExecutor`. Zatrzymanie, o które kontekst nie prosił (Pekko po błędzie fatalnym, gdy nie kończy JVM), trafia przez `registerOnTermination` do `FatalErrorHandler` jako `ActorSystemStoppedException`, jak `IORuntimeStoppedException` w ADR 0031.

## Konsekwencje

- Aktory, strumienie i `IO` dzielą jedną pulę obliczeniową, a etapy blokujące Pekko czekają na wątkach wirtualnych Coilix.
- Użytkownik Pekko nie może włączyć hooka JVM ani kończenia JVM przez Pekko; zachowanie po błędzie fatalnym ustala `coilix.fatal-errors`.
- Kolejne kroki COILIX-00029: `pekko-cats-effect` (mosty `IO` ↔ Pekko z anulowaniem), potem `processing-pekko` po silniku procesorów z COILIX-00026.
