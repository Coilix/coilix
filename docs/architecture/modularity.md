# Niezależność modułów

## Cel

Użytkownik dodaje tylko te moduły, których potrzebuje. Aplikacja konsolowa używa `boot` i nie ma na classpath serwera HTTP, sterowników baz danych ani security. Dodanie `web` do zależności włącza endpointy bez zmiany kodu startowego.

## Warstwy

| Warstwa | Moduły | Wymagane | Może zależeć od |
|---|---|---|---|
| rdzeń | `common`, `annotations`, `config`, `context` | tak | tylko rdzenia |
| start | `boot` | nie (kontekst można zbudować ręcznie) | rdzenia |
| funkcje | `data`, `web`, `security` | nie | rdzenia, nigdy od `boot` ani od innej funkcji |
| integracje | np. `security-web` | nie | rdzenia i dokładnie tych funkcji, które łączy |
| narzędzia | `slf4s-plugin`, `intellij-plugin` | nie (tylko przy `@Slf4s`) | niczego z frameworka w runtime |
| przykłady | `demo` | nie, niepublikowane | czegokolwiek |

## Zasady

1. **Rdzeń i `boot` nie znają modułów funkcji.** Nie zależą od nich w `build.sbt` i nie odwołują się do ich klas, nawet przez nazwę w stringu.
2. **Moduły funkcji nie zależą od siebie nawzajem.** Połączenie dwóch funkcji (np. security dla endpointów HTTP) idzie do osobnego modułu integracyjnego, który zależy od obu.
3. **Moduł funkcji podłącza się przez SPI.** Udostępnia implementację traitu `CoilixModule` z `context` i rejestruje ją w `META-INF/services/io.github.coilix.context.CoilixModule`. `boot` wczytuje moduły z classpath przez `java.util.ServiceLoader` i dodaje ich definicje beanów do kontekstu.
4. **Moduł można wyłączyć w configu** (`coilix.modules.<nazwa>.enabled = false`) mimo obecności na classpath.
5. **Ciężkie zależności zewnętrzne tylko w modułach funkcji.** Rdzeń zależy wyłącznie od biblioteki Scali, `slf4j-api`, bibliotek configu i skanera classpath. Serwer HTTP, JDBC i kryptografia nie mogą trafić do rdzenia.
6. **Adnotacje.** Adnotacje rdzenia są w `annotations`. Adnotacje konkretnej funkcji (np. mapowania HTTP) trafiają do osobnego modułu Java tej funkcji (np. `web-annotations`), żeby `annotations` nie rosło o rzeczy niepotrzebne aplikacji bez tej funkcji.
7. **Każdy moduł funkcji ma test, że działa bez innych funkcji na classpath**, a `boot` ma test, że startuje bez żadnego modułu funkcji.

## Stan obecny

```
common ← config ← context ← boot ← demo
annotations ↗        ↖ data
                     ↖ web ← security
slf4s-plugin (plugin kompilatora dla modułów z @Slf4s)
```

- `boot` zależy tylko od `context`, więc zasada 1 jest spełniona już teraz.
- `security` zależy od `web`, co łamie zasadę 2. Security ma sens także bez HTTP (np. autoryzacja metod), więc trzeba je podzielić na `security` (rdzeń, bez `web`) i `security-web` (integracja).
- `data`, `web` i `security` są puste, więc podział nic nie psuje.
- Brak mechanizmu SPI: `boot` dodaje tylko definicje ze skanowania pakietu aplikacji (`DefaultCoilixBootstrap.start` przyjmuje już `extraDefinitions`, więc jest gdzie je podpiąć).
- `config` zawiera trzy formaty (HOCON, properties, YAML). YAML ciągnie `snakeyaml-engine` do rdzenia. Opcjonalnie: wydzielić `config-yaml` jako moduł funkcji. Niski priorytet, bo biblioteka jest mała.

## Projekt SPI

```scala
trait CoilixModule {
  def name: String

  def definitions(using components: ComponentProvider): List[BeanDefinition]
}

trait ModuleLoader {
  def load(using components: ComponentProvider): List[CoilixModule]
}
```

- `ServiceLoaderModuleLoader` (w `context`) wczytuje moduły przez `ServiceLoader` i pomija wyłączone w configu.
- `ModuleLoader` ma `given ConfigPath` (`coilix.context.module-loader`) i `default`, więc można go podmienić, np. w testach.
- `DefaultCoilixBootstrap` łączy definicje z modułów z definicjami ze skanowania. Konflikt (ten sam typ z modułu i z aplikacji) rozstrzygają istniejące reguły `@Primary` i `CandidateSelector`, a nie kolejność.

## Weryfikacja w buildzie

Do `build.sbt` trafia sprawdzenie, że w zależnościach rdzenia i `boot` nie ma modułów funkcji (`dependsOn` w drzewie projektów). Sprawdzenie działa przy `sbt testFull`, żeby przypadkowe dodanie zależności przerwało build.
