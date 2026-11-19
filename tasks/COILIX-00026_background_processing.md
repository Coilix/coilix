# COILIX-00026: Przetwarzanie w tle (`processing`)

> Szkic do doszlifowania. Wzorem jest moduł `flow` z bulldozera (`OptimusFlow`, shardy, `StatefulFlow`), ale API piszemy od nowa pod zasady Coilix. Decyzje opiekuna z 2026-11-18 i 2026-11-19:
>
> - nazwa to „procesor”,
> - rdzeń jest blokujący i działa na wątkach wirtualnych, a `IO` (COILIX-00020) i Pekko Streams (COILIX-00029) wpinają się jako osobne silniki wykonania na wspólnym modelu,
> - obsługę błędów projektujemy od razu docelowo,
> - jeden procesor ma wiele shardów i wiele równoległych workerów,
> - shardy rozkładają się na repliki aplikacji (pody w Kubernetesie), które znają liczbę replik i swój numer,
> - procesory nie są beanami: zbiera je i przechowuje `ProcessorManager`, który jest jedynym beanem i jedyną drogą sterowania,
> - identyfikator procesora to wartość sealed traitu aplikacji, a nie `String`,
> - liczbę shardów ustawia się w configu i można ją zmienić,
> - repliki znają się nawzajem i komunikują przez API (COILIX-00027).

## Cel

Aplikacja deklaruje długo działające przetwarzanie w tle jako klasę z adnotacją `@Processor`: konsumpcję topiców, odpytywanie SFTP, przetwarzanie stanowe, zapis wsadowy do Elasticsearch. `ProcessorManager` zbiera procesory przy starcie i tworzy je z zależnościami z kontekstu. Dzieli pracę każdego procesora na shardy, rozkłada shardy na repliki i na każdej replice uruchamia osobnego workera dla każdego jej sharda. Framework:

- nadzoruje workery: ponawia elementy, restartuje workery, wstrzymuje je przy awarii odbiorcy i kieruje trujące elementy do dead letter,
- zbiera statystyki,
- zmienia liczbę shardów i przekazuje shardy między replikami,
- zamyka workery razem z kontekstem.

Sterowanie (start, stop, pauza, wznowienie, restart, zmiana liczby shardów) idzie tylko przez `ProcessorManager`, lokalnie albo w całym klastrze.

Obsługiwane style:

- **bezstanowy**: element → przetworzenie → potwierdzenie,
- **wsadowy**: paczka elementów → przetworzenie z wynikiem per element → potwierdzenie,
- **stanowy**: element → klucz → stan → krok → utrwalenie stanu → emisja wyniku → potwierdzenie, plus cykliczny tick po stanach,
- **własny**: surowy kontrakt dla silników strumieni (Pekko, fs2) i pętli blokujących.

## Stan obecny

W Coilix:

- Nie ma obsługi wątków ani zadań w tle. COILIX-00005 planuje `TaskExecutor`, `@Async` i `@Scheduled`, ale nie nadzór nad długo działającym przetwarzaniem.
- Jest cykl życia (`@PostConstruct`, `@PreDestroy`, zamykanie kontekstu, COILIX-00004) i moduły opcjonalne przez `CoilixModule` (COILIX-00010).
- Skaner klas rejestruje każdą klasę z adnotacją komponentu jako bean. Nie ma sposobu, żeby moduł zebrał klasy z własną adnotacją i utworzył je z zależnościami z kontekstu, ale bez rejestrowania jako beany.
- Aplikacja nie wie, ile ma replik ani którą z nich jest (COILIX-00027).
- Kontener jest synchroniczny, efekty `F[_]` czekają na COILIX-00020.

W bulldozerze (`flow`, `core/.../flow/stateful`, `kafka/.../dlq`):

- `OptimusFlow` + `FlowMeta`: rejestr i `create(ctx)` z ręcznym `summonIO`, nazwa flow jako `String`.
- `FlowType` to liczba shardów na pod (1, 2, 3, 4, 6, 12). Każdy pod uruchamia wszystkie shardy, a podział między pody robi grupa konsumentów Kafki.
- `ShardDefinition`: pętla start → obserwacja → restart, `recover` przed każdym startem, miękkie zamykanie z limitem czasu.
- `ShardControl` dla `KillSwitch`, `Cancellable` i `Consumer.Control` Kafki, podpinane konwersjami niejawnymi.
- `StatefulFlow` z `StatefulProcessor`, `StateStore`, `Emitter`, `Step` i regułą „commit dopiero po utrwaleniu stanu”.
- Metryki Kamon w `ShardMetrics`, konfiguracja `FlowConfig(enabled, disabled, runAll)`.

Problemy, których nie przenosimy:

1. Licznik restartów nigdy się nie zeruje, a opóźnienie się kumuluje (`4s * n` plus suma poprzednich, limit 4 min). Po kilku rzadkich błędach w ciągu dni shard poddaje się na zawsze.
2. Każdy błąd elementu restartuje cały shard. Nie ma ponowienia samego elementu, pominięcia, dead letter ani wykrywania trującego elementu, więc jeden zły rekord blokuje partycję do ręcznej interwencji.
3. Polityka restartu, limity czasu (10 s, 30 s, 60 s) i poziomy logów są zaszyte w kodzie.
4. Na zakończenie czeka się przez odpytywanie `Future.value` co 50–100 ms, bo anulowanie `IO.fromFuture` wisi. Na wątkach wirtualnych wystarczy blokujące czekanie z limitem czasu.
5. `StateTopicStore.bootstrapPointers` i `readAt` są niezaimplementowane, więc po restarcie stan stanowego flow przepada po cichu.
6. Statystyki są tylko w Kamonie. Z kodu nie da się zapytać o stan flow, a pojedynczego flow nie da się ręcznie zatrzymać, wstrzymać ani wznowić.
7. Liczba shardów jest zaszyta w kodzie (`FlowType`), shard nie ma globalnego numeru i bez Kafki nie da się podzielić pracy między pody.
8. Rdzeń flow zależy od Pekko Streams i cats-effect.

## Słownik

| Pojęcie | Znaczenie |
|---|---|
| procesor | klasa z `@Processor`, tworzona i przechowywana przez `ProcessorManager`; nie jest beanem i nie da się jej wstrzyknąć |
| identyfikator | wartość `ProcessorId`, zwykle `case object` sealed traitu aplikacji; po nim adresuje się procesor w managerze i w configu |
| slot | najmniejsza, stała część danych procesora o numerze `0 until slotCount`: partycja Kafki albo kubełek hasha klucza; nie zmienia się przy zmianie shardów ani replik |
| shard | jednostka pracy z globalnym numerem `0 until shardCount`, właściciel zbioru slotów; liczba z configu, zmienialna |
| replika | jedna kopia aplikacji (pod), z numerem spośród liczby replik (COILIX-00027) |
| przydział | które sloty należą do którego sharda i które shardy do której repliki |
| worker | nadzorowane wykonanie jednego sharda na tej replice, na własnym wątku wirtualnym |
| źródło | `InputSource[T]`, z którego worker pobiera elementy slotów swojego sharda |
| element | `Input[T]`: wartość, slot, identyfikator, numer dostarczenia i potwierdzenie |
| wyjście | `Output[R]`, do którego procesor emituje wyniki |
| etap | miejsce błędu: pobranie, przetworzenie, stan, emisja, potwierdzenie, tick, przygotowanie |
| decyzja | co zrobić z błędem: ponowić, pominąć, dead letter, wstrzymać, zrestartować, zatrzymać |

