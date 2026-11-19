# COILIX-00016: Zdarzenia aplikacji (`events`)

> Wymagania opiekuna z 2026-11-19:
>
> - rozwiązanie ma być dobre i docelowe, a nie tylko lokalna szyna,
> - domyślna implementacja trzyma zdarzenia w pamięci, ale da się wpiąć trwałą: tabela w bazie albo topic Kafki,
> - aplikacja nie zamyka się, dopóki pamięciowa implementacja ma w kolejce zdarzenia do dostarczenia.

## Cel

Beany publikują zdarzenia domenowe, a inne beany je odbierają, bez zależności od siebie nawzajem:

```scala
final case class OrderPlaced(orderId: OrderId, customer: CustomerId) derives EventCodec

@Service
final class Checkout(events: EventPublisher) {
  def place(order: Order): Unit = {
    orders.save(order)
    events.publish(OrderPlaced(order.id, order.customer), key = order.customer.value)
  }
}

@Component
final class ConfirmationMailer(mail: MailSender) {
  @EventListener
  def onOrderPlaced(event: OrderPlaced): Unit = mail.sendConfirmation(event.customer, event.orderId)
}
```

- Publikujący nie czeka na odbiorców. Dostarczanie jest asynchroniczne, a każdy odbiorca ma własną kolejkę, więc wolny albo awaryjny odbiorca nie blokuje pozostałych.
- To, gdzie zdarzenia czekają na dostarczenie, jest wymienne (SPI `EventTransport`). Domyślnie pamięć. Moduły dodają bazę danych (wzorzec outbox) i Kafkę. Kod publikujący i odbiorcy się nie zmieniają.
- Zamknięcie aplikacji najpierw zatrzymuje źródła nowych zdarzeń, potem czeka, aż transport pamięciowy dostarczy wszystko z kolejek (z limitem czasu), a dopiero potem niszczy beany.
- Framework publikuje własne zdarzenia cyklu życia: `ContextReady`, `ApplicationStarted`, `ContextClosing`, `ApplicationFailed`.

## Stan obecny

- Brak mechanizmu zdarzeń. Jedyne punkty startu to `ApplicationRunner` i `ContextLifecycleListener` (COILIX-00005), a przy zamykaniu `@PreDestroy`.
- Są gotowe klocki:
  - `TaskExecutor` na wątkach wirtualnych i `TaskScheduler` (`06_concurrency`, ADR 0020),
  - `ContextLifecycleListener` z `onContextStarted` i `onContextClosing` przed `@PreDestroy`,
  - `GenericType` i `TypeInfo` do dopasowania po pełnym typie (ADR 0019),
  - `DefinitionProcessor` i `ComponentCollector` do zbierania metod i klas z adnotacjami,
  - `ReplicaIdentity` (`07_cluster`) do rozróżnienia replik.
- Szkic poprzedniej wersji tego taska zakładał tylko synchroniczną publikację w wątku publikującego. To zostaje jako tryb dla zdarzeń frameworka, a nie jako główny model.

## Model

### Zdarzenie i koperta

- Zdarzenie to dowolna wartość aplikacji (zwykle case class z sealed traitu). Nie musi rozszerzać typu frameworka.
- Transport przenosi `EventEnvelope`: `id` (UUID, unikalny w całej aplikacji), `eventType` (`GenericType`), `key` (opcjonalny klucz kolejności), `publishedAt`, `publisher` (`Member` z `ReplicaIdentity`, gdy jest moduł klastra), `headers` (np. identyfikator korelacji z MDC) i `payload`.
- Transport pamięciowy trzyma obiekt zdarzenia. Transport trwały zapisuje bajty z `EventCodec[E]` (typeclass z `derives`, uzgodnione z serializacją z COILIX-00024) i nazwę typu z `@EventName("order-placed")`, a domyślnie pełną nazwę klasy. Nazwa jest częścią kontraktu z zapisanymi zdarzeniami, więc zmiana nazwy klasy nie gubi starych wpisów, gdy ma `@EventName`.

### Publikacja

- `EventPublisher` jest beanem wbudowanym: `publish[E: TypeInfo](event: E)`, `publish(event, key)` i `publishAll(events)`. Obok jest wersja zwięzła z `using EventPublisher` (`Events.publish(event)`), zgodnie z zasadą API z CLAUDE.md.
- `publish` wraca, gdy transport przyjął zdarzenie: pamięć po dodaniu do kolejek, baza po zapisie wiersza, Kafka po potwierdzeniu brokera. Wyjątek z transportu (np. pełna kolejka po limicie czasu, niedostępny broker) trafia do publikującego, więc zdarzenie nigdy nie znika po cichu przy publikacji.
- Zdarzenie bez żadnego odbiorcy jest przyjmowane i logowane na `DEBUG`. Przy transporcie trwałym jest zapisywane, bo odbiorca może dojść w nowszej wersji aplikacji.

