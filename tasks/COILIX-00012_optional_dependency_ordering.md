# COILIX-00012: Kolejność tworzenia przy zależnościach `Option[T]` i `List[T]`

## Cel

Bean z zależnością `Option[T]` albo `List[T]` dostaje wszystkie pasujące beany z kontekstu, a nie tylko te, które przypadkiem powstały przed nim.

## Stan obecny

- `TopologicalDependencyResolver.requiredTypes` bierze pod uwagę tylko `Dependency.Required`. `Optional` i `Collection` nie są warunkiem gotowości beana.
- Beany gotowe w tej samej rundzie trafiają do wyniku w kolejności definicji, czyli po nazwie klasy (`ReflectionsClassPathScanner` sortuje po `getName`).
- `ReflectiveBeanInstantiator` rozwiązuje `Optional` i `Collection` przez `registry.find` / `registry.findAll`, czyli tylko wśród beanów już zarejestrowanych.
- Skutek: `final class A(b: Option[Zeta])` przy beanie `Zeta` bez zależności dostaje `None`, bo `A` jest przed `Zeta` alfabetycznie. Ten sam problem dotyczy `List[T]`: lista może być niepełna.

## Zakres

1. `Optional` i `Collection` stają się warunkami kolejności: bean czeka na wszystkie istniejące definicje pasujących typów.
2. Brak pasującej definicji nie jest błędem (dla `Optional` to `None`, dla `Collection` pusta lista).
3. Cykl przez zależność opcjonalną daje `CircularDependencyException` z nazwami beanów w cyklu, tak jak cykl przez `Required`.
4. Testy w `TopologicalDependencyResolverTest` (nowy plik) i test kontekstu z beanami nazwanymi tak, żeby kolejność alfabetyczna była odwrotna do zależności.

## Proponowany projekt

- `TopologicalDependencyResolver.prerequisites` zbiera trzy rodzaje zależności:
  - `Required`: jak dziś, `selector.select` i błąd przy braku,
  - `Optional`: `selector.select` bez błędu przy braku,
  - `Collection`: wszystkie definicje przypisywalne do typu, bez `CandidateSelector`.
- Beany z `provided` (singletony) dalej nie są warunkiem.
- Bez zmian w `BeanDefinition` i `Dependency`.

## Kryteria akceptacji

1. `Option[T]` dostaje `Some`, gdy bean `T` jest w kontekście, niezależnie od nazw klas.
2. `List[T]` zawiera wszystkie beany `T` z kontekstu.
3. Brak beana daje `None` / `Nil`, bez wyjątku.
4. Wpis w `CHANGELOG.md` w sekcji `Fixed`.

## Poza zakresem

- Leniwe dostawcy (`Provider[T]`) do przerywania cykli. To część COILIX-00015.

## Ryzyka i otwarte pytania

- Aplikacje, które dziś mają cykl ukryty przez `Option`, zaczną dostawać `CircularDependencyException`. W fazie `0.x` to akceptowalne, ale wymaga wpisu w changelogu.
- `Optional` z kilkoma kandydatami bez `@Primary`: dziś `find` zwraca wynik `CandidateSelector`, który rzuca `AmbiguousBeanException`. Trzeba zdecydować, czy tak zostaje.
