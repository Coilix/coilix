# 0049: Kontekst w `Resource`, runnery w `IO`, jedna granica synchroniczna

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00031 (krok 7)

## Kontekst

Po ADR 0047 aplikacja startowała słuchaczy w `IO`, ale kontener nadal uruchamiał `IO` poza fiberem w trzech miejscach:

- `IOEffectInterpreter` otwierał beany z `IO` i `Resource` przez `allocated.unsafeRunSync()` na beanie `ioRuntime` i zwalniał je drugim `unsafeRunSync` przy zamykaniu (`InterpretedBeans`),
- `IORunnerAdapter` czekał na `IOApplicationRunner` przez `unsafeRunSync`,
- demo i przykład z przewodnika (`ExchangeRates.ratesNow`) wołały `unsafeRunSync` w beanach.

Builder, bootstrap i `CoilixApplicationRunner` zwracały gotowy kontekst, więc `CoilixIOStartup` owijał całe tworzenie beanów w `IO.blocking`. Kryterium 4 COILIX-00031 wymaga, żeby demo i przewodniki nie wołały `unsafeRun*`.

## Decyzja

- **Beany z efektów jako `Resource`.** `EffectInterpreter.interpret` zwraca `Resource[IO, BeanRegistry]`. `InterpretedBeans` znika. Interpreter nie szuka już beana `IORuntime`: zasób otwiera fiber, który tworzy kontekst.
- **Kontekst jako `Resource`.** `ApplicationContextBuilder.create`, `CoilixBootstrap.start` i `CoilixApplicationRunner.start` zwracają `Resource[IO, …]` zwalniany przez `ConfigurableApplicationContext.shutdown`. Planowanie, skanowanie i fabryki działają w `IO.blocking`, bo używają refleksji i wołają konstruktory, które mogą blokować. Sam kontener zostaje synchroniczny w środku, zgodnie z ADR 0043. `DefaultApplicationContext` dostaje `release: IO[Unit]` (zwolnienie zasobu interpretera) i niszczy pozostałe beany także po błędzie słuchacza albo `release`.
- **Runnery w `IO`.** `RunnerAdapter.run`, `OrderedRunner.run` i `RunnerInvoker.invoke` zwracają `IO[Unit]`. Zwykły `ApplicationRunner` działa w `IO.blocking`, a `IORunnerAdapter` nie potrzebuje runtime. `StartupEvents.around` przyjmuje runnery jako `IO` i nie zamyka kontekstu, bo robi to właściciel zasobu.
- **Testkit w `IO`.** `TestContextFactory.resource` daje wystartowany kontekst testu jako `Resource`. `create` (testy synchroniczne) otwiera ten zasób, a `CoilixIOTestSetup.resource` używa go wprost, bez `IO.blocking`.
- **Jedna granica synchroniczna.** `unsafeRunSync` zostaje tylko w `SynchronousLifecycle`: `opened` i `started` (kontekst dla kodu poza `IO`, na globalnym runtime cats-effect), `run` (np. `close()` i testy synchroniczne, na beanie `ioRuntime`). `close()` niszczy beany w wątku wołającym, więc bean `IORuntime` nie jest zamykany, zanim skończą się słuchacze.

## Konsekwencje

- Ścieżka `CoilixIOApp` od wczytania konfiguracji do zamknięcia kontekstu nie uruchamia `IO` poza fiberem aplikacji. Demo i przykłady z przewodników nie wołają `unsafeRun*`.
- Zmiana niezgodna API publicznego (`0.x`: `MINOR`): własne `EffectInterpreter`, `RunnerAdapter`, `RunnerInvoker`, `CoilixBootstrap`, `ApplicationContextBuilder`, `CoilixApplicationRunner` i `TestContextFactory` dostosowują sygnatury do `IO` i `Resource`.
- Beany z efektów w kontekście budowanym synchronicznie (`build`, testkit synchroniczny) otwierają się na globalnym runtime cats-effect, a nie na beanie `ioRuntime`. W aplikacji oba są tym samym runtime `IOApp`.
- Błędy startu nadal płyną kanałem błędów `IO` jako wyjątki. Błędy typowane (`EitherT` z typem błędu warstwy niższej, decyzja o rzuceniu w warstwie wyższej) to osobny krok.