### Odbiorcy

- Metoda z `@EventListener`, która przyjmuje jeden parametr typu zdarzenia, albo bean implementujący `EventListener[E]` z `onEvent(event: E)`. Metoda może przyjąć też `EventEnvelope[E]`, gdy potrzebuje metadanych.
- Dopasowanie po pełnym typie z dziedziczeniem: odbiorca `OrderEvent` dostaje każde zdarzenie z sealed traitu, a `Repo[IO]` jest innym typem niż `Repo[Option]`.
- Każdy odbiorca to osobna **subskrypcja** z nazwą `<bean>.<metoda>` albo `@EventListener(name = "...")`. Nazwa jest stała między wydaniami, bo transport trwały trzyma pod nią pozycję odczytu. Dwie subskrypcje o tej samej nazwie to błąd startu.
- Kolejność:
  - zdarzenia z tym samym kluczem trafiają do danej subskrypcji w kolejności publikacji,
  - zdarzenia bez klucza albo z różnymi kluczami mogą być przetwarzane równolegle, do `concurrency` wątków wirtualnych na subskrypcję (domyślnie 1, czyli pełna kolejność),
  - kolejność między różnymi subskrypcjami nie jest określona, bo każda ma własną kolejkę.
- `@EventListener(synchronous = true)` uruchamia odbiorcę w wątku publikującego, przed powrotem z `publish`. Służy do zdarzeń frameworka i rzadkich przypadków, gdy publikujący musi wiedzieć, że odbiorca skończył. Wyjątek takiego odbiorcy trafia do publikującego. Odbiorca synchroniczny nie jest trwały i nie przechodzi przez transport.

### Gwarancje dostarczenia

| Transport | Przy działającej aplikacji | Po awarii procesu | Między replikami |
|---|---|---|---|
| pamięć | co najmniej raz, z ponowieniami | zdarzenia w kolejce przepadają | nie, każda replika ma własną szynę |
| baza (outbox) | co najmniej raz | zostają w tabeli i są dostarczone po starcie | tak |
| Kafka | co najmniej raz | zostają w topicu | tak |

- Odbiorca musi być idempotentny, bo przy każdym transporcie zdarzenie może przyjść ponownie (ponowienie po błędzie, restart po awarii). `EventEnvelope.id` służy do deduplikacji, a moduł `data` może dać gotowy `ProcessedEvents` w tabeli.
- „Dokładnie raz” nie jest obiecywane.

### Zasięg subskrypcji w klastrze

- `@EventListener(scope = Scope.Application)` (domyślnie): każde zdarzenie obsługuje jedna replika. Przy Kafce to grupa konsumentów na subskrypcję. Przy bazie replika blokuje paczkę wierszy (`FOR UPDATE SKIP LOCKED`) albo partycję klucza.
- `scope = Scope.Replica`: każde zdarzenie dostaje każda replika, np. do unieważnienia lokalnego cache. Przy Kafce grupa ma w nazwie `ReplicaIdentity.self.id`. Przy bazie każda replika ma własną pozycję.
- Transport pamięciowy obsługuje oba zasięgi tylko w obrębie jednej repliki. Zdarzenia między replikami wymagają transportu trwałego. Start z subskrypcją `Scope.Application`, transportem pamięciowym i `replicaCount > 1` loguje ostrzeżenie, bo każda replika obsłuży tylko swoje zdarzenia.

### Błędy odbiorców

- Wyjątek odbiorcy uruchamia `RetryPolicy` subskrypcji: liczba prób, opóźnienie wykładnicze z rozrzutem, limit opóźnienia. Domyślnie 5 prób od 1 s do 1 min. Ustawia się to w `coilix.events.subscriptions.<nazwa>.retry` albo w `@EventListener`.
- Po wyczerpaniu prób zdarzenie trafia do `DeadLetterStore` z koperta, nazwą subskrypcji, liczbą prób i ostatnim błędem. Subskrypcja idzie dalej, więc jedno trujące zdarzenie nie blokuje kolejki. Zdarzenia z tym samym kluczem czekają, aż poprzednie zostanie dostarczone albo trafi do dead letter.
- `onFailure = Pause` zamiast dead letter wstrzymuje subskrypcję do ręcznego wznowienia, gdy kolejność jest ważniejsza niż ciągłość, np. przy projekcjach.
- Domyślny `DeadLetterStore` jest w pamięci i logowany na `ERROR`. Moduły trwałe dają własny: tabelę albo topic `.dlq`.
- `EventBus` pozwala ponowić zdarzenia z dead letter dla subskrypcji (`redeliver`), wstrzymać ją i wznowić.
- `RetryPolicy` jest wspólnym, czystym modelem w `06_concurrency`, z którego korzysta też obsługa błędów procesorów (COILIX-00026), żeby nie było dwóch różnych polityk ponowień.

