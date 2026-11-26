# 0050: Błędy typowane w `EitherT`, decyzja o rzuceniu w warstwie wyższej

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00031 (krok 10)

## Kontekst

Po ADR 0049 kontekst i start aplikacji działają w `Resource` i `IO`, ale każdy błąd kontenera i startu płynie kanałem błędów `IO` jako wyjątek. Warstwa wyżej dostaje `Throwable` i nie wie z typu, co może się nie udać. Musi łapać konkretne klasy wyjątków albo przepuszczać wszystko dalej, a przewidywalny błąd (brak beana, cykl, zła podmiana beana) wygląda tak samo jak bug. `CoilixIOApp` logował każdy błąd startu ze stosem wywołań, także zwykły błąd konfiguracji aplikacji.

## Decyzja

- **Przewidywalne błędy jako typ warstwy.** Każda warstwa ma `enum` swoich błędów: `ContextError` w `context` (`MissingBean`, `AmbiguousBean`, `CircularDependency`, `InvalidDependency`, `EffectfulBeans`, `CreationFailed`) i `StartupError` w `boot` (`NotABootApplication`, `ConflictingEffectInterpreters`, `UnmatchedBeanReplacement`, `CollectedComponentConflict`, `CollectedComponentRequired`, `Configuration`, `Context`). Warstwa wyższa obejmuje błędy niższej przypadkiem z ich typem (`StartupError.Context(error: ContextError)`), więc nic nie ginie po drodze.
- **`Either` w kodzie synchronicznym, `EitherT` w efektach.** `CandidateSelector.choose`, `DependencyResolver.resolve`/`resolveLevels`, `ContextPlanner.plan` i `BasePackageResolver.resolve` zwracają `Either`. Operacje w zasobie zwracają `ResourceT[E, A]`, czyli `EitherT[Resource[IO, *], E, A]`: `ApplicationContextBuilder.create`, `CoilixBootstrap.start`, `CoilixApplicationRunner.start` i `CoilixIOStartup.application`.
- **Decyzja o rzuceniu na górze.** Dopiero warstwa najwyższa zamienia `Left` na coś innego. `CoilixIOStartup.run` loguje komunikat błędu bez stosu wywołań i zwraca `ExitCode.Error`. `ApplicationContextBuilder.build`, `CandidateSelector.select` i testkit rzucają `exception` błędu, czyli te same wyjątki co wcześniej (`NoSuchBeanException`, `AmbiguousBeanException`…), więc kod synchroniczny i testy z `intercept` działają bez zmian. `ContextError.raised` i `StartupError.raised` robią to samo dla `ResourceT`.
- **Granica z kodem synchronicznym.** Tworzenie beanów i wczytywanie konfiguracji zostają synchroniczne (ADR 0043) i zgłaszają błędy wyjątkami. Na granicy builder zamienia `CoilixException` z tworzenia beanów na `ContextError.CreationFailed`, a runner `ConfigurationException` z wczytywania konfiguracji na `StartupError.Configuration`.
- **Defekty zostają w `IO`.** Wyjątek, który nie jest błędem frameworka (bug w komponencie, `OutOfMemoryError`), dalej kończy efekt błędem i trafia do `CoilixIOApp.failed` ze stosem wywołań.

## Konsekwencje

- Typ mówi, co może się nie udać w każdej warstwie, a wywołujący wybiera: obsługuje błąd, opakowuje go w swój typ albo rzuca.
- Aplikacja, która nie może wystartować z powodu konfiguracji albo braku beana, kończy się czytelnym komunikatem i kodem wyjścia `1`, bez stosu wywołań.
- Zmiana niezgodna API publicznego (`0.x`: `MINOR`): własne `CandidateSelector` implementują `choose`, a własne `DependencyResolver`, `ContextPlanner`, `ApplicationContextBuilder`, `CoilixBootstrap`, `CoilixApplicationRunner` i `BasePackageResolver` zwracają `Either` albo `ResourceT`.
- Klasy wyjątków zostają jako forma rzucana błędów typowanych. Przy kolejnych przepisaniach (konfiguracja, `processing`, `events`) ta sama zasada: przewidywalny błąd jako typ warstwy w `Either` albo `EitherT`, rzucenie w warstwie najwyższej.
