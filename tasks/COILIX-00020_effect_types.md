# COILIX-00020: Integracja z cats-effect (`IO`)

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-19: rdzeń zostaje w stylu bezpośrednim (kod blokujący na wątkach wirtualnych), a efekty wpinają się w wyznaczonych punktach. Wspieramy `cats.effect.IO`. ZIO i fs2 są poza planem, a Pekko Streams to osobny task (COILIX-00029).

> Decyzje opiekuna z 2026-11-23 (wywiad przed krokami 5b i 5c, zastępują wcześniejsze otwarte pytania):
>
> - **Start:** oba sposoby. `CoilixIOApp` (`IOApp`, anulowanie zamyka kontekst) i zwykłe `Coilix.run`, które wykonuje plan na `IORuntime` z beana z jednym `unsafeRun` na granicy.
> - **Kolejność tworzenia:** niezależne beany `IO` i `Resource` otwierane równolegle poziomami grafu zależności, zamykane w odwrotnej kolejności poziomów. Kolejność wynikającą z efektu ubocznego, a nie z wstrzykiwania (np. migracje przed cache), wyraża `@DependsOn("nazwa")`: krawędź w grafie bez wstrzykiwania. Bez liczbowych priorytetów budowy, bo kłóciłyby się z grafem. Graf jest jedynym źródłem kolejności.
> - **`@DependsOn`** dotyczy wszystkich beanów, więc powstaje w rdzeniu (`annotations`, `TopologicalDependencyResolver`, błąd startu przy nieznanej nazwie, przewodnik) jako osobny PR przed 5b.
> - **Cykl życia:** `@PostConstruct` i `@PreDestroy` mogą zwracać `IO[Unit]`, obok zwykłych zwracających `Unit`.
> - **Runnery:** `IOApplicationRunner` i `ApplicationRunner` w jednej liście według `@Order`, wykonywane po kolei; synchroniczne w `IO.blocking`.
> - **`@Lazy` i `@Prototype` na beanie `IO` albo `Resource`:** błąd startu. Bean efektowy jest zawsze tworzony przy starcie, bez ukrytych `unsafeRun` w trakcie działania.
> - **Zamykanie:** globalny limit `coilix.cats-effect.shutdown-timeout` (domyślnie 30 s) i nadpisanie dla jednego beana adnotacją `@ShutdownTimeout("2m")` na metodzie `@Bean`, czas jako tekst albo placeholder jak w `@Scheduled`. Po limicie finalizer jest anulowany i zalogowany, zamykanie idzie dalej. Błąd jednego finalizera jest logowany i nie zatrzymuje pozostałych. `@ShutdownTimeout` jest w `02_annotations`, bo adnotacje runtime muszą być w Javie.
> - **`CoilixIOApp` po runnerach** czeka na anulowanie (SIGTERM), wtedy zamyka kontekst. `coilix.cats-effect.exit-after-runners = true` kończy aplikację po runnerach (zadania wsadowe).
> - **Testy w `IO`:** osobny moduł `testkit-cats-effect` z munit-cats-effect jako `Provided`: `CoilixTestSetup.useIO` i `CoilixIOSuite` z `coilixTest` zwracającym `IO` i kontekstem jako `given`.
> - **Później, poza 5b i 5c:** beany generyczne w efekcie (`SqlRepo[F]` z `using Sync[F]` i wiązanie `coilix.effect.type`) oraz most MDC dla fiberów (`IOLocal` ↔ MDC).

## Cel

Aplikacja napisana na `IO` używa Coilix bez rezygnacji z efektów i zasobów:

- bean może powstać jako `IO[A]` albo `Resource[IO, A]`, a zasoby są otwierane w kolejności zależności i zamykane w odwrotnej,
- metody cyklu życia i runnery mogą zwracać `IO[Unit]`,
- aplikacja startuje jako `IOApp` (`CoilixIOApp`), a kontekst jest `Resource[IO, ApplicationContext]`,
- `IORuntime` jest beanem z własną pulą work-stealing cats-effect, która staje się jedyną pulą obliczeniową aplikacji (ADR 0028): `TaskExecutor` Coilix i Pekko działają na `runtime.compute`,
- zależności typu `Repository[IO]` i `Sync[IO]` są rozpoznawane po pełnym typie,
- moduły z punktami rozszerzeń (procesory COILIX-00026, HTTP COILIX-00023) dostają silniki i instancje dla `IO`, które działają natywnie w `IO`, bez przekładania na `Future`.