### Przeciążenie

- Każda subskrypcja transportu pamięciowego ma ograniczoną kolejkę (`capacity`, domyślnie 10 000).
- Gdy któraś kolejka jest pełna, `publish` czeka do `publish-timeout` (domyślnie 5 s, na wątku wirtualnym to tanie), a potem rzuca `EventQueueFullException`. Ciche gubienie zdarzeń jest wykluczone.
- Rozmiar kolejek i opóźnienie najstarszego zdarzenia są w statystykach, więc przeciążenie widać, zanim zacznie blokować.

## Zamykanie aplikacji

Wymaganie: aplikacja nie kończy się, dopóki transport pamięciowy ma zdarzenia do dostarczenia.

1. Zamknięcie zaczyna się od shutdown hooka (SIGTERM, Ctrl+C, koniec `main`) albo od `close()` kontekstu. Wątki dostarczające nie są wątkami demonami, ale zamknięcie zawsze idzie przez kontekst, więc JVM czeka na shutdown hook, który czeka na opróżnienie.
2. `ContextLifecycleListener` dostaje fazy zamykania (zmiana w rdzeniu, krok 1), żeby kolejność nie zależała od kolejności tworzenia beanów:
   - **producenci**: zatrzymanie `@Scheduled`, procesorów (COILIX-00026), serwera HTTP (COILIX-00023), czyli źródeł nowych zdarzeń z zewnątrz,
   - **zdarzenia**: `EventBus` przestaje przyjmować zdarzenia spoza odbiorców i czeka, aż kolejki będą puste i nic nie jest w trakcie przetwarzania. Odbiorca może dalej publikować w trakcie opróżniania, bo łańcuchy zdarzeń muszą się domknąć. Czekanie kończy się po `coilix.events.shutdown-timeout` (domyślnie 30 s),
   - **zasoby**: wykonawcy, harmonogram, połączenia, a potem `@PreDestroy` wszystkich beanów.
3. Po limicie czasu `EventBus` przerywa odbiorców i loguje na `ERROR` liczbę niedostarczonych zdarzeń na subskrypcję. Przy transporcie pamięciowym te zdarzenia przepadają, a przy trwałym zostają w bazie albo w topicu i będą dostarczone po starcie.
4. Publikacja spoza odbiorcy po rozpoczęciu zamykania rzuca `EventBusClosedException`, żeby nikt nie zgubił zdarzenia, licząc, że zostanie dostarczone.
5. Przewodnik opisuje zgranie z Kubernetesem: `terminationGracePeriodSeconds` musi być większe od sumy limitów zamykania (`events.shutdown-timeout` i limity wykonawców).

## SPI

```scala
trait EventTransport {
  def name: String

  def append(envelopes: List[EventEnvelope[?]]): Unit

  def subscribe(subscription: SubscriptionSpec, handler: DeliveryHandler): SubscriptionHandle

  def pending(subscription: String): Long

  def drain(timeout: FiniteDuration): DrainResult

  def close(): Unit
}
```

- `DeliveryHandler` dostaje paczkę kopert i zwraca wynik per koperta: dostarczona, do ponowienia albo do dead letter. Transport sam potwierdza pozycję (offset w Kafce, pozycję subskrypcji w tabeli) dopiero po wyniku, więc „co najmniej raz” wynika z kontraktu, a nie z każdej implementacji z osobna.
- `EventBus` (rdzeń modułu) robi resztę wspólnie dla wszystkich transportów: dopasowanie typów, kolejność per klucz, ponowienia, dead letter, statystyki, zamykanie. Implementacja transportu robi tylko trwałość, podział i pozycje.
- **Wybór transportu.** Transport jest beanem. Pamięciowy ma `@ConditionalOnMissingBean`, więc dodanie modułu `events-kafka` albo `events-jdbc` go zastępuje. Kilka transportów naraz wybiera routing w configu:

  ```yaml
  coilix.events:
    default-transport: memory
    routes:
      - types: [com.shop.orders.OrderEvent]
        transport: kafka
  ```

  Typ trafia do pierwszej pasującej trasy (z dziedziczeniem), a reszta do `default-transport`. Dzięki temu zdarzenia domenowe mogą iść przez Kafkę, a lokalne powiadomienia zostać w pamięci.