Trzy poziomy oddzielają dane od pracy i od infrastruktury:

```
klucz/partycja ──(stałe)──▶ slot ──(config, zmienialne)──▶ shard ──(przydział)──▶ replika
```

Przykład: topik ma 24 partycje, czyli 24 sloty. Procesor ma `shards = 6`, a aplikacja ma 3 repliki. Każdy shard ma 4 sloty (partycje), a każda replika 2 shardy, czyli 2 workery. Zmiana na `shards = 12` daje po 2 sloty na shard i po 4 workery na replikę. Partycje, klucze i stan zostają w tych samych slotach, zmienia się tylko to, który worker je obsługuje.

## Zakres

### 1. Deklaracja i identyfikator

```scala
sealed trait AppProcessor extends ProcessorId

object AppProcessor {
  case object VehicleSessions extends AppProcessor

  case object EntityIndexer extends AppProcessor

  case object DailyReport extends AppProcessor
}

@Processor
final class VehicleSessions(topics: KafkaSources, states: KafkaStateStores, outputs: KafkaOutputs)
    extends StatefulProcessor[Telemetry, Vin, Session, SessionClosed] {

  override val id: AppProcessor = AppProcessor.VehicleSessions
  ...
}
```

- `@Processor` w module Java `processing-annotations` (wzorem `web-annotations` z COILIX-00023) to sam znacznik. Nie ma w nim nazwy ani liczby shardów: identyfikator jest w kodzie, a liczba shardów w configu (punkt 3).
- `ProcessorId` to trait frameworka z `def name: String`. Domyślna nazwa to nazwa obiektu w kebab case (`VehicleSessions` → `vehicle-sessions`), użyta w kluczach configu, metrykach, logach i API między replikami. Można ją nadpisać, np. dla zgodności z nazwą z bulldozera.
- Sealed trait aplikacji daje sprawdzanie wyczerpania w `match` i zamyka listę procesorów w jednym miejscu. Manager przyjmuje `ProcessorId`, więc literówka w nazwie nie skompiluje się, a nie skończy się błędem w runtime.
- Przy starcie manager sprawdza, że identyfikatory są unikalne (dwa procesory z tym samym `id` to błąd startu). Opcjonalnie, z `coilix.processors.require-all-ids = true`, sprawdza też, że każda wartość sealed traitu ma procesor (listę wartości daje mechanizm `@Enumerated` z COILIX-00022 albo makro `ProcessorIds.of[AppProcessor]`).
- Identyfikatora nie ma w adnotacji, bo adnotacja Javy przyjmuje tylko stałe, a `classOf` obiektu Scali jest w adnotacji niewygodny i nie daje wartości, tylko klasę.

### 2. Procesory nie są beanami

- Skaner nie rejestruje klas z `@Processor` jako beanów. Przekazuje je do `ProcessorManager` przez nowy punkt rozszerzenia rdzenia `ComponentCollector` (krok 2 w „Krokach”): moduł deklaruje adnotację, którą zbiera, a kontekst oddaje mu znalezione klasy.
- Manager tworzy każdy procesor raz na replikę przez mechanizm instancjonowania kontekstu: wybór konstruktora, zależności z beanów, `@Value`, `@PostConstruct`. Wywołuje też `@PreDestroy` przy zamykaniu. Procesor nie trafia do rejestru beanów, więc żaden bean nie może go wstrzyknąć ani wywołać `process` z pominięciem nadzoru.
- Procesor jest niemutowalny, tak jak każda klasa w Coilix. Stan wykonania (shardy, workery, liczniki, polityki) trzyma manager, a stan per shard obiekty tworzone przez `source(shard)` i `store(shard)`.
- Jedynym beanem modułu jest `ProcessorManager` (oraz komponenty z `given ConfigPath`, np. `RestartPolicy`). Inne beany wstrzykują manager i sterują procesorami po identyfikatorze.
- Manager ma jeden wątek sterujący z kolejką poleceń. Polecenia z API, z innych replik i od nadzorców wykonują się po kolei, więc zmiany stanu nie potrzebują blokad. Stan to niemutowalna migawka podmieniana w jednym `AtomicReference`, a odczyt (`status`, `statistics`) jej nie blokuje.

### 3. Sloty, shardy i liczba shardów z configu

```scala
final case class Shard(processor: ProcessorId, index: Int, count: Int, slots: SlotSet, replica: Member, epoch: Long) {
  def owns(slot: Int): Boolean

  def stopRequested: Boolean

  def metrics: ShardMetrics
}
```

- **Sloty** są stałe dla procesora. Liczbę daje źródło (liczba partycji topiku) albo config `slots` dla źródeł z kluczem (domyślnie 256). Klucz trafia do slotu przez `ShardKeys.slotOf(key, slotCount)`, czyli stabilny hash (murmur3 z bajtów klucza, a nie `hashCode`, który może się zmienić między wersjami JVM i Scali).
- **Shardy** grupują sloty: slot `s` należy do sharda `s % shardCount`. Liczbę ustawia się w configu (`coilix.processors.<nazwa>.shards`), a procesor może podać wartość domyślną przez `def defaultShards: Int`. Warunek: `shards <= slots`, inaczej błąd startu.
- **Zmiana liczby shardów** nie przenosi danych, bo stan i offsety są przypięte do slotów, a nie do shardów:
  - po zmianie configu i restarcie replik, z ochroną epoką (punkt 13),
  - w działającym klastrze przez `ProcessorManager.cluster.reshard(id, n)`, koordynowane przez API między replikami (punkt 9, COILIX-00027): wszystkie repliki oddają shardy, koordynator ogłasza nowy podział z wyższą epoką, a repliki przejmują nowe shardy,
  - na jednej replice bez klastra przez `ProcessorManager.reshard(id, n)`.
- **Przydział shardów do replik** (`ShardAssignment`, komponent z `given ConfigPath`, wybierany też per procesor):
  - `round-robin` (domyślny): shard `i` trafia do repliki `i % replicaCount`,
  - `range`: ciągłe przedziały,
  - `pinned(replica)`: wszystkie shardy na jednej replice; `shards = 1` z `pinned(0)` to procesor singleton,
  - `all`: każdy shard na każdej replice, dla źródeł, które same dzielą pracę między repliki, np. grupa konsumentów Kafki w trybie `subscribe`,
  - `coordinated`: koordynator klastra (COILIX-00027) rozdziela shardy między żywe repliki i przenosi shardy z repliki, która odpadła.