## Stan obecny

- Kontener jest synchroniczny. Bean powstaje z konstruktora albo metody `@Bean`, a `@PostConstruct`, `@PreDestroy` i `ApplicationRunner.run` zwracają `Unit`.
- Kolejność tworzenia wylicza `DependencyResolver` (sortowanie topologiczne), ale wynik nie jest dostępny poza `DefaultApplicationContextBuilder`, więc inny interpreter nie może przejść po tym planie.
- Zależności generyczne są rozpoznawane tylko dla `Option[T]` i `List[T]` (`GenericDependencyTypeMapper`). Pozostałe typy są dopasowywane po klasie, więc `Repository[IO]` i `Repository[Either]` byłyby tym samym typem.
- Nie ma pul wątków ani schedulera jako beanów (COILIX-00005).
- W bibliotece implicits opiekuna jest wzór: `OptimusRuntime` z pulą blokującą na wątkach wirtualnych i `blockingOx`, który uruchamia blokujący kod z `IO` na tej puli (`F.evalOn(F.delay(thunk), rt.blockingCtx)`).

### Wynik eksperymentu z typami (2026-11-19, Scala 3.9.0, cats-effect 3.6)

Sygnatury generyczne JVM zachowują argumenty typów wyższego rzędu w deklaracjach:

| Deklaracja | Co widzi refleksja |
|---|---|
| parametr konstruktora `repo: Repo[IO]` | `Repo[IO]` |
| parametr `sync: Sync[IO]` | `Sync[IO]` |
| `class IoRepo extends Repo[IO]` | interfejs `Repo[IO]` |
| `@Bean def repo: Repo[IO]` | typ zwracany `Repo[IO]` |
| parametr `Repo[[A] =>> Either[String, A]]` | `Repo[Either]` (lambda typu traci częściowe zastosowanie) |
| `class SqlRepo[F[_]](using Sync[F]) extends Repo[F]` | `Repo[F]`, `Sync[F]` (zmienna typu) |

Wnioski:

- `Repo[IO]` da się odróżnić od innych `Repo[F]` bez `TypeTag` ani makr, bo kontener dopasowuje po deklaracjach.
- Lambdy typów są stratne. Taka zależność daje czytelny błąd przy starcie, a nie ciche złe dopasowanie.
- Implementacja generyczna (`SqlRepo[F]`) potrzebuje wiązania `F`, np. przez metodę `@Bean` z konkretnym typem albo jedno wiązanie efektu na aplikację.
- Instancje rejestrowane w runtime (singletony, `addBean` w testkicie) nie mają argumentów typu. Przy rejestracji trzeba podać pełny typ.

## Zakres

### 1. Rdzeń: dopasowanie po typach sparametryzowanych

- `DependencyTypeMapper` i wybór kandydatów porównują pełny `java.lang.reflect.Type`: klasę i argumenty typów, z wariancją tylko tam, gdzie da się ją odczytać, a w pozostałych miejscach z równością argumentów.
- `BeanDefinition` i `RegisteredBean` niosą pełny typ (`beanType: Type`) obok klasy. Rejestracja instancji przyjmuje `[T: TypeInfo]`, czyli typeclass z pełnym typem wyprowadzanym makrem z typu statycznego, zamiast samego `ClassTag`.
- Zmienna typu w implementacji (`SqlRepo[F]`) jest wiązana:
  - przez typ zwracany metody `@Bean` (`def repo: Repo[IO] = SqlRepo[IO]`),
  - albo przez wiązanie efektu aplikacji `coilix.effect.type = "cats.effect.IO"`, które podstawia `F` w klasach skanowanych z jedną zmienną typu wyższego rzędu.
- Zależność z lambdą typu albo z niezwiązaną zmienną typu to błąd startu z nazwą beana i parametru.
- Dotychczasowe dopasowanie dla zależności niegenerycznych się nie zmienia.

### 2. Rdzeń: plan kontekstu jako wartość

```scala
trait ContextPlanner {
  def plan(definitions: List[BeanDefinition], singletons: List[RegisteredBean]): ContextPlan
}

final case class ContextPlan(steps: List[PlanStep]) {
  def creationOrder: List[BeanDefinition]

  def destructionOrder: List[BeanDefinition]
}
```

