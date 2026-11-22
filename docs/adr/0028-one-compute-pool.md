# 0028: Jedna pula obliczeniowa i osobny wykonawca blokujący

- Status: proponowana
- Data: 2026-11-22
- Task: COILIX-00005 (zastępuje część ADR 0020), wpływa na COILIX-00020 i COILIX-00029
- Wzór: podział na `ComputeExecutionContext` i `BlockingExecutionContext` z `OptimusRuntime` w `org.optimus:implicits` (koncepcja, kod napisany od nowa)

## Kontekst

ADR 0020 ustawił `TaskExecutor` domyślnie na wątek wirtualny na zadanie. Na `TaskExecutor` trafia jednak każda praca: `@Async`, asynchroniczni odbiorcy zdarzeń, `submit`. Wątki wirtualne pomagają tylko pracy, która czeka. Obliczenia nic nie zyskują, a do tego zajmują wątek nośny bez wywłaszczania. Na JDK 21 blokowanie w `synchronized` przypina nośnik. Wykonawca nie miał limitu, a na JDK 17 tworzył wątek platformowy na zadanie. Eksperyment z COILIX-00030 pokazał też, że wątków wirtualnych nie widać w `ThreadMXBean`, więc domyślna pula była dla diagnostyki ślepą plamą.

Planowane integracje to cats-effect (jedyny wspierany efekt; ZIO odpada) i Pekko. Każda ma własną pulę CPU, a JDK ma pulę nośników wątków wirtualnych. Przy domyślnych ustawieniach aplikacja z obiema integracjami miałaby kilka pul wielkości liczby rdzeni. Gdy wszystkie mają pracę, system przełącza wątki, a każde przejście `IO` → Pekko → zadanie Coilix to skok między pulami.

## Eksperymenty (2026-11-22, JDK 25, 32 rdzenie, cats-effect 3.7.1, Pekko 2.0.0)

| Pytanie | Wynik |
|---|---|
| Wątki CPU przy osobnych pulach cats-effect i Pekko | 75 wątków: 32 `io-compute`, 32 Pekko `default-dispatcher`, 3 `internal-dispatcher` |
| Pekko na `IORuntime.compute`, `pekko.actor.internal-dispatcher = "pekko.actor.default-dispatcher"` | 40 wątków, jedna pula CPU (32), aktor i strumień działają |
| Łańcuch 100 000 zadań przekazywanych z wątku roboczego | zostaje na tym samym wątku: WSTP 97,9%, `ForkJoinPool` 98,6% |
| 30 × `IO.sleep(100 ms)` przy obciążeniu Pekko na tej samej puli | 3,16 s zamiast 3 s, bez ostrzeżenia o zagłodzeniu |
| `IO.blocking` i `IO.interruptible` przy własnej puli blokującej (`setBlocking`) | blokują „w miejscu” na `io-compute-blocker-*`; własna pula jest pomijana |
| `IO.blocking(...).evalOn(wątki wirtualne)` | działa na wątku wirtualnym |
| Błąd fatalny w fiberze albo w `Runnable` na WSTP | cats-effect wypisuje go sam, kończy `Future` błędem i zamyka runtime; woła hook z `IORuntimeBuilder.addShutdownHook`, a domyślny handler wątków nie jest wołany |
| `IORuntime` z zewnętrznym `ForkJoinPool` (wzór `OptimusRuntime`) | traci timery i odpytywanie I/O w wątkach roboczych, `IO.blocking` w miejscu i kolejki pod fibery; nie robimy tak |

## Decyzja

- W aplikacji jest jedna pula obliczeniowa wielkości około liczby rdzeni i nigdy więcej niż jedna. Kod blokujący nie trafia na nią nigdy.
- `concurrency` ma dwa typy wykonawców ze wspólnym `ManagedExecutor`:
  - `TaskExecutor` to pula obliczeniowa: `ForkJoinPool` w trybie asynchronicznym z `coilix.concurrency.compute.threads` (domyślnie `availableProcessors`, czyli limit CPU kontenera) wątkami platformowymi `coilix-compute-*`.
  - `BlockingExecutor` służy do wywołań, które czekają: wątek wirtualny na zadanie (JDK 21+), a na JDK 17 albo z `blocking.virtual-threads = false` pula najwyżej `blocking.platform-threads` (64) wątków platformowych. Opcjonalny limit `blocking.max-concurrency` to semafor, na który zadanie czeka na własnym wątku, a nie na wątku obliczeniowym.