- Gdy shardów jest mniej niż replik, część replik nie ma workerów danego procesora. To normalny stan, widoczny w `topology` i niezgłaszany jako błąd.
- Opcjonalny limit `max-shards-per-replica` chroni replikę przed przejęciem zbyt wielu shardów po awarii innych replik w trybie `coordinated`.

### 4. Style procesorów

```scala
trait ProcessorDefinition {
  def id: ProcessorId

  def defaultShards: Int = 1

  def prepare(shard: Shard): Unit = ()

  def onAcquired(shard: Shard): Unit = ()

  def onReleased(shard: Shard, reason: ReleaseReason): Unit = ()
}

trait StatelessProcessor[T] extends ProcessorDefinition {
  def source(shard: Shard): InputSource[T]

  def process(element: T, context: ElementContext): Unit

  def failures: FailureHandling = FailureHandling.default

  def concurrency: Concurrency = Concurrency.sequential
}

trait BatchProcessor[T] extends ProcessorDefinition {
  def source(shard: Shard): InputSource[T]

  def process(batch: Batch[T], context: BatchContext): BatchResult[T]

  def batching: Batching = Batching(maxSize = 500, maxWait = 1.second)

  def failures: FailureHandling = FailureHandling.default
}

trait StatefulProcessor[T, K, S, R] extends ProcessorDefinition {
  def source(shard: Shard): InputSource[T]

  def store(shard: Shard): StateStore[K, S]

  def output(shard: Shard): Output[R]

  def key(input: T): K

  def initial(key: K): S

  def step(state: S, input: T, context: ElementContext): Step[S, R]

  def onTick(state: S, idle: FiniteDuration, context: TickContext): Step[S, R] = Step.keep(state)

  def retention: StateRetention[S] = StateRetention.default

  def failures: FailureHandling = FailureHandling.default
}

trait CustomProcessor extends ProcessorDefinition {
  def run(shard: Shard): Execution
}

trait BlockingProcessor extends ProcessorDefinition {
  def run(shard: Shard): Unit
}
```

- `prepare` działa przed każdym startem workera. To odpowiednik `recover`: odtworzenie offsetów, bootstrap stanu slotów sharda. Błąd w `prepare` przechodzi przez politykę restartu.
- `onAcquired` i `onReleased` mówią, kiedy replika przejęła albo oddała shard. `ReleaseReason` to `Shutdown`, `Reassigned`, `Resharded`, `Stopped` albo `Failed`.
- `ElementContext` daje slot i identyfikator elementu, numer dostarczenia, numer próby, `Shard`, zegar i `metrics`.
- `Concurrency` to równoległość wewnątrz jednego sharda: `sequential`, `parallel(n)` albo `perKey(n)(key)`. Przy równoległości potwierdzenia idą w kolejności źródła w obrębie slotu (znacznik najwyższego ciągłego potwierdzenia), więc commit offsetów Kafki zostaje poprawny.
- `BatchResult[T]` mówi, które elementy się udały, a które nie i z jakim błędem, np. częściowe porażki bulk w Elasticsearch. Nieudane elementy przechodzą przez obsługę błędów pojedynczo.
- `StateRetention[S]` to `removable(state, idle)` i `evictable(state, idle)` z bulldozera.
- `CustomProcessor` zwraca `Execution` z `stop()` i `completion: CompletionStage[Unit]`. `BlockingProcessor` działa, dopóki `shard.stopRequested` jest fałszywe, a przy zatrzymaniu dostaje przerwanie wątku.

### 5. Źródło, element, wyjście

SPI niezależne od silnika strumieni:

```scala
trait InputSource[T] extends AutoCloseable {
  def slotCount: Option[Int] = None

  def poll(max: Int, timeout: FiniteDuration): List[Input[T]]

  def lag: Map[Int, Long] = Map.empty

  def pause(): Unit = ()

  def resume(): Unit = ()
}

final case class Input[+T](value: T, slot: Int, id: InputId, delivery: Int, acknowledgement: Acknowledgement)

trait Acknowledgement {
  def acknowledge(): Unit

  def reject(): Unit = ()
}

trait Output[R] extends AutoCloseable {
  def emit(elements: List[R]): Unit
}
```

- Źródło tworzy się per shard (`source(shard)`) i czyta tylko sloty sharda. Zamyka się przy oddaniu sharda, więc offsety, połączenia i bufory należą do jednego workera.
- `slotCount` źródła (np. liczba partycji) ma pierwszeństwo przed `slots` z configu. Zmiana liczby slotów (nowe partycje topiku) to migracja, którą `StateStore` wykrywa przy starcie.
- `InputId` to stabilny identyfikator, np. `topic-partition-offset` albo ścieżka pliku z czasem modyfikacji. Na nim opiera się licznik dostarczeń i wykrywanie trujących elementów po restarcie.
- `delivery` to numer dostarczenia. Źródło, które wie, że element wraca, podaje go samo. W przeciwnym razie liczy go nadzorca w pamięci po `InputId`.
- `pause` i `resume` wstrzymują pobieranie bez utraty przydziału (Kafka `pause` partycji).
- `lag` per slot trafia do statystyk i zdrowia.
- Worker woła `poll` w pętli na wątku wirtualnym, więc konektor nie potrzebuje Pekko. W `processing` są implementacje w pamięci (także ze slotami) do testów i demo.

### 6. Obsługa błędów

Każdy błąd ma **etap** (`FailureStage`) i trafia do `FailureHandling`, które zwraca **decyzję** (`FailureDecision`).

| Etap | Kiedy |
|---|---|
| `Fetch` | `poll` źródła |
| `Process` | `process`, `step`, element nieudany w `BatchResult` |
| `State` | odczyt, zapis albo usunięcie w `StateStore` |
| `Emit` | `Output.emit` |
| `Acknowledge` | `Acknowledgement.acknowledge` |
| `Tick` | `onTick` i retencja stanu |
| `Prepare` | `prepare` workera |

| Decyzja | Skutek |
|---|---|
| `Retry(backoff)` | ponawia ten sam krok w workerze, bez restartu, z opóźnieniem z `Backoff` |
| `Skip` | potwierdza element bez efektu, zwiększa licznik `skipped` i loguje opis elementu |
| `DeadLetter` | zapisuje element do `DeadLetterQueue`, potem go potwierdza |
| `Pause(duration)` | wstrzymuje źródło sharda i ponawia po czasie, bez restartu i bez utraty stanu w pamięci (np. Elasticsearch niedostępny) |
| `Restart` | restartuje workera według `RestartPolicy`; niepotwierdzony element wraca |
| `Stop` | worker przechodzi w `Failed` i czeka na ręczny `start`; shard zostaje przypisany do repliki |

Decyzje łączy się w eskalację:

