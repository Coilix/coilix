# COILIX-00020: Integracja z cats-effect (`IO`)

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-19: rdzeń zostaje w stylu bezpośrednim (kod blokujący na wątkach wirtualnych), a efekty wpinają się w wyznaczonych punktach. Wspieramy `cats.effect.IO`. ZIO i fs2 są poza planem, a Pekko Streams to osobny task (COILIX-00029).

## Cel

Aplikacja napisana na `IO` używa Coilix bez rezygnacji z efektów i zasobów:

- bean może powstać jako `IO[A]` albo `Resource[IO, A]`, a zasoby są otwierane w kolejności zależności i zamykane w odwrotnej,
- metody cyklu życia i runnery mogą zwracać `IO[Unit]`,
- aplikacja startuje jako `IOApp` (`CoilixIOApp`), a kontekst jest `Resource[IO, ApplicationContext]`,
- `IORuntime` jest beanem zbudowanym z pul Coilix, więc kod blokujący z `IO` trafia na te same wątki wirtualne co reszta aplikacji,
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

- Pula blokująca na wątkach wirtualnych i scheduler jako beany z cyklem życia, z rozmiarami i nazwami wątków z configu. Jeśli COILIX-00005 będzie gotowy wcześniej, to są jego `TaskExecutor` i `TaskScheduler`. Jeśli nie, ten task dodaje minimalne `ExecutionResources` w rdzeniu, a COILIX-00005 je przejmuje.

### 4. Moduł `cats-effect` (`coilix-cats-effect`)

- **`IORuntime` jako bean** (`CoilixIORuntimeFactory`, komponent z `given ConfigPath`):
  - pula obliczeniowa: własna pula work-stealing cats-effect (z rozmiarem z configu), bo zwykła pula jest dla fiberów wyraźnie wolniejsza,
  - pula blokująca (`IO.blocking`, `IO.interruptible`): wątki wirtualne z zasobów wykonania Coilix,
  - scheduler: scheduler Coilix,
  - zamknięcie runtime'u razem z kontekstem, po beanach, które go używają.
- **Rozszerzenia jako `given` i metody rozszerzające**, w stylu z CLAUDE.md: np. `IO.coilixBlocking(thunk)` jest zbędne, bo `IO.blocking` już trafia na wątki wirtualne. W API modułu `IORuntime` jest dostępny jako `given` tam, gdzie trzeba go przekazać dalej (most do Pekko w COILIX-00029).
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
4. Zasoby wykonania (albo COILIX-00005).
5. `cats-effect`: `IORuntime` jako bean, beany `IO[A]` i `Resource[IO, A]`, cykl życia i runnery w `IO`, `CoilixIOApp`.
6. `testkit-cats-effect` i przewodnik w obu językach.

## Kryteria akceptacji

1. Bean z zależnościami `Repo[IO]` i `Sync[IO]` dostaje właściwe instancje, gdy w kontekście jest też bean `Repo[Option]`. Zależność z lambdą typu daje błąd startu z nazwą parametru.
2. `@Bean def repo: Repo[IO] = SqlRepo[IO]` i wiązanie `coilix.effect.type` pozwalają skanować `SqlRepo[F]` z `using Sync[F]`.
3. `Resource[IO, A]` z metody `@Bean` jest otwierany przed beanami zależnymi i zamykany po nich. Test sprawdza kolejność otwarć i zamknięć trzech zasobów z zależnościami.
4. `@PreDestroy` zwracające `IO[Unit]` wykonuje się przy zamykaniu, a błąd jednego finalizatora nie zatrzymuje pozostałych i jest zalogowany.
5. `CoilixIOApp` startuje aplikację z runnerem `IO`, a anulowanie (SIGTERM w teście przez anulowanie fibera) zamyka zasoby w odwrotnej kolejności.
6. `IO.blocking` w beanie wykonuje się na wątku wirtualnym Coilix (test sprawdza nazwę i `Thread.isVirtual`).
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
3. Dwa tryby startu (`CoilixIOApp` i `Coilix.run` z `IORuntime` z beana) to dwie ścieżki do utrzymania. Alternatywa: tylko `CoilixIOApp` dla aplikacji z beanami efektowymi.
4. Pula obliczeniowa cats-effect zostaje poza kontrolą Coilix (tylko rozmiar z configu). Wspólne są pula blokująca i scheduler.
