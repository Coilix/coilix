# COILIX-00028: Statyczne kontrole zasad kodu

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-19: kontrole semantyczne jako własne warty wartremovera (Scala 3, drzewo z typami), składniowe z automatyczną poprawką jako własne reguły scalafix, a później publikowane reguły migracyjne dla użytkowników Coilix.

## Cel

Zasady z `CLAUDE.md`, których dziś pilnuje tylko review, mają być sprawdzane przez build i CI. Kod, który je łamie, ma się nie zbudować albo nie przejść CI, a tam, gdzie się da, `sbt` ma go poprawić sam. Przy zmianach niezgodnych w `0.x` Coilix ma dawać użytkownikom reguły, które same przepiszą ich kod.

## Stan obecny

Pilnowane przez build:

- wartremover: `Var`, `Null`, `Return`, `While`, `MutableDataStructures`, `OptionPartial`, `TryPartial`, `ScalaApp` (`forbiddenWarts` w `build.sbt`),
- składnia z klamrami: `-no-indent -old-syntax`,
- formatowanie i rozwinięte importy: scalafmt, `checkFormat` w CI,
- warstwy modułów: `checkModuleLayers` (`project/ModuleLayers.scala`),
- wypisywanie na konsolę: `checkForbiddenCalls` (`project/ForbiddenCalls.scala`), czyli regex `\b(println|print|printf)\s*\(` po liniach źródeł:
  - zgłasza fałszywe alarmy dla tekstu w stringach i w Scaladocu,
  - nie łapie `Console.out.println`, `System.out.println`, `System.err` ani wywołań przez alias.

Pilnowane tylko przez review:

- zakaz zwykłych komentarzy (`//`, `/* */`); dziś w kodzie jest ich 0, więc reguła tylko utrwala stan,
- jeden typ w pliku o nazwie pliku, oprócz obiektu towarzyszącego,
- zakaz słowa „Spring” w nazwach typów, pakietów i kluczy configu,
- importy pełne, bez względnych (scalafmt rozwija `{A, B}`, ale nie zamienia `import wiring.Greeter` na pełną ścieżkę),
- `[T: ClassTag]` zamiast parametru `Class[T]` w publicznym API; w kodzie jest 129 wystąpień `Class[...]`, większość w kodzie refleksyjnym i SPI (`List[Class[?]]` w `CoilixModule`), gdzie są uzasadnione,
- `asInstanceOf` tylko w `ClassTags`; dziś są 3 wystąpienia w `src/main`: `ClassTags`, `ReflectiveEnumResolver` i `ValueTypeDefinition`,
- komponent frameworka ma w obiekcie towarzyszącym `given ConfigPath[T]` i `default`,
- komentarz `/** */` na każdym publicznym elemencie w `src/main` publikowanych modułów (`01`–`89`); `sbt doc` zgłasza zepsute linki, ale nie brak komentarza,
- gałąź `case`/`if` dłuższa niż linia idzie do osobnej metody.

Narzędzia (sprawdzone 2026-11-19 na Maven Central):

- `sbt-scalafix` 0.14.10 jest wydany dla sbt 2 (`sbt-scalafix_sbt2_3`), a `scalafix-cli` dla każdej wersji Scali 3.
- Własne reguły scalafix pisze się w Scali 2.13, bo `scalafix-core` jest tylko dla 2.12 i 2.13. Analizowany kod może być w Scali 3, a `scalafix-testkit` istnieje dla Scali 3.
- Reguły semantyczne scalafix czytają SemanticDB. Scala 3 zapisuje w nim mniej niż Scala 2 (m.in. bez części niejawnych argumentów i konwersji).
- Wartremover 3 pozwala pisać własne warty dla Scali 3 na drzewie z pełnymi typami (TASTy reflection) w zwykłym module Scali 3.

## Zakres

### 1. Podział narzędzi

| Zasada | Narzędzie | Poprawka automatyczna | Uzasadnienie |
|---|---|---|---|
| konsola (`println`, `print`, `printf`, `Console.*`, `System.out`, `System.err`) | wart `ConsoleOutput` | nie | po symbolu, bez fałszywych alarmów, zastępuje `ForbiddenCalls` |
| `asInstanceOf` | `Wart.AsInstanceOf` z wyjątkami | nie | gotowy wart, wyjątki przez `@SuppressWarnings` z uzasadnieniem |
| `Class[T]` w publicznym API | wart `ClassParameterInApi` | nie | potrzebne typy, widoczność i parametry typu |
| `given ConfigPath` i `default` komponentu | wart `ComponentCompanion` | nie | potrzebne typy obiektu towarzyszącego |
| brak Scaladoca na publicznym elemencie | wart `MissingScaladoc` | nie | drzewo kompilatora ma komentarze i widoczność |
| zwykłe komentarze | reguła scalafix `NoPlainComments` | nie (usuwanie komentarza bez decyzji człowieka jest ryzykowne) | składniowa |
| jeden typ w pliku | reguła scalafix `OneTypePerFile` | nie | składniowa, potrzebna nazwa pliku |
| „Spring” w nazwach | reguła scalafix `NoSpringNames` | nie | składniowa, obejmuje też literały kluczy `coilix.*` |
| importy względne | reguła scalafix `FullyQualifiedImports` | tak | semantyczna (SemanticDB daje pełną nazwę symbolu), poprawka ma sens |
| długie gałęzie `case`/`if` | reguła scalafix `ExtractLongBranches`, najpierw tylko raport | nie | do decyzji po raporcie (pytanie 2) |