```scala
override def failures: FailureHandling =
  FailureHandling
    .on[SocketTimeoutException](Retry(Backoff.exponential(200.millis, max = 10.seconds)).upTo(5).thenPause(1.minute))
    .on[ValidationException](DeadLetter)
    .on[MalformedInputException](Skip)
    .at(FailureStage.Emit)(Retry(Backoff.fixed(1.second)).upTo(10).thenRestart)
    .poisonAfter(deliveries = 3)(DeadLetter)
    .otherwise(Restart)
```

- Reguły sprawdza się po kolei. Dopasowanie jest po typie wyjątku (`[E <: Throwable: ClassTag]`, także przyczyny w łańcuchu `getCause`), po etapie albo po własnym predykacie.
- `poisonAfter` kieruje element dostarczony po raz `n` (po restartach) do podanej decyzji, więc jeden zły rekord nie blokuje partycji na zawsze.
- `ErrorClassifier` to komponent z `given ConfigPath`. Pozwala całej aplikacji oznaczać wyjątki jako przejściowe (`Transient`) albo trwałe (`Permanent`), a reguły mogą dopasować klasę zamiast typu: `.on(ErrorClass.Transient)(...)`.
- Konfiguracja może nadpisać reguły z kodu (punkt 11). Domyślne `FailureHandling.default` to `Restart` z `poisonAfter(5)(Stop)`.
- **Walidacja przy starcie**: decyzje, które złamałyby spójność, są błędem startu, a nie cichym zachowaniem.
  - `Skip` i `DeadLetter` są dozwolone tylko na etapie `Process`, a na etapie `Emit` dopiero po jawnym `allowLossOnEmit`.
  - Na etapach `State` i `Acknowledge` dozwolone są tylko `Retry`, `Pause`, `Restart` i `Stop`, bo pominięcie zapisu stanu przy potwierdzonym wejściu gubi dane.
- W `StatefulProcessor` `Skip` na etapie `Process` potwierdza wejście bez zmiany stanu i bez emisji.

### 7. Dead letter

```scala
final case class DeadLetter[T](
  processor: ProcessorId,
  slot: Int,
  shard: Int,
  replica: String,
  inputId: InputId,
  value: T,
  stage: FailureStage,
  error: FailureInfo,
  attempts: Int,
  deliveries: Int,
  failedAt: Instant
)

trait DeadLetterQueue[T] {
  def publish(letter: DeadLetter[T]): Unit
}

trait DeadLetterBrowser[T] {
  def list(processor: ProcessorId, limit: Int): List[DeadLetter[T]]

  def replay(ids: List[InputId]): ReplayResult

  def discard(ids: List[InputId]): Unit
}
```

- Procesor wskazuje kolejkę przez `def deadLetters: DeadLetterQueue[T]` albo bean `DeadLetterQueue` z configu. Domyślna kolejka loguje i trzyma ostatnie wpisy w pamięci z limitem.
- `FailureInfo` to klasa wyjątku, komunikat i ślad stosu jako tekst, żeby wpis dało się serializować.
- `DeadLetterBrowser` jest opcjonalny (ma go kolejka w pamięci i topik Kafki). `replay` kieruje element do slotu, z którego pochodzi. Jeśli slot należy do sharda innej repliki, manager przekazuje polecenie tej replice przez API klastra.
- Błąd zapisu do dead letter daje `Restart`, bo element nie może zniknąć.
- `ElementDescriber[T]` (given albo domyślnie `toString` obcięty do limitu) opisuje element w logach i wpisach, żeby nie logować całych dokumentów ani pól `Secret`.

### 8. Nadzór workerów

Manager tworzy `ShardSupervisor` dla każdego sharda przydzielonego tej replice. Nadzorca prowadzi jawną maszynę stanów workera:

```
Created → Preparing → Running ⇄ Paused
                  ↘      ↓
                   Backoff → Preparing
                         ↓
         Releasing → Released | Stopped | Completed | Failed
```

- Workery są niezależne: restart, wstrzymanie albo `Failed` jednego sharda nie zatrzymuje pozostałych.
- Nadzorca nie zmienia stanu sam. Wysyła zdarzenia do wątku sterującego managera, który podejmuje decyzję i zapisuje nową migawkę.
- `RestartPolicy` (komponent z `given ConfigPath`) to `Backoff` wykładniczy z jitterem i maksimum, limit prób w oknie czasu (`max-attempts` w `within`) i zerowanie po `reset-after` stabilnej pracy. Po wyczerpaniu worker przechodzi w `Failed`.
- `CompletionPolicy`: po normalnym zakończeniu źródła `stop` (zadanie wsadowe) albo `restart(delay)`.
- `Backoff` jest wspólny dla `Retry`, `Pause` i `RestartPolicy`.
- Zatrzymanie: flaga, `Execution.stop()`, czekanie z limitem, potem przerwanie wątku i porzucenie z wpisem w logu. Bez odpytywania co 50 ms.
- Oddanie sharda (zmiana przydziału, zmiana liczby shardów, zamknięcie) przechodzi przez `Releasing`: worker kończy bieżący element, potwierdza przetworzone, zrzuca stan slotów i zamyka źródło.

### 9. `ProcessorManager`

```scala
trait ProcessorManager {
  def processors: List[ProcessorDescriptor]

  def topology: ProcessingTopology

  def status(id: ProcessorId): ProcessorStatus

  def statistics(id: ProcessorId): ProcessorStatistics

  def health: ProcessingHealth

  def start(id: ProcessorId): Unit

  def stop(id: ProcessorId, timeout: FiniteDuration): StopResult

  def restart(id: ProcessorId): Unit

  def restart(id: ProcessorId, shard: Int): Unit

  def pause(id: ProcessorId): Unit

  def pause(id: ProcessorId, shard: Int): Unit

  def resume(id: ProcessorId): Unit

  def resume(id: ProcessorId, shard: Int): Unit

  def reshard(id: ProcessorId, shards: Int): ReshardResult

  def resetFailures(id: ProcessorId): Unit

  def stopAll(timeout: FiniteDuration): List[StopResult]

  def cluster: ClusterProcessorOperations
}

trait ClusterProcessorOperations {
  def topology: ClusterProcessingTopology

  def status(id: ProcessorId): Map[Member, Either[ClusterError, ProcessorStatus]]

  def statistics(id: ProcessorId): ClusterProcessorStatistics

  def health: Map[Member, Either[ClusterError, ProcessingHealth]]

  def start(id: ProcessorId): ClusterResult

  def stop(id: ProcessorId, timeout: FiniteDuration): ClusterResult

  def pause(id: ProcessorId): ClusterResult

  def resume(id: ProcessorId): ClusterResult

  def restart(id: ProcessorId, shard: Int): ClusterResult

  def reshard(id: ProcessorId, shards: Int): ClusterResult
}
```

