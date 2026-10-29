# Scaling

Framework IoC dla Scali 3 z API wzorowanym na Springu. Build: sbt 2, testy: munit. Licencja: Apache-2.0 (do zatwierdzenia, `docs/legal/`).

Projekt zmienia nazwę na **Mortise** (SCALING-00008). Do czasu tej zmiany nowy kod nadal używa nazw `Scaling`, pakietu `org.scaling` i kluczy `scaling.*`, żeby zmiana objęła wszystko naraz.

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

Moduły `data`, `web` i `security` są opcjonalne (zasady w `docs/architecture/modularity.md`):

- `common`, `annotations`, `config`, `context` i `boot` nie zależą od modułów funkcji (`data`, `web`, `security`) ani nie odwołują się do ich klas.
- Moduły funkcji nie zależą od siebie nawzajem. Połączenie dwóch funkcji trafia do osobnego modułu integracyjnego.
- Moduł funkcji podłącza się przez `ScalingModule` i `META-INF/services` (SCALING-00010), a nie przez zależność `boot` od niego.
- Ciężkie biblioteki zewnętrzne (serwer HTTP, JDBC, kryptografia) tylko w modułach funkcji.

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
- Bez zwykłych komentarzy (`//`, `/* */`). Jedyne dozwolone komentarze to Scaladoc i Javadoc według sekcji „Dokumentacja”.
- Typy w API przez `[T: ClassTag]`, a nie jawne parametry `Class[T]`. Klasę runtime pobiera się przez `org.scaling.common.ClassTags` (`runtimeTypeOf[T]`, `tag.runtimeType`), bez `asInstanceOf` w kodzie wywołującym.
- Java i Scala w osobnych modułach. Java tylko tam, gdzie jest wymagana (adnotacje runtime).
- Każdy nowy plik (kod, testy, zasoby, dokumenty) od razu dodaje się do gita przez `git add`. Przeniesienia robi się przez `git mv`. Commit tylko na wyraźną prośbę.
- Formatowanie robi scalafmt przy każdej kompilacji (`scalafmtOnCompile`, konfiguracja w `.scalafmt.conf`), a każdy warning kompilatora Scali i Javy przerywa build (`-Werror`).
- Każda nowa klasa ma testy munit w `src/test/scala` swojego modułu.
- Zależności zawsze w najnowszej stabilnej wersji. Wersje są w `project/Dependencies.scala`. Wyjątek: Scala publikowanych modułów jest w najnowszej wersji linii LTS (SCALING-00009, `docs/architecture/versioning.md`).
- Nowa zależność musi mieć licencję zgodną z Apache-2.0 (lista w `docs/legal/dependency-licenses.md`). GPL i AGPL są zakazane.
- Nie kopiuje się kodu, dokumentacji, komunikatów ani testów ze Springa i innych frameworków, także w parafrazie. Nazwy adnotacji i pojęć mogą odpowiadać Springowi, teksty piszemy od nowa.
- Słowa „Spring” nie używa się w nazwach typów, pakietów, modułów, artefaktów, kluczy configu ani tytułów dokumentacji. W opisach tylko opisowo („API modeled on Spring”), bez sugerowania powiązania z Broadcom (`docs/legal/spring-similarity.md`).

## Dokumentacja

Dokumentację API generuje Scaladoc (moduły Scali) i Javadoc (`02_annotations`) z komentarzy `/** */` w kodzie. Będzie publikowana na GitHub Pages w wersji dla każdego wydania (plan w `tasks/SCALING-00007_documentation_site.md`).

- Scaladoc i Javadoc pisze się po angielsku.
- Komentarz `/** */` ma każdy publiczny element w `src/main` modułów `01`–`10`: trait, klasa, case class, obiekt, enum, metoda, `val`, `given` i typ wyjątku. Bez komentarza zostają elementy `private` i `private[...]`, testy i moduł `11_intellij_plugin`.
- Komentarz opisuje kontrakt (co robi, co zwraca, kiedy rzuca wyjątek), a nie implementację. Pierwsze zdanie to samodzielne podsumowanie, bo trafia na listę członków typu.
- Parametry opisuje się przez `@param`, `@tparam`, `@return` i `@throws` tylko wtedy, gdy nazwa i typ nie mówią wszystkiego. Nie powtarza się sygnatury słowami.
- Do innych typów linkuje się przez `[[org.scaling.context.BeanRegistry]]` z pełną nazwą, nie przez sam tekst w backtickach.
- Przykład użycia w bloku kodu ` ```scala ` dodaje się do API, którego używa programista aplikacji: adnotacje, `ApplicationContext`, `ScalingApplication`, `ConfigPath` i punkty rozszerzeń podmieniane kluczem `class`.
- Implementacja traitu (`Default*`, `Reflective*` itd.) ma komentarz na poziomie klasy: czym różni się od innych implementacji i jaki klucz configu ją wybiera. Metod nadpisujących nie dokumentuje się ponownie, bo Scaladoc dziedziczy opis z traitu. Komentarz przy nadpisaniu dodaje się tylko, gdy zachowanie zawęża albo rozszerza kontrakt.
- Obiekt towarzyszący traitu komponentu opisuje swój `ConfigPath` (klucz w configu) i domyślną implementację z `default`.
- Przy zmianie zachowania aktualizuje się Scaladoc w tym samym commicie co kod.
- Po zmianach w `src/main` uruchamia się `sbt doc`. Scaladoc zgłasza zepsute linki jako ostrzeżenie `Couldn't resolve a member for the given link query`, które nie przerywa builda, więc trzeba je sprawdzić w logu i poprawić.

## Wersjonowanie

Standard jest w `docs/architecture/versioning.md`: SemVer, jedna wersja dla wszystkich modułów, `early-semver`, wersja z tagu `vX.Y.Z`.

- Zmiana niezgodna (usunięcie lub zmiana publicznego API, w tym klas `Default*` i kluczy configu `scaling.*`) w fazie `0.x` podnosi `MINOR`, a od `1.0.0` `MAJOR`.
- Przed usunięciem publicznego elementu najpierw `@deprecated("use X instead", since = "<następna wersja>")`.
- Każda zmiana widoczna dla użytkownika dostaje wpis w `CHANGELOG.md` w sekcji `Unreleased` (po SCALING-00009), z ID taska i oznaczeniem **BREAKING** przy zmianie niezgodnej.

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
- `sbt doc` generuje dokumentację API każdego modułu do `target/out/jvm/<scala>/<moduł>/api` (`02_annotations`: `target/out/jvm/u/annotations/api`).
- `sbt intellij-plugin/installIntellijPlugin` instaluje plugin IDE (potem restart IntelliJ).
- Gdy działa sbt uruchomione przez IntelliJ, sbt z terminala trzeba odpalać z `-Dsbt.server.autostart=false`.
- IntelliJ buduje własnym kompilatorem (nie przez sbt shell, bo sbt 2 nie kopiuje zasobów do `classes`). Jar pluginu `@Slf4s` tworzy tylko sbt. Gdy IntelliJ zgłasza `No plugin in path ...slf4s-plugin_3-0.1.0-SNAPSHOT.jar`, trzeba uruchomić `sbt slf4s-plugin/package`. Jeśli jar istnieje, a błąd zostaje, trzeba zrestartować serwer kompilacji Scali w IntelliJ (trzyma starą wersję jara po przebudowie przez sbt).
