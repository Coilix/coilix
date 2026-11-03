# COILIX-00003: Konfiguracja: pliki, `@Value`, `@Configuration` i nadpisywanie komponentów frameworka

## Cel

Aplikacja czyta konfigurację z `application.conf`, `application.yaml`/`application.yml` i `application.properties`. Beany dostają wartości przez `@Value`, sekcje configu wiążą się z case class przez `@ConfigurationProperties`, a beany można definiować metodami `@Bean` w klasach `@Configuration`.

Dodatkowo każdy wewnętrzny komponent frameworka (np. `ConstructorSelector`) da się podmienić z configu, bez przepisywania całego złożenia `default`.

## Stan obecny

- Brak jakiejkolwiek konfiguracji. Jedyne wejście z zewnątrz to `ApplicationArguments`.
- Złożenia `default` w obiektach towarzyszących (`BeanDefinitionFactory.default`, `ApplicationContextBuilder.default`, ...) są zaszyte na sztywno.
- Beany powstają tylko z konstruktorów klas `@Component`/`@Service`/`@Repository`.

## Zakres

1. Nowy moduł `04_config` (`dev.coilix.config`). Kolejne moduły przesuwają się o jeden numer (`05_context` ... `11_intellij_plugin`).
2. Ładowanie configu:
   - formaty HOCON (`.conf`), properties i YAML. Wszystkie lądują w jednym drzewie (Typesafe Config, YAML przez snakeyaml-engine),
   - kolejność ważności (od najwyższej): opcje `--klucz=wartość` z linii poleceń, właściwości systemowe JVM, `application.properties`, `application.yaml`, `application.yml`, `application.conf`,
   - `Environment` jako bean do wstrzyknięcia.
3. Wiązanie configu z typami (`ConfigBinder`):
   - typy proste: `String`, `Int`, `Long`, `Double`, `Boolean`, `FiniteDuration`, `java.time.Duration`,
   - `Option[T]`, `List[T]`/`Seq[T]`/`Set[T]`, `Map[String, T]`,
   - klasy (w tym case class) przez publiczny konstruktor: każdy parametr szukany pod `ścieżka.nazwaParametru` albo w wersji kebab-case (`maxSize` -> `max-size`),
   - brakujący parametr: domyślna wartość Scali, potem `None` dla `Option`, inaczej błąd,
   - klucz `class` w sekcji wskazuje konkretną klasę. Musi być podtypem oczekiwanego typu, a jej parametry wiążą się rekurencyjnie z tej samej sekcji.
4. Adnotacje w `02_annotations`:
   - `@Value("${klucz}")` i `@Value("${klucz:domyślna}")` na parametrach konstruktora i metod `@Bean`,
   - `@ConfigurationProperties("prefiks")` na klasie: bean powstaje z sekcji configu,
   - `@Configuration` na klasie i `@Bean` na metodach: każda metoda to definicja beana, a jej parametry to zależności.
5. Nadpisywanie komponentów frameworka:
   - `ConfigPath[T]`, czyli given w obiekcie towarzyszącym traitu ze ścieżką, np. `coilix.context.constructor-selector`,
   - `ComponentProvider.get[T: ConfigPath](default: => T): T`. Gdy w configu nie ma ścieżki, zwraca `default`. Gdy jest, buduje klasę z klucza `class` przez `ConfigBinder`,
   - każde `default` w obiektach towarzyszących `context` i `boot` przechodzi przez `ComponentProvider`.
6. Migracja `10_demo`: `application.yaml`, `@Value`, `@ConfigurationProperties`, `@Configuration` z `@Bean` oraz przykład nadpisania komponentu frameworka.

## Proponowany projekt

Przykład nadpisania:

```hocon
coilix.boot.startup-message-formatter {
  class = "dev.coilix.demo.DemoStartupMessageFormatter"
  prefix = ">>"
}
```

```scala
object StartupMessageFormatter {
  given ConfigPath[StartupMessageFormatter] = ConfigPath("coilix.boot.startup-message-formatter")

  def default(using components: ComponentProvider): StartupMessageFormatter =
    components.get[StartupMessageFormatter](DefaultStartupMessageFormatter())
}
```

- `04_config`: `Environment`, `ConfigTree` (surowe drzewo), `ConfigBinder`, `ScalarReader`, `ValueExpression`, `EnvironmentLoader` i `ConfigFormat` (HOCON, properties, YAML), `ConfigPath`, `ComponentProvider`.
- `05_context`:
  - `BeanDefinition` dostaje `InstanceFactory` zamiast `Constructor` (konstruktor, metoda `@Bean` albo wiązanie configu),
  - `Dependency.Value` dla parametrów `@Value`,
  - `BeanDefinitionFactory.create` zwraca listę definicji (klasa i jej metody `@Bean`).
- `09_boot`: runner najpierw parsuje argumenty i ładuje `Environment`, a dopiero potem składa resztę startu z `ComponentProvider`.

## Kryteria akceptacji

1. Ta sama konfiguracja działa w `.conf`, `.yaml` i `.properties`, a kolejność ważności jest zgodna z zakresem.
2. `@Value` wstrzykuje wartość z configu z konwersją typu i obsługą domyślnej wartości.
3. `@ConfigurationProperties` tworzy bean z sekcji configu, z domyślnymi wartościami Scali i nazwami kebab-case.
4. Metody `@Bean` w klasie `@Configuration` tworzą beany z zależnościami.
5. Wskazanie klasy pod ścieżką komponentu frameworka podmienia go, a parametry konstruktora tej klasy są czytane z configu.
6. Brak ścieżki daje domyślną implementację. Nieistniejąca klasa, zły typ albo brak wartości parametru kończą się `ConfigurationException` z czytelnym komunikatem.
7. `sbt demo/run` pokazuje wartości z `application.yaml` i nadpisany komponent.

## Poza zakresem

- Profile (`application-dev.yaml`) i zmienne środowiskowe.
- Przeładowywanie configu w trakcie działania.
- Proxy dla klas `@Configuration` (wywołanie jednej metody `@Bean` z drugiej tworzy nowy obiekt, nie zwraca beana).

## Ryzyka i otwarte pytania

- Nazwy parametrów konstruktora: Scala 3 zapisuje je w bytecode (sprawdzone), ale klasy użytkownika z innych kompilatorów mogą ich nie mieć.
- Typ beana z metody `@Bean` to zadeklarowany typ zwracany, więc `getBean` po klasie implementacji go nie znajdzie.
- Komponenty używane przed załadowaniem configu (parser argumentów, loader configu) nie dają się nadpisać.