- Metody na `ProcessorManager` działają lokalnie, na workerach tej repliki.
- `cluster` działa na wszystkich replikach przez API klastra z COILIX-00027 (endpointy procesorów rejestruje `processing`). `restart(id, shard)` trafia do repliki, która ma ten shard. `statistics` składa sumę klastra i rozbicie per replika. `ClusterResult` ma wynik każdej repliki, więc niedostępna replika nie ukrywa sukcesu pozostałych.
- `reshard` w klastrze idzie przez koordynatora: zebranie zgody wszystkich żywych replik, oddanie shardów, nowy podział z wyższą epoką i przejęcie. Gdy któraś replika nie odpowiada, operacja się nie zaczyna. Lokalne `reshard` jest dozwolone tylko przy jednej replice, inaczej daje `ClusterRequiredException`.
- Bez skonfigurowanego transportu klastra `cluster` działa tylko na tej replice i mówi to w wyniku (`ClusterResult.localOnly`), zamiast udawać sukces w klastrze.
- `topology` zwraca tożsamość repliki, liczbę replik i dla każdego procesora `slotCount`, `shardCount`, epokę, strategię przydziału oraz listę shardów tej repliki ze slotami.
- Operacja lokalna na shardzie, którego ta replika nie ma, daje `ShardNotOwnedException` z repliką, która go ma.
- Operacje są bezpieczne przy wywołaniach z wielu wątków (idą przez kolejkę wątku sterującego) i idempotentne: `stop` zatrzymanego procesora nic nie robi, `pause` wstrzymanego też nie.
- `start` po `Failed` zeruje politykę restartu i liczniki dostarczeń.
- Identyfikator bez procesora daje `NoSuchProcessorException`, a niedozwolone przejście (np. `resume` w trakcie `Releasing`) `IllegalProcessorStateException` z bieżącym stanem.
- `ProcessorDescriptor` zawiera identyfikator, styl, klasę, `slotCount`, `shardCount`, strategię przydziału i efektywną konfigurację (z polityką błędów do podglądu).
- `ProcessingHealth` tej repliki to `Up`, `Degraded` (worker w `Backoff` albo `Paused`, lag ponad próg, rozjazd configu z innymi replikami) albo `Down` (worker w `Failed`), z powodem per procesor i shard. `Down` nie oznacza nieżywego poda, więc w przewodniku podpinamy to pod readiness albo alerty, a nie pod liveness.

### 10. Statystyki i zdarzenia

- `ProcessorStatistics` to niemutowalna migawka z sumą tej repliki i listą `ShardStatistics`:
  - numer sharda, sloty, epoka, stan i czas wejścia w ten stan, czas przejęcia sharda i startu workera,
  - restarty łącznie i od resetu, następny restart, ostatni błąd (etap, klasa, komunikat, czas),
  - elementy przetworzone, ponowione, pominięte, w dead letter i w toku,
  - czas przetwarzania (liczba, suma, maksimum, percentyle z histogramu o stałych przedziałach),
  - tempo z ostatniej minuty i pięciu minut, lag per slot, ostatnie przetworzenie,
  - liczba kluczy stanu w pamięci i zrzuconych.
- Workery stylów bezstanowego, wsadowego i stanowego liczą statystyki same. Procesor własny raportuje przez `shard.metrics` (`recordProcessed`, `recordFailed`, `gauge`).
- Metryki mają etykiety `processor`, `shard` i `replica`, więc system metryk składa widok klastra bez udziału managera.
- Zdarzenia `ProcessorEvent` (sealed): `StateChanged`, `ShardAcquired`, `ShardReleased`, `Resharded`, `ElementFailed`, `ElementRetried`, `ElementSkipped`, `ElementDeadLettered`, `RestartScheduled`, `WorkerFailed`, `Paused`, `Resumed`. Odbierają je beany `ProcessorListener` (metryki, alerty). Są wywoływane poza wątkiem przetwarzania, żeby wolny listener nie hamował workera.

### 11. Konfiguracja procesorów

```hocon
coilix.processors {
  autostart = true
  shutdown-timeout = 30s
  require-all-ids = false
  defaults {
    slots = 256
    assignment = round-robin
    on-completion = stop
    poll { max = 500, timeout = 1s }
    restart {
      backoff { initial = 1s, max = 2m, multiplier = 2.0, jitter = 0.2 }
      max-attempts = 10
      within = 15m
      reset-after = 5m
    }
    failures = [
      { exceptions = ["java.net.SocketTimeoutException"], decision = retry, max-attempts = 5, then = pause, pause = 1m }
      { stage = emit, decision = retry, backoff { initial = 1s }, max-attempts = 10, then = restart }
      { decision = restart }
    ]
    poison-after = 5
    dead-letters { class = "dev.coilix.processing.InMemoryDeadLetterQueue", capacity = 1000 }
    health { lag-threshold = 10000 }
  }
  vehicle-sessions {
    shards = 12
    shards = ${?VEHICLE_SESSIONS_SHARDS}
    tick-interval = 10s
  }
  entity-indexer {
    shards = 6
    concurrency { mode = per-key, parallelism = 8 }
  }
  daily-report {
    shards = 1
    assignment = pinned
    pinned-replica = 0
  }
}
```

- Kolejność pierwszeństwa: wartości domyślne frameworka, potem `defaults`, `defaultShards` i reguły z kodu procesora, a na końcu klucze procesora.
- Klucz procesora to `ProcessorId.name`. Klucz, który nie pasuje do żadnego procesora, to ostrzeżenie przy starcie (literówka w configu).
- `shards`, `slots` i `assignment` muszą być takie same na wszystkich replikach. Repliki porównują odcisk tych wartości przez API klastra (COILIX-00027). Przy rozjeździe procesor przechodzi w `Degraded` i nie przejmuje shardów z wyższą epoką, dopóki rozjazd trwa.
- `failures` z configu zastępuje reguły z kodu w całości, bez łączenia list, żeby efektywna polityka była jednoznaczna. `ProcessorDescriptor` pokazuje, skąd pochodzi.
- `enabled = false` rejestruje procesor w managerze jako `Stopped`, więc można go uruchomić ręcznie.
- Klasy wyjątków z configu są sprawdzane przy starcie: nieznana klasa to błąd startu.
- Przeładowanie configu w działającej aplikacji jest poza zakresem. Liczbę shardów w trakcie działania zmienia `reshard`, a wartość z configu obowiązuje od następnego startu.

### 12. Przetwarzanie stanowe

Reguły z bulldozera:

- stan jest utrwalany (albo usuwany) przed potwierdzeniem wejścia, a emisja następuje po utrwaleniu,
- krok musi być idempotentny (at-least-once),
- tick przechodzi po stanach w pamięci sharda, a eviction zrzuca brudny stan i zwalnia pamięć.

`Step[S, R]` ma `state`, `emit`, `persist`, `acknowledge`, `remove` i konstruktory `keep`, `update`, `done`. Niespójnej kombinacji (potwierdzenie bez utrwalenia) nie da się zbudować, bo konstruktory jej nie oferują. Zastępuje to `require` w runtime.

