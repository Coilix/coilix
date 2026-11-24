# COILIX-00029: Integracja z Pekko Streams

> Szkic do doszlifowania. Decyzja opiekuna z 2026-11-19: Pekko Streams to podstawowy silnik strumieni (wartość materializowana daje sterowanie grafem), łączony z `IO` z cats-effect (COILIX-00020). Wzorem są `OptimusFlow` i `ShardControl` z bulldozera oraz mosty `IO` ↔ Pekko z biblioteki implicits.

## Cel

- `ActorSystem` i `Materializer` są beanami z cyklem życia, jednymi na aplikację, zamykanymi razem z kontekstem w dobrej kolejności.
- Procesor tła (COILIX-00026) może być grafem Pekko uruchamianym per shard: framework go materializuje, steruje nim przez wartość materializowaną, nadzoruje, restartuje i zbiera statystyki tak samo jak inne procesory.
- Kod grafu może używać `IO` (`mapIO`, `Source.io`, `Sink.foreachIO`) z `IORuntime` z beana jako `given`, z anulowaniem `IO` przy zatrzymaniu strumienia.
- Moduły konektorów (Kafka, Elasticsearch, SFTP) mogą dawać źródła i ujścia Pekko z typami sterowania, które framework umie zatrzymać.

## Stan obecny

W Coilix nie ma Pekko. Procesory mają silnik na wątkach wirtualnych (COILIX-00026), a `IO` przychodzi w COILIX-00020.

W bulldozerze:

- `OptimusFlow.from(using ShardCtx): Source[?, ShardControl]`, a konwersje niejawne `cSNU`, `cSC`, `cSKC` zamieniają `NotUsed`, `Cancellable` i `Consumer.Control` w `ShardControl` (dla `NotUsed` przez doklejenie `KillSwitches.single`),
- `ShardControl` ma trzy implementacje z różnym zamykaniem: kill switch, `Cancellable` (z odpytywaniem `isCancelled`) i Kafka (`Control.shutdown()` bez czekania, potem kill switch, a po limicie czasu `abort`),
- `ShardDefinition` czeka na `Future` zakończenia grafu przez odpytywanie co 50–100 ms, bo anulowanie `IO.fromFuture` wisi na niedostępnym brokerze,
- `recover` w `IO` przed każdym startem grafu, doklejany jako `Source.io(...)` przed właściwym źródłem,
- atrybuty nadzoru (`Supervision.Stop` dla `NonFatal`) i poziomy logów zaszyte w kodzie.

W implicits (`Extensions.scala`): `Source.io`, `Source.fromIOList`, `Source.fromIOIterable`, `Sink.foreachIO`, `Source.mapIO`/`mapAsyncIO`/`evalMapIO`/`filterIO`/`flatMapConcatIO`/`flatMapMergeIO`/`recoverWithIO`, te same dla `Flow`, `RunnableGraph.runIO`, wszystkie z `using IORuntime` i `unsafeToFuture()`. Po zatrzymaniu strumienia uruchomione `IO` biegnie dalej, bo `Future` nie da się anulować.

## Zakres

### 1. Moduł `pekko` (`coilix-pekko`)

- `ActorSystem` jako bean (`ActorSystemFactory`, komponent z `given ConfigPath`):
  - nazwa i konfiguracja Pekko z sekcji `coilix.pekko` configu Coilix (przekazywana do Typesafe Config Pekko), bez osobnego `application.conf`,
  - domyślny dispatcher na puli obliczeniowej aplikacji: `ActorSystem(name, config, classLoader, defaultExecutionContext = Some(taskExecutor.executionContext))`, czyli na `ForkJoinPool` Coilix albo na WSTP z `IORuntime`, gdy jest moduł `cats-effect` (ADR 0028). Moduł nie zależy od `cats-effect`, bo bierze tylko `TaskExecutor`,
  - `pekko.actor.internal-dispatcher = "pekko.actor.default-dispatcher"`, żeby Pekko nie tworzyło drugiej puli (eksperyment w ADR 0028: 40 wątków zamiast 75 na 32 rdzeniach),
  - `default-blocking-io-dispatcher` przez własny `ExecutorServiceConfigurator` kierowany do `BlockingExecutor`,
  - `throughput` skrzynek domyślne (krótkie porcje wiadomości), bo działają na puli współdzielonej z `IO`; aktor, który blokuje, dostaje dispatcher blokujący,
  - `CoordinatedShutdown` bez własnego shutdown hooka JVM: zamyka go kontekst, po beanach, które zależą od `ActorSystem`.
- `Materializer` jako bean zależny od `ActorSystem`.
- Rozszerzenia jako `given` w stylu z CLAUDE.md: `ActorSystem` i `Materializer` dostępne jako `given` w kodzie modułów, które ich potrzebują.

### 2. Moduł `pekko-cats-effect` (`coilix-pekko-cats-effect`)

Port mostów z implicits jako metod rozszerzających z `using IORuntime`:

