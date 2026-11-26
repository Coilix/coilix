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
| `01_common` | `common` | `dev.coilix.common` | Scala | wspólne typy i rozszerzenia, `IOLogger` dla `logF` (ADR 0046) |
| `02_annotations` | `annotations` | `dev.coilix.annotation` | Java | adnotacje runtime |
| `03_slf4s_plugin` | `slf4s-plugin` | `dev.coilix.slf4s` | Scala | plugin kompilatora `@Slf4s` (pola `log` i `logF`) |
| `04_config` | `config` | `dev.coilix.config` | Scala | ładowanie konfiguracji i nadpisywanie komponentów |
| `05_context` | `context` | `dev.coilix.context` | Scala | kontener IoC |
| `06_concurrency` | `concurrency` | `dev.coilix.concurrency` | Scala | `IORuntime` cats-effect jako jedyna pula obliczeniowa (ADR 0031, 0042), fs2 (ADR 0043), wykonawca blokujący, harmonogram i beany w `IO` (ADR 0020, 0032) |
| `07_cluster` | `cluster` | `dev.coilix.cluster` | Scala | tożsamość repliki, później członkostwo i API między replikami (ADR 0021) |
| `08_events` | `events` | `dev.coilix.events` | Scala | zdarzenia aplikacji w pamięci z odbiorcami synchronicznymi i asynchronicznymi (ADR 0025) |
| `09_diagnostics` | `diagnostics` | `dev.coilix.diagnostics` | Scala | zdrowie aplikacji (`Health`, wskaźniki) i kontrole wątków z wykrywaniem zakleszczeń (ADR 0039, 0040) |
| `15_boot` | `boot` | `dev.coilix.boot` | Scala | start aplikacji, także jako `CoilixIOApp` z runnerami w `IO` (ADR 0033, 0034) |
| `16_testkit` | `testkit` | `dev.coilix.testkit` | Scala | kontekst Coilix w testach munit, także w `IO` z munit-cats-effect (ADR 0016, 0035) |
| `20_data` | `data` | `dev.coilix.data` | Scala | bazy danych (planowane) |
| `21_web` | `web` | `dev.coilix.web` | Scala | endpointy (planowane) |
| `22_security` | `security` | `dev.coilix.security` | Scala | security (planowane) |
| `26_processing_annotations` | `processing-annotations` | `dev.coilix.processing.annotation` | Java | adnotacja `@Processor` (ADR 0037) |
| `27_processing` | `processing` | `dev.coilix.processing` | Scala | procesory tła z shardami, nadzorem i `ProcessorManager` na `IO` (ADR 0037, 0044) |
| `28_logging` | `logging` | `dev.coilix.logging` | Scala | backend SLF4J 2: poziomy per pakiet z `coilix.logging`, `LogLevels` w runtime, format i mosty JUL, Log4j 2, JCL (ADR 0046) |
| `90_demo` | `demo` | `dev.coilix.demo` | Scala | aplikacja demo |
| `99_docs` | `docs` | `com.example` | Scala, Docusaurus | przykłady do przewodników, wspólna dokumentacja API i strona `docs.coilix.dev` |

