# 0053: Błędy typowane zdarzeń

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 10)

## Kontekst

Ostatnim modułem rdzenia, który zgłaszał przewidywalne błędy tylko wyjątkami, był `events`. Skaner odbiorców rzucał `InvalidEventListenerException` dla złej metody `@EventListener`, a `EventPublisher.publish` rzucał `EventBusClosedException`, gdy kontekst się zamykał. Kod, który może publikować w czasie zamykania (zadanie w tle, fiber procesora), musiał łapać klasę wyjątku, żeby odróżnić zamkniętą szynę od błędu odbiorcy.

## Decyzja

- **`EventListenerError` dla odbiorców.** Przypadki: `OverloadedMethod`, `InvalidMethod`, `UndeclaredEventType`, `UnknownEventType`. Skaner zwraca `Either[EventListenerError, List[RegisteredListener]]`. Szyna jest warstwą najwyższą tego sprawdzenia: w `onContextStarted` kończy start wyjątkiem `InvalidEventListenerException` z tym samym komunikatem co wcześniej.
- **`PublishError` dla publikacji.** Przypadki `Closing` i `Closed` z typem zdarzenia. `EventPublisher.tryPublish` zwraca `Either[PublishError, Unit]`, a `publish` jest finalną metodą, która rzuca `exception` błędu, tak jak dotąd. `Events.tryPublish` robi to samo z wydawcą z `using`.
- **Błąd odbiorcy zostaje wyjątkiem.** Wyjątek synchronicznego odbiorcy to defekt jego kodu, nie przewidywalny stan szyny, więc dalej trafia do wywołującego `publish` i `tryPublish`.

## Konsekwencje

- Kod, który może działać w czasie zamykania, widzi w typie, że zdarzenie może nie zostać przyjęte, i sam decyduje: zapisze to w logu albo pominie. Demo zapisuje w logu inspekcję, której nie zgłosiło.
- Zmiana niezgodna dla własnych implementacji `EventPublisher`: implementują `tryPublish` zamiast `publish`. Wywołania `publish` się nie zmieniają.
- Z ADR 0050–0053 każdy moduł rdzenia ma swój typ przewidywalnych błędów. Następne moduły (Kafka) stosują tę samą zasadę od początku.