- `Source.io`, `Source.fromIOList`, `Source.fromIOIterable`, `Source.resource(Resource[IO, A])` (zasób zamykany przy zakończeniu strumienia),
- `mapIO`, `mapAsyncIO`, `mapAsyncUnorderedIO`, `evalMapIO`, `filterIO`, `flatMapConcatIO`, `flatMapMergeIO`, `recoverWithIO` dla `Source` i `Flow`,
- `Sink.foreachIO`, `Sink.foreachIO(parallelism)`,
- `RunnableGraph.runIO: IO[Mat]` i `Source.runIO(sink): IO[...]` z czekaniem na zakończenie jako `IO`, z anulowaniem przez kill switch.

Różnice względem implicits:

- **Anulowanie**: `IO` w etapie uruchamiane przez `unsafeToFutureCancelable`. Gdy etap się kończy (zatrzymanie, błąd, kill switch), trwające `IO` są anulowane.
- **Granica `Future`**: `IO` → `Future` jest tylko w tych etapach, bo `mapAsync` Pekko przyjmuje `Future`. Silniki frameworka nie przekładają `IO` na `Future`.
- Każda metoda ma odpowiednik jawny (`IOStages.mapAsync(parallelism)(f)`), z którego korzysta wersja rozszerzająca.

### 3. Moduł `processing-pekko` (`coilix-processing-pekko`)

```scala
trait PekkoProcessor extends ProcessorDefinition {
  type Control

  def source(shard: Shard)(using PekkoShardContext): Source[?, Control]

  given stoppable: Stoppable[Control]
}

trait Stoppable[M] {
  def stop(control: M, completion: Future[Done], timeout: FiniteDuration): StopOutcome
}
```

- Silnik `PekkoProcessorEngine` (SPI `ProcessorEngine` z COILIX-00026) dla każdego sharda tej repliki:
  1. `prepare(shard)`,
  2. materializuje `source(shard)` z `Sink.ignore` i atrybutami nadzoru,
  3. przekazuje zakończenie grafu (`Future[Done]`) do wspólnego modelu nadzoru (`RestartPolicy`, `CompletionPolicy`, statystyki, przekazanie sharda).
- **`Stoppable[M]`** zamiast konwersji niejawnych z bulldozera. Instancje w `processing-pekko`:
  - `UniqueKillSwitch` (`shutdown`, po limicie czasu `abort`),
  - `Cancellable` (`cancel`, czekanie na zakończenie bez odpytywania `isCancelled`),
  - `NotUsed` przez doklejenie kill switcha w silniku (`source.viaMat(KillSwitches.single)(Keep.right)`),
  - para `(A, B)` z instancjami dla obu (np. `Consumer.Control` i kill switch).

  Instancja dla `Consumer.Control` jest w `kafka-pekko` (task Kafki).
- **Czekanie na zakończenie** przez `Future[Done]` z limitem czasu (`Await` na wątku wirtualnym nadzorcy albo `IO.fromFuture` z `IO.timeout` w wariancie IO), bez odpytywania. Porzucenie grafu po limicie (bulldozerowy `abandonGraphSignal`) jest jawnym stanem `Abandoned` w statystykach i logach.
- **Statystyki**: silnik liczy zakończenia, błędy i restarty sam. Elementy przetworzone liczy etap `ShardMetrics.counted` (rozszerzenie `.countedAs("processed")`), który procesor wstawia w grafie tam, gdzie element jest gotowy, bo silnik nie widzi wnętrza grafu.
- **Obsługa błędów elementu**: graf Pekko nie przechodzi przez `FailureHandling` per element, bo framework nie widzi elementów. Dostępne są:
  - etapy `withFailureHandling(failures)` dla `mapIO`/`mapAsync`, które stosują te same decyzje (`Retry`, `Skip`, `DeadLetter`) wewnątrz etapu,
  - błąd grafu jako całości, który idzie do `RestartPolicy` jak w innych silnikach.
- **`PekkoShardContext`** daje `Shard`, `Materializer`, logger z MDC sharda i `ShardMetrics` jako `given` w `source`, tak jak `ShardCtx` w bulldozerze, ale bez dziedziczenia po rejestrze serwisów.
- **Wariant z `IO`** w `processing-pekko-cats-effect`, tworzonym dopiero przy pierwszym procesorze, który go potrzebuje: `prepare(shard): IO[Unit]` i `IORuntime` jako `given` w `source`.

Przykład:

```scala
@Processor
final class UserEvents(kafka: KafkaStreams, tokens: TokenCache)(using IORuntime) extends PekkoProcessor {
  override type Control = Consumer.Control

  override val id: AppProcessor = AppProcessor.UserEvents

  override def source(shard: Shard)(using PekkoShardContext): Source[?, Consumer.Control] =
    kafka.committable(UserEventsTopic, shard)
      .mapAsyncIO(1)(message => tokens.invalidate(message.value.userId).as(message.offset))
      .countedAs("processed")
      .via(kafka.committer)

  override given stoppable: Stoppable[Consumer.Control] = KafkaStoppable.drainAndShutdown
}
```