Warty dają błąd kompilacji, więc działają też w IntelliJ przy budowaniu przez sbt. Reguły scalafix działają przez `sbt scalafixAll` (poprawki) i `sbt "scalafixAll --check"` (CI).

### 2. Moduły

| Moduł | Język | Publikowany | Zawartość |
|---|---|---|---|
| `warts` | Scala 3 | nie | własne warty i ich testy |
| `scalafix-rules` | Scala 2.13 | nie | reguły projektu z punktu 1 |
| `scalafix-tests` | Scala 3 (wejście) i 2.13 (testkit) | nie | pary plików wejście/oczekiwane wyjście dla reguł |
| `coilix-scalafix` | Scala 2.13 | tak, później | reguły migracyjne dla użytkowników (punkt 5) |

- Moduły narzędziowe nie są częścią frameworka. Dostają `.settings(unpublished)`, nie mają numeru poziomu, a `checkModuleLayers` ich nie sprawdza.
- Proponowana nazwa katalogów: `tools/warts`, `tools/scalafix-rules`, `tools/scalafix-tests`. Dzięki temu w drzewie IntelliJ nie mieszają się z modułami `01`–`99`, a CLAUDE.md dostaje sekcję „Narzędzia”.
- `warts` jest zależnością `wartremoverClasspaths` publikowanych modułów (`01`–`89`) i `99_docs`. Warty kompilują się przed modułami, które sprawdzają.
- `scalafix-rules` trafia do `scalafixDependencies` przez `dependsOn` z konfiguracją `ScalafixConfig`, bez publikowania lokalnego.
- Wersja Scali 2.13 to wyjątek od zasady „tylko Scala 3”, tak jak kiedyś plugin IntelliJ. Opisuje go ADR.

### 3. Własne warty

1. `ConsoleOutput`:
   - zgłasza wywołania `scala.Predef.println`, `print` i `printf`, dowolne metody `scala.Console`, `System.out` i `System.err`,
   - zastępuje `checkForbiddenCalls` i `project/ForbiddenCalls.scala`, które usuwamy w tym samym PR,
   - komunikat: `Console output is forbidden, use @Slf4s logging (log.info, log.warn, ...)`.
2. `Wart.AsInstanceOf` włączony w `forbiddenWarts`. Trzy obecne wystąpienia albo przechodzą przez `ClassTags`, albo dostają `@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))` z uzasadnieniem w Scaladocu. Wybór należy do kroku implementacji.
3. `ClassParameterInApi`:
   - zgłasza publiczną albo chronioną metodę, konstruktor lub metodę rozszerzającą z parametrem typu `T` i parametrem wartości typu `Class[T]`, `Class[? <: T]` albo `java.lang.Class[T]`,
   - nie zgłasza `Class[?]` bez związku z parametrem typu (refleksja, SPI), elementów `private`/`private[...]`, kodu testów ani nadpisań metod z bibliotek Javy,
   - komunikat podpowiada `[T: ClassTag]` i `dev.coilix.common.ClassTags`.
4. `ComponentCompanion`:
   - trait, który ma implementację `Default*` w tym samym module i jest wymieniony w tabeli punktów rozszerzeń, musi mieć obiekt towarzyszący z `given ConfigPath[Trait]` i `def default(using ComponentProvider): Trait`,
   - na początek rozpoznanie przez znacznik, np. adnotację `@ExtensionPoint` na traicie w `02_annotations`, zamiast zgadywania po nazwach; decyzja w pytaniu 3,
   - przy okazji `checkExtensionPoints` sprawdza, że każdy taki trait jest w tabeli w `docs/architecture/overview.md`.
5. `MissingScaladoc`:
   - publiczny element w `src/main` publikowanych modułów (`01`–`89`) bez komentarza `/** */` to błąd,
   - pomija nadpisania (Scaladoc dziedziczy opis), elementy `private` i `private[...]`, syntetyczne elementy case class i enumów,
   - jeśli drzewo z TASTy nie daje komentarzy w fazie wartremovera, fallback to osobne zadanie sbt na parserze scalameta (pytanie 4).

