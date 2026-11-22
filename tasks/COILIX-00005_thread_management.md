# COILIX-00005: Zarządzanie wątkami

## Cel

Framework zarządza wątkami za aplikację, tak jak Spring z `TaskExecutor`, `@Async` i `@Scheduled`:

- pule wątków są beanami tworzonymi i zamykanymi przez kontekst,
- metodę beana można uruchomić asynchronicznie adnotacją,
- zadania cykliczne deklaruje się adnotacją na metodzie,
- zamknięcie kontekstu czeka na zakończenie zadań (graceful shutdown).

Aktualizacja 2026-11-22 (ADR 0028): domyślny `TaskExecutor` to pula obliczeniowa (`ForkJoinPool`, wątek na rdzeń), a wątki wirtualne są w osobnym `BlockingExecutor` dla kodu, który czeka (`@Async(blocking = true)`, `@EventListener(async = true, blocking = true)`). Domyślne wykonawcy ustępują przez `@ConditionalOnMissingBean` wykonawcom z integracji (cats-effect, Pekko), żeby aplikacja miała zawsze jedną pulę CPU.

## Stan obecny

- Brak jakiejkolwiek obsługi wątków. Kontekst budowany jest jednowątkowo.
- Rejestr beanów jest niemutowalny, więc odczyt z kontekstu z wielu wątków jest już bezpieczny.
- Mechanizmy potrzebne do sprzątania (`close()`, `@PreDestroy`, `BeanPostProcessor`) dochodzą w taskach COILIX-00002 i COILIX-00004.

## Zakres

1. Abstrakcja wykonawcy:
   - `TaskExecutor` (trait) z `execute` i `submit`, zwracającym `Future[T]`,
   - implementacja na wątkach wirtualnych (domyślna),
   - implementacja na stałej puli wątków platformowych (np. do zadań CPU),
   - nazwane wątki (`coilix-task-1`, ...).
2. `TaskScheduler` z zadaniami `fixedRate`, `fixedDelay` i `initialDelay`. Cron to osobny krok w tym samym tasku albo kolejny task.
3. `@Async` na metodzie beana:
   - wywołanie idzie do `TaskExecutor`,
   - metoda zwracająca `Future[T]` daje wynik z wykonawcy, a metoda zwracająca `Unit` jest odpalana i zapominana,
   - realizowane przez proxy JDK (`java.lang.reflect.Proxy`) na traicie beana. Zasada „najpierw interfejs” sprawia, że każdy bean ma trait, więc proxy jest wykonalne.
4. `@Scheduled(fixedRate = ..., fixedDelay = ..., initialDelay = ...)` na bezargumentowej metodzie beana, rejestrowane po starcie kontekstu.
5. Kontrola przez konfigurację beanów: aplikacja może zarejestrować własny `TaskExecutor` jako bean `@Primary`.
6. Graceful shutdown przy `close()` kontekstu:
   - zatrzymanie schedulera,
   - zamknięcie wykonawców z limitem czasu,
   - zalogowanie zadań, które się nie zakończyły.
7. Przekazywanie kontekstu logowania (MDC) do zadań asynchronicznych.

## Proponowany projekt

Nowy moduł rdzenia `concurrency` (`dev.coilix.concurrency`), zależny od `context`, w katalogu `06_concurrency` (ADR 0018, ADR 0020), bez przenumerowania innych modułów.

Alternatywa: wątki w `05_context`. Odradzane, bo kontekst rośnie.

Komponenty:

- `TaskExecutor`, `VirtualThreadTaskExecutor`, `FixedThreadPoolTaskExecutor`, `NamedThreadFactory`,
- `TaskScheduler`, `ScheduledExecutorTaskScheduler`, `ScheduledTask` (uchwyt do anulowania),
- `AsyncProxyFactory` (trait) i `JdkAsyncProxyFactory`,
- `AsyncBeanPostProcessor`, który podmienia bean na proxy (korzysta z `BeanPostProcessor` z taska COILIX-00004),
- `ScheduledMethodRegistrar`, który po starcie kontekstu rejestruje metody `@Scheduled`,
- `ExecutorShutdown` z `@PreDestroy`, który zamyka wykonawców.

Adnotacje `@Async` i `@Scheduled` w `02_annotations`.

## Kroki (osobne PR-y)

1. Moduł `06_concurrency`: `TaskExecutor` na wątkach wirtualnych i na stałej puli, `TaskScheduler` z `fixedRate`, `fixedDelay` i `initialDelay`, ustawienia `coilix.concurrency`, zamykanie z limitem czasu, własny wykonawca przez `@Primary`. Zrobione (ADR 0020). Kryteria 3 i 4.
2. `@Async` przez proxy JDK z błędem startu dla beana bez traitu. Kryteria 1 i 5. Zrobione przez `AsyncDefinitionProcessor` i `CoilixModule.definitionProcessors` zamiast `BeanPostProcessor`, bo proxy zmienia typ beana jeszcze przed planowaniem (ADR 0022).
3. `@Scheduled` (`ScheduledMethodRegistrar`) i demo z nazwami wątków w logach. Kryteria 2 i 6. Zrobione przed krokiem 2, razem z `ContextLifecycleListener` w rdzeniu kontekstu (ADR 0020).
4. Przekazywanie MDC do zadań. Zrobione (ADR 0029): `ContextPropagator` z domyślnym `MdcContextPropagator`, propagacja w obu wykonawcach i harmonogramie.

## Kryteria akceptacji

1. Metoda `@Async` zwracająca `Future[T]` wykonuje się na wątku z puli frameworka. Test sprawdza nazwę wątku i wynik.
2. `@Scheduled(fixedRate = ...)` wykonuje się cyklicznie. Test na kontrolowanym zegarze albo z krótkim interwałem i limitem czasu.
3. `close()` kontekstu czeka na trwające zadania do limitu czasu, a potem je przerywa i loguje.
4. Własny `TaskExecutor` zarejestrowany jako `@Primary` zastępuje domyślny.
5. Wywołanie metody `@Async` na beanie bez traitu daje czytelny błąd przy starcie, a nie ciche wykonanie synchroniczne.
6. Demo ma przykładową metodę `@Async` i zadanie `@Scheduled`, widoczne w logach razem z nazwami wątków.

## Poza zakresem

- Reaktywne strumienie i integracja z bibliotekami efektów (cats-effect, ZIO).
- Transakcje i zakresy beanów powiązane z wątkiem.
- Rozproszone harmonogramy (blokady między instancjami).

## Ryzyka i otwarte pytania

- Pule wątków i schedulery są z natury mutowalne. Zgodnie z `CLAUDE.md` to dopuszczalny wyjątek, ale stan powinien być zamknięty w implementacjach JDK, bez własnych `var`.
- `StructuredTaskScope` jest w JDK 25 nadal w preview. Nie używamy go, dopóki nie będzie stabilny, natomiast `ScopedValue` jest już stabilne i może zastąpić `ThreadLocal` przy przekazywaniu kontekstu.
- Proxy JDK działa tylko na interfejsach. Wywołania `@Async` z wnętrza tego samego beana (`this.metoda()`) ominą proxy, tak jak w Springu. Trzeba to opisać i przetestować.
- Wątki wirtualne są od JDK 21, a artefakty są dla JDK 17, więc tworzy się je przez refleksję z powrotem do wątków platformowych (ADR 0020).
