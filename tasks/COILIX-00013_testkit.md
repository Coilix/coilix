# COILIX-00013: Wsparcie testów (`coilix-testkit`)

## Cel

Programista aplikacji pisze test munit, który buduje kontekst Coilix z wybranych pakietów, podmienia wybrane beany na atrapy i ustawia config tylko dla testu, bez ręcznego składania `StartupComponents`.

## Stan obecny

- Testy frameworka składają kontekst ręcznie (`Definitions.scala`, `Providers.scala` w `05_context/src/test`) albo wołają `Coilix.run` z aplikacją fixture (`09_boot/src/test`).
- `Coilix.run` czyta config z classpath i właściwości systemowe. Nie ma sposobu, żeby dać testowi własne wartości bez plików w `src/test/resources`.
- Nie ma sposobu, żeby zastąpić bean aplikacji atrapą bez zmiany kodu (`@Primary` w źródłach testowych działa, ale wymaga klasy na każdy przypadek).
- `close()` kontekstu po teście trzeba wołać ręcznie.

## Zakres

1. Nowy moduł `testkit` (`coilix-testkit`), publikowany, zależny od `boot` i `munit` (`Provided`, żeby nie narzucać wersji).
2. Budowanie kontekstu dla testu:
   - pakiety do skanowania albo klasa `@CoilixApplication`,
   - mapa właściwości z najwyższym priorytetem,
   - beany podane jako instancje, które wygrywają z beanami tego samego typu ze skanowania,
   - bez banera, shutdown hooka i `ApplicationRunner`, chyba że test o to poprosi.
3. Fixture munit (`FunFixture` i `Fixture` na cały suite), która zamyka kontekst po teście.
4. Testy modułu na aplikacji fixture w `src/test`.
5. Testy `10_demo` przepisane na testkit jako przykład użycia.

## Proponowany projekt

```scala
final class UserServiceTest extends munit.FunSuite {
  private val context = CoilixFixture(
    CoilixTestSetup
      .forApplication[DemoApplication.type]
      .withProperties("mail.host" -> "localhost")
      .withBean[UserRepository](FixedUserRepository(List("Ann")))
  )

  context.test("lists users from the repository") { ctx =>
    assertEquals(ctx.getBean[UserService].listUsers(), List("Ann"))
  }
}
```

- `CoilixTestSetup` (case class, niemutowalny builder): pakiety, właściwości, beany, flagi runnerów.
- `TestContextFactory` (trait, `given ConfigPath`, `default`) i `DefaultTestContextFactory`, który składa `StartupComponents` z wyłączonym banerem i hookiem.
- Podmiana beana: definicje ze skanowania, których typ jest przypisywalny do typu podanego w `withBean`, są usuwane przed budową kontekstu.
- `CoilixFixture` w osobnym pliku, jedyne miejsce zależne od munit.

## Kryteria akceptacji

1. Test z przykładu wyżej działa w `10_demo`.
2. Właściwości z `withProperties` wygrywają z plikami i właściwościami systemowymi.
3. `withBean` zastępuje bean ze skanowania i jest wstrzykiwany do beanów zależnych.
4. Kontekst jest zamknięty po teście (po COILIX-00004 także `@PreDestroy`).
5. Moduł działa bez innych modułów funkcji na classpath.

## Poza zakresem

- Testy wycinków (tylko warstwa web, tylko data). Przyjdą razem z tymi modułami.
- Integracja z ScalaTest i innymi bibliotekami testów. Rdzeń (`CoilixTestSetup`, `TestContextFactory`) ma nie zależeć od munit, więc da się ją dodać później.
- Atrapy (mocki). Testkit przyjmuje gotowe instancje, a bibliotekę mocków wybiera użytkownik.

## Ryzyka i otwarte pytania

- Numer katalogu: `testkit` zależy od `boot` (`09`), więc według zasady poziomów musi mieć numer wyższy. Propozycja: `10_testkit`, przesunięcie `demo` na `11` i pluginu IntelliJ na `12`. Koliduje to z planowanym `12_docs` z COILIX-00007 i przenumerowaniem z COILIX-00010, więc numery trzeba ustalić razem.
- Czy `withBean` ma usuwać tylko definicje tego samego typu, czy wszystkie przypisywalne? Druga opcja jest wygodniejsza, ale może usunąć więcej, niż test zakłada.
