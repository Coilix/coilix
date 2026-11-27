# 0059: Moduł Kafki z nazwanymi klientami

- Status: proponowana
- Data: 2026-11-28
- Task: COILIX-00032 (krok 5, część 1)
- Rozwija: 0043 (Kafka na fs2-kafka)

## Kontekst

ADR 0043 wybrał fs2-kafka jako klienta Kafki i rozdzielił klienta (moduł funkcji) od źródła dla procesorów (moduł integracyjny). Aplikacje opiekuna, które przejdą z bulldozera, pracują z kilkoma klastrami Kafki naraz, więc jeden bean klienta nie wystarcza. Bulldozer ma do tego `KafkaClients` (lista klientów po `clientId`), `KafkaTopic.owner` (nazwa klienta tematu), `KafkaAdminClient` z operacjami na offsetach i grupach, które wykorzystuje `KafkaRecoveryService`, oraz ustawienia SASL/SSL w `KafkaAuthConfig`.

## Decyzja

- **Moduł `29_kafka` (`coilix-kafka`, grupa wydań `kafka`)** zależy od `context` i fs2-kafka. Podlega wartowi `ThreadPrimitives` jak moduły runtime'u.
- **Klient na klaster.** `coilix.kafka.clients.<nazwa>` opisuje jeden klaster: `bootstrap-servers`, `client-id`, `auth` (protokół jako enum, hasła i JAAS jako `Secret`), `consumer`, `producer`, `commit` i `admin`, każdy z typowanymi ustawieniami najczęstszych kluczy i mapą `properties` na resztę. Bean `kafkaClients` (`KafkaClients`) otwiera klienta dla każdego wpisu w `Resource` i znajduje go po nazwie (`client`, `find`) albo po temacie (`forTopic`). Aplikacja, która woli wstrzykiwać klienta, deklaruje bean o nazwie klienta i używa `@Qualifier`. Bez `coilix.kafka` aplikacja nie ma klientów, a moduł niczego nie otwiera.
- **`KafkaClient`** ma jednego producenta bajtowego na klaster (publikacje typują kodeki tematu), otwiera konsumenta dla każdego strumienia (`subscribe` z grupą, `assign` z wybranymi partycjami, surowy `consumer`) i daje `commits`, czyli zatwierdzanie paczkami z `commit.max-batch` i `commit.max-interval`. Strumień niczego nie zatwierdza sam, więc przetwarzanie jest „co najmniej raz”. Typy fs2-kafka (`CommittableConsumerRecord`, `CommittableOffset`, `KafkaConsumer`) są częścią API: moduł nie opakowuje biblioteki, którą ADR 0043 wybrał świadomie.
- **`KafkaTopic[K, V]`** to wartość z nazwą, kodekami kluczy i wartości (`TopicCodec`), nazwą klienta (domyślnie `default`) i `TopicSettings` do tworzenia tematu. Kodeki liczb mają format serializerów klienta Kafki, więc inne aplikacje je czytają. Rekord, którego nie da się zdekodować, kończy strumień błędem `RecordDecodingException`. Dead letter queue z bulldozera przyjdzie z konektorami COILIX-00026.
- **`KafkaAdmin`** na kliencie administracyjnym Kafki: tematy, partycje, offsety najstarsze, najnowsze i zatwierdzone, przesunięcie offsetów grupy i usuwanie grup, z limitem czasu na operację i ostrzeżeniem o wolnej operacji. Brakujący temat daje pusty zbiór partycji, a brakująca grupa brak offsetów, bo tego potrzebuje odzyskiwanie offsetów (krok 5, część 3).
- **Testy na brokerze w JVM** (embedded-kafka w wersji Kafki z fs2-kafka), bez Dockera.

| Bulldozer | Coilix |
|---|---|
| `KafkaClientsConfig`, `KafkaClientConfig` | `KafkaSettings`, `KafkaClientSettings` pod `coilix.kafka.clients` |
| `KafkaClients.getClient(clientId)` | `KafkaClients.client(name)`, `find`, `forTopic` |
| `KafkaTopic.owner` | `KafkaTopic.client` |
| `KafkaTopic.Json`, `KafkaTopic.Proto` | `TopicCodec` (JSON z modułem serializacji COILIX-00024) |
| `KafkaAuthConfig` | `KafkaAuthSettings` z `Secret` |
| `ConsumerParams`, `ProducerParams`, `CommitterParams` | `KafkaConsumerSettings`, `KafkaProducerSettings`, `KafkaCommitSettings` |
| `allPartitionsCommittable` | `KafkaClient.subscribe` |
| `selectedPartitionsCommittable` | `KafkaClient.assign` (przydział partycji do shardów w module integracyjnym) |
| `producerFlow`, `sendSingle` | `publishing`, `publish`, `publishAll` |
| `committerFlow` | `commits` |
| `KafkaAdminClient` | `KafkaAdmin` |

## Konsekwencje

- Aplikacja z kilkoma klastrami konfiguruje je obok siebie, a kod publikujący do tematów różnych klastrów nie potrzebuje nazw klientów.
- Strumień, który ma się skończyć, kończy się za `commits`, bo zakończenie czytania zamyka konsumenta z offsetami czekającymi na paczkę. Moduł integracyjny procesorów (krok 5, część 2) zatrzymuje czytanie przez `stopConsuming`, żeby ostatnie offsety zostały zatwierdzone.
- Pola konfiguracji typu `Option` nie mogą mieć typu prostego (`Option[Int]`), bo Scala zapisuje je w sygnaturze jako `Option<Object>` i binder nie zna typu elementu. Takie ustawienia mają domyślne wartości Kafki.
- Partycjoner `EntityIdPartitioner` i metryki Kamona z bulldozera nie są przeniesione; metryki przyjdą z diagnostyką (COILIX-00030).
