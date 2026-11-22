# Niezależność modułów

## Cel

Użytkownik dodaje tylko te moduły, których potrzebuje. Aplikacja konsolowa używa `boot` i nie ma na classpath serwera HTTP, sterowników baz danych ani security. Dodanie `web` do zależności włącza endpointy bez zmiany kodu startowego.

## Warstwy

| Warstwa | Moduły | Wymagane | Może zależeć od |
|---|---|---|---|
| rdzeń | `common`, `annotations`, `config`, `context`, `concurrency`, `cluster`, `events` | tak | tylko rdzenia |
| start | `boot` | nie (kontekst można zbudować ręcznie) | rdzenia |
| testy | `testkit` | nie (tylko w zależnościach testowych) | rdzenia i `boot` |
| funkcje | `data`, `web`, `security` | nie | rdzenia, nigdy od `boot` ani od innej funkcji |
| integracje | np. `security-web` | nie | rdzenia i dokładnie tych funkcji, które łączy |
| narzędzia | `slf4s-plugin` | nie (tylko przy `@Slf4s`) | niczego z frameworka w runtime |
| przykłady | `demo` | nie, niepublikowane | czegokolwiek |

## Zasady

1. **Rdzeń i `boot` nie znają modułów funkcji.** Nie zależą od nich w `build.sbt` i nie odwołują się do ich klas, nawet przez nazwę w stringu.
2. **Moduły funkcji nie zależą od siebie nawzajem.** Połączenie dwóch funkcji (np. security dla endpointów HTTP) idzie do osobnego modułu integracyjnego, który zależy od obu.
3. **Moduł funkcji podłącza się przez SPI.** Udostępnia implementację traitu `CoilixModule` z `context` i rejestruje ją w `META-INF/services/dev.coilix.context.CoilixModule`. `boot` wczytuje moduły z classpath przez `java.util.ServiceLoader` i dodaje ich definicje beanów do kontekstu.
4. **Moduł można wyłączyć w configu** (`coilix.modules.<nazwa>.enabled = false`) mimo obecności na classpath.
5. **Ciężkie zależności zewnętrzne tylko w modułach funkcji.** Rdzeń zależy wyłącznie od biblioteki Scali, `slf4j-api`, bibliotek configu i skanera classpath. Serwer HTTP, JDBC i kryptografia nie mogą trafić do rdzenia.
6. **Adnotacje.** Adnotacje rdzenia są w `annotations`. Adnotacje konkretnej funkcji (np. mapowania HTTP) trafiają do osobnego modułu Java tej funkcji (np. `web-annotations`), żeby `annotations` nie rosło o rzeczy niepotrzebne aplikacji bez tej funkcji.
7. **Każdy moduł funkcji ma test, że działa bez innych funkcji na classpath**, a `boot` ma test, że startuje bez żadnego modułu funkcji.

## Stan obecny

```
common ← config ← context ← boot ← testkit ← demo (testy)
                              boot ← demo
annotations ↗        ↖ data
                     ↖ web
                     ↖ security
slf4s-plugin (plugin kompilatora dla modułów z @Slf4s)
```

- `boot` zależy tylko od `context`, `concurrency` i `events` (moduły rdzenia, które podłączają się przez `CoilixModule` jak moduły funkcji, ADR 0020 i 0025), a `data`, `web` i `security` tylko od `context` (COILIX-00010). Pilnuje tego `checkModuleLayers` w buildzie.
- Numery katalogów idą zakresami (ADR 0018): biblioteki rdzenia `01`–`14`, `15_boot`, `16_testkit`, funkcje i integracje od `20`, niepublikowane aplikacje `90`–`98` i `99_docs`. Nowy moduł nie wymaga przenumerowania istniejących. `security-web` dostanie kolejny wolny numer od `20` razem z pierwszym kodem, który go potrzebuje.
- `data`, `web` i `security` są jeszcze puste.
- `config` zawiera trzy formaty (HOCON, properties, YAML). YAML ciągnie `snakeyaml-engine` do rdzenia. Opcjonalnie: wydzielić `config-yaml` jako moduł funkcji. Niski priorytet, bo biblioteka jest mała.

## SPI

```scala
trait CoilixModule {
  def name: String

  def components: List[Class[?]]

  def collects: List[Class[? <: Annotation]] = Nil
}

trait ModuleLoader {
  def load(): List[CoilixModule]
}
```

- Moduł podaje klasy komponentów, a nie gotowe definicje. `DefaultCoilixBootstrap` tworzy z nich definicje tym samym `BeanDefinitionFactory` co dla klas ze skanowania, więc działają adnotacje komponentów, `@Bean` w klasach `@Configuration` i metody cyklu życia.
- `ServiceLoaderModuleLoader` (w `context`) wczytuje moduły przez `ServiceLoader` z classloadera kontekstowego wątku, sortuje je po nazwie i pomija te z `coilix.modules.<nazwa>.enabled = false`. Każdy wczytany i pominięty moduł trafia do logu.
- `ModuleLoader` ma `given ConfigPath` (`coilix.context.module-loader`) i `default`, więc można go podmienić, np. w testach.
- Moduł może zbierać klasy z własną adnotacją zamiast robić z nich beany (`collects`, ADR 0017). Dostaje je przez bean `CollectedComponents` i sam decyduje, kiedy je tworzy. Adnotację trzyma moduł adnotacji tej funkcji (np. `processing-annotations`), więc rdzeń jej nie zna.
- Definicje z modułów idą razem z definicjami ze skanowania, bez usuwania duplikatów. Konflikt (ten sam typ z modułu i z aplikacji) rozstrzygają istniejące reguły `@Primary` i `CandidateSelector`, a nie kolejność.

## Weryfikacja w buildzie

Zadanie `checkModuleLayers` (`project/ModuleLayers.scala`) sprawdza drzewo `dependsOn`: rdzeń i `boot` nie mogą przechodnio zależeć od `data`, `web` ani `security`, a moduły funkcji od siebie nawzajem. Kompilacja roota zależy od tego zadania, więc przypadkowa zależność przerywa `sbt testFull` z listą naruszeń.
