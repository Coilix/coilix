# SCALING-00004: Cykl życia beanów: `@PostConstruct` i `@PreDestroy`

## Cel

Beany mogą deklarować metody wywoływane:

- po utworzeniu i wstrzyknięciu zależności (`@PostConstruct`),
- przy zamykaniu kontekstu (`@PreDestroy`).

Semantyka jak w Springu, ale z własnymi adnotacjami frameworka, bez zależności od `jakarta.annotation`.

## Stan obecny

- `05_context/src/main/scala/ReflectiveBeanInstantiator.scala` tylko wywołuje konstruktor i nie ma żadnego kroku po utworzeniu.
- `DefaultApplicationContextBuilder` składa rejestr przez `foldLeft` po beanach w kolejności zależności, więc kolejność tworzenia jest znana.
- Kontekst nie ma `close()`. To dochodzi w tasku SCALING-00002.

## Zakres

1. Adnotacje `@PostConstruct` i `@PreDestroy` w `02_annotations`, z `@Target(METHOD)` i retencją runtime.
2. Wywoływanie `@PostConstruct`:
   - zaraz po utworzeniu beana, przed rejestracją, więc beany zależne dostają już zainicjalizowany obiekt,
   - tylko metody bezargumentowe, publiczne, zwracające `Unit`,
   - kilka metod w jednym beanie wywoływanych w ustalonej kolejności (po nazwie).
3. Wywoływanie `@PreDestroy` przy `close()` kontekstu:
   - w odwrotnej kolejności tworzenia beanów,
   - wyjątek w jednej metodzie jest logowany i nie przerywa niszczenia pozostałych beanów.
4. Czytelne błędy:
   - `@PostConstruct` z argumentami daje błąd z nazwą beana i metody już na etapie definicji, nie w trakcie wywołania,
   - wyjątek rzucony w `@PostConstruct` jest opakowany w `BeanCreationException` z nazwą beana.
5. Opisanie metod cyklu życia w `BeanDefinition`, żeby budowanie i niszczenie korzystały z tej samej informacji.

## Proponowany projekt

- `02_annotations`: `PostConstruct.java`, `PreDestroy.java`.
- `05_context`:
  - `LifecycleMethods`: case class z listami metod init i destroy, jako nowe pole w `BeanDefinition`,
  - `LifecycleMethodScanner` (trait) i `AnnotatedLifecycleMethodScanner`, wywoływane z `ReflectiveBeanDefinitionFactory`,
  - `BeanPostProcessor` (trait) jako punkt rozszerzeń w stylu Springa. Pierwsza implementacja to `PostConstructProcessor`, a kolejne (np. `@Async` z taska SCALING-00005) dojdą w ten sam sposób,
  - `BeanDestroyer` (trait) i `PreDestroyInvoker`, wywoływane z `close()` kontekstu,
  - `InvalidLifecycleMethodException` i `BeanCreationException` w osobnych plikach.
- `RegisteredBean` zachowuje definicję albo jej metody destroy, żeby `close()` wiedziało, co wywołać.

## Kryteria akceptacji

1. Bean z `@PostConstruct` ma zainicjalizowany stan, zanim zostanie wstrzyknięty do innego beana. Test sprawdza to beanem zależnym.
2. `close()` wywołuje `@PreDestroy` w odwrotnej kolejności tworzenia.
3. Wyjątek w `@PreDestroy` jednego beana nie blokuje pozostałych.
4. Błędna sygnatura metody cyklu życia daje wyjątek z nazwą beana i metody.
5. Demo ma bean z `@PostConstruct` i `@PreDestroy`, a logi pokazują obie fazy przy starcie i przy Ctrl+C.

## Poza zakresem

- Interfejsy w stylu `InitializingBean` / `DisposableBean`. Można je dodać później jako kolejny `BeanPostProcessor`.
- Zakresy beanów inne niż singleton (prototype, request).

## Ryzyka i otwarte pytania

- Czy `@PostConstruct` ma być dozwolone na metodach prywatnych? Spring na to pozwala. Proponowane: tak, przez `setAccessible`, ale dopiero po decyzji, bo to omija enkapsulację.
- Metody Scali bez nawiasów (`def init: Unit`) też są bezargumentowe w bajtkodzie, więc powinny działać. Warto to potwierdzić testem.
