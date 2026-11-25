# 0043: Coilix na IO, Resource i fs2, bez Pekko i bez tagless final

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00032

## Kontekst

ADR 0036 dodał moduł `pekko` z `ActorSystem` i `Materializer` na pulach Coilix. Pekko Streams miało być silnikiem strumieni, bo bulldozer, który ma przejść na Coilix, opiera na nim flowy (`OptimusFlow`, `ShardDefinition`, `ShardControl`). ADR 0042 przeniósł runtime na `IO` i pozwolił pisać komponenty aplikacji w tagless final (`OrderService[F[_]: Async]`), a kontener wiąże ich `F[_]` z `IO`.

Opiekun 2026-11-25 kazał sprawdzić, czy flowy bulldozera da się napisać na fs2, i zapowiedział rezygnację z Pekko, jeśli tak. Przegląd modułu `flow` i jedenastu flowów bulldozera (`core`, `catalog`) pokazał, że wszystkie są liniowe:

| Bulldozer (Pekko) | fs2 i fs2-kafka |
|---|---|
| `committableSource` z ręcznym przydziałem partycji do sharda | `KafkaConsumer.stream(...).assign(topic, partycje).records` |
| `mapAsync` i `mapAsyncIO(n)` | `parEvalMap(n)` |
| `groupedWithin(50, 5.seconds)` | `groupWithin(50, 5.seconds)` |
| `Committer.flow` | `commitBatchWithin` |
| `Producer.flexiFlow` | `KafkaProducer.pipe` |
| `Source.tick` z `merge` (`StatefulFlow`) | `awakeEvery` z `mergeHaltL` |
| `Source.io` (`recover`, katalog) | `Stream.eval` |
| własny `GraphStage` (`EveryNth`) | `zipWithIndex` z `evalTap`, w ogólnym przypadku `Pull` |
| `KillSwitch`, `Consumer.Control`, `Cancellable` | sygnał zatrzymania źródła (niżej) |

Nie ma tam podstrumieni z `groupBy`, grafów `GraphDSL`, klastra ani shardingu Pekko („shard” to pojęcie bulldozera). `ShardDefinition` i `ShardControl` odpytują `Future` zakończenia grafu co 50–100 ms, bo anulowanie `IO.fromFuture` wisi na niedostępnym brokerze. Poza flowami Pekko jest w bulldozerze w HTTP i WebSocket (pekko-http, aktory subskrypcji GraphQL), w SFTP (pekko-connectors-ftp) i w dwóch parserach JSON na `GraphStage`. Żadne z nich nie wymaga Pekko.

Po przeglądzie opiekun postawił trzy warunki:

1. Zatrzymanie flowu nie może być zwykłym anulowaniem fibera. Przy Kafce to, co źródło już przeczytało, reszta przepływu musi przetworzyć i zatwierdzić. Mechanizm ma być bezpieczny.
2. HTTP też na fs2, nie na pekko-http.
3. Coilix jest frameworkiem na frameworku, rdzeniem innych aplikacji: logika frameworka to własne implementacje Coilix nad cats-effect, fs2 i bibliotekami z nimi zgodnymi.

Rozważone było też ukrycie `IO` za własnym typem (nieprzezroczysty alias) i dalsze wspieranie tagless final. Tagless final nic tu nie daje: efekt zawsze jest `IO` (ADR 0042 wyklucza inne), testy na innym efekcie są w praktyce rzadkie, a `TestControl` i podmiana beanów wystarczają. Kosztuje za to sporo: kod kontenera do wiązania `F[_]`, klasy typów jako beany, dłuższe sygnatury, gorsze błędy kompilacji i dwa style w dokumentacji. Komponent z `F[_]: Async` i tak importuje klasy typów cats-effect. Opiekun zdecydował: „robimy na IO, Resource i fs2”.

## Decyzja

