# Scaling

Framework IoC w stylu Springa napisany w Scali 3. Build: sbt 2, testy: munit.

## Moduły

Każdy moduł ma prefiks numeryczny, żeby w drzewie IntelliJ moduły bazowe były na górze.
Numer odpowiada poziomowi zależności: moduł może zależeć tylko od modułów o niższym numerze.
Root projektu nie ma źródeł, tylko agreguje moduły.

| Katalog | Projekt sbt | Pakiet | Język | Zawartość |
|---|---|---|---|---|
| `01_common` | `common` | `org.scaling.common` | Scala | wspólne typy i rozszerzenia |
| `02_annotations` | `annotations` | `org.scaling.annotation` | Java | adnotacje runtime |
| `03_slf4s_plugin` | `slf4s-plugin` | `org.scaling.slf4s` | Scala | plugin kompilatora `@Slf4s` |
| `04_config` | `config` | `org.scaling.config` | Scala | ładowanie konfiguracji i nadpisywanie komponentów |
| `05_context` | `context` | `org.scaling.context` | Scala | kontener IoC |
| `06_data` | `data` | `org.scaling.data` | Scala | bazy danych (planowane) |
| `07_web` | `web` | `org.scaling.web` | Scala | endpointy (planowane) |
| `08_security` | `security` | `org.scaling.security` | Scala | security (planowane) |
| `09_boot` | `boot` | `org.scaling.boot` | Scala | start aplikacji |
| `10_demo` | `demo` | `org.scaling.demo` | Scala | aplikacja demo |
| `11_intellij_plugin` | `intellij-plugin` | `org.scaling.intellij` | Scala 2.13 | plugin IntelliJ dla `@Slf4s` |

`11_intellij_plugin` to jedyny moduł w Scali 2.13, bo działa w classloaderze pluginu Scala dla IntelliJ (opis w `11_intellij_plugin/README.md`).

Nowy moduł dodaje się w `build.sbt` przez `scalaModule`, `slf4sModule` (z loggerem `@Slf4s`) albo `javaModule`.
Moduły używają `idePackagePrefix`, więc pliki leżą bezpośrednio w `src/main/scala`, bez katalogów pakietu.

## Zasady kodu

- Składnia tylko z klamrami `{}` i `if (...)`. Zakaz składni wcięć, `:` po nagłówku, `then`, `do` i `end` (wymusza `-no-indent -old-syntax`).
- Zakaz `var`, `null`, `return`, `while` i mutowalnych kolekcji (wymusza wartremover). Zamiast tego rekurencja, `fold` i niemutowalne struktury. Mutowalność tylko tam, gdzie inaczej się nie da (np. API kompilatora).
- Zakaz `println`, `print`, `printf` (wymusza `checkForbiddenCalls`). Logowanie przez `@Slf4s` i pole `log`.
- Najpierw interfejs (`trait`), potem implementacja. Domyślne złożenie implementacji w `default` w obiekcie towarzyszącym traitu.
- Komponent frameworka ma w obiekcie towarzyszącym traitu `given ConfigPath[T]` (np. `scaling.context.constructor-selector`) i `def default(using components: ComponentProvider): T = components.get[T](...)`. Dzięki temu użytkownik podmienia go w configu kluczem `class`.
- Jeden plik to jeden typ o tej samej nazwie co plik. Dozwolony jest tylko obiekt towarzyszący o tej samej nazwie.
- Metody w traitach, klasach i obiektach oddziela jedna pusta linia, także same sygnatury w traicie:

  ```scala
  trait BeanRegistry {
    def beans: List[RegisteredBean]

    def register(bean: RegisteredBean): BeanRegistry
  }
  ```

- Małe metody z jedną odpowiedzialnością. Gałąź `case`, `if` albo `for`/`yield` dłuższa niż jedna linia idzie do osobnej metody, nie do bloku.
- Delegowanie zamiast rozbudowanych metod. Kod rozdrobniony, ale czysty.
- Bez komentarzy i bez Scaladoc.
- Typy w API przez `[T: ClassTag]`, a nie jawne parametry `Class[T]`. Klasę runtime pobiera się przez `org.scaling.common.ClassTags` (`runtimeTypeOf[T]`, `tag.runtimeType`), bez `asInstanceOf` w kodzie wywołującym.
- Java i Scala w osobnych modułach. Java tylko tam, gdzie jest wymagana (adnotacje runtime).
- Każdy nowy plik (kod, testy, zasoby, dokumenty) od razu dodaje się do gita przez `git add`. Przeniesienia robi się przez `git mv`. Commit tylko na wyraźną prośbę.
- Formatowanie robi scalafmt przy każdej kompilacji (`scalafmtOnCompile`, konfiguracja w `.scalafmt.conf`), a każdy warning kompilatora Scali i Javy przerywa build (`-Werror`).
- Każda nowa klasa ma testy munit w `src/test/scala` swojego modułu.
- Zależności zawsze w najnowszej stabilnej wersji. Wersje są w `project/Dependencies.scala`.

## Taski

Backlog kolejnych etapów jest w `tasks/`. Indeks i statusy są w `tasks/README.md`. Po skończeniu taska aktualizuj jego status.
Taski mają identyfikatory `SCALING-NNNNN` (plik `tasks/SCALING-NNNNN_nazwa.md`). `SCALING-00000` to zmiany spoza tasków (build, narzędzia, szkielet).

## Commity

- Wiadomość po angielsku, zaczyna się od ID taska: `SCALING-00003: Add configuration loading and component overrides`.
- Treść opisuje tylko to, co zmiana dodaje lub zmienia. Bez wzmianek o narzędziach czy agentach, które ją przygotowały, i bez stopek `Co-Authored-By`.
- Jeden commit to jedna logiczna zmiana. Pliki grupuje się według tego, co wprowadzają.

## Komendy

- `sbt testFull` uruchamia wszystkie testy (`sbt test` w sbt 2 to `testQuick` z cache).
- `sbt demo/run` uruchamia demo.
- `sbt intellij-plugin/installIntellijPlugin` instaluje plugin IDE (potem restart IntelliJ).
- Gdy działa sbt uruchomione przez IntelliJ, sbt z terminala trzeba odpalać z `-Dsbt.server.autostart=false`.
- IntelliJ buduje własnym kompilatorem (nie przez sbt shell, bo sbt 2 nie kopiuje zasobów do `classes`). Jar pluginu `@Slf4s` tworzy tylko sbt. Gdy IntelliJ zgłasza `No plugin in path ...slf4s-plugin_3-0.1.0-SNAPSHOT.jar`, trzeba uruchomić `sbt slf4s-plugin/package`. Jeśli jar istnieje, a błąd zostaje, trzeba zrestartować serwer kompilacji Scali w IntelliJ (trzyma starą wersję jara po przebudowie przez sbt).
