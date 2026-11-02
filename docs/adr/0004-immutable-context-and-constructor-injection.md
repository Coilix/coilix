# 0004: Niemutowalny kontekst i wstrzykiwanie przez konstruktor

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00002

## Kontekst

Spring wstrzykuje zależności przez konstruktor, pola i settery, a jego rejestr beanów jest mutowalny. Zasady kodu Coilix zakazują `var`, `null` i mutowalnych kolekcji.

## Decyzja

- Zależności wstrzykuje się tylko przez konstruktor (albo parametry metody `@Bean`). Nie ma wstrzykiwania w pola ani settery.
- `TopologicalDependencyResolver` ustala kolejność tworzenia z góry, a `DefaultApplicationContextBuilder` składa `ImmutableBeanRegistry` przez `foldLeft`.
- Cykl zależności to błąd startu (`CircularDependencyException`).

## Konsekwencje

- Beany mogą być niemutowalne (`final class` z `val`), a odczyt z kontekstu jest bezpieczny wątkowo bez synchronizacji.
- Cykli nie da się rozwiązać przez późne wstrzyknięcie. Trzeba je usunąć z projektu albo przerwać przez `Option`/fabrykę.
- Zasięgi inne niż singleton i leniwe beany wymagają osobnego mechanizmu (np. dostawcy instancji), bo rejestr po zbudowaniu się nie zmienia.