- **Transport bazodanowy (outbox).** Publikacja w transakcji zapisuje wiersz w tej samej transakcji co zmianę stanu. Zdarzenie powstaje wtedy i tylko wtedy, gdy zmiana została zatwierdzona. Wymaga modułu `data` (COILIX-00007 i dalsze).
- **Transport Kafki.** Jeden topic na typ albo na trasę (`coilix.events.kafka.topic`), klucz zdarzenia jako klucz rekordu (kolejność per partycja), nagłówki z koperty, grupa konsumentów na subskrypcję. Konektor dzieli klienta Kafki z konektorem procesorów (COILIX-00026), żeby nie było dwóch konfiguracji klienta.

## Zdarzenia frameworka

- `ContextReady` (po `onContextStarted`, przed pierwszym `ApplicationRunner`), `ApplicationStarted` (po runnerach), `ContextClosing` (na początku zamykania) i `ApplicationFailed` (błąd startu, z wyjątkiem).
- Są dostarczane synchronicznie i tylko lokalnie. Nigdy nie idą przez transport trwały.
- Wyjątek odbiorcy przy `ContextClosing` jest logowany i nie przerywa zamykania.

## Testy

- Testkit: `CoilixTestSetup.withSynchronousEvents` uruchamia odbiorców w wątku publikującego, więc test sprawdza skutki bez czekania.
- `events.awaitIdle(timeout)` czeka, aż wszystkie kolejki będą puste, dla testów z prawdziwym dostarczaniem asynchronicznym.
- `recordedEvents[E]` zwraca zdarzenia opublikowane w teście. Testkit ma rejestrator jako transport-podsłuch, który działa przy każdym transporcie.
- Moduły trwałe mają testy kontraktowe z jednego zestawu (`EventTransportContract`), uruchamiane na pamięci, na bazie z Testcontainers i na Kafce z Testcontainers. Gwarancje są wtedy sprawdzone tak samo dla każdej implementacji.

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `concurrency` | rdzeń (zmiana) | — | `RetryPolicy` jako wspólny model ponowień |
| `context` | rdzeń (zmiana) | — | fazy zamykania w `ContextLifecycleListener` |
| `events` (`08_events`) | rdzeń | `concurrency`, `cluster` (opcjonalnie przez `Option[ReplicaIdentity]`) | `EventPublisher`, `@EventListener`, `EventListener[E]`, `EventEnvelope`, `EventBus`, `EventTransport`, `InMemoryEventTransport`, `DeadLetterStore`, routing, statystyki, zdarzenia frameworka, zamykanie z opróżnianiem |
| `boot` | rdzeń (zmiana) | `events` | publikacja `ApplicationStarted` i `ApplicationFailed` |
| `testkit` | rdzeń (zmiana) | `events` | zdarzenia synchroniczne, `awaitIdle`, `recordedEvents` |
| `events-jdbc` | funkcja | `events`, `data` | outbox w tabeli, pozycje subskrypcji, dead letter w tabeli, migracja schematu |
| `events-kafka` | funkcja | `events`, klient Kafki | topic na trasę, grupa na subskrypcję, dead letter na topicu `.dlq` |

`events` jest w rdzeniu, bo `boot` publikuje zdarzenia frameworka, a moduły funkcji (procesory, HTTP) publikują i odbierają zdarzenia, choć nie mogą zależeć od siebie nawzajem. Pamięciowy transport nie ma zależności zewnętrznych.

## Kroki (osobne PR-y)

1. **Rdzeń.** Fazy zamykania w `ContextLifecycleListener` i `RetryPolicy` w `concurrency`, z migracją `ScheduledMethodRegistrar` do fazy producentów. ADR „Model zdarzeń i fazy zamykania”.
2. **Moduł `08_events` z transportem pamięciowym.** `EventPublisher`, `@EventListener` i `EventListener[E]`, dopasowanie po `GenericType`, subskrypcje z kolejnością per klucz, ponowienia, dead letter w pamięci, ograniczone kolejki, zamykanie z opróżnianiem, statystyki. Przewodnik w obu językach i użycie w demo.
3. **Zdarzenia frameworka i testkit.** `ContextReady`, `ApplicationStarted`, `ContextClosing`, `ApplicationFailed`, zdarzenia synchroniczne, `awaitIdle`, `recordedEvents`.
4. **SPI i kontrakt.** Routing transportów, `EventCodec` i `@EventName`, zestaw testów `EventTransportContract`.
5. **`events-jdbc`.** Po pierwszej wersji modułu `data`.
6. **`events-kafka`.** Razem z konektorem Kafki z COILIX-00026.

