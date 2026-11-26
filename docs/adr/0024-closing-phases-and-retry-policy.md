# 0024: Fazy zamykania kontekstu i wspólna polityka ponowień

- Status: proponowana, częściowo zastąpiona przez [0047](0047-lifecycle-listeners-in-io.md) (metody słuchacza w `IO`)
- Data: 2026-11-20
- Task: COILIX-00016 (krok 1)

## Kontekst

Moduł zdarzeń (COILIX-00016) musi przy zamykaniu opróżnić kolejki, zanim zostaną zniszczone beany, z których korzystają odbiorcy, i dopiero wtedy, gdy nic nie dokłada już nowych zdarzeń. `ContextLifecycleListener` z COILIX-00005 wywoływał `onContextClosing` w odwrotnej kolejności tworzenia. Kolejność zatrzymania `@Scheduled`, opróżniania zdarzeń i zamykania zasobów zależałaby więc od tego, który bean powstał wcześniej, czyli od grafu zależności, a nie od roli beana.

Zdarzenia i procesory (COILIX-00026) ponawiają nieudaną pracę. Dwie osobne polityki ponowień dałyby dwa zestawy kluczy configu i dwa zachowania.

## Decyzja

- Rdzeń kontekstu dostaje `ClosingPhase` z fazami `Producers`, `Events` i `Resources`. `ContextLifecycleListener.closingPhase` (domyślnie `Producers`) wskazuje fazę słuchacza. `DefaultApplicationContext.close()` wywołuje słuchaczy faza po fazie, w obrębie fazy w odwrotnej kolejności tworzenia, a `BeanDestroyer` (`@PreDestroy`) działa po ostatniej fazie.
- Domyślną fazą są producenci, bo słuchacz zwykle zatrzymuje pracę, którą sam uruchomił, a ta musi stanąć przed opróżnianiem i zwalnianiem zasobów.
- `ScheduledMethodRegistrar` deklaruje fazę `Producers` jawnie, bo zatrzymanie `@Scheduled` przed opróżnianiem zdarzeń jest częścią kontraktu modułu zdarzeń.
- `RetryPolicy` w module `concurrency` to niemutowalny model: liczba prób, opóźnienie początkowe, mnożnik, limit opóźnienia i rozrzut. Tylko liczy opóźnienia (`delayAfter`) i nie uruchamia zadań, więc zdarzenia, procesory i kod aplikacji planują ponowienia własnymi środkami, np. przez `TaskScheduler`.

## Konsekwencje

- Kolejność zamykania wynika z roli słuchacza, a nie z kolejności tworzenia. Słuchacz, który powinien działać po opróżnieniu zdarzeń, musi wskazać fazę `Resources`.
- Wykonawca i harmonogram nadal zamykają się przez `@PreDestroy`, czyli po wszystkich fazach. Opróżnianie zdarzeń może więc korzystać z wykonawcy.
- Faza `Events` jest w rdzeniu, choć moduł zdarzeń jeszcze nie istnieje. Korzystają z niej też aplikacje z własnymi kolejkami (outbox, bufory).
- Zmiana `ContextLifecycleListener` jest zgodna wstecz w źródle, a API nie było jeszcze wydane, więc nie wymaga wpisu **BREAKING**.
