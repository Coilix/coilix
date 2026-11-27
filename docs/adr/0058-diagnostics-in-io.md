# 0058: Diagnostyka w `IO`

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 6)
- Zastępuje część: 0040 (`HealthIndicator.state` i odczyty `Health` w stylu bezpośrednim, `FindingReporter` zwracający `Unit`, monitor na `ScheduledExecutorService` z `Atomic*`)

## Kontekst

Moduł `diagnostics` (ADR 0040) miał kontrakty w stylu bezpośrednim: `HealthIndicator.state` zwracał `HealthState`, a `Health.liveness`, `readiness` i `indicators` czytały `AtomicReference`. Wskaźnik, który chciał zapytać bazę albo inny serwis w `IO`, musiał uruchamiać efekt poza fiberem. `DiagnosticsMonitor` trzymał historię i reakcję na zakleszczenie w `Atomic*`, a odświeżenie zdrowia uruchamiał przez `SynchronousLifecycle.runOn`, czyli czekał na pulę obliczeniową, której zablokowanie miał właśnie wykrywać.

## Decyzja

- **Kontrakty w `IO`.** `HealthIndicator.state` zwraca `IO[HealthState]`, `Health.liveness`, `readiness` i `indicators` zwracają `IO` ze stanem z ostatniego `refresh`, a `FindingReporter.report` zwraca `IO[Unit]`. Wskaźnik, którego `IO` kończy się błędem, liczy się jako `Down` z błędem jako powodem.
- **Stan w `Ref`.** `DefaultHealth`, `SettableHealthIndicator`, `StartupHealthIndicator`, `LoggingFindingReporter` i `DiagnosticsMonitor` są `case class` ze stanem w `Ref.unsafe` tworzonym w `apply`, więc zostają zwykłymi beanami (jak `ScheduledMethodRegistrar`, ADR 0056). `LoggingFindingReporter` wybiera wyniki do logu jednym `Ref.modify`, a ostrzeżenie jednego rodzaju trafia do logu raz na rundę.
- **Monitor na własnym wątku, bez puli.** Rundy są pętlą w `IO` przesuniętą przez `evalOn` na jednowątkowy `ScheduledExecutorService` `coilix-diagnostics`, a pauza między rundami to `IO.async` budzone przez ten sam wykonawca, nie `IO.sleep`, którego zegar działa na wątkach puli obliczeniowej. Rundy i oczekiwanie nie potrzebują więc wątku puli, a kontrola zakleszczeń działa przy zablokowanej puli. Fiber rund jest anulowany na początku zamykania, a wykonawca zamykany potem.
- **Błędy fatalne bez zmian.** Próbkowanie i kontrole wątków działają w jednym bloku `IO`, który sam łapie `LinkageError` (zapis w logu) i `VirtualMachineError` (przekazanie do `FatalErrorHandler`), zanim zobaczy je cats-effect. Nieudany raport albo odświeżenie kończy tylko swoją rundę.
- Bean `diagnosticsMonitor` nie dostaje już `IORuntime`.

## Konsekwencje

- Wskaźniki i reportery piszą się w `IO` jak reszta aplikacji, a kod czytający `Health` składa odczyt z innym `IO`.
- Moduł nie używa `Atomic*` ani `synchronized`. Jedynym wątkiem poza pulą jest `coilix-diagnostics`, który wart z kroku 8 COILIX-00031 ma traktować jako wyjątek.
- Zmiana niezgodna: `state` wskaźników zwraca `IO[HealthState]` (`IO(...)` wokół dotychczasowego wyrażenia), odczyty `Health` są w `IO`, a własny `FindingReporter` zwraca `IO[Unit]`.
