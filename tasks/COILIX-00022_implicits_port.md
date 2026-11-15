# COILIX-00022: Typy wartości, enumy, walidacja i `EditOption` z biblioteki implicits

## Cel

Przenieść do Coilixa jak najwięcej sprawdzonego kodu z prywatnej biblioteki opiekuna `org.optimus:implicits` (Apache-2.0, ten sam autor), przepisanego na styl Coilixa: adnotacje Javy wykrywane refleksyjnie zamiast companion traitów, bez zależności od cats, circe, sangrii, enumeratum i pureconfig w rdzeniu.

Typy wartości i enumy mają działać od razu w konfiguracji (`@Value`, `@ConfigurationProperties`), a później w modułach `data` i `web`.

## Stan obecny

- `06_data` jest pusty (tylko `.gitkeep`), a moduł nie ma specyfikacji (ROADMAP: „not yet specified”).
- `DefaultScalarReader` w `04_config` czyta tylko `String`, liczby, `Boolean` i czasy trwania. Nie zna typów opakowujących jedną wartość ani enumów.
- implicits (wersja 1.8.3) ma potrzebne klocki, ale zbudowane na zewnętrznych bibliotekach:
  - `AnyValObject[T, A]`: companion trait dla opakowania jednej wartości, wyprowadza kodeki circe i skalar sangrii makrem,
  - `EnumProduct` i `EnumSettings[E]`: enumy na enumeratum z `values = findValues`, kodekami circe, typem GraphQL i czytnikiem pureconfig,
  - `EditOption[T]` (`NoEdit`, `SetTo`): trójstanowa zmiana pola w PATCH, z instancjami cats, circe i sangrii,
  - `Violation`, `Validator[T]`, `Statement[T]`, `DomainError`: walidacja z kumulacją błędów na `cats.data.Validated`,
  - `HiddenString`: tekst, który nie pokazuje się w `toString`,
  - `Registry` i `RegistryExtension`: rejestr rozszerzany przez `ServiceLoader`,
  - cache na scaffeine, i18n komunikatów, `OxClock`, `Email`, `HasDefault`.

## Zakres

Decyzje opiekuna (2026-11-15): adnotacje i refleksja, nazwy `@ValueType` i `@Enumerated`, części ogólne w rdzeniu, bazodanowe w `data`. Nazwy z przyrostkiem `Object` są niedozwolone.

Każdy punkt to osobny PR.

1. **`EditOption[T]`** w `01_common` (`dev.coilix.common`):
   - `NoEdit` i `SetTo(Option[T])` z obecnym API (`map`, `flatMap`, `fold`, `filter`, `zip`, `orElse`, `getOrElse`, `toOption`, `toNestedOption`, predykaty),
   - bez instancji cats, circe i sangrii. Te trafią do modułów integracyjnych (poza zakresem).
2. **Walidacja** w `01_common`:
   - `Violation(message)` i `ViolationTemplate[T](template)(arguments*)` jako makra `inline`: kod z nazwy `val`, szablon sprawdzany przy kompilacji, tak jak `tvb` w implicits (ADR 0013). Jawny kod przez `Violation(code, message)`, `Validator[T]` i `Statement[T]` z tym samym płynnym API (`test`, `testNot`, `testAll`, `testAny`, `and`, `subValidator`, `onCaseOf`, `optional`, `validateEach`, rozmiary list),
   - wynik `Either[List[Violation], T]` zamiast `Validated`,
   - `ValidationException` dziedziczący po `CoilixException` zamiast `DomainError`.
3. **Typy wartości** (zamiennik `AnyValObject`):
   - `@ValueType` w `02_annotations` dla klasy z jednym polem konstruktora, np. `@ValueType final case class UserId(value: Long)`,
   - trait `ValueTypeResolver` i implementacja refleksyjna w `04_config`: rozpoznaje klasę, podaje typ wewnętrzny, tworzy instancję z wartości i wyciąga wartość z instancji,
   - wyjątek z konstruktora (np. `require`) zamieniony na czytelny błąd z nazwą typu i wartością,
   - `Secret` (dawne `HiddenString`): typ wartości dla haseł i tokenów, `toString` zwraca `*****`.
4. **Enumy** (zamiennik `EnumProduct` i `EnumSettings`):
   - `@Enumerated` dla natywnego `enum` Scali 3 i `@EnumName("…")` dla własnej nazwy przypadku,
   - trait `EnumResolver` i implementacja refleksyjna w `04_config`: lista przypadków, wyszukiwanie po nazwie z opcjonalnym ignorowaniem wielkości liter, nazwa przypadku,
   - błąd przy nieznanej nazwie podaje dozwolone wartości.
5. **Konfiguracja** w `04_config`: `ReflectiveConfigBinder` wiąże typy `@ValueType` (przez typ wewnętrzny, więc także `List[String]` w środku) i `@Enumerated`, więc działają w `@Value`, `@ConfigurationProperties`, `Option` i kolekcjach. Changelog `core`.
6. **Przewodnik** „Typy wartości i enumy” w obu językach, po scaleniu sekcji przewodników z COILIX-00004.
7. **Mapowanie kolumn w `06_data`**: kodeki kolumn dla typów wartości i enumów. Dopiero razem ze specyfikacją modułu `data` (repozytoria, transakcje, sterownik). Ten task tylko zostawia na to miejsce w resolverach.

