# 0032: Beany z IO i Resource przez interpreter planu z modułu

- Status: proponowana
- Data: 2026-11-23
- Task: COILIX-00020 (krok 5b, część 1)

## Kontekst

Decyzje opiekuna z 2026-11-23 (task COILIX-00020): beany `IO[A]` i `Resource[IO, A]` otwierane równolegle poziomami grafu zależności i zamykane w odwrotnej kolejności, jeden `unsafeRun` na granicy `Coilix.run`, limit zamykania globalny i dla beana (`@ShutdownTimeout`), błąd startu dla `@Lazy` i `@Prototype` na beanie z efektu. Rdzeń nie zna cats-effect, a `IORuntime` jest beanem z modułu (ADR 0031), więc istnieje dopiero w trakcie budowy kontekstu.

## Decyzja

- **Poziomy w planie.** `PlanStep.Create` ma numer poziomu, `ContextPlan.levels` grupuje po nim, a `DependencyResolver.resolveLevels` daje poziomy (domyślnie każdy bean osobno, `TopologicalDependencyResolver` daje rundy swojego sortowania, w których beany nie zależą od siebie).
- **SPI `EffectInterpreter`** w `context`, podłączany przez `CoilixModule.effectInterpreter`. `DefaultCoilixBootstrap` przekazuje go do `ApplicationContextBuilder.build(definitions, singletons, interpreter)`; dwa moduły z interpreterem to `ConflictingEffectInterpretersException`. Builder tworzy sam poziomy przed pierwszym poziomem z beanem z efektu, a resztę przekazuje interpreterowi. Interpreter zwraca `InterpretedBeans(registry, release)`; kontekst woła `release` przy zamykaniu przed `BeanDestroyer`. Beany utworzone przez interpreter są rejestrowane bez metod `@PreDestroy`, bo zamyka je `release`. Gdy interpreter rzuci wyjątek, builder niszczy beany utworzone wcześniej.
- **`BeanInstantiator.produce` i `complete`.** `produce` woła fabrykę i zwraca jej wynik (dla beana z efektu sam efekt), `complete` szuka metod cyklu życia na wartości i uruchamia post-processing. `instantiate` = `complete(produce)`. Bez tego `@PostConstruct` klasy wartości byłby wołany na obiekcie `IO`.
- **Moduł `cats-effect`.** `EffectfulBeanProcessor` (`DefinitionProcessor`) dopisuje beanom z efektu `dependsOn` na `ioRuntime` i `catsEffectSettings` (krawędź z ADR o `@DependsOn`), więc runtime powstaje w części synchronicznej, a beany `@Lazy`/`@Prototype` z efektu zatrzymuje `InvalidEffectfulBeanException`. `IOEffectInterpreter` składa pozostałe poziomy w jeden `Resource[IO, BeanRegistry]` (`IOBeanResources`: poziom przez `parTraverse`, zwykłe beany w `IO.blocking` z `@PreDestroy` jako finalizerem, `IO[A]` przez `flatMap`, `Resource[IO, A]` przez `allocated` i własny finalizer), otwiera go jednym `allocated.unsafeRunSync()` na beanie `ioRuntime` i zwalnia drugim przy zamykaniu.
- **Limity zamykania.** `ShutdownTimeouts`: finalizer startuje w fiberze, po `coilix.cats-effect.shutdown-timeout` (30 s) albo czasie z `@ShutdownTimeout` (`02_annotations`, czas albo placeholder jak w `@Scheduled`) jest anulowany w tle i zalogowany; błąd finalizera jest logowany. Zamykanie idzie dalej w obu przypadkach.

## Konsekwencje

- Beany jednego poziomu w części interpretowanej powstają równolegle, także zwykłe konstruktory (w `IO.blocking`); kolejność rejestracji, a więc i lista `createdBeans`, zostaje kolejnością z planu.
- Leniwy zwykły bean z poziomów interpretera powstaje na żądanie jak dotąd, ale niszczy go `BeanDestroyer` po `release`, czyli po zasobach, od których może zależeć. Leniwe beany zależne od zasobów to rzadki przypadek; do poprawy, gdy się pojawi.
- Synchroniczny `Coilix.run` bez modułu nadal odrzuca beany z efektu (`EffectfulBeanException`).
- Krok 5b część 2: `@PostConstruct`/`@PreDestroy` zwracające `IO[Unit]` i `IOApplicationRunner`. Krok 5c: `CoilixIOApp` (wtedy interpreter wejdzie w `Resource` aplikacji zamiast `unsafeRunSync`) i `testkit-cats-effect`.
