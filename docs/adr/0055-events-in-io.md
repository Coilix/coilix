# 0055: Szyna zdarzeń na `IO`

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 4)
- Zastępuje część: 0024 i 0025 (wykonanie odbiorców na wątkach `TaskExecutor`, ponowienia przez `TaskScheduler`, przerywanie wątków przy zamykaniu)

## Kontekst

Szyna zdarzeń była ostatnim dużym kawałkiem runtime'u na wątkach: odbiorcy zwracali `Unit`, asynchroniczne dostarczenia szły jako zadania `TaskExecutor` albo `BlockingExecutor`, ponowienia przez `TaskScheduler`, a stan był w `AtomicReference` z `synchronized` i `wait`/`notify`. Kod aplikacji jest już w `IO` (ADR 0042, 0049), więc odbiorca w `IO` musiał uruchamiać efekty poza fiberem albo wracać do kodu synchronicznego.

## Decyzja

- **Publikacja i odbiorcy w `IO`.** `publish` zwraca `IO[Unit]`, `tryPublish` `EitherT[IO, PublishError, Unit]` (ADR 0053). Metoda `@EventListener` i `ApplicationEventListener.onEvent` zwracają `IO[Unit]`; skaner odrzuca metodę, która zwraca coś innego.
- **Odbiorcy synchroniczni w fiberze publikującego, asynchroniczni na własnych fiberach.** Asynchroniczne dostarczenie startuje pod `Supervisor` szyny, więc dziedziczy wartości `IOLocal` publikującego, w tym MDC `coilix-logging` (ADR 0054). Ponowienie czeka przez `IO.sleep`, w wyścigu z sygnałem zamykania, który kończy czekające ponowienia od razu.
- **Atrybut `blocking` usunięty.** O tym, czy odbiorca blokuje, decyduje jego `IO`: wywołanie, które czeka, idzie w `IO.blocking`. Szyna nie potrzebuje już `TaskExecutor`, `BlockingExecutor` ani `TaskScheduler`.
- **Stan w `Ref`, `SignallingRef`, `Deferred` i `IOLocal`.** Etap szyny i zdarzenia sprzed startu są w jednym `Ref`, więc zdarzenie albo czeka na start, albo jest dostarczane. Liczba dostarczeń jest w `SignallingRef`, na którym czeka `awaitIdle`. To, czy fiber jest odbiorcą (który może publikować w czasie zamykania), mówi `IOLocal`.
- **Zamykanie przez anulowanie.** Po `shutdown-timeout` szyna anuluje działające dostarczenia zamiast przerywać wątki. Szyna jest beanem z `Resource`, którego zwolnienie anuluje to, co jeszcze działa.
- **`close()` na globalnym runtime.** Szyna jako bean z efektu sprawia, że interpreter tworzy też późniejsze beany, w tym właściciela runtime'u kontekstu, którego `@PreDestroy` wyłącza ten runtime przy zwalnianiu. Synchroniczne `close()` zamyka więc kontekst na globalnym runtime cats-effect (`SynchronousLifecycle.closed`), tak jak `started` go otwiera; zamykanie na runtime kontekstu wyłączało runtime, na którym samo działało, i nigdy się nie kończyło.
- **Zdrowie w `IO` w zakresie potrzebnym szynie.** `Health.refresh` zwraca `IO[Unit]`, bo publikuje `HealthChanged`. Wątek diagnostyki zostaje platformowy i wykonuje odświeżenie przez `SynchronousLifecycle.runOn`; reszta diagnostyki przechodzi na `IO` w kroku 6.

## Konsekwencje

- Odbiorcy piszą się jak reszta kodu aplikacji w `IO`, bez `unsafeRun*`, a asynchroniczni odbiorcy widzą MDC publikującego.
- Zmiana niezgodna API: odbiorcy zwracają `IO[Unit]`, kod publikujący uruchamia efekt `publish`, a zdarzenie sprzed startu publikuje się z `@PostConstruct` zwracającego `IO[Unit]` zamiast z konstruktora.
- Błąd fatalny w asynchronicznym odbiorcy obsługuje runtime cats-effect, tak jak w każdym innym fiberze, a nie `FatalErrorHandler` wykonawcy.
- Testy synchroniczne (`setup.use`) czekają na odbiorców przez `awaitEvents()`, które blokuje wątek przez `SynchronousLifecycle`; testy w `IO` używają `EventBus.awaitIdle`.
