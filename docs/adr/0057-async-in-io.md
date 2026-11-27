# 0057: `@Async` w `IO`

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 5, część 2)
- Zastępuje część: 0022 (metody `@Async` zwracające `Unit` albo `Future`, proxy zależne od `TaskExecutor`)

## Kontekst

Metoda `@Async` zwracała `Future` albo `Unit`, a proxy wysyłało jej wywołanie na `TaskExecutor` albo `BlockingExecutor`. Kod aplikacji jest w `IO` (ADR 0042, 0043), więc wywołujący opakowywał wynik w `IO.fromFuture`, a metoda `Unit` uruchamiała pracę poza fiberem, bez anulowania przy zamykaniu. `@Scheduled` i szyna zdarzeń działają już na fiberach (ADR 0055, 0056).

## Decyzja

- **Metoda `@Async` zwraca `IO`.** Wywołanie przez proxy zwraca `IO`, które woła bean dopiero przy wykonaniu: na puli obliczeniowej (po `IO.cede`) albo, dla `blocking = true`, przez `evalOn` na `BlockingExecutor`. Na wybranej puli działa więc także kod, który buduje efekt metody, np. blokujące przygotowanie przed `IO.pure`.
- **`IO[A]` oddaje wynik, `IO[Unit]` startuje w tle.** Procesor definicji rozpoznaje `IO[Unit]` po sygnaturze generycznej metody (`IO<BoxedUnit>`). Taka metoda startuje na własnym fiberze, a `IO` wywołania kończy się od razu, tak jak wcześniej metoda `Unit`. Inny wynik dostaje wywołujący, który na niego czeka.
- **Fibery w `AsyncTasks`.** Nowy bean trzyma fibery metod w tle w `Ref.unsafe` (jak `ScheduledMethodRegistrar`, ADR 0056), zapisuje ich błędy w logu, usuwa fiber po zakończeniu i anuluje pozostałe w fazie `Producers`. Wywołanie po rozpoczęciu zamykania jest pomijane z ostrzeżeniem w logu.
- **Proxy bez `TaskExecutor`.** Fabryka proxy dostaje `BlockingExecutor` i `AsyncTasks`. Pula obliczeniowa to runtime, który wykonuje `IO`. `AsyncProxyFactory` i `AsyncInvocationHandler` są `case class`, a `AsyncMethod` opisuje metodę (etykieta, `blocking`, praca w tle).
- Metoda, która nie zwraca `IO`, zatrzymuje start wyjątkiem `InvalidAsyncMethodException`.

## Konsekwencje

- Wywołujący składa wynik metody `@Async` z innym `IO` bez `IO.fromFuture`, a metody w tle kończą się razem z kontekstem.
- MDC z `coilix-logging` przechodzi przez `IOLocal` (ADR 0054), więc metoda w tle i metoda na puli blokującej logują z MDC wywołującego.
- Zmiana niezgodna: metody `@Async` zwracające `Unit` albo `Future` trzeba przepisać na `IO[Unit]` albo `IO[A]`, a wywołania z `Future` na składanie `IO`.
