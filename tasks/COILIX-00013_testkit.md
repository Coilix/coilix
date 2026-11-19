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
5. Testy demo przepisane na testkit jako przykład użycia.

## Proponowany projekt

> Zrealizowane 2026-11-19, decyzje w [ADR 0016](../docs/adr/0016-testkit-module.md).

```scala
final class WelcomeMailTest extends munit.FunSuite with CoilixFixtures {
  private val mailer  = RecordingMailer()
  private val context = coilixContext(
    CoilixTestSetup
      .forApplication[NewsletterApplication.type]
      .withProperties("newsletter.smtp-host" -> "localhost")
      .withProfiles("test")
      .replaceBean[Mailer](mailer)
  )

  context.test("sends a welcome mail") { ctx =>
    ctx.getBean[WelcomeMail].sendTo("ann@example.com")
    assertEquals(mailer.sent.map(_._1), List("ann@example.com"))
  }
}
```

- `CoilixTestSetup` (case class, niemutowalny builder): pakiety (`forApplication[T]`, `forPackages`), właściwości, profile, beany, argumenty, flaga runnerów.
- `TestContextFactory` (trait) i `DefaultTestContextFactory`, który składa start jak `Coilix.run`, bez banera, logu startu i hooka. Bez `given ConfigPath`, tak jak `CoilixApplicationRunner`, bo działa przed wczytaniem konfiguracji.
- Podmiana beana w `boot`: `CoilixBootstrap.start(..., BeanReplacements)` usuwa definicje ze skanowania i modułów przypisywalne do podmienianego typu. `replaceBean[T]` musi coś zastąpić (inaczej `UnmatchedBeanReplacementException`), a `addBean[T]` dodaje bean, którego aplikacja nie ma.
- Styl zwięzły (prośba opiekuna, wzór z testkitu bulldozera): `setup.use { ... }`, `CoilixSuite` z `coilixTest(name) { bean[T] ... }` i `shared.test(name) { ... }`, helpery `CoilixContextOps` na `using`.
- `CoilixFixtures` (trait domieszany do suite'u, bo `FunFixture` w munit jest klasą wewnętrzną suite'u) z `coilixContext` i `coilixSuiteContext`, jedyne miejsca zależne od munit obok `SuiteContextFixture`.

## Kryteria akceptacji

1. Testy `11_demo` i przykład przewodnika w `12_docs/examples/next/test` działają na testkicie.
2. Właściwości z `withProperties` wygrywają z plikami i właściwościami systemowymi.
3. `replaceBean` zastępuje bean ze skanowania i jest wstrzykiwany do beanów zależnych, a `addBean` dodaje brakujący bean.
4. Kontekst jest zamknięty po teście (po COILIX-00004 także `@PreDestroy`).
5. Moduł działa bez innych modułów funkcji na classpath.

## Poza zakresem

- Testy wycinków (tylko warstwa web, tylko data). Przyjdą razem z tymi modułami.
- Integracja z ScalaTest i innymi bibliotekami testów. Rdzeń (`CoilixTestSetup`, `TestContextFactory`) ma nie zależeć od munit, więc da się ją dodać później.
- Atrapy (mocki). Testkit przyjmuje gotowe instancje, a bibliotekę mocków wybiera użytkownik.

## Ryzyka i otwarte pytania

- Numer katalogu: rozstrzygnięte, `10_testkit` i `11_demo` (ADR 0016).
- Zakres podmiany: rozstrzygnięte, usuwane są wszystkie definicje przypisywalne do typu, a podmiana bez dopasowania jest błędem. Do dodania beana służy osobne `addBean`.
