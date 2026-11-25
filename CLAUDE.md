# Coilix

Framework IoC dla Scali 3 z API wzorowanym na Springu. Build: sbt 2, testy: munit. Licencja: Apache-2.0 (`docs/legal/`).

Artefakty mają `groupId` `dev.coilix`, pakiety `dev.coilix.*`, a klucze configu prefiks `coilix.*`. Repozytorium: [Coilix/coilix](https://github.com/Coilix/coilix). Identyfikatory tasków mają prefiks `COILIX-`.

## Moduły

Każdy moduł ma prefiks numeryczny, żeby w drzewie IntelliJ moduły bazowe były na górze.
Numer odpowiada poziomowi zależności: moduł może zależeć tylko od modułów o niższym numerze. Zakresy (ADR 0018):

- `01`–`14`: biblioteki rdzenia (grupa `core`), nowy moduł rdzenia dostaje kolejny wolny numer w tym zakresie,
- `15_boot` i `16_testkit`, a `17`–`19` dla kolejnych modułów rdzenia nad `boot`,
- `20`–`89`: moduły funkcji i integracji, numerowane w kolejności powstawania; integracja ma numer wyższy niż funkcje, które łączy,
- `90`–`98`: niepublikowane aplikacje (`90_demo`, później benchmarki),
- `99_docs`: przykłady i strona dokumentacji, zależne od wszystkich publikowanych modułów.

Root projektu nie ma źródeł, tylko agreguje moduły.

| Katalog | Projekt sbt | Pakiet | Język | Zawartość |
|---|---|---|---|---|
| `01_common` | `common` | `dev.coilix.common` | Scala | wspólne typy i rozszerzenia |
| `02_annotations` | `annotations` | `dev.coilix.annotation` | Java | adnotacje runtime |
| `03_slf4s_plugin` | `slf4s-plugin` | `dev.coilix.slf4s` | Scala | plugin kompilatora `@Slf4s` |
| `04_config` | `config` | `dev.coilix.config` | Scala | ładowanie konfiguracji i nadpisywanie komponentów |
| `05_context` | `context` | `dev.coilix.context` | Scala | kontener IoC |
| `06_concurrency` | `concurrency` | `dev.coilix.concurrency` | Scala | wykonawca zadań i harmonogram jako beany (ADR 0020) |
| `07_cluster` | `cluster` | `dev.coilix.cluster` | Scala | tożsamość repliki, później członkostwo i API między replikami (ADR 0021) |
| `08_events` | `events` | `dev.coilix.events` | Scala | zdarzenia aplikacji w pamięci z odbiorcami synchronicznymi i asynchronicznymi (ADR 0025) |
| `09_diagnostics` | `diagnostics` | `dev.coilix.diagnostics` | Scala | zdrowie aplikacji (`Health`, wskaźniki) i kontrole wątków z wykrywaniem zakleszczeń (ADR 0039, 0040) |
| `15_boot` | `boot` | `dev.coilix.boot` | Scala | start aplikacji |
| `16_testkit` | `testkit` | `dev.coilix.testkit` | Scala | kontekst Coilix w testach munit (ADR 0016) |
| `20_data` | `data` | `dev.coilix.data` | Scala | bazy danych (planowane) |
| `21_web` | `web` | `dev.coilix.web` | Scala | endpointy (planowane) |
| `22_security` | `security` | `dev.coilix.security` | Scala | security (planowane) |
| `23_cats_effect` | `cats-effect` | `dev.coilix.catseffect` | Scala | `IORuntime` jako bean i jedyna pula obliczeniowa (ADR 0031), beany i start w `IO` |
| `24_testkit_cats_effect` | `testkit-cats-effect` | `dev.coilix.testkit.catseffect` | Scala | testy w `IO` z munit-cats-effect (ADR 0035) |
| `25_pekko` | `pekko` | `dev.coilix.pekko` | Scala | `ActorSystem` i `Materializer` jako beany na pulach Coilix (ADR 0036) |
| `26_processing_annotations` | `processing-annotations` | `dev.coilix.processing.annotation` | Java | adnotacja `@Processor` (ADR 0037) |
| `27_processing` | `processing` | `dev.coilix.processing` | Scala | procesory tła z shardami, nadzorem i `ProcessorManager` (ADR 0037) |
| `90_demo` | `demo` | `dev.coilix.demo` | Scala | aplikacja demo |
| `99_docs` | `docs` | `com.example` | Scala, Docusaurus | przykłady do przewodników, wspólna dokumentacja API i strona `docs.coilix.dev` |

Plugin IntelliJ dla frameworka (`@Slf4s` w edytorze) jest w osobnym repozytorium [Coilix/coilix-intellij](https://github.com/Coilix/coilix-intellij) z buildem Gradle (ADR 0014).

`99_docs` nie jest publikowany. Kod Scali leży w `examples/next/scala` (a nie w `src/main/scala`) i jest w pakiecie `com.example`, bo pokazuje kod aplikacji użytkownika. Strona Docusaurus jest w `99_docs/website/` i podlega zasadom z `docs/process/documentation.md`.

Moduły `data`, `web`, `security`, `cats-effect`, `pekko` i `processing` są opcjonalne (zasady w `docs/architecture/modularity.md`):

- `common`, `annotations`, `config`, `context`, `concurrency`, `cluster`, `events`, `diagnostics`, `boot` i `testkit` nie zależą od modułów funkcji (`data`, `web`, `security`, `cats-effect`, `pekko`, `processing`) ani nie odwołują się do ich klas.
- Moduły funkcji nie zależą od siebie nawzajem. Połączenie dwóch funkcji trafia do osobnego modułu integracyjnego.
- Moduł funkcji podłącza się przez `CoilixModule` i `META-INF/services/dev.coilix.context.CoilixModule`, a nie przez zależność `boot` od niego. Aplikacja wyłącza moduł kluczem `coilix.modules.<nazwa>.enabled = false`.
- Build sprawdza te zasady zadaniem `checkModuleLayers` (`project/ModuleLayers.scala`), uruchamianym przy kompilacji roota, więc także przy `sbt testFull`.
- Ciężkie biblioteki zewnętrzne (serwer HTTP, JDBC, kryptografia) tylko w modułach funkcji.

Nowy moduł dodaje się w `build.sbt` przez `scalaModule`, `slf4sModule` (z loggerem `@Slf4s`) albo `javaModule`. Artefakt dostaje nazwę `coilix-<projekt sbt>`, a każdy moduł potrzebuje `description` (trafia do POM-a). Moduł, którego nie publikujemy (`demo` i puste moduły funkcji do czasu pierwszej implementacji), dostaje `.settings(unpublished)`, czyli `publish / skip := true` bez bazy MiMa.
Moduły używają `idePackagePrefix`, więc pliki leżą bezpośrednio w `src/main/scala`, bez katalogów pakietu.
Plik w podpakiecie, np. fixture testu w katalogu `wiring`, ma pakiet modułu w osobnej klauzuli: `package dev.coilix.context`, a w następnej linii `package wiring`. Tego wymaga IntelliJ przy `idePackagePrefix`, a typy z pakietu modułu są wtedy widoczne bez importu.
Importy są zawsze pełne (`import dev.coilix.context.wiring.Greeter`), po jednym typie w linii i bez importów względnych (`import wiring.Greeter`). `*` tylko przy importach metod rozszerzających i dużych API, np. `scala.jdk.CollectionConverters.*` albo `dotty.tools.dotc.ast.untpd.*`. Tak formatuje je scalafmt (`rewrite.imports.expand`), a styl kodu IntelliJ w `.idea/codeStyles/` jest ustawiony tak samo, więc „Optimize Imports” ich nie zmienia.

## Narzędzia

Moduły narzędziowe w `tools/` nie są częścią frameworka: nie mają numeru poziomu, nie są publikowane (`unpublished`) i nie podlegają `checkModuleLayers` (ADR 0038).

| Katalog | Projekt sbt | Pakiet | Zawartość |
|---|---|---|---|
| `tools/warts` | `warts` | `dev.coilix.warts` | własne warty wartremovera dla zasad kodu (`ConsoleOutput`, `ClassParameterInApi`), podpięte do każdego modułu Scali |

- Zasada z tego pliku, którą da się sprawdzić automatycznie, dostaje wart albo regułę (COILIX-00028).
- Nowy wart dopisuje się do `coilixWarts` w `build.sbt` i dostaje testy munit z `WartTestTraverser`: przypadek zgłaszany, niezgłaszany i wyłączony przez `@SuppressWarnings`.
- `WartTestTraverser` to makro, które uruchamia wart przy kompilacji testu, a zinc nie kompiluje testów ponownie po zmianie samego ciała wartu. Po zmianie wartu testy uruchamia się przez `sbt warts/clean warts/testFull`.

## Zasady kodu

- Składnia tylko z klamrami `{}` i `if (...)`. Zakaz składni wcięć, `:` po nagłówku, `then`, `do` i `end` (wymusza `-no-indent -old-syntax`).
- Zakaz `var`, `null`, `return`, `while` i mutowalnych kolekcji (wymusza wartremover). Zamiast tego rekurencja, `fold` i niemutowalne struktury. Mutowalność tylko tam, gdzie inaczej się nie da (np. API kompilatora).
- Zakaz wypisywania na konsolę: `println`, `print`, `printf`, `Console.*`, `System.out` i `System.err` (wymusza wart `ConsoleOutput`). Logowanie przez `@Slf4s` i pole `log`.
- Zakaz `asInstanceOf` (wymusza `Wart.AsInstanceOf`). Rzutowanie, bez którego się nie da (typ wymazany w kodzie refleksyjnym), idzie do małej metody z `@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))`, a jej Scaladoc mówi, dlaczego jest bezpieczne.
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
- API ma być zwięzłe i wygodne w użyciu: tam, gdzie to upraszcza kod wywołujący, używa się `given`, `using`, funkcji kontekstowych (`A ?=> B`), metod rozszerzających i metod generycznych wołanych samym typem, np. `setup.use { bean[Reception].welcome("Ann") }` zamiast przekazywania kontekstu w każdym wywołaniu. Obok zostaje wersja jawna (parametr zamiast `using`), z której korzysta wersja zwięzła. Wzorem jest testkit (`CoilixSuite`, `CoilixContextOps`).
- Typy w API przez `[T: ClassTag]`, a nie jawne parametry `Class[T]` (wymusza wart `ClassParameterInApi` w `src/main`). Klasę runtime pobiera się przez `dev.coilix.common.ClassTags` (`runtimeTypeOf[T]`, `tag.runtimeType`), bez `asInstanceOf` w kodzie wywołującym.
- Java i Scala w osobnych modułach. Java tylko tam, gdzie jest wymagana (adnotacje runtime).
- Każdy nowy plik (kod, testy, zasoby, dokumenty) od razu dodaje się do gita przez `git add`. Przeniesienia robi się przez `git mv`. Commit tylko na wyraźną prośbę.
- Formatowanie robi scalafmt (konfiguracja w `.scalafmt.conf`) i google-java-format w stylu AOSP dla Javy (`sbt-java-formatter`), lokalnie przy każdej kompilacji. W CI formatowanie przy kompilacji jest wyłączone (`insideCI`), a krok `Check formatting` przerywa build, gdy kod albo pliki buildu (`*.sbt`, `project/*.scala`) nie są sformatowane. Każdy warning kompilatora Scali i Javy przerywa build (`-Werror`).
- Każda nowa klasa ma testy munit w `src/test/scala` swojego modułu.
- Każda funkcja widoczna dla programisty aplikacji (adnotacja, bean modułu, punkt rozszerzeń) jest użyta w aplikacji demo (`90_demo`) w naturalnym miejscu jej domeny i sprawdzona testem demo, w tym samym PR co funkcja.
- Zależności zawsze w najnowszej stabilnej wersji. Wersje są w `project/Dependencies.scala`. Wyjątek: Scala publikowanych modułów jest w najnowszej wersji linii LTS (COILIX-00009, `docs/architecture/versioning.md`).
- Nowa zależność musi mieć licencję zgodną z Apache-2.0 (lista w `docs/legal/dependency-licenses.md`). GPL i AGPL są zakazane. Sprawdza to `sbt checkLicenses`.
- Nie kopiuje się kodu, dokumentacji, komunikatów ani testów ze Springa i innych frameworków, także w parafrazie. Nazwy adnotacji i pojęć mogą odpowiadać Springowi, teksty piszemy od nowa.
- Z prywatnych bibliotek opiekuna (bulldozer i `org.optimus:implicits`) wolno przenosić koncepcje, algorytmy i podział na komponenty, ale nie kod 1:1. Przeniesiony kod dostaje formę Coilix: własne nazwy typów, metod, parametrów i kluczy configu, formatowanie i zasady z tego pliku (klamry, bez `var` i `null`, jeden typ na plik, Scaladoc po angielsku) i styl bezpośredni rdzenia zamiast cats-effect tam, gdzie oryginał używa `IO`. Testy pisze się od nowa, a ADR albo task wskazuje, z czego wzorowano rozwiązanie.
- Słowa „Spring” nie używa się w nazwach typów, pakietów, modułów, artefaktów, kluczy configu ani tytułów dokumentacji. W opisach tylko opisowo („API modeled on Spring”), bez sugerowania powiązania z Broadcom (`docs/legal/spring-similarity.md`).

## Dokumentacja

Opis działania frameworka jest w `docs/architecture/overview.md` (start, budowa kontekstu, tabela punktów rozszerzeń), a decyzje architektoniczne w `docs/adr/`. Nowy komponent z `given ConfigPath` dopisuje się do tabeli w `overview.md`, a decyzję dotyczącą publicznego API, modułów, zależności albo procesu wydań zapisuje się jako nowy ADR w tym samym PR.

Dokumentację API generuje Scaladoc (moduły Scali) i Javadoc (`02_annotations`) z komentarzy `/** */` w kodzie. Przewodniki po angielsku i po polsku, wersje, changelog i publikacja na `docs.coilix.dev` podlegają zasadom z `docs/process/documentation.md` (ADR 0010). Zmiana widoczna dla użytkownika aktualizuje przewodnik w obu językach w tym samym PR.

- Scaladoc i Javadoc pisze się po angielsku.
- Komentarz `/** */` ma każdy publiczny element w `src/main` modułów `01`–`90`: trait, klasa, case class, obiekt, enum, metoda, `val`, `given` i typ wyjątku. Bez komentarza zostają elementy `private` i `private[...]` oraz testy.
- Komentarz opisuje kontrakt (co robi, co zwraca, kiedy rzuca wyjątek), a nie implementację. Pierwsze zdanie to samodzielne podsumowanie, bo trafia na listę członków typu.
- Parametry opisuje się przez `@param`, `@tparam`, `@return` i `@throws` tylko wtedy, gdy nazwa i typ nie mówią wszystkiego. Nie powtarza się sygnatury słowami.
- Do innych typów linkuje się przez `[[dev.coilix.context.BeanRegistry]]` z pełną nazwą, nie przez sam tekst w backtickach.
- Przykład użycia w bloku kodu ` ```scala ` dodaje się do API, którego używa programista aplikacji: adnotacje, `ApplicationContext`, `CoilixApplication`, `ConfigPath` i punkty rozszerzeń podmieniane kluczem `class`.
- Implementacja traitu (`Default*`, `Reflective*` itd.) ma komentarz na poziomie klasy: czym różni się od innych implementacji i jaki klucz configu ją wybiera. Metod nadpisujących nie dokumentuje się ponownie, bo Scaladoc dziedziczy opis z traitu. Komentarz przy nadpisaniu dodaje się tylko, gdy zachowanie zawęża albo rozszerza kontrakt.
- Obiekt towarzyszący traitu komponentu opisuje swój `ConfigPath` (klucz w configu) i domyślną implementację z `default`.
- Przy zmianie zachowania aktualizuje się Scaladoc w tym samym commicie co kod.
- Po zmianach w `src/main` uruchamia się `sbt doc`. Scaladoc zgłasza zepsute linki jako ostrzeżenie `Couldn't resolve a member for the given link query`, które nie przerywa builda, więc trzeba je sprawdzić w logu i poprawić. Javadoc dla `02_annotations` działa z `-Xdoclint:all -Werror`, ale sbt 2 nie przerywa zadania przy błędzie javadoc, więc brak komentarza widać w logu jako `warnings found and -Werror specified`. CI sprawdza oba komunikaty i wtedy przerywa build.

## Wersjonowanie

Standard jest w `docs/architecture/versioning.md`: SemVer, `early-semver` i grupy wydań z osobnymi wersjami (ADR 0011). Rdzeń (`common`, `annotations`, `slf4s-plugin`, `config`, `context`, `concurrency`, `cluster`, `events`, `diagnostics`, `boot`, `testkit`) ma jedną wersję z tagu `core-vX.Y.Z`, każdy moduł funkcji własną z tagu `<moduł>-vX.Y.Z`.

- Nowy moduł rdzenia dostaje w `build.sbt` `.settings(coreGroup)`, a nowy moduł funkcji `.settings(featureGroup("<id>"))` i wpis `<id>` na liście w `ReleaseGroups.buildSettings(...)`.

- Zmiana niezgodna (usunięcie lub zmiana publicznego API, w tym klas `Default*` i kluczy configu `coilix.*`) w fazie `0.x` podnosi `MINOR`, a od `1.0.0` `MAJOR`.
- Przed usunięciem publicznego elementu najpierw `@deprecated("use X instead", since = "<następna wersja>")`.
- Każda zmiana widoczna dla użytkownika dostaje wpis w `changelog/<grupa>/unreleased.md` każdej grupy wydań, której moduły zmienia (`core` albo moduł funkcji), z ID taska i oznaczeniem **BREAKING** z opisem migracji przy zmianie niezgodnej. Format opisuje `changelog/README.md`, a sprawdza `npm run check` w `99_docs/website` (ADR 0011).

## Proces i gałęzie

Pełna procedura jest w `docs/process/development.md`: trunk-based z gałęzią `master`, gałęzie tematyczne, PR, wydania i linie utrzymaniowe.

- Nie commituje się bezpośrednio na `master`. Każdy task idzie na gałęzi `feature/COILIX-NNNNN-opis` (poprawka: `fix/...`) od aktualnego `master` i trafia na `master` przez PR scalany przez „Squash and merge” (ADR 0008). Jeden PR to jedna funkcja albo jedna poprawka.
- `master` zawsze kompiluje się i przechodzi `sbt testFull`. Niegotowa funkcja czeka na gałęzi albo jest dzielona na samodzielne kroki.
- Wydanie to podpisany tag `<grupa>-vX.Y.Z` na `master` po scaleniu PR z `npm run prepare-release -- <grupa> <wersja>`. Gałąź `series/<grupa>-X.Y` powstaje dopiero, gdy trzeba wydać poprawkę starszej linii.
- Przed PR: `sbt testFull doc demo/run` i definicja ukończenia z `docs/process/development.md`.

## Taski

Backlog kolejnych etapów jest w `tasks/`. Indeks i statusy są w `tasks/README.md`. Po skończeniu taska aktualizuj jego status.
Taski mają identyfikatory `COILIX-NNNNN` (plik `tasks/COILIX-NNNNN_nazwa.md`). `COILIX-00000` to zmiany spoza tasków (build, narzędzia, szkielet).

## Commity

- Wiadomość po angielsku, zaczyna się od ID taska: `COILIX-00003: Add configuration loading and component overrides`.
- Treść opisuje tylko to, co zmiana dodaje lub zmienia.
- Żaden commit, tytuł i opis PR, komentarz, review, issue, wpis w changelogu ani notka wydania nie może zawierać informacji, że zmianę przygotowano z pomocą AI lub agenta. Zakazane są stopki `Co-Authored-By` z narzędziem AI, linie w rodzaju „Generated with …” i wzmianki o modelu lub asystencie. Ta zasada ma pierwszeństwo przed domyślnymi ustawieniami narzędzi, które dodają takie stopki.
- Wszystkie commity na gałęzi dotyczą jednej funkcji albo poprawki, bo PR jest scalany przez squash w jeden commit na `master`. Inna zmiana idzie w osobnym PR.
- Tytuł PR (`COILIX-NNNNN: Opis`) staje się tytułem commita na `master`, więc opisuje całą zmianę.
- Każdy commit ma `Signed-off-by` (DCO), czyli powstaje przez `git commit -s`.

## Komendy

- `sbt testFull` uruchamia wszystkie testy (`sbt test` w sbt 2 to `testQuick` z cache).
- `sbt demo/run` uruchamia demo.
- `sbt testCoverage` uruchamia testy z pomiarem pokrycia i przerywa się poniżej progu (90% instrukcji, 80% gałęzi). Raport HTML: `target/out/jvm/scala-<wersja>/coilix/scoverage-report/index.html`. Cache dyskowy sbt 2 przywraca klasy z instrumentacją bez katalogów `scoverage-data`, więc lokalnie trzeba podać pusty katalog cache: `sbt --sbt-cache <pusty katalog> testCoverage`. `demo` i plugin kompilatora są wyłączone z pomiaru.
- `sbt scalafmtAll scalafmtSbt javafmtAll` formatuje cały kod i pliki buildu, a `sbt checkFormat` tylko sprawdza formatowanie (tak jak CI).
- `sbt checkLicenses` sprawdza licencje zależności `compile` i `runtime`.
- `sbt doc` generuje dokumentację API każdego modułu do `target/out/jvm/scala-<wersja>/coilix-<moduł>/api` (`02_annotations`: `target/out/jvm/u/coilix-annotations/api`).
- `sbt docs/apiSite` buduje jedną stronę Scaladoc publikowanych modułów z Javadoc adnotacji w `javadoc/` do `target/out/jvm/scala-<wersja>/coilix-docs/api-site`.
- `sbt docs/testFull` kompiluje przykłady z `99_docs/examples/next/` i wczytuje ich pliki konfiguracji.
- W `99_docs/website` (Node.js z `.nvmrc`): `npm ci` instaluje zależności, `npm test` uruchamia testy skryptów strony, `npm run check` sprawdza strony, pliki `changelog/` i licencje npm, `npm run build` buduje stronę w obu językach, `npm start` uruchamia podgląd (`-- --locale pl` dla polskiego).
- `sbt version` pokazuje wersję każdego modułu wyliczoną z tagów jego grupy (sbt-dynver), a `sbt "show releaseGroup"` grupę modułu. Wersji nie wpisuje się ręcznie.
- `sbt mimaReportBinaryIssues` sprawdza kompatybilność binarną z poprzednim wydaniem tej samej linii.
- `sbt "releaseVerify <grupa> <wersja>"` sprawdza przed wydaniem moduły grupy (wersja z tagu, testy, licencje, MiMa, `checkReleasedCore`). W `99_docs/website` `npm run prepare-release -- <grupa> <wersja>` przygotowuje PR wydania (`docs/process/development.md`).
- Wspólne konfiguracje uruchomieniowe IntelliJ są w `.run/` (`Demo` oraz `sbt testFull`, `sbt checkFormat`, `sbt demo/run`, `sbt doc`). Nową konfigurację dla wszystkich zapisuje się tam przez „Store as project file”.
- Styl kodu IntelliJ jest w `.idea/codeStyles/`: formatowanie Scali (Ctrl+Alt+L) idzie przez scalafmt z `.scalafmt.conf`, a kod generowany przez IDE ma klamry zamiast składni wcięć.
- Gdy działa sbt uruchomione przez IntelliJ, sbt z terminala trzeba odpalać z `-Dsbt.server.autostart=false`.
- W workflowach GitHuba każde wywołanie sbt ma `--server`, a powłoka to `bash` z `pipefail` (`defaults.run.shell`). Runner sbt 2 domyślnie używa cienkiego klienta, który nie przyjmuje kilku komend naraz i poddaje się, gdy serwer startuje dłużej niż 300 s, a bez `pipefail` `sbt ... | tee` przechodzi mimo błędu. Wywołanie sbt w workflowie idzie przez `.github/scripts/sbt-retry.sh <log> <argumenty>`, który powtarza komendę tylko po błędzie pobierania (np. HTTP 429 z Maven Central), a zależności są w cache `sbt-deps-*` z `restore-keys` (ADR 0023). Wyjątkiem jest publikacja (`ci-release`), której nie powtarza się automatycznie.
- IntelliJ buduje własnym kompilatorem (nie przez sbt shell, bo sbt 2 nie kopiuje zasobów do `classes`). Jar pluginu `@Slf4s` tworzy tylko sbt. Jar ma stałą nazwę `coilix-slf4s-plugin.jar` (niezależną od wersji z gita). Gdy IntelliJ zgłasza `No plugin in path ...coilix-slf4s-plugin.jar`, trzeba uruchomić `sbt slf4s-plugin/package`. Jeśli jar istnieje, a błąd zostaje, trzeba zrestartować serwer kompilacji Scali w IntelliJ (trzyma starą wersję jara po przebudowie przez sbt).