Plugin IntelliJ dla frameworka (`@Slf4s` w edytorze) jest w osobnym repozytorium [Coilix/coilix-intellij](https://github.com/Coilix/coilix-intellij) z buildem Gradle (ADR 0014).

`99_docs` nie jest publikowany. Kod Scali leży w `examples/next/scala` (a nie w `src/main/scala`) i jest w pakiecie `com.example`, bo pokazuje kod aplikacji użytkownika. Strona Docusaurus jest w `99_docs/website/` i podlega zasadom z `docs/process/documentation.md`.

Moduły `data`, `web`, `security`, `processing` i `logging` są opcjonalne: rdzeń (`01`–`16`) nie zależy od nich ani od ich klas, moduły funkcji nie zależą od siebie nawzajem (połączenie idzie do modułu integracyjnego), a moduł funkcji podłącza się przez `CoilixModule` w `META-INF/services` i wyłącza kluczem `coilix.modules.<nazwa>.enabled = false`. Ciężkie biblioteki zewnętrzne (serwer HTTP, JDBC, kryptografia) tylko w modułach funkcji. Pilnuje tego `checkModuleLayers` przy `sbt testFull`, szczegóły w `docs/architecture/modularity.md`.

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
- Kontrakty, które runtime woła w kodzie aplikacji po starcie (procesory, odbiorcy zdarzeń, `@Scheduled`, `@Async`, wskaźniki zdrowia, przyszłe web i data), zwracają `cats.effect.IO`, a nie `Unit` ani `Future` (ADR 0042, migracja w COILIX-00031). Komponenty aplikacji i API Coilix używają konkretnych `IO`, `Resource[IO, _]` i `fs2.Stream[IO, _]`, bez tagless final i bez własnego typu efektu (ADR 0043). Runtime pisze się na `Ref`, `Deferred`, `Queue`, `Supervisor` i `Resource`, bez `synchronized`, `wait`/`notify`, `Atomic*` i `Thread.sleep`. Synchroniczny zostaje tylko kontener (skanowanie, plan, tworzenie beanów).
- Zakaz wypisywania na konsolę: `println`, `print`, `printf`, `Console.*`, `System.out` i `System.err` (wymusza wart `ConsoleOutput`). Logowanie przez `@Slf4s`: pole `log` w zwykłym kodzie i `logF` (`IOLogger`, metody zwracają `IO[Unit]`) w kodzie w `IO`, bez opakowywania `log` w `IO(...)` (ADR 0046).
- Zakaz `asInstanceOf` (wymusza `Wart.AsInstanceOf`). Rzutowanie, bez którego się nie da (typ wymazany w kodzie refleksyjnym), idzie do małej metody z `@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))`, a jej Scaladoc mówi, dlaczego jest bezpieczne.
- Najpierw interfejs (`trait`), potem implementacja. Domyślne złożenie implementacji w `default` w obiekcie towarzyszącym traitu.
- Komponent frameworka ma w obiekcie towarzyszącym traitu `given ConfigPath[T]` (np. `coilix.context.constructor-selector`) i `def default(using components: ComponentProvider): T = components.get[T](...)`. Dzięki temu użytkownik podmienia go w configu kluczem `class`.
- Klasa, która nie musi być rozszerzana i nie wynika z wymagań Javy, jest `case class` (bez `final`), także serwis, komponent i implementacja traitu, a nie tylko dane. Publiczne `val` są parametrami konstruktora, a nie polami liczonymi w środku. Gdy klasa przyjmowała jeden obiekt i wyliczała z niego kilka pól, konstruktor główny przyjmuje te pola wprost, a `apply` w obiekcie towarzyszącym przyjmuje ten jeden obiekt i rozkłada go na konstruktor szczegółowy:

  ```scala
  case class SmtpMailer(host: String, port: Int, sender: String) extends Mailer

  object SmtpMailer {
    def apply(settings: MailSettings): SmtpMailer = SmtpMailer(settings.host, settings.port, settings.sender)
  }
  ```

  Wyjątki: klasy, które coś dziedziczą z Javy albo muszą być otwarte (punkt rozszerzeń dla aplikacji) i wyjątki (`extends CoilixException`). Stan serwisu (np. `Ref`) też jest parametrem konstruktora, tworzonym w `apply` albo w metodzie `@Bean`. Kontener wybiera konstruktor główny, więc bean case class dostaje zależności jak każdy inny.
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
- Test kodu w `IO` jest testem w `IO`: `CatsEffectSuite with CoilixIOSuite` z `coilixTest`, którego ciało zwraca `IO`, bez `unsafeRunSync` i innych `unsafeRun*`.
- Każda funkcja widoczna dla programisty aplikacji (adnotacja, bean modułu, punkt rozszerzeń) jest użyta w aplikacji demo (`90_demo`) w naturalnym miejscu jej domeny i sprawdzona testem demo, w tym samym PR co funkcja.
- Zależności zawsze w najnowszej stabilnej wersji. Wersje są w `project/Dependencies.scala`. Wyjątek: Scala publikowanych modułów jest w najnowszej wersji linii LTS (COILIX-00009, `docs/architecture/versioning.md`).
- Nowa zależność musi mieć licencję zgodną z Apache-2.0 (lista w `docs/legal/dependency-licenses.md`). GPL i AGPL są zakazane. Sprawdza to `sbt checkLicenses`.
- Nie kopiuje się kodu, dokumentacji, komunikatów ani testów ze Springa i innych frameworków, także w parafrazie. Nazwy adnotacji i pojęć mogą odpowiadać Springowi, teksty piszemy od nowa.
- Z prywatnych bibliotek opiekuna (bulldozer i `org.optimus:implicits`) wolno przenosić koncepcje, algorytmy i podział na komponenty, ale nie kod 1:1. Przeniesiony kod dostaje formę Coilix: własne nazwy typów, metod, parametrów i kluczy configu, formatowanie i zasady z tego pliku (klamry, bez `var` i `null`, jeden typ na plik, Scaladoc po angielsku) i model efektów Coilix (ADR 0042, 0043): framework i komponenty na `IO`, `Resource` i fs2, kontener synchroniczny. Testy pisze się od nowa, a ADR albo task wskazuje, z czego wzorowano rozwiązanie.
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

Standard jest w `docs/architecture/versioning.md` (SemVer, `early-semver`, grupy wydań z tagami `core-vX.Y.Z` i `<moduł>-vX.Y.Z`, ADR 0011).

- Nowy moduł rdzenia dostaje w `build.sbt` `.settings(coreGroup)`, a nowy moduł funkcji `.settings(featureGroup("<id>"))` i wpis `<id>` w `ReleaseGroups.buildSettings(...)`.
- Zmiana niezgodna (publiczne API, w tym klasy `Default*` i klucze `coilix.*`) w fazie `0.x` podnosi `MINOR`. Przed usunięciem publicznego elementu najpierw `@deprecated("use X instead", since = "<następna wersja>")`.
- Każda zmiana widoczna dla użytkownika dostaje wpis w `changelog/<grupa>/unreleased.md` każdej zmienionej grupy wydań, z ID taska i **BREAKING** z opisem migracji przy zmianie niezgodnej (format: `changelog/README.md`).

## Proces i gałęzie

Pełna procedura jest w `docs/process/development.md`: trunk-based z gałęzią `master`, gałęzie tematyczne, PR, wydania i linie utrzymaniowe.

- Nie commituje się bezpośrednio na `master`. Każdy task idzie na gałęzi `feature/COILIX-NNNNN-opis` (poprawka: `fix/...`) od aktualnego `master` i trafia na `master` przez PR scalany przez „Squash and merge” (ADR 0008). Jeden PR to jedna funkcja albo jedna poprawka.
- `master` zawsze kompiluje się i przechodzi `sbt testFull`. Niegotowa funkcja czeka na gałęzi albo jest dzielona na samodzielne kroki.
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
- `sbt scalafmtAll scalafmtSbt javafmtAll` formatuje cały kod i pliki buildu, a `sbt checkFormat` tylko sprawdza formatowanie (tak jak CI).
- `sbt checkLicenses` sprawdza licencje zależności `compile` i `runtime`.
- `sbt doc` generuje dokumentację API (zepsute linki: sekcja „Dokumentacja”).
- Gdy działa sbt uruchomione przez IntelliJ, sbt z terminala trzeba odpalać z `-Dsbt.server.autostart=false`.
- Pokrycie (`testCoverage`), strona dokumentacji (`docs/apiSite`, `npm` w `99_docs/website`), wersje i wydania (`releaseVerify`, MiMa), konfiguracja IntelliJ (`.run/`, `No plugin in path ...coilix-slf4s-plugin.jar`) i sbt w workflowach GitHuba (`--server`, `sbt-retry.sh`) są w `docs/process/tooling.md`. Czytaj go, gdy task tego dotyczy.

## Praca agenta

- Kierunek modelu efektów wyznaczają ADR 0042 i 0043: `IO`, `Resource` i fs2, bez tagless final i bez Pekko. Plan, notatka albo wznowiona sesja, które mówią inaczej, są nieaktualne.
- Przy „co dalej?” najpierw sprawdź stan: `git status --short --branch`, otwarte PR-y (`gh pr list --state open`) i status taska w `tasks/README.md`. Dopiero potem proponuj następny krok.
- Konflikty, rebase i kolejność zależnych PR-ów obsługuje skill `pr-conflicts` (`.claude/skills/pr-conflicts/SKILL.md`). Uruchamiaj go po scaleniu PR-a, od którego zależy inny, i zanim wypchniesz gałąź zbudowaną na innej gałęzi.
- Walidację raportuj z nazwą komendy i wynikiem (przeszło / co padło). Długie wyjście sbt filtruj (`tail`, `grep -E "error|warn|Failed|Passed"`), zamiast wczytywać całe.
- W jednej procedurze używaj jednej powłoki (Bash z ukośnikami w ścieżkach). Błąd środowiska (ścieżka, powłoka, zablokowany port sbt) sprawdź, zanim powtórzysz komendę.