- **Typy efektu i strumienia są jawne.** API Coilix i komponenty aplikacji używają konkretnych `cats.effect.IO`, `cats.effect.Resource[IO, _]` i `fs2.Stream[IO, _]`. Nie ma własnego typu efektu ani aliasu.
- **Bez tagless final.** Kontener nie wiąże niezwiązanych `F[_]` z `IO` i nie rejestruje `Async[IO]` jako beana. Znika `EffectTypeBinding`, `CoilixModule.effectType`, `ConflictingEffectTypesException` i bean `ioAsync`. Zależność z niezwiązanym parametrem typu znów jest błędem startu, jak w ADR 0019. Pozostała część ADR 0042 (kontrakty runtime'u w `IO`, kontener synchroniczny, prymitywy cats-effect zamiast `synchronized` i `Atomic*`) obowiązuje bez zmian.
- **Bez Pekko.** Moduł `25_pekko` i grupa wydań `pekko` znikają. Nie został wydany, więc to nic nie kosztuje. Przykłady z demo i przewodników przechodzą na fs2. Numer 25 zostaje wolny.
- **fs2 w rdzeniu.** `fs2-core` jest zależnością rdzenia, tak jak cats-effect, bo strumień jest typem kontraktów (np. źródeł procesorów). Biblioteki zewnętrzne zależne od fs2 (fs2-kafka, http4s, fs2-io) trafiają tylko do modułów funkcji.
- **Własne implementacje logiki frameworka.** Coilix daje własne typy i implementacje tam, gdzie jest logika frameworka, której aplikacja nie ma móc zrobić źle: cykl życia procesora i jego shardów, zatrzymanie z opróżnianiem, restart z backoffem, źródła i zatwierdzanie Kafki, rejestracja tras HTTP, konfiguracja i metryki. Ciało przepływu aplikacja pisze zwykłymi operatorami fs2.
- **Zatrzymanie z opróżnianiem (drain).** Procesor oparty na strumieniu aplikacja opisuje jako źródło i przepływ (`Pipe`). Uruchamia go Coilix, który sam wstawia sygnał zatrzymania. Zatrzymanie sharda przebiega tak:
  1. Coilix zatrzymuje tylko źródło. Źródło Kafki przestaje pobierać rekordy (`stopConsuming`), inne źródło kończy się przez `interruptWhen` wstawione zaraz za nim, a nie na końcu strumienia.
  2. Reszta przepływu dostaje zwykły koniec strumienia: `parEvalMap` czeka na rozpoczęte zadania, `groupWithin` oddaje niepełną paczkę, a zatwierdzanie paczkami zatwierdza ostatnie offsety. Coilix czeka, aż strumień skończy się sam.
  3. Dopiero po `drain-timeout` Coilix anuluje fiber sharda i zapisuje w logu, że opróżnianie nie zdążyło. Niezatwierdzone offsety Kafka dostarczy ponownie po restarcie, zgodnie z gwarancją „co najmniej raz”.
  Aplikacja nie dostaje sygnału zatrzymania do ręki, więc nie może go wstawić w złym miejscu. Odpytywanie `Future` z bulldozera nie jest potrzebne, bo zakończenie strumienia to zakończenie fibera.
- **HTTP na http4s.** Moduł `21_web` powstaje na http4s z serwerem Ember. WebSocket i subskrypcje GraphQL to strumienie fs2 zamiast aktorów.
- **Kafka na fs2-kafka.** Klient Kafki to moduł funkcji. Źródło Kafki dla procesorów (przydział partycji do shardów, zatwierdzanie paczkami, opróżnianie) to moduł integracyjny procesorów i Kafki, zgodnie z zasadą, że moduły funkcji nie zależą od siebie.
- **Konektory bez gotowej biblioteki fs2** (SFTP) owija się w `Resource` i `Stream` nad biblioteką Javy, z wywołaniami blokującymi na `BlockingExecutor`. Strumieniowe parsowanie JSON korzysta z biblioteki zgodnej z fs2 (np. fs2-data-json), wybranej w tasku konektora.

## Konsekwencje

- Zastąpione: ADR 0036 w całości. Z ADR 0042 punkty „Komponenty aplikacji mogą być w tagless final” i „Klasy typów jako beany” oraz otwarte pytanie o strumienie (źródła procesorów to `fs2.Stream[IO, _]`). Z ADR 0019 wraca zasada, że niezwiązane `F[_]` w zależności to błąd. Task COILIX-00029 (Pekko Streams) zostaje zamknięty jako zastąpiony przez COILIX-00032.
- Aplikacja ma na classpath cats-effect i fs2-core.
- Bulldozer przenosi flowy na procesory Coilix na fs2. Jego `ShardDefinition` i `ShardControl` zastępuje cykl życia sharda Coilix z opróżnianiem, a serwisy z `using OptimusRuntime` dostają zależności przez konstruktor.
- Komponenty aplikacji w tagless final (`LabourRates`, `JobEstimates` w demo) przechodzą na `IO`, a sekcja przewodnika o nich znika.
- Kolejność prac jest w COILIX-00032: usunięcie wiązania `F[_]`, usunięcie `25_pekko` z przejściem demo i przykładów na fs2, `processing` na `IO` z `fs2.Stream` (krok 3 COILIX-00031) z opróżnianiem, potem Kafka i `web` na http4s.
