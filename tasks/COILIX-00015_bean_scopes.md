# COILIX-00015: Zasięgi beanów i leniwe tworzenie

## Cel

Oprócz singletonów kontekst obsługuje beany tworzone przy każdym pobraniu (prototype) i singletony tworzone dopiero przy pierwszym użyciu (lazy), bez łamania niemutowalności rejestru ([ADR 0004](../docs/adr/0004-immutable-context-and-constructor-injection.md)).

## Stan obecny

- Każdy bean to singleton tworzony przy starcie kontekstu.
- `RegisteredBean` trzyma gotową instancję, a `ImmutableBeanRegistry` po zbudowaniu się nie zmienia.
- Cyklu zależności nie da się przerwać, bo nie ma leniwego dostępu do beana.

## Zakres

1. Adnotacja `@Scope("prototype")` na klasie i metodzie `@Bean`. Domyślnie `singleton`.
2. Adnotacja `@Lazy`: singleton tworzony przy pierwszym `getBean` albo pierwszym użyciu przez `Provider[T]`.
3. `Provider[T]` jako rodzaj zależności (`Dependency.Deferred`): bean dostaje dostawcę zamiast instancji. Pozwala wstrzyknąć prototype do singletona i przerwać cykl.
4. Prototype nie jest niszczony przez kontekst (`@PreDestroy` z COILIX-00004 go nie dotyczy). Opisane w Scaladoc.
5. Błąd przy starcie, gdy singleton bez `Provider` zależy od prototype i użytkownik oczekuje nowej instancji. Proponowane: ostrzeżenie w logu, nie błąd.

## Proponowany projekt

- `RegisteredBean` z instancją zamienia się na dostawcę instancji (`BeanInstance`: gotowa, leniwa albo za każdym razem nowa).
- Leniwy singleton przez `lazy val` w niemutowalnym obiekcie, więc bez `var` i synchronizacji w kodzie frameworka.
- `TopologicalDependencyResolver` nie traktuje `Deferred` jako warunku kolejności, więc cykl przez `Provider` jest dozwolony.
- `Provider[T]` w module `context` jako trait z `get(): T`.

## Kryteria akceptacji

1. Dwa `getBean` na beanie prototype dają dwie różne instancje.
2. `@Lazy` nie tworzy beana przy starcie (sprawdzane w logu albo licznikiem w teście).
3. Cykl `A(b: Provider[B])`, `B(a: A)` startuje bez błędu.
4. `Provider[T]` dla prototype daje nową instancję przy każdym `get()`.

## Poza zakresem

- Zasięgi request i session. Przyjdą z modułem `web`.
- Proxy zasięgowe (wstrzyknięcie prototype jako zwykłego `T`, które zmienia instancję pod spodem).

## Ryzyka i otwarte pytania

- Zmiana `RegisteredBean` dotyka publicznego API `BeanRegistry` i `ConfigurableApplicationContext`. To zmiana **BREAKING** w `0.x`.
- Wyjątek w leniwym tworzeniu pojawia się w trakcie działania aplikacji, a nie przy starcie. Trzeba to wyraźnie opisać.