Każdy wart ma testy munit na fragmentach kodu kompilowanych w teście: przypadek zgłaszany, niezgłaszany i wyjątek.

### 4. Reguły scalafix projektu

1. `NoPlainComments`: zgłasza `//` i `/* */` (poza `/** */`) w źródłach i testach. Wyjątek: dyrektywy narzędzi (`// scalafmt: off`, `// format: off`), jeśli kiedyś będą potrzebne, przez jawną listę w `.scalafix.conf`.
2. `OneTypePerFile`: zgłasza plik z więcej niż jednym typem najwyższego poziomu (`class`, `trait`, `object`, `enum`, `type`, `given` z nazwą), poza parą typ i obiekt towarzyszący. Zgłasza też typ, którego nazwa nie odpowiada nazwie pliku. Wyjątki: `package object` i pliki fixture'ów testów z listy w configu reguły.
3. `NoSpringNames`: zgłasza „Spring” (bez rozróżniania wielkości liter) w nazwach typów, pakietów i literałach zaczynających się od `coilix.`. Scaladoc i przewodniki są poza regułą, bo dozwolony jest opis „API modeled on Spring”.
4. `FullyQualifiedImports`: zamienia import względny (`import wiring.Greeter`) na pełny (`import dev.coilix.context.wiring.Greeter`) na podstawie SemanticDB. Wymaga `-Xsemanticdb` w modułach, który włączamy tylko na czas zadania scalafix, żeby nie spowalniać zwykłej kompilacji (pytanie 5).
5. `ExtractLongBranches`: na początek tylko raport (`--check` nie przerywa CI) gałęzi `case`, `if`/`else` i `for`/`yield` dłuższych niż jedna linia. Po raporcie decyzja: reguła blokująca, ostrzeżenie albo rezygnacja.

Konfiguracja w `.scalafix.conf` w korzeniu repo. CI dostaje krok `Check scalafix rules` z `sbt "scalafixAll --check"` obok `Check formatting`, a ruleset CI dopisuje go do wymaganych checków (zmiana w `.github/rulesets/` i ręczne wgranie, akcja ręczna).

### 5. Reguły migracyjne dla użytkowników (później)

- Publikowany artefakt `dev.coilix:coilix-scalafix` z regułami dla zmian **BREAKING**. Nazwa reguły zawiera wersję docelową, np. `Coilix_0_3`.
- Użytkownik uruchamia `scalafix dependency:Coilix_0_3@dev.coilix:coilix-scalafix:<wersja>`.
- Wpis **BREAKING** w `changelog/<grupa>/unreleased.md` wskazuje regułę, jeśli istnieje. `changelog/README.md` i `docs/process/development.md` dostają ten krok.
- Pierwsza reguła powstaje przy pierwszej zmianie niezgodnej po `core-v0.1.0`. Do tego czasu tylko szkielet modułu i proces, bez publikacji.
- Grupa wydań: osobna grupa `scalafix` z ADR 0011 (własne tagi `scalafix-vX.Y.Z`), bo reguły migracyjne wychodzą razem z wersją rdzenia, ale nie zmieniają API rdzenia.

### 6. Dokumentacja

- `CLAUDE.md`: sekcja „Narzędzia” z modułami z punktu 2, zamiana opisu `checkForbiddenCalls` na wart, nowe komendy (`sbt scalafixAll`, `sbt "scalafixAll --check"`), zasada „nowa reguła z `CLAUDE.md` dostaje wart albo regułę scalafix, jeśli da się ją sprawdzić automatycznie”.
- `docs/process/development.md`: kontrole w definicji ukończenia.
- `CONTRIBUTING.md`: jak uruchomić poprawki automatyczne przed PR.
- ADR: podział warty i scalafix, moduły narzędziowe w Scali 2.13, reguły migracyjne jako artefakt.

## Kroki (osobne PR-y)

1. Ten szkic.
2. Moduł `tools/warts`, wart `ConsoleOutput` w miejsce `ForbiddenCalls`, włączenie `Wart.AsInstanceOf` i porządek z trzema wystąpieniami. ADR. Zrobione (ADR 0038). Wystąpień `asInstanceOf` było 11: tablica z refleksji przeszła na dopasowanie wzorca, pozostałe są w małych metodach z `@SuppressWarnings` i uzasadnieniem w Scaladocu. Pomiar `clean Test/compile`: 17–20 s przed, 18–20 s po.
3. `ClassParameterInApi` z przeglądem 129 wystąpień `Class[...]` i poprawkami publicznego API (zmiana niezgodna w `0.x`, wpis w changelogu, jeśli dotyczy wydanego API).
4. `tools/scalafix-rules` i `tools/scalafix-tests`, reguły `NoPlainComments`, `OneTypePerFile`, `NoSpringNames`, krok CI i ruleset.
5. `FullyQualifiedImports` z SemanticDB i raport `ExtractLongBranches`.
6. `MissingScaladoc`.
7. `@ExtensionPoint`, `ComponentCompanion` i `checkExtensionPoints`.
8. Szkielet `coilix-scalafix` i proces reguł migracyjnych, przed `core-v0.1.0`.