## Kryteria akceptacji

1. Zdarzenie opublikowane przez bean trafia do dwóch odbiorców (`@EventListener` i `EventListener[E]`), a odbiorca typu nadrzędnego z sealed traitu też je dostaje.
2. `publish` wraca, zanim odbiorca skończy (test z odbiorcą czekającym na zatrzask). Wyjątek odbiorcy nie dociera do publikującego.
3. Zdarzenia z tym samym kluczem przychodzą do subskrypcji w kolejności publikacji także przy `concurrency = 4`, a zdarzenia z różnymi kluczami są przetwarzane równolegle.
4. Odbiorca, który dwa razy rzuca wyjątek, dostaje zdarzenie za trzecim razem. Odbiorca, który rzuca zawsze, po wyczerpaniu prób zostawia zdarzenie w dead letter, a następne zdarzenia tej subskrypcji są dostarczane. Ponowienie z dead letter działa.
5. Wolny odbiorca nie opóźnia innej subskrypcji tego samego zdarzenia.
6. Pełna kolejka sprawia, że `publish` czeka, a po `publish-timeout` rzuca `EventQueueFullException`.
7. **Zamykanie.** `close()` z 1000 zdarzeniami w kolejce wolnego odbiorcy kończy się po dostarczeniu wszystkich, przed `@PreDestroy` beanów, z których korzysta odbiorca. Odbiorca publikujący kolejne zdarzenie w trakcie opróżniania ma je dostarczone. Po `shutdown-timeout` zamykanie loguje liczbę niedostarczonych zdarzeń i się kończy.
8. Publikacja spoza odbiorcy po rozpoczęciu zamykania rzuca `EventBusClosedException`.
9. Zadania `@Scheduled` są zatrzymane, zanim zacznie się opróżnianie zdarzeń, więc nie dokładają nowych.
10. `ContextReady` przychodzi przed pierwszym `ApplicationRunner`, `ContextClosing` przed `@PreDestroy`, a `ApplicationFailed` przy błędzie startu.
11. Bean z `@ConditionalOnMissingBean` w `InMemoryEventTransport` ustępuje transportowi z modułu, a routing wysyła wskazany typ do drugiego transportu (test z transportem testowym).
12. Zestaw `EventTransportContract` przechodzi dla transportu pamięciowego.
13. `events` nie ma zależności zewnętrznych, co sprawdza `checkModuleLayers`.
14. Demo publikuje zdarzenie po zakończonym zleceniu, ma dwóch odbiorców (powiadomienie i statystyki) i loguje opróżnianie kolejki przy zamykaniu.

## Poza zakresem

- Event sourcing (odtwarzanie stanu z historii zdarzeń) i projekcje z rebuildem. Transport trwały to umożliwi, ale API do tego to osobny task.
- „Dokładnie raz” i transakcje rozproszone.
- Zdarzenia między różnymi aplikacjami jako publiczny kontrakt (schematy, wersjonowanie, rejestr schematów). Transport Kafki działa w obrębie jednej aplikacji. Integracja z innymi systemami idzie przez procesory (COILIX-00026).
- Odbiorcy w `IO`. Przyjdą z modułem `cats-effect` (COILIX-00020) jako odbiorca zwracający `IO[Unit]`.

## Ryzyka i otwarte pytania

1. Fazy zamykania zmieniają `ContextLifecycleListener` z COILIX-00005. Najlepiej zrobić to przed `core-v0.1.0`, póki API rdzenia nie jest wydane.
2. Nazwy subskrypcji muszą być stabilne między wydaniami, bo transport trwały trzyma pod nimi pozycje. Zmiana nazwy metody bez `name` w adnotacji zaczęłaby subskrypcję od nowa. Przewodnik musi to mocno podkreślić, a start może ostrzegać o subskrypcjach w bazie, których nie ma już w kodzie.
3. Transport pamięciowy przy awarii procesu gubi zdarzenia. To świadomy kompromis domyślnej konfiguracji. Przewodnik ma wprost mówić, kiedy wybrać outbox.
4. `EventCodec` dubluje część COILIX-00024 i `ClusterCodec` z COILIX-00027. Trzeba uzgodnić jeden mechanizm `derives` dla trzech zastosowań.
5. Długi limit opróżniania przy zamykaniu wydłuża wdrożenia w Kubernetesie. Domyślne 30 s mieści się w domyślnym `terminationGracePeriodSeconds` tylko razem z krótkimi limitami wykonawców, więc przewodnik podaje gotowe wartości.
