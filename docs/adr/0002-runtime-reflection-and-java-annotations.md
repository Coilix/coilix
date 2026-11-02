# 0002: Refleksja w runtime i adnotacje w Javie

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00002, COILIX-00003

## Kontekst

Kontener musi znaleźć klasy komponentów, ich konstruktory i parametry z adnotacjami (`@Qualifier`, `@Value`). Da się to zrobić w czasie kompilacji (makra, derywacja) albo w runtime (refleksja JVM). Adnotacje Scali (`StaticAnnotation`) nie są widoczne przez refleksję JVM w runtime, a adnotacje Javy z `@Retention(RUNTIME)` są.

## Decyzja

- Komponenty wykrywa się w runtime: `ReflectionsClassPathScanner` (biblioteka `org.reflections`) skanuje pakiety aplikacji, a definicje beanów powstają z refleksji JVM (`ReflectiveBeanDefinitionFactory`).
- Adnotacje frameworka są napisane w Javie w osobnym module `02_annotations`, z retencją runtime.
- Java jest tylko tam, gdzie jest wymagana. Reszta frameworka jest w Scali 3.

## Konsekwencje

- API znane programistom Springa, bez makr po stronie użytkownika i bez generowanego kodu.
- Błędy powiązań (brak beana, cykl, niejednoznaczność) wychodzą przy starcie aplikacji, a nie przy kompilacji.
- Skanowanie classpath kosztuje czas startu. Warto to mierzyć (benchmarki w ROADMAP.md).
- Informacja o typach generycznych pochodzi z sygnatur JVM (`GenericDependencyTypeMapper`), więc typy wyższego rzędu i typy zależne Scali nie są w pełni obsługiwane.
- Utrudnia to przyszłe wsparcie GraalVM native-image (potrzebna konfiguracja refleksji).
