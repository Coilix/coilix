# 0030: Beany prototype i leniwe w niemutowalnym rejestrze

- Status: proponowana
- Data: 2026-11-22
- Task: COILIX-00015

## Kontekst

Każdy bean był singletonem tworzonym przy starcie, a `RegisteredBean` trzymał gotową instancję w niemutowalnym rejestrze (ADR 0004). Brakowało beanów tworzonych przy każdym użyciu (koszyk, arkusz zlecenia), beanów kosztownych i opcjonalnych tworzonych dopiero przy pierwszym użyciu oraz sposobu na przerwanie cyklu zależności.

## Decyzja

- Adnotacje `@Prototype` i `@Lazy` (znaczniki na klasie i metodzie `@Bean`) zamiast `@Scope("prototype")`: bez napisów, literówka to błąd kompilacji. `BeanDefinition.creation` (`BeanCreation.Eager`, `Lazy`, `Prototype`) czyta je `ReflectiveBeanDefinitionFactory`.
- `RegisteredBean` zostaje gotową instancją. Rejestr przechowuje `BeanEntry`: `RegisteredBean` albo `DeferredBean`, który tworzy instancję przy wyszukaniu, raz dla leniwego singletonu (zapamiętuje ją w `AtomicReference`, tworzenie pod `synchronized`, więc dwa wątki dostają tę samą) i za każdym razem dla prototypu. Rejestr dalej tylko rośnie przez nowe wartości (`register`, `defer`), a zapamiętana instancja leniwego beana jest stanem samego `DeferredBean`.
- `BeanRegistry.beans` i nowe `ApplicationContext.createdBeans` zwracają beany, które mają instancję: każdy eager i leniwe już utworzone, nigdy prototypy. `beanNames` zwraca wszystkie nazwy. Słuchacze kontekstu, szyna zdarzeń i `@Scheduled` przeglądają `createdBeans`, więc nie tworzą beanów leniwych. Niszczenie dotyczy tylko beanów z instancją, więc prototypy nie mają `@PreDestroy` z kontekstu.
- `Provider[T]` to zależność `Dependency.Deferred`. Nie porządkuje tworzenia, więc przerywa cykl, ale brak beana `T` (albo niejednoznaczność) zatrzymuje start. `RegistryProvider` szuka w `BeanRegistry.complete`, czyli w rejestrze uruchomionego kontekstu, który `DefaultApplicationContextBuilder` ustawia w `RegistryCompletion` po wykonaniu planu. Provider użyty w trakcie startu widzi rejestr z chwili tworzenia swojego beana.
- Singleton, który wymaga prototypu bezpośrednio, dostaje jedną instancję, a builder loguje ostrzeżenie z podpowiedzią o `Provider`. Ostrzeżenie zamiast błędu, bo czasem jedna instancja jest zamierzona.

## Konsekwencje

- Zmiana niezgodna przed `core-v0.1.0`: `BeanRegistry` ma nowe metody `beanNames`, `defer` i `complete`, `ApplicationContext` metodę `createdBeans`, `ImmutableBeanRegistry` przyjmuje `BeanEntry` i `RegistryCompletion`. Własne implementacje tych traitów trzeba uzupełnić.
- Błąd w konstruktorze beana leniwego wychodzi przy pierwszym użyciu, nie przy starcie. Przewodnik zaleca `@Lazy` tylko dla beanów kosztownych i opcjonalnych.
- Bean tworzony przy starcie, który wymaga beana leniwego bezpośrednio, tworzy go przy starcie. Leniwość zachowuje `Provider`.
- Leniwy bean nieutworzony przy starcie nie jest przeszukiwany pod kątem `@EventListener` i `@Scheduled`.
- Zasięgi request i session przyjdą z modułem `web`, a proxy zasięgowe zostają poza zakresem.
