# 0017: Komponenty zbierane przez moduły

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00026 (krok 2)

## Kontekst

Procesory tła z COILIX-00026 nie mają być beanami: tworzy je i trzyma `ProcessorManager`, a żaden bean nie może ich wstrzyknąć ani wywołać z pominięciem nadzoru. Mimo to procesor potrzebuje tego, co ma bean: zależności z kontekstu, `@Value`, `@PostConstruct` i `@PreDestroy`. Skaner rdzenia znał tylko adnotacje komponentów i każdą znalezioną klasę zamieniał na bean. Tej samej możliwości mogą potrzebować inne moduły, np. kontrolery HTTP z COILIX-00023.

Rdzeń nie może znać adnotacji modułów funkcji (`docs/architecture/modularity.md`), więc `@Processor` nie może trafić do listy adnotacji skanera.

## Decyzja

- `CoilixModule` dostaje `def collects: List[Class[? <: Annotation]] = Nil`. Moduł podaje adnotacje, których klasy zbiera zamiast robić z nich beany. Wartość domyślna nie zmienia istniejących modułów.
- `ClassPathScanner` dostaje `scanAnnotated(basePackage, annotations)`, które zwraca konkretne klasy z tymi adnotacjami, bez definicji beanów. `DefaultCoilixBootstrap` zbiera je ze skanowanych pakietów i z `components` modułów. Komponent modułu z adnotacją zbieraną nie staje się beanem, więc moduł może dostarczać też własne zbierane klasy.
- Nowy komponent `ComponentCollector` (`coilix.context.component-collector`, domyślnie `DefaultComponentCollector`) robi z zebranych klas jedną definicję beana `collectedComponents` typu `CollectedComponents`. Definicje klas tworzy zwykły `BeanDefinitionFactory`, a warunki (`@Profile`, `@ConditionalOnProperty`, `@ConditionalOnClass`) sprawdza zwykły `DefinitionFilter`.
- Definicja `collectedComponents` ma jako zależności sumę zależności zebranych klas. Dzięki temu nie trzeba zmieniać `ApplicationContextBuilder` ani `DependencyResolver`: kolejność, brakujący bean i cykl są obsługiwane tak samo jak dla beanów i wychodzą przy starcie, a nie dopiero przy tworzeniu procesora. Wartości zależności są rozwiązywane raz, więc każda instancja zebranej klasy dostaje te same beany.
- `CollectedComponents` ma `classes`, `annotatedWith[A]` i `create[T](componentClass)`. Każde `create` tworzy nową instancję, przepuszcza ją przez `BeanPostProcessor` (`@PostConstruct`) i zwraca `ManagedComponent[T]`, którego `close` wywołuje `@PreDestroy` przez `BeanDestroyer` (raz). Kontekst nie niszczy zebranych instancji sam, bo nie wie, ile ich powstało. Robi to moduł, zwykle z własnej metody `@PreDestroy`.
- Bean `collectedComponents` jest rejestrowany tylko wtedy, gdy jakiś włączony moduł zbiera adnotacje, więc aplikacje bez takich modułów mają te same beany co wcześniej.
- Klasa z adnotacją komponentu i adnotacją zbieraną to `CollectedComponentConflictException` przy starcie. Bean, który wymaga zebranej klasy, dostaje `NoSuchBeanException` z nazwą adnotacji i podpowiedzią, żeby wstrzyknął bean modułu, a nie ogólny komunikat o braku beana.

## Konsekwencje

- `ProcessorManager` (COILIX-00026, krok 4) wstrzykuje `CollectedComponents`, bierze `annotatedWith[Processor]` i tworzy procesory przez `create[ProcessorDefinition]`. Kontrolery HTTP mogą użyć tego samego mechanizmu.
- Zebrana klasa nie może być zależnością beana, `@Bean` ani `ApplicationRunner`. Metody `@Bean` w zebranej klasie nie dodają beanów.
- `@ConditionalOnMissingBean` na zebranej klasie porównuje ją tylko z innymi zebranymi klasami, bo filtr działa przed budową kontekstu.
- `DefaultCoilixBootstrap` ma nowy parametr konstruktora, a `ClassPathScanner` nową metodę abstrakcyjną. Rdzeń nie ma jeszcze wydania, więc nie jest to zmiana niezgodna wobec użytkowników.
