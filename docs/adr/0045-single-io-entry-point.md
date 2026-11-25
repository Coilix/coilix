# 0045: Jeden punkt wejścia aplikacji: CoilixIOApp

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00031 (krok 7)

## Kontekst

Aplikacja Coilix mogła startować na dwa sposoby: synchronicznie przez `Coilix.run` z metody `main` (hook JVM zamykający kontekst przez `ShutdownHookRegistrar`, runnery przez `RunnerInvoker`, zwrócony otwarty kontekst) albo jako `CoilixIOApp`, czyli `IOApp` cats-effect (ADR 0034). Od ADR 0042 i 0043 runtime jest na `IO`, a każda aplikacja ma cats-effect na classpath. Dwie ścieżki startu to dwa zachowania przy zamykaniu, dwa miejsca na obsługę błędów fatalnych i powód, dla którego kontrakty wołane po starcie (np. `ContextLifecycleListener`) nie mogą zwracać `IO` bez `unsafeRunSync`. Opiekun 2026-11-26: „w zasadzie aplikacja mogłaby być oparta na IOApp”.

## Decyzja

- **`CoilixIOApp` jest jedynym punktem wejścia.** Aplikacja to obiekt z `@CoilixApplication`, który rozszerza `CoilixIOApp`. `Coilix.run` i `CoilixApplicationRunner.run` znikają, a `CoilixApplicationRunner` zostaje do budowy kontekstu (`start`), z której korzysta `CoilixIOStartup`.
- **Zamykanie przez anulowanie zamiast hooka JVM.** `IOApp` anuluje główny fiber po `SIGTERM` i Ctrl+C, a kontekst jest `Resource`, więc się zamyka. `ShutdownHookRegistrar`, `RuntimeShutdownHookRegistrar` i klucz `coilix.boot.shutdown-hook-registrar` znikają.
- **Błędy fatalne innych wątków.** Dopóki zasób aplikacji z `CoilixIOStartup.application` żyje, `FatalErrorHandler` kontekstu jest domyślnym handlerem niezłapanych wyjątków procesu. Przy zwolnieniu wraca poprzedni handler.
- **Testy** budują kontekst jak dotąd przez testkit (synchronicznie albo w `IO`), bez banera i logu startu. `RunnerInvoker` zostaje dla `withRunners` w testkicie.

## Konsekwencje

- Częściowo zastąpione: ADR 0034 (dwa sposoby startu) i ADR 0027 (handler błędów fatalnych instalowany przy `Coilix.run`).
- Przykład pierwszej aplikacji, README i przewodniki pokazują `object App extends CoilixIOApp`.
- Następne kroki: dodatki z `OptimusIOApp` bulldozera (log głodzenia CPU i `reportFailure` przez logger, wykrywanie zablokowanych wątków, PID w logu startu, czytelny błąd startu z kodem wyjścia, readiness `Down` do końca runnerów), potem `ContextLifecycleListener` w `IO`.
