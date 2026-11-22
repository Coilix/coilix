# 0026: Zdarzenia frameworka i zdarzenia w testkicie

- Status: proponowana
- Data: 2026-11-22
- Task: COILIX-00016 (krok 3)

## Kontekst

Moduł `events` (ADR 0025) dostarcza zdarzenia aplikacji, ale sam framework nie mówi przez niego, kiedy aplikacja jest gotowa, kiedy się zamyka i kiedy start się nie udał. Testy kodu z odbiorcami asynchronicznymi muszą czekać na `EventBus.awaitIdle` i nie mają jak sprawdzić, jakie zdarzenia opublikowano. Specyfikacja COILIX-00016 wymaga też, żeby `ContextClosing` przychodziło przed fazą `Producers`, a szyna zamyka się dopiero w fazie `Events`.

## Decyzja

- Moduł `events` dostaje cztery zdarzenia frameworka jako zwykłe case classy: `ContextReady(context)`, `ApplicationStarted(context)`, `ContextClosing(context)` i `ApplicationFailed(context, failure)`. Nie mają wspólnego sealed traitu, bo zasada „jeden typ na plik” nie pozwala trzymać przypadków sealed traitu w osobnych plikach. Odbiorcy słuchają ich jak każdego innego zdarzenia.
- `ClosingPhase` dostaje pierwszą fazę `Announcement`, przed `Producers`. Bean `contextClosingPublisher` (`ContextClosingPublisher`, `ContextLifecycleListener` w tej fazie) publikuje w niej `ContextClosing`. Szyna jest wtedy jeszcze otwarta, a na odbiorców asynchronicznych `ContextClosing` czeka faza `Events`. Wyjątek odbiorcy jest logowany i nie przerywa zamykania. Rozważone publikowanie z samej szyny odpadło, bo słuchacz kontekstu ma jedną fazę, a szyna musi zostać w fazie `Events`.
- `boot` publikuje `ContextReady`, `ApplicationStarted` i `ApplicationFailed` przez `StartupEvents.around(context)(runners)`. Wołają to `DefaultCoilixApplicationRunner` i `DefaultTestContextFactory`, więc kontekst testowy startuje tak samo jak aplikacja. `ContextReady` przychodzi po `onContextStarted` wszystkich słuchaczy, a nie z `onContextStarted` szyny, bo słuchacze startują w kolejności tworzenia. Kontekst z wyłączonym modułem zdarzeń niczego nie publikuje.
- Wyjątek runnera albo odbiorcy `ContextReady` lub `ApplicationStarted` jest publikowany jako `ApplicationFailed`, potem kontekst jest zamykany, a wyjątek rzucany dalej. Wcześniej `Coilix.run` zostawiał po błędzie runnera otwarty kontekst do shutdown hooka. Błąd przy tworzeniu beanów i w `onContextStarted` nie daje `ApplicationFailed`, bo kontekst jeszcze nie istnieje albo szyna nie zna odbiorców. Zatrzymanie po błędach fatalnych opisze COILIX-00030.
- `EventsSettings.synchronous` (`coilix.events.synchronous`, domyślnie `false`) uruchamia odbiorców asynchronicznych w wątku publikującego, po synchronicznych. Ich wyjątki są nadal tylko logowane, a ponowienia idą od razu, bez opóźnień polityki. Ustawienie jest w module zdarzeń, a nie w testkicie, więc testkit nie zależy od wnętrza szyny.
- Testkit: `CoilixTestSetup.withSynchronousEvents` ustawia `coilix.events.synchronous = true`. Każdy kontekst testkitu ma bean `eventRecorder` (`EventRecorder`, synchroniczny `ApplicationEventListener[AnyRef]`). `CoilixContextOps.recordedEvents[E]` zwraca nagrane zdarzenia klasy `E`, a `awaitEvents(timeout)` czeka na `EventBus.awaitIdle`.

## Konsekwencje

- Odbiorca może zareagować na koniec startu albo na początek zamykania bez implementowania `ContextLifecycleListener` i bez znajomości faz.
- `recordedEvents` filtruje po klasie, bez argumentów typu: `recordedEvents[Changed[Customer]]` zwraca też `Changed[Product]`. Rekorder nie zna statycznego typu z `publish`, a pełne dopasowanie po typie zostaje w odbiorcach.
- Każdy kontekst testkitu ma o jeden bean więcej (`eventRecorder`), a każda aplikacja bean `contextClosingPublisher`. Testy listujące wszystkie beany muszą je uwzględnić.
- Nowa faza `Announcement` jest zmianą zgodną: słuchacze bez jawnej fazy zostają w `Producers`.
