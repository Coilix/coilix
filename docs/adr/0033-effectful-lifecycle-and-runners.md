# 0033: Cykl życia i runnery w IO

- Status: proponowana
- Data: 2026-11-24
- Task: COILIX-00020 (krok 5b, część 2)

## Kontekst

Decyzje opiekuna z 2026-11-23 (task COILIX-00020): `@PostConstruct` i `@PreDestroy` mogą zwracać `IO[Unit]` obok zwykłych zwracających `Unit`, a `IOApplicationRunner` i `ApplicationRunner` działają w jednej liście według `@Order`. Rdzeń nie zna cats-effect (ADR 0031), a beany z efektu tworzy interpreter planu z modułu (ADR 0032). `IOApplicationRunner` potrzebuje `ApplicationArguments`, które są w `boot`.

## Decyzja

- **Metody cyklu życia z efektem w rdzeniu.** `AnnotatedLifecycleMethodScanner` przyjmuje `EffectfulResultRecognizer` (domyślnie ten sam co dla metod `@Bean`) i dopuszcza metody zwracające efekt `Unit` rozpoznany jako `BeanResult.Deferred`, np. `IO[Unit]`. `Resource` i efekt innej wartości to nadal `InvalidLifecycleMethodException`. `LifecycleMethods` dzieli metody na `plainInit`/`effectfulInit` i `plainDestroy`/`effectfulDestroy` po typie zwracanym (sprawdzona metoda zwraca `Unit` albo efekt). `PostConstructProcessor` i `PreDestroyInvoker` wołają tylko zwykłe.
- **Bean efektowy.** `BeanDefinition.isEffectful` = wynik z efektu albo metoda cyklu życia z efektem. `ContextPlan.effectful`, podział planu w builderze i `EffectfulBeanException` używają tej definicji, więc bez interpretera taki bean zatrzymuje start, a z interpreterem trafia do niego.
- **Metody znalezione dopiero na instancji.** Gdy klasa instancji (np. implementacja traitu z metody `@Bean`) ma metodę z efektem, a bean powstaje w builderze albo na żądanie (leniwy, prototyp), `LifecycleMethods.withoutEffects` rzuca `BeanCreationException` z podpowiedzią, żeby zadeklarować metody w typie beana. Efektu nie da się tam uruchomić, a cicho pominięty byłby błędem.
- **Moduł `cats-effect`.** `EffectfulBeanProcessor` traktuje takie beany jak beany z efektu (`dependsOn` na runtime i ustawienia, `@Lazy`/`@Prototype` → `InvalidEffectfulBeanException`). `IOBeanResources` po utworzeniu beana uruchamia `effectfulInit` po kolei (błąd → `BeanCreationException`), a w finalizerze najpierw `effectfulDestroy` (błąd logowany), potem `BeanDestroyer` dla zwykłych, wszystko w limicie z `ShutdownTimeouts`.
- **Runnery.** Nowy punkt rozszerzeń w `boot`: bean `RunnerAdapter` (`runners(context)`, `run(runner, arguments)`). `OrderedRunnerInvoker` łączy beany `ApplicationRunner` z runnerami wszystkich adapterów w jedną listę po `@Order` klasy runnera, przy remisie w kolejności tworzenia. Moduł daje `IOApplicationRunner` i bean `ioRunnerAdapter`, który czeka na efekt przez `unsafeRunSync` na `ioRuntime`. Moduł `cats-effect` zależy teraz od `boot` (wcześniej od `concurrency`), co zasady warstw dopuszczają.

## Konsekwencje

- Metody `@PostConstruct` z efektem działają po zwykłych, a `@PreDestroy` z efektem przed zwykłymi, niezależnie od kolejności nazw. Kolejność po nazwach obowiązuje w każdej z tych grup.
- Bean z metodą cyklu życia w `IO` jest zawsze tworzony przy starcie, jak bean z efektu.
- `RunnerAdapter` jest ogólny, więc przyszłe moduły (np. Pekko) dołożą własne runnery bez zmian w `boot`.
- W krok 5c `CoilixIOApp` uruchomi runnery wewnątrz `IO` (zwykłe w `IO.blocking`) zamiast przez `unsafeRunSync` w adapterze.