## Proponowany projekt

- Adnotacje Javy z retencją runtime w `02_annotations`: `ValueType.java` (`@Target(TYPE)`), `Enumerated.java` (`@Target(TYPE)`), `EnumName.java` (`@Target(FIELD)`, bo przypadek enuma jest polem obiektu towarzyszącego).
- `01_common` dostaje pakiety `dev.coilix.common.validation` i `dev.coilix.common.edit`, które nie potrzebują adnotacji.
- Resolvery i `Secret` są w `04_config` (pakiety `dev.coilix.config.valuetype` i `dev.coilix.config.enums`), bo `01_common` nie może zależeć od `02_annotations` (moduł zależy tylko od modułów o niższym numerze). `config` jest najniższym modułem, który widzi adnotacje, a `context`, `data` i `web` zależą od niego.
- Resolvery to traity z domyślną implementacją w companionie, tak jak inne wymienne komponenty (ADR 0003).
- Wyniki refleksji są cachowane per klasa w niemutowalnej strukturze budowanej leniwie albo przy starcie kontekstu, bez globalnego stanu mutowalnego.
- Każda przeniesiona klasa dostaje testy munit przepisane z testów implicits tam, gdzie mają sens bez cats i circe.

## Kryteria akceptacji

1. `@ValueType final case class Port(value: Int)` i `@Enumerated enum Mode` dają się wstrzyknąć przez `@Value` i związać w `@ConfigurationProperties`. Testy to sprawdzają.
2. Zła wartość (typ wartości odrzuca ją w konstruktorze, nieznana nazwa enuma) przerywa start z komunikatem z nazwą typu, wartością i ścieżką konfiguracji.
3. `Secret` nie ujawnia wartości w `toString` ani w logach Coilixa.
4. `Validator` zbiera wszystkie naruszenia, a `ValidationException` je wymienia.
5. `EditOption` ma testy wszystkich trzech stanów dla każdej operacji.
6. Rdzeń nie dostaje nowych zależności zewnętrznych (`sbt checkLicenses` bez zmian).
7. Pokrycie nie spada poniżej progów z buildu.

## Poza zakresem

- Moduły integracyjne z circe, sangrią i cats (kodeki JSON, typy GraphQL, instancje type classów). Osobny task po COILIX-00020.
- `Registry` i `RegistryExtension`: mechanizm `ServiceLoader` jest wzorem dla `CoilixModule` w COILIX-00010 i trafi tam, a nie tutaj.
- Cache (scaffeine), i18n komunikatów naruszeń i `OxClock` (wystarczy `java.time.Clock`). Mogą wrócić jako osobne taski.
- `Email` i inne typy domenowe. Framework nie powinien narzucać modelu domeny, a `Email` może zostać przykładem w przewodniku.
- `HasDefault`: binder konfiguracji ma już wartości domyślne Scali (`ScalaDefaultArguments`).
- Derywacja `derives` i makra do wykrywania typów (ADR 0012). Wyjątkiem są makra sprawdzające naruszenia (ADR 0013).

## Ryzyka i otwarte pytania

- **`extends AnyVal`:** sprawdzone (2026-11-15). Parametr konstruktora typu `Id extends AnyVal` ma w sygnaturze JVM typ `long`, a `Id` widać tylko w argumentach typów generycznych (`List<Id>`, `Option<Id>`). Typ `@ValueType` nie powinien więc dziedziczyć po `AnyVal`. Refleksja nie odróżni takiej klasy od zwykłej, więc ostrzega przed tym Javadoc adnotacji.
- **Adnotacje na przypadkach enuma:** sprawdzone (2026-11-15). Adnotacja Javy na `case Earth` trafia na statyczne pole `Earth` w klasie obiektu towarzyszącego (`Planet$`), także dla przypadku z argumentami konstruktora enuma, a enum ma statyczne `values` i `valueOf`. Enum z przypadkami z parametrami (`case Circle(r: Double)`) nie ma `values`, więc `@Enumerated` go odrzuca. Enumy Javy nie są obsługiwane. `@EnumName` ma więc `@Target(FIELD)`.
- Komunikaty `Violation` w implicits dostają nazwę z makra (nazwa `val`). Opiekun chce to zachować, więc makra wracają (ADR 0013). Zmiana nazwy `val` zmienia kod naruszenia.
- Kod pochodzi z biblioteki tego samego autora na tej samej licencji, więc przeniesienie nie wymaga zgód. Commity i tak podpisuje się przez DCO.

## Stan realizacji (2026-11-16)

#35 `EditOption` scalony. #37 (`@ValueType` i `Secret`) został scalony do gałęzi #36, więc #36 wnosi walidację z makrami (ADR 0013) i typy wartości. Dalej #38 `@Enumerated` i #39 wiązanie w konfiguracji. Zostały przewodnik (punkt 6) i mapowanie kolumn (punkt 7).
