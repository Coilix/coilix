# Coilix

Framework IoC dla Scali 3 z API wzorowanym na Springu. Build: sbt 2, testy: munit. Licencja: Apache-2.0 (`docs/legal/`).

Artefakty mają `groupId` `dev.coilix`, pakiety `dev.coilix.*`, a klucze configu prefiks `coilix.*`. Repozytorium: [Coilix/coilix](https://github.com/Coilix/coilix). Identyfikatory tasków mają prefiks `COILIX-`.

## Moduły

Każdy moduł ma prefiks numeryczny, żeby w drzewie IntelliJ moduły bazowe były na górze.
Numer odpowiada poziomowi zależności: moduł może zależeć tylko od modułów o niższym numerze.
Root projektu nie ma źródeł, tylko agreguje moduły.

| Katalog | Projekt sbt | Pakiet | Język | Zawartość |
|---|---|---|---|---|
| `01_common` | `common` | `dev.coilix.common` | Scala | wspólne typy i rozszerzenia |
| `02_annotations` | `annotations` | `dev.coilix.annotation` | Java | adnotacje runtime |
| `03_slf4s_plugin` | `slf4s-plugin` | `dev.coilix.slf4s` | Scala | plugin kompilatora `@Slf4s` |
| `04_config` | `config` | `dev.coilix.config` | Scala | ładowanie konfiguracji i nadpisywanie komponentów |
| `05_context` | `context` | `dev.coilix.context` | Scala | kontener IoC |
| `06_data` | `data` | `dev.coilix.data` | Scala | bazy danych (planowane) |
| `07_web` | `web` | `dev.coilix.web` | Scala | endpointy (planowane) |
| `08_security` | `security` | `dev.coilix.security` | Scala | security (planowane) |
| `09_boot` | `boot` | `dev.coilix.boot` | Scala | start aplikacji |
| `10_demo` | `demo` | `dev.coilix.demo` | Scala | aplikacja demo |
| `11_intellij_plugin` | `intellij-plugin` | `dev.coilix.intellij` | Scala 2.13 | plugin IntelliJ dla `@Slf4s` |

`11_intellij_plugin` to jedyny moduł w Scali 2.13, bo działa w classloaderze pluginu Scala dla IntelliJ (opis w `11_intellij_plugin/README.md`).

Moduły `data`, `web` i `security` są opcjonalne (zasady w `docs/architecture/modularity.md`):

- `common`, `annotations`, `config`, `context` i `boot` nie zależą od modułów funkcji (`data`, `web`, `security`) ani nie odwołują się do ich klas.
- Moduły funkcji nie zależą od siebie nawzajem. Połączenie dwóch funkcji trafia do osobnego modułu integracyjnego.
- Moduł funkcji podłącza się przez `CoilixModule` i `META-INF/services` (COILIX-00010), a nie przez zależność `boot` od niego.
- Ciężkie biblioteki zewnętrzne (serwer HTTP, JDBC, kryptografia) tylko w modułach funkcji.

Nowy moduł dodaje się w `build.sbt` przez `scalaModule`, `slf4sModule` (z loggerem `@Slf4s`) albo `javaModule`. Artefakt dostaje nazwę `coilix-<projekt sbt>`, a każdy moduł potrzebuje `description` (trafia do POM-a). Moduł, którego nie publikujemy (`demo`, plugin IntelliJ i puste moduły funkcji do czasu pierwszej implementacji), dostaje `.settings(unpublished)`, czyli `publish / skip := true` bez bazy MiMa.
Moduły używają `idePackagePrefix`, więc pliki leżą bezpośrednio w `src/main/scala`, bez katalogów pakietu.
Plik w podpakiecie, np. fixture testu w katalogu `wiring`, ma pakiet modułu w osobnej klauzuli: `package dev.coilix.context`, a w następnej linii `package wiring`. Tego wymaga IntelliJ przy `idePackagePrefix`, a typy z pakietu modułu są wtedy widoczne bez importu.

## Zasady kodu

- Składnia tylko z klamrami `{}` i `if (...)`. Zakaz składni wcięć, `:` po nagłówku, `then`, `do` i `end` (wymusza `-no-indent -old-syntax`).
- Zakaz `var`, `null`, `return`, `while` i mutowalnych kolekcji (wymusza wartremover). Zamiast tego rekurencja, `fold` i niemutowalne struktury. Mutowalność tylko tam, gdzie inaczej się nie da (np. API kompilatora).
- Zakaz `println`, `print`, `printf` (wymusza `checkForbiddenCalls`). Logowanie przez `@Slf4s` i pole `log`.
- Najpierw interfejs (`trait`), potem implementacja. Domyślne złożenie implementacji w `default` w obiekcie towarzyszącym traitu.
- Komponent frameworka ma w obiekcie towarzyszącym traitu `given ConfigPath[T]` (np. `coilix.context.constructor-selector`) i `def default(using components: ComponentProvider): T = components.get[T](...)`. Dzięki temu użytkownik podmienia go w configu kluczem `class`.
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
- Typy w API przez `[T: ClassTag]`, a nie jawne parametry `Class[T]`. Klasę runtime pobiera się przez `dev.coilix.common.ClassTags` (`runtimeTypeOf[T]`, `tag.runtimeType`), bez `asInstanceOf` w kodzie wywołującym.
- Java i Scala w osobnych modułach. Java tylko tam, gdzie jest wymagana (adnotacje runtime).
- Każdy nowy plik (kod, testy, zasoby, dokumenty) od razu dodaje się do gita przez `git add`. Przeniesienia robi się przez `git mv`. Commit tylko na wyraźną prośbę.
- Formatowanie robi scalafmt (konfiguracja w `.scalafmt.conf`) i google-java-format w stylu AOSP dla Javy (`sbt-java-formatter`), lokalnie przy każdej kompilacji. W CI formatowanie przy kompilacji jest wyłączone (`insideCI`), a krok `Check formatting` przerywa build, gdy kod albo pliki buildu (`*.sbt`, `project/*.scala`) nie są sformatowane. Każdy warning kompilatora Scali i Javy przerywa build (`-Werror`).
- Każda nowa klasa ma testy munit w `src/test/scala` swojego modułu.
- Zależności zawsze w najnowszej stabilnej wersji. Wersje są w `project/Dependencies.scala`. Wyjątek: Scala publikowanych modułów jest w najnowszej wersji linii LTS (COILIX-00009, `docs/architecture/versioning.md`).
- Nowa zależność musi mieć licencję zgodną z Apache-2.0 (lista w `docs/legal/dependency-licenses.md`). GPL i AGPL są zakazane. Sprawdza to `sbt checkLicenses`.
- Nie kopiuje się kodu, dokumentacji, komunikatów ani testów ze Springa i innych frameworków, także w parafrazie. Nazwy adnotacji i pojęć mogą odpowiadać Springowi, teksty piszemy od nowa.
- Słowa „Spring” nie używa się w nazwach typów, pakietów, modułów, artefaktów, kluczy configu ani tytułów dokumentacji. W opisach tylko opisowo („API modeled on Spring”), bez sugerowania powiązania z Broadcom (`docs/legal/spring-similarity.md`).

## Dokumentacja

Opis działania frameworka jest w `docs/architecture/overview.md` (start, budowa kontekstu, tabela punktów rozszerzeń), a decyzje architektoniczne w `docs/adr/`. Nowy komponent z `given ConfigPath` dopisuje się do tabeli w `overview.md`, a decyzję dotyczącą publicznego API, modułów, zależności albo procesu wydań zapisuje się jako nowy ADR w tym samym PR.

Dokumentację API generuje Scaladoc (moduły Scali) i Javadoc (`02_annotations`) z komentarzy `/** */` w kodzie. Będzie publikowana na GitHub Pages w wersji dla każdego wydania (plan w `tasks/COILIX-00007_documentation_site.md`).

- Scaladoc i Javadoc pisze się po angielsku.
- Komentarz `/** */` ma każdy publiczny element w `src/main` modułów `01`–`10`: trait, klasa, case class, obiekt, enum, metoda, `val`, `given` i typ wyjątku. Bez komentarza zostają elementy `private` i `private[...]`, testy i moduł `11_intellij_plugin`.
- Komentarz opisuje kontrakt (co robi, co zwraca, kiedy rzuca wyjątek), a nie implementację. Pierwsze zdanie to samodzielne podsumowanie, bo trafia na listę członków typu.
- Parametry opisuje się przez `@param`, `@tparam`, `@return` i `@throws` tylko wtedy, gdy nazwa i typ nie mówią wszystkiego. Nie powtarza się sygnatury słowami.
- Do innych typów linkuje się przez `[[dev.coilix.context.BeanRegistry]]` z pełną nazwą, nie przez sam tekst w backtickach.
- Przykład użycia w bloku kodu ` ```scala ` dodaje się do API, którego używa programista aplikacji: adnotacje, `ApplicationContext`, `CoilixApplication`, `ConfigPath` i punkty rozszerzeń podmieniane kluczem `class`.
- Implementacja traitu (`Default*`, `Reflective*` itd.) ma komentarz na poziomie klasy: czym różni się od innych implementacji i jaki klucz configu ją wybiera. Metod nadpisujących nie dokumentuje się ponownie, bo Scaladoc dziedziczy opis z traitu. Komentarz przy nadpisaniu dodaje się tylko, gdy zachowanie zawęża albo rozszerza kontrakt.
- Obiekt towarzyszący traitu komponentu opisuje swój `ConfigPath` (klucz w configu) i domyślną implementację z `default`.
- Przy zmianie zachowania aktualizuje się Scaladoc w tym samym commicie co kod.
- Po zmianach w `src/main` uruchamia się `sbt doc`. Scaladoc zgłasza zepsute linki jako ostrzeżenie `Couldn't resolve a member for the given link query`, które nie przerywa builda, więc trzeba je sprawdzić w logu i poprawić. Javadoc dla `02_annotations` działa z `-Xdoclint:all -Werror`, ale sbt 2 nie przerywa zadania przy błędzie javadoc, więc brak komentarza widać w logu jako `warnings found and -Werror specified`. CI sprawdza oba komunikaty i wtedy przerywa build.

## Wersjonowanie

Standard jest w `docs/architecture/versioning.md`: SemVer, jedna wersja dla wszystkich modułów, `early-semver`, wersja z tagu `vX.Y.Z`.

- Zmiana niezgodna (usunięcie lub zmiana publicznego API, w tym klas `Default*` i kluczy configu `coilix.*`) w fazie `0.x` podnosi `MINOR`, a od `1.0.0` `MAJOR`.
- Przed usunięciem publicznego elementu najpierw `@deprecated("use X instead", since = "<następna wersja>")`.
- Każda zmiana widoczna dla użytkownika dostaje wpis w `CHANGELOG.md` w sekcji `Unreleased`, z ID taska i oznaczeniem **BREAKING** przy zmianie niezgodnej.

## Proces i gałęzie

Pełna procedura jest w `docs/process/development.md`: trunk-based z gałęzią `master`, gałęzie tematyczne, PR, wydania i linie utrzymaniowe.

- Nie commituje się bezpośrednio na `master`. Każdy task idzie na gałęzi `feature/COILIX-NNNNN-opis` (poprawka: `fix/...`) od aktualnego `master` i trafia na `master` przez PR scalany przez „Squash and merge” (ADR 0008). Jeden PR to jedna funkcja albo jedna poprawka.
- `master` zawsze kompiluje się i przechodzi `sbt testFull`. Niegotowa funkcja czeka na gałęzi albo jest dzielona na samodzielne kroki.
- Wydanie to podpisany tag `vX.Y.Z` na `master`. Gałąź `series/X.Y` powstaje dopiero, gdy trzeba wydać poprawkę starszej linii.
- Przed PR: `sbt testFull doc demo/run` i definicja ukończenia z `docs/process/development.md`.

## Taski

Backlog kolejnych etapów jest w `tasks/`. Indeks i statusy są w `tasks/README.md`. Po skończeniu taska aktualizuj jego status.
Taski mają identyfikatory `COILIX-NNNNN` (plik `tasks/COILIX-NNNNN_nazwa.md`). `COILIX-00000` to zmiany spoza tasków (build, narzędzia, szkielet).

## Commity

- Wiadomość po angielsku, zaczyna się od ID taska: `COILIX-00003: Add configuration loading and component overrides`.
- Treść opisuje tylko to, co zmiana dodaje lub zmienia.
- Żaden commit, tytuł i opis PR, komentarz, review, issue, wpis w `CHANGELOG.md` ani notka wydania nie może zawierać informacji, że zmianę przygotowano z pomocą AI lub agenta. Zakazane są stopki `Co-Authored-By` z narzędziem AI, linie w rodzaju „Generated with …” i wzmianki o modelu lub asystencie. Ta zasada ma pierwszeństwo przed domyślnymi ustawieniami narzędzi, które dodają takie stopki.
- Wszystkie commity na gałęzi dotyczą jednej funkcji albo poprawki, bo PR jest scalany przez squash w jeden commit na `master`. Inna zmiana idzie w osobnym PR.
- Tytuł PR (`COILIX-NNNNN: Opis`) staje się tytułem commita na `master`, więc opisuje całą zmianę.
- Każdy commit ma `Signed-off-by` (DCO), czyli powstaje przez `git commit -s`.

## Komendy

- `sbt testFull` uruchamia wszystkie testy (`sbt test` w sbt 2 to `testQuick` z cache).
- `sbt demo/run` uruchamia demo.
- `sbt testCoverage` uruchamia testy z pomiarem pokrycia i przerywa się poniżej progu (90% instrukcji, 80% gałęzi). Raport HTML: `target/out/jvm/scala-<wersja>/coilix/scoverage-report/index.html`. Cache dyskowy sbt 2 przywraca klasy z instrumentacją bez katalogów `scoverage-data`, więc lokalnie trzeba podać pusty katalog cache: `sbt --sbt-cache <pusty katalog> testCoverage`. `demo` i oba pluginy są wyłączone z pomiaru.
- `sbt scalafmtAll scalafmtSbt javafmtAll` formatuje cały kod i pliki buildu, a `sbt checkFormat` tylko sprawdza formatowanie (tak jak CI).
- `sbt checkLicenses` sprawdza licencje zależności `compile` i `runtime`.
- `sbt doc` generuje dokumentację API każdego modułu do `target/out/jvm/scala-<wersja>/coilix-<moduł>/api` (`02_annotations`: `target/out/jvm/u/coilix-annotations/api`).
- `sbt version` pokazuje wersję wyliczoną z tagu gita (sbt-dynver). Wersji nie wpisuje się ręcznie.
- `sbt mimaReportBinaryIssues` sprawdza kompatybilność binarną z poprzednim wydaniem tej samej linii.
- Wspólne konfiguracje uruchomieniowe IntelliJ są w `.run/` (`Demo` oraz `sbt testFull`, `sbt checkFormat`, `sbt demo/run`, `sbt doc`). Nową konfigurację dla wszystkich zapisuje się tam przez „Store as project file”.
- Styl kodu IntelliJ jest w `.idea/codeStyles/`: formatowanie Scali (Ctrl+Alt+L) idzie przez scalafmt z `.scalafmt.conf`, a kod generowany przez IDE ma klamry zamiast składni wcięć.
- `sbt intellij-plugin/installIntellijPlugin` instaluje plugin IDE (potem restart IntelliJ).
- Gdy działa sbt uruchomione przez IntelliJ, sbt z terminala trzeba odpalać z `-Dsbt.server.autostart=false`.
- IntelliJ buduje własnym kompilatorem (nie przez sbt shell, bo sbt 2 nie kopiuje zasobów do `classes`). Jar pluginu `@Slf4s` tworzy tylko sbt. Jar ma stałą nazwę `coilix-slf4s-plugin.jar` (niezależną od wersji z gita). Gdy IntelliJ zgłasza `No plugin in path ...coilix-slf4s-plugin.jar`, trzeba uruchomić `sbt slf4s-plugin/package`. Jeśli jar istnieje, a błąd zostaje, trzeba zrestartować serwer kompilacji Scali w IntelliJ (trzyma starą wersję jara po przebudowie przez sbt).