- `@Async(blocking = true)`, `@EventListener(async = true, blocking = true)` i `ApplicationEventListener.blocking` wybierają `BlockingExecutor`. Domyślnie jest pula obliczeniowa.
- Pulą obliczeniową zarządza najbardziej wymagający runtime w aplikacji, a pozostali z niej korzystają. Domyślne beany `taskExecutor` i `blockingExecutor` mają `@ConditionalOnMissingBean(TaskExecutor)` i `@ConditionalOnMissingBean(BlockingExecutor)`, więc integracja, która dostarcza własny wykonawca, wyłącza domyślny i jego pula w ogóle nie powstaje. Żeby bean domyślny mógł mieć konkretny typ z `@PreDestroy`, a ustępować każdemu beanowi szerszego typu, `@ConditionalOnMissingBean` dostaje atrybut `value` z typami do sprawdzenia.

| Aplikacja | Pula obliczeniowa | Blokowanie | Timery |
|---|---|---|---|
| bez integracji | `ForkJoinPool` Coilix | `BlockingExecutor` | `TaskScheduler` (1 wątek, tylko wyzwala) |
| tylko cats-effect | WSTP z `IORuntime` z domyślnego buildera; moduł rejestruje `TaskExecutor` na `runtime.compute` | `IO.blocking` w miejscu; kod poza `IO` na `BlockingExecutor`; masowe czekanie z `IO` przez `evalOn` na `BlockingExecutor` | timery WSTP |
| tylko Pekko | `ForkJoinPool` Coilix jako `defaultExecutionContext` `ActorSystem`; `internal-dispatcher` wskazuje na domyślny | `default-blocking-io-dispatcher` przez `ExecutorServiceConfigurator` na `BlockingExecutor` | scheduler Pekko + `TaskScheduler`, oba śpią |
| cats-effect i Pekko | WSTP dla `IO`, Pekko i `TaskExecutor` | jak wyżej | jak wyżej |

- Moduł Pekko nigdy nie dostarcza puli, tylko korzysta z `TaskExecutor` i `BlockingExecutor`, więc nie zależy od modułu cats-effect, a działa na WSTP, gdy ten jest obecny. Kolejność zamykania wynika z zależności beanów: `ActorSystem` przed `IORuntime` albo pulą Coilix.
- Moduł cats-effect łapie błąd fatalny przez hook zamknięcia runtime'u: zamknięcie, którego nie zlecił kontekst, przekazuje do `FatalErrorHandler` (ADR 0027).
- Metody `@Scheduled` nadal działają na wątku harmonogramu. Długa praca przekazuje się wykonawcy, co opisuje przewodnik.

## Konsekwencje

- Aplikacja w każdej konfiguracji ma jedną pulę CPU, a przejścia między `IO`, Pekko i zadaniami Coilix zostają w tej samej puli i zwykle na tym samym wątku.
- Pula obliczeniowa jest platformowa, więc monitor wątków (COILIX-00030 krok 2) widzi ją w każdej konfiguracji. Dla wątków wirtualnych `BlockingExecutor` zostaje licznik długich zadań.
- Zmiana niezgodna przed `core-v0.1.0`: `coilix.concurrency.executor.*` zastępują `coilix.concurrency.compute.*` i `coilix.concurrency.blocking.*`, wątki nazywają się `coilix-compute-*` i `coilix-blocking-*`, `ExecutorSettings` znika, `DefaultEventBus` dostaje `BlockingExecutor`.
- Nośniki wątków wirtualnych to osobna pula wielkości liczby rdzeni. Jest tania tylko wtedy, gdy zadania wirtualne naprawdę czekają, dlatego obliczenia na `BlockingExecutor` są błędem użycia.
- `IORuntime` z obcej puli w `OptimusRuntime` był błędem i nie jest powtarzany.