`StateStore[K, S]` to SPI: `get`, `put`, `flush`, `flushIfDirty`, `flushAll`, `remove`, `evict`, `snapshot`, `bootstrap(slots)`, opcjonalnie `fence(epoch)`. Store tworzy się per shard, ale wewnętrznie dzieli stan po slotach, więc zmiana liczby shardów przenosi całe sloty bez przeliczania kluczy. Implementacja w pamięci jest w `processing`, a na kompaktowanym topiku w `processing-kafka`, z działającym bootstrapem, odczytem po wskaźniku i ochroną epoką. Store zapisuje `slotCount`, więc zmiana liczby slotów bez migracji blokuje start.

### 13. Przekazanie sharda i epoka

Ten sam mechanizm obsługuje zmianę przydziału, zmianę liczby shardów i odejście repliki:

1. Nowy podział dostaje wyższą epokę: przy przydziale statycznym z restartu, przy `coordinated` i `reshard` od koordynatora.
2. Shardy utracone przechodzą przez `Releasing` i wołają `onReleased(Reassigned)` albo `onReleased(Resharded)`.
3. Shardy zyskane startują z `prepare` (bootstrap stanu ich slotów, odtworzenie offsetów) i `onAcquired`.

Przy rolling update z nową konfiguracją dwie repliki mogą przez chwilę obsługiwać te same sloty. Przetwarzanie at-least-once to toleruje, a dla `StatefulProcessor` jest **ochrona przed starym właścicielem**: store zapisuje epokę właściciela slotu, a zapis ze starszą epoką jest odrzucany jako błąd etapu `State` z decyzją `Restart`. Store na kompaktowanym topiku dostaje to w tasku Kafki (transakcyjny producent albo epoka w rekordzie).

### 14. Wątki i cykl życia

- Każdy worker działa na nazwanym wątku wirtualnym `coilix-processor-<nazwa>-<shard>`, przez `TaskExecutor` z COILIX-00005, gdy już będzie. Równoległość wewnątrz sharda (`Concurrency`) idzie na kolejnych wątkach wirtualnych z limitem. Manager ma jeden wątek sterujący `coilix-processor-manager`.
- MDC zawiera nazwę procesora, numer sharda, slot, `replica-id` i `InputId` w każdym logu przetwarzania.
- Procesory z `coilix.processors.autostart = true` startują po zbudowaniu kontekstu i po `ApplicationRunner`. Przy przydziale `coordinated` dopiero po dołączeniu repliki do klastra.
- Zamknięcie kontekstu (SIGTERM z Kubernetesa) oddaje wszystkie shardy równolegle (`onReleased(Shutdown)`) w `shutdown-timeout`, ogłasza odejście repliki w klastrze i loguje workery, które nie zdążyły. Przewodnik każe ustawić `terminationGracePeriodSeconds` większe niż `shutdown-timeout`.

### 15. Wspólny model i silniki wykonania

Moduł `processing` dzieli się na dwie warstwy, żeby `IO` i Pekko Streams nie wymagały przekładania na `Future` ani osobnej logiki nadzoru:

- **Wspólny model** (czyste dane i funkcje, bez wykonywania): identyfikatory, sloty, shardy i przydział, maszyna stanów workera jako przejścia `(stan, zdarzenie) => (stan, polecenia)`, `RestartPolicy`, `CompletionPolicy`, `Backoff`, `FailureHandling` zwracające decyzje, statystyki, zdarzenia, konfiguracja, dead letter, `ProcessorManager` i operacje w klastrze.
- **Silnik wykonania** (SPI `ProcessorEngine`): uruchamia workera sharda w swoim modelu współbieżności, wykonuje polecenia modelu (start, czekanie, backoff, zatrzymanie) i raportuje zdarzenia (element przetworzony, błąd, zakończenie).

```scala
trait ProcessorEngine {
  def supports(definition: ProcessorDefinition): Boolean

  def start(definition: ProcessorDefinition, shard: Shard, events: WorkerEvents): RunningWorker
}

trait RunningWorker {
  def stop(timeout: FiniteDuration): StopOutcome

  def pause(): Unit

  def resume(): Unit
}
```

- Silnik dobierany jest automatycznie po traicie procesora. Aplikacja nic nie konfiguruje, a moduły integracyjne rejestrują silniki przez `CoilixModule`. Dwa silniki dla tego samego procesora albo brak silnika to błąd startu z nazwą procesora i brakującym modułem.

| Procesor | Silnik | Moduł |
|---|---|---|
| `StatelessProcessor`, `BatchProcessor`, `StatefulProcessor`, `CustomProcessor`, `BlockingProcessor` | wątki wirtualne | `processing` |
| `IOStatelessProcessor`, `IOBatchProcessor`, `IOStatefulProcessor` | fibery `IO` (backoff `IO.sleep`, zatrzymanie przez anulowanie, nadzór na `Supervisor`) | `processing-cats-effect` (COILIX-00020) |
| `PekkoProcessor` | graf Pekko per shard, sterowany wartością materializowaną przez `Stoppable[M]` | `processing-pekko` (COILIX-00029) |

- Warianty `IO*` mają te same metody co styl bezpośredni, ale zwracają `IO`: `process(element, context): IO[Unit]`, `step(...): IO[Step[S, R]]`, `InputSource`/`Output`/`StateStore` w wersjach `IOInputSource` itd. Model (`Step`, decyzje, statystyki) jest wspólny.
- Runtime'y (`IORuntime`, `ActorSystem`) są beanami modułów `cats-effect` i `pekko`, jednymi na aplikację. Silniki je wstrzykują, więc zamykają się przed nimi.
- Silnik zastępuje się kluczem `class` w `coilix.processors.engines.<nazwa>`, jak inne komponenty frameworka.

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `context` | rdzeń (zmiana) | — | `ComponentCollector` i instancjonowanie klas spoza rejestru beanów z cyklem życia |
| `cluster` | rdzeń | `context` | COILIX-00027: członkostwo, tożsamość repliki, koordynator, API między replikami |
| `processing-annotations` | Java | brak | `@Processor` |
| `processing` | funkcja | `context`, `cluster`, `processing-annotations` | API z punktów 1–15, wspólny model, `ProcessorEngine` i silnik na wątkach wirtualnych, `ShardAssignment`, `ShardKeys`, endpointy klastra procesorów, implementacje w pamięci, `CoilixModule` |
| `processing-cats-effect` | integracja | `processing`, `cats-effect` (COILIX-00020) | silnik fiberów `IO`, procesory `IO*`, `IOInputSource`, `IOOutput`, `IOStateStore` |
| `processing-pekko` | integracja | `processing`, `pekko` (COILIX-00029) | `PekkoProcessor`, `Stoppable[M]`, `PekkoShardContext`, silnik grafów |
| `processing-web` | integracja | `processing`, `web` | publiczne endpointy administracyjne: topologia klastra, status, statystyki, zdrowie, sterowanie, przegląd i `replay` dead letter |
| `processing-micrometer` | integracja | `processing`, Micrometer | `ProcessorListener` publikujący metryki z etykietami `processor`, `shard`, `replica` |

