# 0027: Błąd fatalny wyłącza aplikację

- Status: proponowana
- Data: 2026-11-22
- Task: COILIX-00030 (krok 1)

## Kontekst

`TaskExecutor`, `TaskScheduler` i odbiorcy zdarzeń łapały tylko `NonFatal`. `OutOfMemoryError` albo `StackOverflowError` w zadaniu zabijał wątek, JVM wypisywała go na stderr, a aplikacja działała dalej z okaleczonymi pulami: odpowiadała na sondy, ale części pracy już nie robiła. Wzorem jest obsługa nieobsłużonych wyjątków puli z `OptimusRuntime` w `org.optimus:implicits` (rozróżnienie `NonFatal` i błędu fatalnego, wymienny callback). Kod jest napisany od nowa, a domyślny callback wyłącza aplikację zamiast nic nie robić.

## Decyzja

- Fatalne są `VirtualMachineError` i `LinkageError` (`FatalErrors.isFatal`). `InterruptedException` i `ControlThrowable`, które `NonFatal` też wyklucza, nie są fatalne i lecą dalej jak dotąd.
- `FatalErrors.guarded` opakowuje zadania wykonawcy i harmonogramu: `NonFatal` logowany, fatalny do `FatalErrorHandler`, reszta rzucana dalej. Fabryki wątków (`ThreadFactories.named`, `NamedThreadFactory`, także wątki wirtualne przez `Thread.Builder.uncaughtExceptionHandler`) dostają handler nieobsłużonych wyjątków, bo błąd fatalny z `submit` (`Future`) kończy wątek z pominięciem `guarded`.
- `FatalErrorHandler` jest beanem modułu `concurrency` (`fatalErrorHandler`), a nie komponentem z `ConfigPath`, bo potrzebuje kontekstu do zamknięcia. Własny handler aplikacji podmienia go przez `@Primary`, jak `TaskExecutor`. `coilix.fatal-errors.action` wybiera `ShutdownFatalErrorHandler` (domyślnie) albo `LoggingFatalErrorHandler`.
- `ShutdownFatalErrorHandler` przy pierwszym błędzie loguje, uruchamia demona `coilix-fatal-watchdog` i wątek `coilix-fatal-shutdown`, który zamyka kontekst i woła `ProcessTerminator.exit(exit-code)` (domyślnie 70). Zamykanie nie idzie z wątku, który padł, bo zamykanie wykonawcy czekałoby na ten wątek. Strażnik woła `halt`, gdy zamknięcie i wyjście nie skończą się w `shutdown-timeout` (domyślnie 15 s), bo `halt` nie odpala hooków i nie da się go zakleszczyć. Kolejne błędy fatalne są tylko logowane.
- `ProcessTerminator` jest beanem (`processTerminator`), żeby testy podmieniały wyjście z procesu przez `replaceBean`.
- Asynchroniczny odbiorca zdarzeń z błędem fatalnym zwalnia licznik trwających dostarczeń, bez ponowień, i przepuszcza błąd do wykonawcy. Inaczej zamykanie po błędzie czekałoby na dostarczenie, które się nie skończy.
- `RuntimeShutdownHookRegistrar` w `boot` ustawia handler kontekstu jako domyślny handler nieobsłużonych wyjątków procesu (`Thread.setDefaultUncaughtExceptionHandler`). Testkit nie rejestruje hooków, więc tego nie robi. Dodatkowo kontekst testkitu ma domyślnie `coilix.fatal-errors.action = log`, żeby błąd fatalny w teście nie kończył JVM. Test zamykania ustawia `shutdown` przez `withProperties` i podmienia `ProcessTerminator`.
- `Coilix.run` loguje na `WARN` opcje JVM, które osłabiają aplikację (`JvmMemoryOptions`): brak `-XX:+ExitOnOutOfMemoryError` i `-XX:+CrashOnOutOfMemoryError` oraz maksymalną stertę mniejszą niż połowa pamięci dostępnej dla JVM.

## Konsekwencje

- Aplikacja po błędzie fatalnym kończy się z kodem 70, a orkiestrator stawia nową. Błąd powtarzalny daje widoczny `CrashLoopBackOff` zamiast cichego zombie.
- `GracefulShutdown.logFailure` znika na rzecz `FatalErrors.guarded`. `ExecutorServiceTaskExecutor`, `ScheduledExecutorTaskScheduler` i ich fabryki dostają parametr `fatalErrors` z domyślnym `FatalErrorHandler.logOnly` dla wykonawców tworzonych poza kontekstem.
- Każda aplikacja ma trzy nowe beany: `fatalErrorSettings`, `processTerminator` i `fatalErrorHandler`.
- Biblioteka ustawiająca domyślny handler procesu to ingerencja w JVM, dlatego robi to tylko `Coilix.run`, a `action = log` ją łagodzi. Monitor wątków i zakleszczeń (krok 2) skorzysta z tego samego handlera.
