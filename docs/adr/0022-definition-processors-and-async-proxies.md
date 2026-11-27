# 0022: Procesory definicji z modułów i proxy dla `@Async`

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00005 (krok 2)
- Częściowo zastąpione przez: 0057 (metody `@Async` zwracają `IO`, proxy zależy od `BlockingExecutor` i `AsyncTasks`)

## Kontekst

`@Async` ma przekierować wywołanie metody beana na `TaskExecutor`. Na JVM bez generowania bajtkodu da się to zrobić tylko proxy JDK, które implementuje interfejsy, czyli traity beana. Szkic COILIX-00005 zakładał `BeanPostProcessor`, który podmienia instancję na proxy. To nie wystarcza:

- `BeanPostProcessor` jest jednym komponentem frameworka z `ConfigPath`, a nie czymś, co moduł może dołożyć.
- Proxy nie jest instancją klasy beana, a typ beana jest ustalony w definicji i użyty przy planowaniu. Bean zarejestrowany pod klasą, a podmieniony na proxy, dawałby błędy rzutowania dopiero przy wstrzyknięciu.
- Proxy potrzebuje beana `TaskExecutor`, więc wykonawca musi powstać przed beanem z `@Async`.
- Metody `@PostConstruct` i `@PreDestroy` są metodami klasy, więc wywołane na proxy rzuciłyby wyjątek.

## Decyzja

- `CoilixModule.definitionProcessors` (domyślnie pusta) zwraca `DefinitionProcessor`y. `DefaultCoilixBootstrap` przepuszcza przez nie definicje skanowane i definicje modułów przed zamianami beanów (`BeanReplacements`) i przed planowaniem. Procesor może zmienić typ, fabrykę i zależności beana.
- `AsyncDefinitionProcessor` z modułu `concurrency` zmienia definicję beana z metodami `@Async`:
  - typ beana to trait, który deklaruje te metody (z argumentami typów odczytanymi z nadtypów klasy),
  - fabryka (`AsyncProxyFactory`) tworzy bean oryginalną fabryką i zwraca proxy wszystkich traitów klasy,
  - ostatnią zależnością jest `TaskExecutor`, więc kolejność tworzenia wynika z grafu zależności.
- Metoda `@Async` zwraca `Future[T]` (wynik z wykonawcy) albo `Unit` (uruchomienie bez czekania). Inny wynik, brak traitu z tą metodą albo metody w kilku traitach to `InvalidAsyncMethodException` przy tworzeniu definicji, czyli przy starcie.
- Proxy implementuje `ProxiedBean` z rdzenia kontekstu. `PostConstructProcessor`, `PreDestroyInvoker` i `ScheduledMethodRegistrar` wywołują metody klasy na obiekcie za proxy (`ProxiedBean.unwrap`).

## Konsekwencje

- Bean z `@Async` wstrzykuje się przez trait. Wstrzyknięcie przez klasę daje `NoSuchBeanException` przy planowaniu, a nie błąd w trakcie działania.
- Wywołanie metody z wnętrza beana (`this.method()`) omija proxy i działa synchronicznie.
- Konteksty budowane bez `boot` (np. testy samego `context`) nie stosują procesorów modułów. Trzeba je wywołać jawnie.
- `DefinitionProcessor` to nowy punkt rozszerzeń dla modułów, np. dla przyszłych proxy transakcji. Zmienia definicje wszystkich modułów, więc kolejność modułów (po nazwie) jest kolejnością procesorów.
