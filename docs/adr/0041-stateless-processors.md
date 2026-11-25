# 0041: Procesory bezstanowe, źródła elementów, statystyki i zdarzenia procesorów

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00026 (krok 5)

## Kontekst

ADR 0037 dał procesorom tła sloty, shardy, nadzór i dwa style, w których kod sam prowadzi pętlę pracy (`BlockingProcessor`, `CustomProcessor`). Większość flow w bulldozerze czyta jednak elementy ze źródła (Kafka, kolejka, pliki), przetwarza każdy osobno i potwierdza go po przetworzeniu. Statystyki są tam tylko w Kamonie, więc z kodu nie da się zapytać o stan przetwarzania. Ten krok dodaje styl dla takiej pracy, SPI źródeł niezależne od silnika strumieni, liczniki shardów i zdarzenia. Obsługa błędów pojedynczych elementów (`FailureHandling`, dead letter) i równoległość w shardzie to kroki 6 i 7.

## Decyzja

- **SPI źródeł.** `InputSource[T]` (`AutoCloseable`) z `poll(max, timeout)`, `lag`, `pause`, `resume`, `slotCount`; `Input[T]` z wartością, slotem, `InputId`, numerem dostarczenia i `Acknowledgement` (`acknowledge`, `reject`); `Output[R]` z `emit`. Źródło powstaje dla każdego startu sharda i czyta tylko jego sloty. Worker woła `poll` w pętli na własnym wątku, więc konektor może blokować i nie potrzebuje Pekko. Zamknięcie źródła oddaje niepotwierdzone elementy do ponownego dostarczenia.
- **Styl `StatelessProcessor[T]`.** `source(shard)` i `process(element, ElementContext)`, z `pollSize` i `pollTimeout` do nadpisania. Worker przetwarza elementy po kolei, potwierdza element po powrocie z `process`, a między elementami sprawdza `stopRequested`. Wyjątek z `process` kończy workera wyjątkiem `ElementFailedException` (shard, `InputId`, numer dostarczenia, przyczyna). Restart idzie przez `RestartPolicy`, a element wraca z wyższym numerem dostarczenia. MDC workera dostaje `input` z identyfikatorem elementu. Styl uruchamia `VirtualThreadEngine`.
- **Sloty źródła.** Szkic chciał, żeby `slotCount` źródła miał pierwszeństwo przed konfiguracją. Sloty shardów ustala jednak menedżer przy starcie, zanim powstanie jakiekolwiek źródło, więc źródło z własną liczbą slotów wymaga takiej samej wartości `slots` procesora. Niezgodność kończy workera wyjątkiem `InvalidProcessorException` z kluczem do ustawienia.
- **Implementacje w pamięci.** `InMemoryStream[T](slotCount, keyOf)`: `publish` wkłada element do slotu jego klucza (`ShardKeys.slotOf`), `source(shard)` czyta sloty sharda, potwierdzony element znika, niepotwierdzony wraca po zamknięciu źródła albo odrzuceniu, `pending` i `acknowledged` służą testom. `InMemoryOutput[R]` przechowuje emitowane elementy. Obie klasy są otwarte, żeby aplikacja mogła zrobić z nich beany przez dziedziczenie.
- **Statystyki.** `ShardMetrics` (liczniki przetworzonych i nieudanych elementów, łączny i najdłuższy czas, czas ostatniego elementu, lag per slot) jest polem `Shard.metrics`. Menedżer tworzy jeden obiekt na shard i przekazuje go każdemu nowemu `Shard` przy restarcie, więc liczniki przeżywają restarty. Workery stylu bezstanowego zapisują je same, a `BlockingProcessor` i `CustomProcessor` przez `shard.metrics`. `ProcessorManager.statistics(id)` zwraca `ProcessorStatistics` z `ShardStatistics` (stan sharda i `ShardMetricsSnapshot`) i sumami. Histogramy, tempo i percentyle ze szkicu czekają na integrację z systemem metryk (`processing-micrometer`).
- **Zdarzenia przez `EventPublisher`.** Zamiast osobnego `ProcessorListener` ze szkicu menedżer publikuje `ProcessorEvent.ShardStateChanged` (każda zmiana `WorkerState`) i `ProcessorEvent.ShardFailed` (błąd workera z `FailureInfo`) przez `EventPublisher` z rdzeniowego modułu `events`, od którego `processing` teraz zależy. Odbiorcy `@EventListener(async = true)` działają poza wątkiem menedżera, czego wymagał szkic, a aplikacja nie uczy się nowego mechanizmu. Zdarzenia elementów (`ElementRetried`, `ElementSkipped`, `ElementDeadLettered`) przyjdą z `FailureHandling`.

## Konsekwencje

- Flow z bulldozera, które czytają elementy i przetwarzają je po jednym, przenoszą się na `StatelessProcessor` z konektorem `InputSource`. Do kroku 6 każdy błąd elementu restartuje workera, więc trujący element blokuje swój shard do wyczerpania prób restartu.
- `ProcessorManager` ma nową metodę `statistics`, co jest zmianą niezgodną dla własnych implementacji tego traitu. Moduł nie jest jeszcze wydany.
- Testy, które zbierają wszystkie zdarzenia kontekstu (`recordedEvents[AnyRef]`), widzą też zdarzenia procesorów.