## Kryteria akceptacji

1. `println("x")`, `Console.err.println("x")` i `System.out.println("x")` w `src/main` albo `src/test` kończą kompilację błędem z podpowiedzią o `@Slf4s`. Tekst `"use println("` w stringu i w Scaladocu nie daje błędu. `project/ForbiddenCalls.scala` nie istnieje.
2. `asInstanceOf` poza dozwolonymi miejscami kończy kompilację błędem.
3. Publiczna metoda `def get[T](kind: Class[T]): T` w `src/main` kończy kompilację błędem z podpowiedzią o `ClassTag`, a `def components: List[Class[?]]` i metoda `private` z `Class[T]` przechodzą.
4. Trait z `@ExtensionPoint` bez `given ConfigPath` albo bez `default` w obiekcie towarzyszącym kończy kompilację błędem, a trait spoza tabeli w `overview.md` przerywa `checkExtensionPoints`.
5. Publiczna metoda bez `/** */` w `src/main` publikowanych modułów (`01`–`89`) kończy kompilację błędem, a nadpisanie bez komentarza przechodzi.
6. `sbt "scalafixAll --check"` przerywa CI dla pliku z komentarzem `//`, dla pliku z dwoma niezależnymi typami i dla klucza `coilix.spring.x`.
7. `sbt scalafixAll` zamienia import względny na pełny, a po poprawce kod się kompiluje i przechodzi `checkFormat`.
8. Każdy wart i każda reguła ma testy (munit dla wartów, testkit dla reguł) z przypadkiem zgłaszanym, niezgłaszanym i wyjątkiem.
9. Zwykła kompilacja nie jest wolniejsza o więcej niż 10% (pomiar `sbt clean compile` przed i po na tej samej maszynie, wynik w opisie PR kroku 2).
10. `CLAUDE.md`, `docs/process/development.md` i `CONTRIBUTING.md` opisują nowe kontrole i komendy.

## Poza zakresem

- Reguły scalafix dla kodu aplikacji użytkowników poza migracjami (np. „dobre praktyki Coilix”). Do rozważenia po pierwszym wydaniu.
- Automatyczna migracja kodu z bulldozera (`OptimusFlow` → `@Processor`). Kod jest zbyt różnorodny, a reguła pokryłaby niewielką część przypadków.
- Zamiana wartremovera na inne narzędzie (np. własny plugin kompilatora). Wartremover wystarcza, a plugin kompilatora zostaje dla funkcji takich jak `@Slf4s`.

## Ryzyka i otwarte pytania

1. Moduły w Scali 2.13 wracają po usunięciu pluginu IntelliJ. Są niepublikowane i małe, ale wymagają drugiego kompilatora w buildzie i osobnych ustawień (bez `-no-indent -old-syntax`, inny scalafmt dialect).
2. `ExtractLongBranches`: czy reguła ma w ogóle blokować build? Zasada jest ważna, ale sztywna; raport z kroku 5 pokaże skalę.
3. `ComponentCompanion`: rozpoznanie punktu rozszerzenia przez nową adnotację `@ExtensionPoint` (jawne, zmienia `02_annotations`) czy przez konwencję `Default*` w tym samym module (bez zmian API, ale zgadywanie).
4. `MissingScaladoc`: czy wartremover dla Scali 3 widzi komentarze Scaladoc (`Symbol.docstring` wymaga `-Yread-docs` albo kompilacji z komentarzami). Jeśli nie, zadanie sbt na scalameta.
5. `-Xsemanticdb` tylko dla scalafix wymaga osobnej kompilacji albo stałego włączenia. Stałe włączenie jest prostsze, ale spowalnia kompilację i zmienia cache sbt 2. Decyzja po pomiarze.
6. Zmiany w publicznym API z kroku 3 (zamiana `Class[T]` na `ClassTag`) są niezgodne. Trzeba je zrobić przed `core-v0.1.0`, inaczej czekają do następnej linii `0.x`.
7. Wersje `sbt-scalafix` dla sbt 2 są młode. Gdy coś nie działa, fallback to `scalafix-cli` uruchamiany z zadania sbt.