Pakiet `dev.coilix.processing`, artefakty `coilix-processing*`. Numery katalogów ustala się razem z przenumerowaniem z COILIX-00010, COILIX-00013, COILIX-00017 i COILIX-00023.

Przykład procesora stanowego:

```scala
@Processor
final class VehicleSessions(topics: KafkaSources, states: KafkaStateStores, outputs: KafkaOutputs)
    extends StatefulProcessor[Telemetry, Vin, Session, SessionClosed] {

  override val id: AppProcessor = AppProcessor.VehicleSessions

  override def defaultShards: Int = 12

  override def source(shard: Shard): InputSource[Telemetry] =
    topics.slots(TelemetryTopic, shard)

  override def store(shard: Shard): StateStore[Vin, Session] =
    states.compacted(SessionTopic, shard)

  override def output(shard: Shard): Output[SessionClosed] =
    outputs.topic(SessionClosedTopic)

  override def key(input: Telemetry): Vin =
    input.vin

  override def initial(key: Vin): Session =
    Session.empty(key)

  override def step(state: Session, input: Telemetry, context: ElementContext): Step[Session, SessionClosed] =
    Step.update(state.record(input))

  override def onTick(state: Session, idle: FiniteDuration, context: TickContext): Step[Session, SessionClosed] = {
    if (idle > 5.minutes) {
      Step.done(state, List(SessionClosed(state)))
    } else {
      Step.keep(state)
    }
  }

  override def failures: FailureHandling =
    FailureHandling
      .on[InvalidTelemetryException](DeadLetter)
      .at(FailureStage.State)(Retry(Backoff.exponential(500.millis, max = 30.seconds)).upTo(20).thenRestart)
      .poisonAfter(deliveries = 3)(DeadLetter)
      .otherwise(Restart)
}
```

Sterowanie z innego beana:

```scala
@Component
final class Maintenance(processors: ProcessorManager) {
  def beforeReindex(): ClusterResult =
    processors.cluster.pause(AppProcessor.EntityIndexer)

  def scaleSessions(): ClusterResult =
    processors.cluster.reshard(AppProcessor.VehicleSessions, shards = 24)
}
```

### Konektory (osobne taski po tym)

Każdy konektor to moduł funkcji z klientem, używalny bez procesorów, i moduł integracyjny z `InputSource`, `Output`, `DeadLetterQueue` i ewentualnie `StateStore`, bo moduły funkcji nie zależą od siebie.

| Task | Moduły | Zawartość z bulldozera | Biblioteka |
|---|---|---|---|
| Kafka | `kafka`, `processing-kafka` | klient, tematy z serializacją (COILIX-00024), admin, partycje jako sloty (`assign` partycji slotów sharda) albo grupa (`subscribe` z przydziałem `all`), commit jako `Acknowledgement` z kolejnością w partycji, `pause`/`resume` partycji, lag per partycja, odtwarzanie offsetów z poprzednich grup (`legacyFlowNames`), dead letter na topiku z `DeadLetterBrowser`, `StateStore` na kompaktowanym topiku współpartycjonowanym z wejściem i z ochroną epoką | `kafka-clients` |
| Elasticsearch | `elasticsearch`, `processing-elasticsearch` | klient, repozytorium encji, `Output` i `BatchResult` z bulk z częściowymi porażkami | elastic4s albo oficjalny klient Java (do decyzji) |
| SFTP | `sftp`, `processing-sftp` | pula połączeń, `SftpService`, ścieżki, `InputSource` nowych plików ze slotem z hasha ścieżki, przeniesienie po potwierdzeniu i do katalogu błędów przy `reject` | sshj |

Kolejność: najpierw Kafka (bez niej nie ma stanowego przetwarzania z bulldozera), potem Elasticsearch, potem SFTP. Na końcu `processing-pekko` dla flow mocno opartych na operatorach Pekko.

### Kroki (osobne PR-y)

1. Ten szkic i COILIX-00027, potem ADR z decyzjami: nazwa, procesory poza rejestrem beanów, identyfikatory, moduły, model wykonania, sloty, shardy i repliki, model błędów.
2. Rdzeń: `ComponentCollector` i instancjonowanie klas spoza rejestru beanów z cyklem życia.
3. COILIX-00027 krok 1: tożsamość repliki i statyczne członkostwo.
4. `processing-annotations` i szkielet `processing`: `ProcessorId`, zbieranie i tworzenie procesorów, wątek sterujący managera, sloty, `Shard`, `ShardAssignment` (strategie statyczne), `ShardKeys`, wspólny model nadzoru (maszyna stanów jako czyste przejścia, `RestartPolicy`, `Backoff`), SPI `ProcessorEngine` i silnik na wątkach wirtualnych, `CustomProcessor`, `BlockingProcessor`, lokalne operacje managera, konfiguracja, cykl życia.
5. `StatelessProcessor`, `InputSource`, `Input`, `Acknowledgement`, `Output`, implementacje w pamięci, statystyki i zdarzenia.
6. `FailureHandling` z etapami, decyzjami, eskalacją, `poisonAfter`, `ErrorClassifier`, walidacją przy starcie i regułami z configu. `DeadLetterQueue` w pamięci z `DeadLetterBrowser`.
7. `Concurrency` (równoległość z potwierdzeniami w kolejności) i `BatchProcessor`.
8. `StatefulProcessor`, `Step`, `StateStore` w pamięci ze slotami, tick i retencja, epoka i kontrola `slotCount`.
9. `pause`, `resume`, lokalne `reshard`, `ProcessingHealth`, przekazanie sharda (`Releasing`).
10. Po COILIX-00027 (API między replikami): `cluster` w managerze, endpointy procesorów, porównanie odcisku configu, przydział `coordinated`, `reshard` w klastrze, `replay` na właściwej replice.
11. Przewodnik w obu językach (z manifestem StatefulSetu) i procesory w demo.
12. `processing-cats-effect` (po COILIX-00020) i `processing-pekko` (COILIX-00029).
13. Konektory, `processing-web` i `processing-micrometer`.

## Kryteria akceptacji