### 4. Konfiguracja

```hocon
coilix.pekko {
  system-name = coilix
  shutdown-timeout = 30s
  config {
    pekko.loglevel = INFO
  }
}

coilix.processors.user-events {
  pekko {
    stop-timeout = 10s
    supervision = stop
    log-levels { element = debug, finish = debug, failure = error }
  }
}
```

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `pekko` | funkcja | rdzeń, pekko-stream | `ActorSystem` i `Materializer` jako beany, konfiguracja, zamykanie z kontekstem |
| `pekko-cats-effect` | integracja | `pekko`, `cats-effect` | mosty `IO` ↔ Pekko z anulowaniem |
| `processing-pekko` | integracja | `processing`, `pekko` | `PekkoProcessor`, `Stoppable[M]`, `PekkoShardContext`, `PekkoProcessorEngine`, etapy statystyk i obsługi błędów |
| `processing-pekko-cats-effect` | integracja (na żądanie) | `processing-pekko`, `pekko-cats-effect` | `prepare` w `IO`, `IORuntime` w `source` |
| `kafka-pekko` | integracja (task Kafki) | `kafka`, `pekko` | źródła i committer z pekko-connectors-kafka, `Stoppable[Consumer.Control]` |

Pekko i pekko-connectors mają licencję Apache-2.0, więc są dozwolone, ale tylko w modułach funkcji i integracji.

## Kroki (osobne PR-y)

1. Moduł `pekko`: beany, konfiguracja, zamykanie z kontekstem, testy z testkitem. Zrobione (ADR 0036): moduł `25_pekko`, beany zamiast `ActorSystemFactory`, przewodnik i demo (`ModuleCensus`).
2. `pekko-cats-effect`: mosty z anulowaniem i testy anulowania.
3. `processing-pekko`: `PekkoProcessor`, `Stoppable` i silnik na wspólnym modelu nadzoru (po krokach 4–8 z COILIX-00026).
4. Przewodnik w obu językach i przykładowy procesor Pekko w demo.
5. `processing-pekko-cats-effect` i `kafka-pekko` razem z przenoszeniem pierwszego flow z bulldozera.

## Kryteria akceptacji

1. `ActorSystem` jest zamykany przy zamknięciu kontekstu po procesorach, które z niego korzystają, a JVM nie dostaje osobnego shutdown hooka Pekko.
2. `mapIO` anuluje trwające `IO`, gdy strumień zostaje zatrzymany kill switchem. Test sprawdza, że finalizator `IO` się wykonał, a efekt po `IO.sleep` nie.
3. Procesor Pekko z `shards = 3` materializuje trzy grafy na jednej replice, a restart jednego sharda po błędzie grafu nie dotyka pozostałych.
4. `Stoppable` dla kill switcha, `Cancellable` i `NotUsed` zatrzymuje graf w limicie czasu, a graf, który się nie zatrzymał, przechodzi w `Abandoned` z wpisem w logu.
5. `ProcessorManager.pause`, `resume` i `stop` działają dla procesora Pekko tak samo jak dla procesora bezstanowego.
6. Statystyki procesora Pekko pokazują elementy z `countedAs`, zakończenia, błędy i restarty.
7. `pekko`, `pekko-cats-effect` i `processing-pekko` nie trafiają na classpath aplikacji, która ich nie dodała, a rdzeń i `processing` nie zależą od Pekko (`checkModuleLayers`).

## Poza zakresem

- Pekko HTTP i aktorzy jako model programowania aplikacji.
- Pekko Cluster i Cluster Sharding. Repliki i shardy obsługuje COILIX-00027 i COILIX-00026.
- fs2 i ZIO Streams.

## Ryzyka i otwarte pytania

1. `pause` dla grafu Pekko: graf nie ma wbudowanej pauzy. Możliwe są zatrzymanie i ponowne uruchomienie (prosto, traci stan w pamięci grafu) albo etap `Valve` wstawiany przez silnik (zachowuje stan, wymaga zgody procesora na doklejenie etapu). Do decyzji w kroku 3.
2. Obsługa błędów elementu w grafie jest słabsza niż w procesorach bezstanowych, bo framework nie widzi elementów. Etapy `withFailureHandling` łagodzą to tylko tam, gdzie procesor ich użyje.
3. Kolejność zamykania przy SIGTERM: kontekst zamyka procesory, potem `ActorSystem`. Pekko domyślnie ma własny hook `CoordinatedShutdown`, który trzeba wyłączyć (`pekko.coordinated-shutdown.run-by-jvm-shutdown-hook = off`), żeby nie wyścigał się z hookiem Coilix.
4. Konfiguracja Pekko z sekcji configu Coilix zamiast `application.conf` może zaskoczyć użytkowników Pekko. Alternatywa: czytać też `application.conf` z niższym priorytetem.
