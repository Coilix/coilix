# COILIX-00016: Zdarzenia aplikacji (`events`)

> Decyzje opiekuna z 2026-11-20 (zastępują wymagania z 2026-11-19):
>
> - moduł zdarzeń jest lekki i mało zobowiązujący: zdarzenia żyją tylko w pamięci jednego procesu,
> - bez transakcji, outboxa, Kafki i dostarczania między replikami; trwałe przetwarzanie to przetwarzanie w tle (COILIX-00026) i jego konektory,
> - odbiorca jest domyślnie synchroniczny, a asynchroniczny na życzenie (`async = true`),
> - zdarzenia z poprzedniego uruchomienia nigdy nie są dostarczane, bo nie ma ich gdzie przechować,
> - przy zamykaniu aplikacja krótko czeka na trwających odbiorców asynchronicznych, ale nie obiecuje dostarczenia wszystkiego.

## Cel

Beany publikują zdarzenia, a inne beany na nie reagują, bez zależności od siebie nawzajem:

```scala
final case class OrderPlaced(orderId: OrderId, customer: CustomerId)

@Service
final class Checkout(events: EventPublisher) {
  def place(order: Order): Unit = {
    orders.save(order)
    events.publish(OrderPlaced(order.id, order.customer))
  }
}

@Component
final class OrderStatistics {
  @EventListener
  def onOrderPlaced(event: OrderPlaced): Unit = placed.incrementAndGet()
}

@Component
final class ConfirmationMailer(mail: MailSender) {
  @EventListener(async = true)
  def onOrderPlaced(event: OrderPlaced): Unit = mail.sendConfirmation(event.customer, event.orderId)
}
```

- Zdarzenie to sygnał wewnątrz procesu, a nie zapis. Gdy coś musi się wydarzyć na pewno (także po awarii procesu albo na innej replice), właściwym narzędziem są procesory z COILIX-00026.
- Framework publikuje własne zdarzenia cyklu życia: `ContextReady`, `ApplicationStarted`, `ContextClosing`, `ApplicationFailed`.

## Stan obecny

- Brak mechanizmu zdarzeń. Punkty startu to `ApplicationRunner` i `ContextLifecycleListener` (COILIX-00005), a przy zamykaniu `@PreDestroy`.
- Gotowe klocki:
  - `TaskExecutor` na wątkach wirtualnych (`06_concurrency`, ADR 0020),
  - fazy zamykania `Producers`, `Events`, `Resources` w `ContextLifecycleListener` i `RetryPolicy` w `concurrency` (krok 1, ADR 0024),
  - `GenericType` i `TypeInfo` do dopasowania po pełnym typie (ADR 0019),
  - `DefinitionProcessor` do zbierania metod z adnotacjami.

## Model

### Publikacja

- `EventPublisher` jest beanem wbudowanym: `publish[E: TypeInfo](event: E): Unit`. Obok jest wersja zwięzła z `using EventPublisher` (`Events.publish(event)`), zgodnie z zasadą API z CLAUDE.md.
- Zdarzenie to dowolna wartość aplikacji (zwykle case class z sealed traitu). Nie rozszerza typu frameworka i nie potrzebuje serializacji.
- Zdarzenie bez odbiorcy jest logowane na `DEBUG` i nic więcej się nie dzieje.
- Zdarzenie opublikowane, zanim kontekst skończy start (np. w konstruktorze beana), czeka w buforze i jest dostarczane zaraz po znalezieniu odbiorców. Wyjątek odbiorcy synchronicznego jest wtedy logowany, bo publikujący już wrócił.

### Odbiorcy

- Metoda z `@EventListener`, która przyjmuje jeden parametr typu zdarzenia, albo bean implementujący `ApplicationEventListener[E]` z `onEvent(event: E)`.
- Trait nazywa się `ApplicationEventListener`, a nie `EventListener`, żeby nie zderzał się z adnotacją przy imporcie. Jego argument typu musi być zapisany w klasie (klasa albo klasa anonimowa, nie lambda).
- Dopasowanie po pełnym typie z dziedziczeniem: odbiorca `OrderEvent` dostaje każde zdarzenie z sealed traitu, a `Changed[Customer]` jest innym typem niż `Changed[Order]`.
- **Synchroniczny** (domyślnie): działa w wątku publikującego, przed powrotem z `publish`. Odbiorcy synchroniczni są wywoływani po kolei. Kolejność między nimi nie jest częścią kontraktu.
- **Asynchroniczny** (`@EventListener(async = true)`): każde dostarczenie to osobne zadanie na `TaskExecutor`. `publish` nie czeka na jego koniec. Kolejność dostarczeń nie jest gwarantowana, także dla jednego odbiorcy. Kto potrzebuje kolejności, używa odbiorcy synchronicznego albo procesora.
- Odbiorca może publikować kolejne zdarzenia.