1. Klasa z `@Processor` nie jest beanem: `ApplicationContext.get` jej nie zwraca, a bean, który próbuje ją wstrzyknąć, kończy start błędem `NoSuchBeanException` z podpowiedzią o `ProcessorManager`. Zależności procesora, `@Value`, `@PostConstruct` i `@PreDestroy` działają.
2. Dwa procesory z tym samym `ProcessorId` to błąd startu z nazwami obu klas. Z `require-all-ids = true` wartość sealed traitu bez procesora to błąd startu.
3. `shards = 12` z configu nadpisuje `defaultShards`. Na replice 1 z 3 przy `round-robin` startują workery shardów 1, 4, 7 i 10 na wątkach `coilix-processor-<nazwa>-<shard>`, a `shards` większe od `slots` to błąd startu.
4. Test z trzema kontekstami pokazuje, że każdy slot ma dokładnie jeden shard, a każdy shard jedną replikę. Po zmianie `shards` z 6 na 12 i po zmianie liczby replik przypisanie kluczy do slotów (`ShardKeys.slotOf`) się nie zmienia.
5. Lokalne `reshard` na jednej replice oddaje shardy przez `Releasing`, a stan `StateStore` w pamięci jest po zmianie dostępny w nowych shardach bez utraty kluczy. Przy kilku replikach lokalne `reshard` daje `ClusterRequiredException`.
6. `cluster.reshard` w teście z trzema kontekstami i transportem w pamięci (COILIX-00027) zmienia podział na wszystkich replikach z jedną nową epoką. Gdy jedna replika nie odpowiada, podział się nie zmienia.
7. `pinned(0)` z `shards = 1` uruchamia procesor tylko na replice 0, a na pozostałych `topology` pokazuje go bez shardów.
8. `Retry(...).upTo(5).thenRestart` ponawia element pięć razy w workerze, a potem restartuje tylko tego workera. Niepotwierdzony element przychodzi ponownie z `delivery = 2`, a pozostałe shardy pracują dalej. Test na kontrolowanym zegarze.
9. `poisonAfter(3)(DeadLetter)` kieruje element do dead letter przy trzecim dostarczeniu, a następne elementy są przetwarzane dalej.
10. `Skip` na etapie `State` albo `Acknowledge` to błąd startu z nazwą procesora i reguły.
11. `Pause(d)` wstrzymuje źródło sharda bez restartu, a stan procesora stanowego w pamięci zostaje.
12. `BatchResult` z częściowymi porażkami potwierdza udane elementy, a nieudane przechodzą przez `FailureHandling` pojedynczo.
13. Przy `Concurrency.parallel(n)` potwierdzenia idą w kolejności źródła w obrębie slotu, nawet gdy elementy kończą się w innej kolejności.
14. Po `max-attempts` restartów w oknie `within` worker przechodzi w `Failed`, a po `reset-after` stabilnej pracy licznik prób wraca do zera.
15. Zapis do `StateStore` w pamięci ze starszą epoką jest odrzucany, a store z innym zapisanym `slotCount` blokuje start.
16. `ProcessorManager` zatrzymuje, wznawia, wstrzymuje i restartuje procesor oraz pojedynczy shard. Shard innej repliki daje lokalnie `ShardNotOwnedException`, a przez `cluster.restart` trafia do właściwej repliki. Niedozwolone przejście daje `IllegalProcessorStateException`.
17. Współbieżne wywołania managera z wielu wątków (test z setkami poleceń) kończą się spójną migawką bez utraconych zmian.
18. `statistics` zwraca dla każdego sharda tej repliki stan, sloty, liczniki, restarty, ostatni błąd i tempo, a `cluster.statistics` sumę i rozbicie per replika. `health` zwraca `Down` przy workerze w `Failed` i `Degraded` przy rozjeździe configu między replikami.
19. W `StatefulProcessor` potwierdzenie wejścia następuje dopiero po utrwaleniu stanu. Test z `StateStore`, który rzuca przy zapisie, pokazuje, że wejście nie zostało potwierdzone.
20. `failures` z configu zastępuje reguły z kodu, a nieznana klasa wyjątku w configu to błąd startu.
21. Zamknięcie kontekstu oddaje wszystkie shardy, a te, które nie zdążyły w `shutdown-timeout`, są zalogowane.
22. `processing` nie zależy od Pekko, cats-effect, klientów Kafki ani Kubernetesa, co sprawdza `checkModuleLayers`.
23. Demo ma procesor bezstanowy z dead letter i procesor stanowy na źródłach w pamięci ze slotami, sterowane z beana przez `ProcessorManager`. Logi pokazują shardy, sloty i statystyki.
24. Ten sam procesor bezstanowy napisany jako `StatelessProcessor` i jako `IOStatelessProcessor` ma te same statystyki, restarty i decyzje `FailureHandling` w teście na tym samym źródle w pamięci, a model nadzoru ma testy bez żadnego silnika (czyste przejścia).

## Poza zakresem

- Konsensus (Raft, Paxos) i silna spójność między replikami. Koordynator z COILIX-00027 wystarcza do przydziału shardów, a ochronę przed rozjazdem daje epoka.
- Exactly-once między topikami.
- Zmiana liczby slotów w działającym klastrze (np. nowe partycje topiku) bez migracji stanu.
- Przeładowanie configu w działającej aplikacji.
- Procesory w stylu `F[_]` (`IO`, `ZIO`): moduły integracyjne po COILIX-00020.
- Harmonogramy cron. Proste zadania cykliczne to `@Scheduled` z COILIX-00005, które może korzystać z tożsamości repliki z COILIX-00027, żeby działać na jednej replice.
- Trwały licznik dostarczeń między restartami aplikacji, poza źródłami, które podają go same.

## Ryzyka i otwarte pytania

1. `ComponentCollector` zmienia skaner w rdzeniu. Trzeba go zaprojektować ogólnie, bo przyda się też innym modułom (np. kontrolery HTTP w COILIX-00023 mogą chcieć tego samego zamiast beanów).
2. Procesor spoza rejestru nie może być zależnością `@Bean`-ów ani `ApplicationRunner`. To zamierzone, ale trzeba to jasno opisać i dać czytelny błąd.
3. Sloty dla źródeł bez partycji (domyślnie 256) ograniczają maksymalną liczbę shardów. Zmiana `slots` to migracja stanu, więc wartość domyślną trzeba dobrać z zapasem.
4. Przy przydziale statycznym rozjazd configu w trakcie rolling update daje chwilowe podwójne albo brakujące shardy. Odcisk configu, epoka i at-least-once to łagodzą, a pełne rozwiązanie daje przydział `coordinated`.
5. „Processor” kojarzy się z `javax.annotation.processing.Processor` (procesory adnotacji). W kodzie aplikacji to inny pakiet i nic nie koliduje, ale w dokumentacji tam, gdzie kontekst nie jest jasny, trzeba pisać „procesor tła”.
6. Model wykonania wymaga JDK 21+ dla wątków wirtualnych. Na JDK 17 zostaje zwykła pula, tak jak w HTTP (COILIX-00023).
7. Licznik dostarczeń w pamięci po `InputId` rośnie z liczbą elementów w obiegu. Trzeba go czyścić po potwierdzeniu i ograniczyć rozmiarem.
8. Moduł `processing-annotations` dodaje kolejny moduł Java. Alternatywa to `@Processor` w `02_annotations`, ale rdzeń nie powinien znać adnotacji funkcji (to samo pytanie co w COILIX-00023).
