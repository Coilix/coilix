# 0040: Moduł rdzenia `diagnostics`: zdrowie aplikacji i kontrole wątków

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00030 (krok 2, część 1)

## Kontekst

COILIX-00030 wymaga, żeby aplikacja wykrywała zakleszczenia i mówiła o swoim zdrowiu, a moduły funkcji (procesory, HTTP) reagowały na spadek gotowości. ADR 0039 ustalił, co z zakleszczeń da się zobaczyć: wątki platformowe przez `ThreadMXBean.findDeadlockedThreads`, a wątki wirtualne dopiero przez zrzut `dumpThreads` na JDK 25 i nowszym.

Wzorem jest inspektor wątków bulldozera (osobny wątek, migawka wątków co interwał, wymienne detektory zakleszczenia, liczby wątków, długiego `BLOCKED` i przyrostu wątków, raport z ograniczeniem powtórzeń). Coilix przenosi podział na komponenty, ale nie kod: bez cats-effect, z własnymi nazwami i modelem zdrowia, którego bulldozer nie ma.

## Decyzja

- **Moduł rdzenia `09_diagnostics`** (`dev.coilix.diagnostics`, artefakt `coilix-diagnostics`, grupa `core`), zależny od `events` (a przez nie od `concurrency`), bez zależności zewnętrznych. `boot` od niego zależy, więc każda aplikacja go ma, a `coilix.modules.diagnostics.enabled = false` go wyłącza. Jest w rdzeniu, bo moduły funkcji muszą znać model zdrowia, a nie mogą zależeć od siebie nawzajem.
- **Zdrowie:** `HealthStatus` (`Up`, `Degraded`, `Down`), `HealthKind` (`Liveness`, `Readiness`), `HealthState` ze statusem i powodem, trait `HealthIndicator` (bean z nazwą, rodzajem i stanem) i bean `health` (`DefaultHealth`). Liveness to najgorszy stan wskaźników `Liveness`, readiness najgorszy stan wszystkich. `refresh()` czyta wskaźniki, a zmiana któregoś statusu publikuje `HealthChanged` przez `EventPublisher`. Wskaźnik, który rzuca wyjątek, liczy się jako `Down`. Moduł daje wskaźnik `threads` (`SettableHealthIndicator`, liveness).
- **Monitor:** bean `diagnosticsMonitor` (`ContextLifecycleListener`) robi pierwszą rundę przy starcie kontekstu, a następne co `coilix.diagnostics.interval` (domyślnie 5 s) na własnym wątku platformowym `coilix-diagnostics` poza pulami aplikacji. Zatrzymuje się na początku zamykania. Runda to migawka wątków platformowych (`ThreadSampler`: stan z `Thread.getState`, stos z `Thread.getAllStackTraces`, blokada i właściciel z `ThreadMXBean`), historia stanów i liczby wątków (`ThreadHistory`), kontrole, raport i odświeżenie zdrowia.
- **Kontrole:** trait `ThreadCheck`, a każdy bean tego typu bierze udział. Wbudowane beany: `DeadlockCheck` (`Critical`), `ThreadCountCheck` (`Warning` i `Critical`), `LongBlockedCheck` (`Warning` po `blocked-after`, domyślnie 30 s) i `ThreadGrowthCheck` (`Warning` przy przyroście o `growth-limit` w `growth-window`, po `startup-grace`). Wynik to `Finding` z wagą, rodzajem, opisem i wątkami. Kontrola, która rzuca wyjątek, jest pomijana w tej rundzie.
- **Raport:** trait `FindingReporter`, domyślnie `LoggingFindingReporter` (`@ConditionalOnMissingBean`): `Critical` w każdej rundzie na `ERROR`, ostrzeżenie danego rodzaju najwyżej raz na `repeat-after` (domyślnie 1 min), ze stosami wątków.
- **Reakcja na zakleszczenie:** `coilix.diagnostics.threads.on-deadlock`: `shutdown` (domyślnie) przekazuje raz `ThreadDeadlockException` do `FatalErrorHandler`, `report` ustawia wskaźnik `threads` na `Down`.
- **Błędy samego monitora:** nieudana runda trafia do logu, a następna idzie normalnie. Do `FatalErrorHandler` trafia tylko `VirtualMachineError`. `LinkageError` na wątku diagnostyki pochodzi z kodu diagnostyki albo kontroli, więc nie świadczy o awarii aplikacji, a monitor nie może jej kończyć dlatego, że nie umie jej obserwować. Tak było w testach: wątek kontekstu, którego test nie zamknął, dostał `LinkageError` z zamkniętego classloadera sbt i zakończył proces sbt.
- **Wątki wirtualne** (sygnał z długich zadań wykonawców i zrzut `dumpThreads` z ADR 0039) przychodzą w drugiej części kroku 2.

## Konsekwencje

- Każda aplikacja na `coilix-boot` wykrywa zakleszczenia wątków platformowych i domyślnie kończy się po nich, a moduły i aplikacja mogą dokładać wskaźniki zdrowia i kontrole jako beany.
- Każdy kontekst, także testowy, ma wątek `coilix-diagnostics` i robi migawkę wątków przy starcie. Test, który celowo tworzy zakleszczenie, musi działać w osobnej JVM (`Test / fork := true` w `diagnostics`), bo zakleszczenie dotyczy całej JVM i zobaczyłyby je konteksty innych testów.
- Endpointy `/health/live` i `/health/ready` dołoży moduł web (COILIX-00023), a monitor pamięci krok 3 tego taska.