### Błędy odbiorców

- Wyjątek odbiorcy synchronicznego trafia do publikującego. Pozostali odbiorcy synchroniczni i tak są wywoływani, a publikujący dostaje pierwszy wyjątek z resztą jako `suppressed`. Odbiorcy asynchroniczni nie są wtedy uruchamiani, bo publikacja się nie udała.
- Wyjątek odbiorcy asynchronicznego jest logowany na `ERROR` z nazwą odbiorcy (`<bean>.<metoda>`) i nie trafia do publikującego.
- Odbiorca asynchroniczny może mieć ponowienia z `RetryPolicy` (ADR 0024) w `coilix.events.listeners.<bean>.<metoda>.retry`. Domyślnie jedna próba. Ponowienie planuje `TaskScheduler`, a po wyczerpaniu prób zdarzenie jest logowane i porzucane. Nie ma dead letter.

### Czego moduł nie robi

- Nie przechowuje zdarzeń: po awarii procesu zdarzenia czekające na odbiorców asynchronicznych przepadają, a po restarcie nic nie jest dostarczane ponownie.
- Nie przenosi zdarzeń między replikami. Każda replika ma własną szynę.
- Nie wiąże się z transakcjami (nie ma odbiorcy „po commicie”). Jeśli taka potrzeba wróci, doda ją moduł `data` jako własny hak.
- Nie ma ograniczonych kolejek ani przeciwciśnienia. Zadania asynchroniczne idą na `TaskExecutor`, który ma własne limity.

## Zamykanie aplikacji

1. Faza `Producers` zatrzymuje źródła nowej pracy (`@Scheduled`, później procesory i serwer HTTP).
2. Faza `Events`: `EventBus` przestaje przyjmować publikacje spoza odbiorców (`EventBusClosedException`) i czeka na trwających odbiorców asynchronicznych, łącznie z tymi, których zdarzenia opublikowali inni odbiorcy w trakcie czekania. Zaplanowane ponowienia są anulowane.
3. Czekanie kończy się po `coilix.events.shutdown-timeout` (domyślnie 10 s). Wtedy `EventBus` przerywa odbiorców i loguje na `ERROR` liczbę porzuconych dostarczeń na odbiorcę.
4. Faza `Resources` i `@PreDestroy` działają dopiero potem, więc odbiorcy nie korzystają z zamkniętych beanów.

## Zdarzenia frameworka

- `ContextReady` (po `onContextStarted`, przed pierwszym `ApplicationRunner`), `ApplicationStarted` (po runnerach), `ContextClosing` (na początku zamykania, przed fazą `Producers`) i `ApplicationFailed` (błąd startu, z wyjątkiem).
- Dostaje je każdy pasujący odbiorca, a odbiorca asynchroniczny `ContextClosing` jest objęty czekaniem z fazy `Events`.
- Wyjątek odbiorcy przy `ContextClosing` i `ApplicationFailed` jest logowany i nie przerywa zamykania.

## Testy

- Testkit: `CoilixTestSetup.withSynchronousEvents` uruchamia także odbiorców asynchronicznych w wątku publikującego, więc test sprawdza skutki bez czekania.
- `events.awaitIdle(timeout)` czeka, aż nie będzie trwających odbiorców asynchronicznych.
- `recordedEvents[E]` zwraca zdarzenia opublikowane w teście.

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `concurrency`, `context` | rdzeń (zmiana) | — | `RetryPolicy` i fazy zamykania (krok 1, zrobione) |
| `events` (`08_events`) | rdzeń | `concurrency` | `EventPublisher`, `Events`, `@EventListener`, `ApplicationEventListener[E]`, `EventBus`, zdarzenia frameworka, zamykanie |
| `boot` | rdzeń (zmiana) | `events` | publikacja `ApplicationStarted` i `ApplicationFailed` |
| `testkit` | rdzeń (zmiana) | `events` | `withSynchronousEvents`, `awaitIdle`, `recordedEvents` |

