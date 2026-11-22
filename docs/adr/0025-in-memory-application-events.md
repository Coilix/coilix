# 0025: Lekkie zdarzenia aplikacji tylko w pamięci

- Status: proponowana
- Data: 2026-11-20
- Task: COILIX-00016 (krok 2)

## Kontekst

Beany potrzebują sposobu, żeby reagować na to, co robią inne beany, bez zależności od nich. Pierwsza specyfikacja COILIX-00016 zakładała szynę z wymiennym transportem (pamięć, outbox w bazie, Kafka), nazwanymi subskrypcjami z pozycjami odczytu, kolejnością per klucz, dead letter i dostarczaniem między replikami. Opiekun uznał, że to dubluje przetwarzanie w tle (COILIX-00026), które i tak ma trwałe źródła, model błędów i przydział między repliki, a moduł zdarzeń ma być lekki i mało zobowiązujący.

## Decyzja

- Nowy moduł rdzenia `events` (`08_events`) zależy tylko od `concurrency`. `boot` zależy od niego, więc każda aplikacja ma bean `eventBus`. Moduł podłącza się przez `CoilixModule` i można go wyłączyć kluczem `coilix.modules.events.enabled`.
- Zdarzenia są tylko w pamięci jednego procesu. Nie ma transportów, serializacji, dostarczania po restarcie ani między replikami. Trwała praca należy do COILIX-00026.
- Odbiorca to metoda z adnotacją `@EventListener` (Java, w `annotations`) albo bean `ApplicationEventListener[E]`. Trait nie nazywa się `EventListener`, żeby nie zderzał się z adnotacją przy imporcie. Dopasowanie idzie po `GenericType` z podtypami i argumentami typu. Typ zdarzenia przy publikacji to typ statyczny z `TypeInfo`, chyba że klasa w runtime jest węższa i nie ma parametrów typu, bo wtedy klasa mówi więcej (np. przypadek enuma opublikowany jako typ enuma).
- Odbiorca jest domyślnie synchroniczny i działa w wątku publikującego. Wyjątek trafia do publikującego, a pozostali odbiorcy synchroniczni i tak działają (kolejne wyjątki jako `suppressed`). `async = true` uruchamia odbiorcę na `TaskExecutor` dopiero po udanych odbiorcach synchronicznych. Wyjątek jest logowany, a ponowienia liczy `RetryPolicy` (ADR 0024) z `coilix.events.listeners.<bean>.<metoda>.retry`. `RetryPolicy` dostaje wartości domyślne parametrów, żeby config ustawiał tylko zmieniane klucze.
- Szyna jest `ContextLifecycleListener`. Odbiorców szuka w `onContextStarted` po wszystkich beanach (za proxy przez `ProxiedBean.unwrap`), jak `ScheduledMethodRegistrar`. Zdarzenia opublikowane wcześniej, np. w konstruktorze, czekają w buforze i są dostarczane zaraz po znalezieniu odbiorców. Rozważony `DefinitionProcessor`, który rejestrowałby odbiorców przy tworzeniu beanów, odpadł: po procesorze `@Async` definicja ma typ traitu i nie widać już metod implementacji.
- Zamykanie w fazie `Events`: publikacja spoza odbiorcy rzuca `EventBusClosedException` (odbiorca może publikować, żeby łańcuchy się domknęły), czekające ponowienia są anulowane, a szyna czeka na trwających odbiorców asynchronicznych do `coilix.events.shutdown-timeout` (domyślnie 10 s). Potem przerywa ich wątki i loguje na `ERROR` liczbę porzuconych dostarczeń na odbiorcę.

## Konsekwencje

- Moduł nie ma zależności zewnętrznych ani stanu poza pamięcią, a jego kontrakt jest prosty do opisania: przy awarii procesu zdarzenia dla odbiorców asynchronicznych przepadają.
- Dostarczenia asynchroniczne nie mają kolejności. Kto jej potrzebuje, używa odbiorcy synchronicznego albo procesora z COILIX-00026.
- Odbiorca `ApplicationEventListener` utworzony z lambdy nie ma typu zdarzenia w runtime i zatrzymuje start wyjątkiem `InvalidEventListenerException`. Musi to być klasa albo klasa anonimowa.
- Odbiorca wywoływany jest bezpośrednio na obiekcie za proxy, więc metoda `@EventListener` w beanie z `@Async` nie przechodzi przez proxy wykonawcy. O asynchroniczności decyduje tylko `async` w adnotacji.
- Zdarzenia frameworka (`ContextReady`, `ApplicationStarted`, `ContextClosing`, `ApplicationFailed`) i pomoce testkitu dochodzą w kroku 3 (ADR 0026).
