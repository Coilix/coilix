# 0035: Moduł testkit-cats-effect

- Status: proponowana
- Data: 2026-11-24
- Task: COILIX-00020 (krok 5c, część 2)

## Kontekst

Decyzja opiekuna z 2026-11-23: testy w `IO` w osobnym module `testkit-cats-effect` z munit-cats-effect jako `Provided`, z `CoilixTestSetup.useIO` i `CoilixIOSuite` (`coilixTest` zwracający `IO`, kontekst jako `given`). `testkit` należy do rdzenia i nie może zależeć od modułu `cats-effect`.

## Decyzja

- **Moduł `24_testkit_cats_effect`** (`dev.coilix.testkit.catseffect`, artefakt `coilix-testkit-cats-effect`) zależy od `testkit` i `cats-effect`. To integracja rdzenia z jedną funkcją, więc nie trafia na listę modułów funkcji w `checkModuleLayers`. Wydaje się w grupie `cats-effect`, razem z modułem, który rozszerza, i ma wpisy w `changelog/cats-effect/`.
- **munit i munit-cats-effect (2.2.1) jako `Provided`**, jak munit w `testkit`: aplikacja wybiera wersje w swoich zależnościach testowych.
- **`CoilixIOTestSetup`**: metody rozszerzające `CoilixTestSetup` — `withIORuntime(runtime)` (gotowy bean `ioRuntime`, więc moduł nie tworzy własnego runtime'u, jak w ADR 0034), `resource` (`Resource[IO, ConfigurableApplicationContext]` przez `TestContextFactory`) i `useIO(body)` z kontekstem jako `given`.
- **`CoilixIOSuite`** dla `CatsEffectSuite`: `coilixTest(name)` z `given CoilixTestSetup` i wersja z jawnym setupem, wzorem `CoilixSuite`. Runtime suite'a (`munitIORuntime`) jest beanem `ioRuntime`, więc test i beany używają jednej puli, tak jak w `CoilixIOApp`.

## Konsekwencje

- Testy w `IO` nie potrzebują `unsafeRunSync` w treści.
- Kontekst nie zamyka runtime'u suite'a, bo gotowe beany nie mają `@PreDestroy`.
- Krok 5c, a z nim zakres 5 tasku COILIX-00020 dla `cats-effect`, jest zamknięty. Zostają beany generyczne w `F[_]` i most MDC dla fiberów.