- `DefaultApplicationContextBuilder` zaczyna od `ContextPlanner.plan` i wykonuje kroki synchronicznie. Zachowanie się nie zmienia.
- `PlanStep` mówi, z czego powstaje bean (konstruktor, metoda `@Bean`, instancja), jakie ma zależności i metody cyklu życia, więc inny interpreter (np. w `IO`) może wykonać ten sam plan.
- `InstanceFactory` dostaje rodzaj wyniku: zwykły, `Deferred` (wynik w efekcie) albo `Managed` (zasób z finalizatorem). Rozpoznawanie typów efektów jest SPI (`EffectfulResultRecognizer`), więc rdzeń nie zna `IO`.
- Synchroniczny builder odrzuca plan z wynikami `Deferred`/`Managed` błędem startu z podpowiedzią o `CoilixIOApp`, zamiast uruchamiać efekt po cichu.

### 3. Rdzeń: zasoby wykonania

- Zrobione w COILIX-00005 (ADR 0020 i 0028): `TaskExecutor` (pula obliczeniowa), `BlockingExecutor` (wątki wirtualne) i `TaskScheduler`. Domyślne `taskExecutor` i `blockingExecutor` mają `@ConditionalOnMissingBean`, więc moduł `cats-effect` podmienia pulę obliczeniową bez tworzenia drugiej.

### 4. Moduł `cats-effect` (`coilix-cats-effect`)