`events` jest w rdzeniu, bo `boot` publikuje zdarzenia frameworka, a moduły funkcji (procesory, HTTP) publikują i odbierają zdarzenia, choć nie mogą zależeć od siebie nawzajem. Moduł nie ma zależności zewnętrznych.

## Kroki (osobne PR-y)

1. **Rdzeń** (zrobione, ADR 0024). Fazy zamykania w `ContextLifecycleListener` i `RetryPolicy` w `concurrency`, z migracją `ScheduledMethodRegistrar` do fazy producentów. ADR 0024 „Fazy zamykania kontekstu i wspólna polityka ponowień”.
2. **Moduł `08_events`.** `EventPublisher` i `Events`, `@EventListener` i `ApplicationEventListener[E]`, dopasowanie po `GenericType`, odbiorcy synchroniczni i asynchroniczni, ponowienia, zamykanie w fazie `Events`. ADR o modelu zdarzeń, przewodnik w obu językach i użycie w demo.
3. **Zdarzenia frameworka i testkit.** `ContextReady`, `ApplicationStarted`, `ContextClosing`, `ApplicationFailed`, `withSynchronousEvents`, `awaitIdle`, `recordedEvents`.

## Kryteria akceptacji

1. Zdarzenie opublikowane przez bean trafia do odbiorcy `@EventListener` i do beana `ApplicationEventListener[E]`, a odbiorca typu nadrzędnego z sealed traitu też je dostaje. Odbiorca `Changed[Customer]` nie dostaje `Changed[Order]`.
2. Odbiorca synchroniczny kończy pracę przed powrotem z `publish`, a jego wyjątek trafia do publikującego. Gdy rzucają dwaj odbiorcy, drugi wyjątek jest w `suppressed`, a odbiorcy asynchroniczni nie są uruchamiani.
3. `publish` wraca, zanim odbiorca asynchroniczny skończy (test z odbiorcą czekającym na zatrzask). Wyjątek odbiorcy asynchronicznego nie dociera do publikującego i jest zalogowany.
4. Odbiorca asynchroniczny z `retry` i dwoma wyjątkami dostaje zdarzenie za trzecim razem. Bez `retry` jest jedna próba.
5. **Zamykanie.** `close()` z wolnym odbiorcą asynchronicznym w trakcie kończy się po jego zakończeniu i przed `@PreDestroy` beanów, z których korzysta. Zdarzenie opublikowane przez odbiorcę w trakcie czekania jest dostarczone. Po `shutdown-timeout` zamykanie loguje liczbę porzuconych dostarczeń i się kończy.
6. Publikacja spoza odbiorcy po rozpoczęciu zamykania rzuca `EventBusClosedException`.
7. Zadania `@Scheduled` są zatrzymane przed fazą `Events`, więc nie dokładają nowych zdarzeń.
8. `ContextReady` przychodzi przed pierwszym `ApplicationRunner`, `ContextClosing` przed fazą `Producers`, a `ApplicationFailed` przy błędzie startu.
9. `events` nie ma zależności zewnętrznych, co sprawdza `checkModuleLayers`.
10. Demo publikuje zdarzenie po zakończonym zleceniu i ma dwóch odbiorców: synchroniczne statystyki i asynchroniczne powiadomienie z ponowieniami.

## Poza zakresem

- Trwałe zdarzenia, outbox, Kafka, dostarczanie między replikami i po restarcie. To zakres przetwarzania w tle (COILIX-00026) i jego konektorów.
- Odbiorcy związani z transakcjami.
- Kolejność per klucz, dead letter, ograniczone kolejki.
- Event sourcing i projekcje.
- Odbiorcy w `IO`. Przyjdą z modułem `cats-effect` (COILIX-00020) jako odbiorca zwracający `IO[Unit]`.

## Ryzyka i otwarte pytania

1. Użytkownicy przyzwyczajeni do szyn z gwarancjami mogą oczekiwać, że zdarzenie asynchroniczne przetrwa restart. Przewodnik ma na początku mówić wprost, że nie przetrwa, i odsyłać do procesorów.
2. Ponowienia odbiorców asynchronicznych są mostem w stronę procesorów. Jeśli zaczną rosnąć (kolejność, dead letter), to znak, że dany przypadek należy do COILIX-00026, a nie do tego modułu.
3. Domyślne 10 s czekania przy zamykaniu trzeba zgrać z `terminationGracePeriodSeconds` w Kubernetesie razem z limitami wykonawców. Przewodnik podaje gotowe wartości.