- **`IORuntime` jako bean** (`CoilixIORuntimeFactory`, komponent z `given ConfigPath`), zbudowany domyślnym `IORuntime.builder()` (ADR 0028):
  - pula obliczeniowa: własna pula work-stealing cats-effect z `coilix.concurrency.compute.threads` wątkami. Obcej puli nie podajemy, bo `IORuntime` traci wtedy timery i odpytywanie I/O w wątkach roboczych, `IO.blocking` w miejscu i kolejki pod fibery (błąd `OptimusRuntime`),
  - moduł rejestruje `TaskExecutor` na `runtime.compute` (bez zamykania puli, bo należy do runtime'u), więc domyślny `ForkJoinPool` Coilix nie powstaje,
  - `IO.blocking` i `IO.interruptible` zostają przy mechanizmie cats-effect: blokują w miejscu na wątku `io-compute-blocker-*`, a pula z `setBlocking` jest wtedy pomijana (eksperyment w ADR 0028),
  - zamknięcie runtime'u razem z kontekstem, po beanach, które go używają; zamknięcie, którego nie zlecił kontekst (cats-effect zamyka runtime po błędzie fatalnym i woła hook z `addShutdownHook`), trafia do `FatalErrorHandler` (ADR 0027).
- **Rozszerzenia jako `given` i metody rozszerzające**, w stylu z CLAUDE.md: `IO.onBlockingExecutor(thunk)` = `IO.blocking(thunk).evalOn(blockingExecutor.executionContext)` dla masowego czekania (tysiące równoczesnych wywołań), które na wątkach wirtualnych kosztuje mniej niż wątki `io-compute-blocker`. W API modułu `IORuntime` jest dostępny jako `given` tam, gdzie trzeba go przekazać dalej (most do Pekko w COILIX-00029).
- **Beany w efekcie**:

  ```scala
  @Configuration
  final class DatabaseConfiguration {
    @Bean
    def transactor(settings: DatabaseSettings): Resource[IO, Transactor[IO]] =
      HikariTransactor.fromSettings(settings)

    @Bean
    def migrations(transactor: Transactor[IO]): IO[MigrationReport] =
      Migrations.run(transactor)
  }
  ```

  `Resource[IO, A]` daje bean typu `A`, otwierany w kolejności zależności i zamykany w odwrotnej. `IO[A]` daje bean typu `A` po wykonaniu efektu. Zależni dostają `A`, nie `IO[A]`.
- **Cykl życia w `IO`**: `@PostConstruct` i `@PreDestroy` mogą zwracać `IO[Unit]`, a `IOApplicationRunner.run(arguments): IO[Unit]` zastępuje runner synchroniczny.
- **Start**:

  ```scala
  @CoilixApplication
  object ShopApplication extends CoilixIOApp
  ```

  `CoilixIOApp` rozszerza `IOApp`, buduje kontekst jako `Resource[IO, ApplicationContext]` przez interpreter `ContextPlan` w `IO` (konstruktory i metody synchroniczne w `IO.blocking`, beany efektowe natywnie), uruchamia runnery `IO` i trzyma aplikację do anulowania. SIGTERM anuluje `IOApp`, co zamyka zasoby w odwrotnej kolejności. W tym trybie runtime należy do `IOApp`, a Coilix dokłada do niego pule blokujące przez `runtimeConfig`/`runtime` override.
- **`Coilix.run` z beanami `IO`**: przy zwykłym starcie moduł rejestruje interpreter, który wykonuje plan na `IORuntime` z beana i zwraca zwykły kontekst. Zamknięcie kontekstu wykonuje finalizatory zasobów. Tylko na granicy `Coilix.run` jest jedno `unsafeRunSync`, bo `Coilix.run` jest synchroniczne z definicji.
- **Testkit**: `CoilixTestSetup.useIO { ... }: IO[A]` i suite dla `munit-cats-effect` (`CoilixIOSuite`) z `coilixTest(name) { ... }` zwracającym `IO`, z kontekstem jako `given` jak w COILIX-00013.

### 5. Silniki dla modułów z punktami rozszerzeń

Moduł `cats-effect` daje tylko runtime, beany efektowe i start. Silniki dla konkretnych funkcji są w modułach integracyjnych, bo moduły funkcji nie zależą od siebie:

- `processing-cats-effect` (COILIX-00026): silnik fiberów i procesory `IOStatelessProcessor`, `IOBatchProcessor`, `IOStatefulProcessor`,
- `web-cats-effect` (COILIX-00023): `ResponseEffect[IO]`,
- `pekko-cats-effect` (COILIX-00029): mosty między `IO` i Pekko Streams.

Silnik działa natywnie w `IO`: backoff to `IO.sleep`, zatrzymanie to anulowanie fibera, nadzór na `Supervisor` i `Deferred`. Polityki i stan (`RestartPolicy`, `FailureHandling`, statystyki) są wspólnym czystym modelem z modułu funkcji.

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `common`, `context` | rdzeń (zmiana) | — | pełne typy w definicjach, `TypeInfo`, `ContextPlanner`, `ContextPlan`, `EffectfulResultRecognizer`, zasoby wykonania |
| `cats-effect` | funkcja | rdzeń, cats-effect | `IORuntime` jako bean, rozpoznawanie `IO` i `Resource`, interpreter planu w `IO`, cykl życia i runnery w `IO`, `CoilixIOApp` |
| `testkit-cats-effect` | integracja | `testkit`, `cats-effect`, munit-cats-effect (`Provided`) | `useIO`, `CoilixIOSuite` |

## Kroki (osobne PR-y)

1. ADR „Model efektów”: styl bezpośredni w rdzeniu, punkty wpięcia, tylko `IO`, wynik eksperymentu z typami, właściciel `IORuntime`, brak przekładania `IO` na `Future` poza mostem do Pekko. Zrobione w ADR 0019.
2. Rdzeń: pełne typy w definicjach i dopasowanie po typach sparametryzowanych, `TypeInfo`, błędy dla lambd typów i niezwiązanych zmiennych. Zrobione (`GenericType`, `TypeInfo`, ADR 0019); wiązanie `coilix.effect.type` przechodzi do kroku 5.
3. Rdzeń: `ContextPlan`, `ContextPlanner` i `EffectfulResultRecognizer`, `DefaultApplicationContextBuilder` na planie. Zrobione (ADR 0019). Rodzaj wyniku (`BeanResult`) jest w `BeanDefinition.result`, a nie w `InstanceFactory`, i ustawia go `ReflectiveBeanMethodScanner`, więc bean efektowy ma od początku typ wartości. Cykl życia zwracający `IO[Unit]` przechodzi do kroku 5.
4. Zasoby wykonania (albo COILIX-00005). Zrobione przez COILIX-00005 krok 1: `TaskExecutor` na wątkach wirtualnych i `TaskScheduler` z modułu `concurrency` (ADR 0020).
5. `cats-effect`, w krokach:
   - 5a: `IORuntime` jako bean i jedyna pula obliczeniowa (zrobione, ADR 0031).
   - rdzeń: `@DependsOn` dla wszystkich beanów (osobny PR przed 5b).
   - 5b: beany `IO[A]` i `Resource[IO, A]` otwierane równolegle poziomami grafu, `@PostConstruct` i `@PreDestroy` zwracające `IO[Unit]`, `IOApplicationRunner` w jednej liście z `ApplicationRunner`, `@ShutdownTimeout` i `coilix.cats-effect.shutdown-timeout`, błąd startu dla `@Lazy` i `@Prototype` na beanie efektowym, `Coilix.run` z jednym `unsafeRun` na granicy.
   - 5c: `CoilixIOApp` (czeka na anulowanie, `exit-after-runners`) i moduł `testkit-cats-effect` (`useIO`, `CoilixIOSuite`).
6. Przewodnik w obu językach przy każdym kroku. Później: beany generyczne w `F[_]` i most MDC dla fiberów.

## Kryteria akceptacji

1. Bean z zależnościami `Repo[IO]` i `Sync[IO]` dostaje właściwe instancje, gdy w kontekście jest też bean `Repo[Option]`. Zależność z lambdą typu daje błąd startu z nazwą parametru.
2. (później, poza 5b i 5c) `@Bean def repo: Repo[IO] = SqlRepo[IO]` i wiązanie `coilix.effect.type` pozwalają skanować `SqlRepo[F]` z `using Sync[F]`.
3. `Resource[IO, A]` z metody `@Bean` jest otwierany przed beanami zależnymi i zamykany po nich. Test sprawdza kolejność otwarć i zamknięć trzech zasobów z zależnościami.
4. `@PreDestroy` zwracające `IO[Unit]` wykonuje się przy zamykaniu, a błąd jednego finalizatora nie zatrzymuje pozostałych i jest zalogowany.
5. `CoilixIOApp` startuje aplikację z runnerem `IO`, a anulowanie (SIGTERM w teście przez anulowanie fibera) zamyka zasoby w odwrotnej kolejności.
6. Z modułem `cats-effect` aplikacja ma jedną pulę obliczeniową: `TaskExecutor` wykonuje zadania na wątkach `io-compute-*`, a wątków `coilix-compute-*` nie ma. `IO.onBlockingExecutor` wykonuje się na wątku wirtualnym Coilix (test sprawdza nazwę i `Thread.isVirtual`).
7. Synchroniczny `Coilix.run` bez modułu `cats-effect` i z beanem `Resource[IO, A]` kończy się błędem startu z podpowiedzią.
8. Test w `testkit-cats-effect` z `coilixTest(name) { bean[Repo[IO]].find(id).map(assertEquals(_, None)) }` przechodzi.
9. Rdzeń nie zależy od cats-effect, co sprawdza `checkModuleLayers`.

## Poza zakresem

- ZIO, fs2 i inne biblioteki efektów.
- Tagless final w API frameworka (`F[_]` wszędzie). API modułów jest dla `IO` albo w stylu bezpośrednim.
- Przeładowanie beanów efektowych w działającej aplikacji.

## Ryzyka i otwarte pytania

1. Pełne typy w `BeanDefinition` i `RegisteredBean` zmieniają publiczne API rdzenia. Trzeba to zrobić przed `core-v0.1.0` albo w następnej linii `0.x` z wpisem **BREAKING**. Dotyczy też `TestBean` z testkitu (COILIX-00013).
2. Wariancja: `Repo[+A]` i podobne. Na początek równość argumentów, a wariancja tylko tam, gdzie da się ją odczytać z sygnatury Scali (TASTy), co może wymagać czytania TASTy zamiast samej refleksji Javy.
3. Dwa tryby startu (`CoilixIOApp` i `Coilix.run` z `IORuntime` z beana) to dwie ścieżki do utrzymania. Rozstrzygnięte 2026-11-23: oba, wspólny interpreter planu w `IO` pod spodem.
4. Pula obliczeniowa cats-effect zostaje poza kontrolą Coilix (tylko rozmiar z configu), ale jest jedyną pulą CPU aplikacji. Do zmierzenia w benchmarkach (COILIX-00017): opóźnienia `IO` przy ciężkim obciążeniu z Pekko na tej samej puli (w eksperymencie z ADR 0028 bez zagłodzenia).
