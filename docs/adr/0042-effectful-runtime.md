# 0042: Runtime Coilix na efektach w stylu tagless final

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00031

## Kontekst

ADR 0019 ustalił, że rdzeń nie zna `IO`: kontener i kontrakty runtime'u są w stylu bezpośrednim, a moduł `cats-effect` dokłada warianty z `IO` (beany i zasoby w ADR 0032, cykl życia i runnery w ADR 0033, `CoilixIOApp` w ADR 0034). Na tej podstawie powstały kontrakty, które runtime wywołuje w kodzie aplikacji i które zwracają `Unit`: `process` procesorów (ADR 0037, 0041), odbiorcy zdarzeń (ADR 0025), metody `@Scheduled` i `@Async` (ADR 0020, 0022), `HealthIndicator.state` (ADR 0040). Ich implementacje opierają się na `synchronized`, `wait`/`notify` i `Atomic*`.

Opiekun ocenił 2026-11-25, że to wygląda jak Java, a nie Scala. W aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu: endpoint, serwisy i repozytoria zwracają efekt. Kontrakt zwracający `Unit` zmusza taką aplikację do `unsafeRun` w środku procesora albo odbiorcy, a utrzymywanie każdego kontraktu w dwóch wersjach (bezpośredniej i `IO`) podwaja API na zawsze. Każda aplikacja i tak pracuje na jakimś efekcie. Opiekun chce „Springa na `IO`” w stylu tagless final, a bulldozer, który ma przejść na Coilix, działa na cats-effect.

Żadna grupa wydań rdzenia nie została jeszcze wydana, więc zmiany niezgodne nic teraz nie kosztują.

## Decyzja

- **Dwie warstwy.** Kontener (`common`, `annotations`, `config`, `context` i składanie kontekstu w `boot`) zostaje synchroniczny: składanie beanów przy starcie nie potrzebuje efektu, a beany z efektu i `Resource` tworzy interpreter planu (ADR 0032). Warstwa runtime, czyli wszystko, co działa po starcie i woła kod aplikacji (`concurrency`, `events`, `diagnostics`, `processing` i przyszłe `web` i `data`), dostaje kontrakty w efekcie.
- **Tagless final.** Kontrakty runtime'u są polimorficzne w `F[_]`, a wymagania wyrażają klasy typów cats-effect: `StatelessProcessor[F[_], T]` z `process(element, context): F[Unit]`, `InputSource[F[_], T]` z `poll(...): F[List[Input[T]]]`, metoda `@EventListener` i `@Scheduled` zwracająca `F[Unit]`, `HealthIndicator[F[_]]` ze `state: F[HealthState]`. Kod aplikacji pisze się tak samo: `final class OrderService[F[_]: Async](repo: OrderRepository[F])`.
- **Efekt aplikacji wybiera się raz.** Obiekt aplikacji deklaruje go typem: `object Shop extends CoilixApp[IO]`. Kontener wiąże z nim każdy niezwiązany parametr typu `F[_]` klasy komponentu i metody `@Bean`, więc `OrderService[F]` staje się beanem `OrderService[IO]`, a `Repo[F]` w zależności oznacza `Repo[IO]`. Zmienia to zasadę z ADR 0019, która odrzucała takie zależności.
- **Klasy typów jako beany.** Kontener rejestruje instancję `Async[F]` efektu aplikacji jako bean. Context bound to parametr konstruktora (`using Async[F]`), więc po podstawieniu zależność `Sync[IO]`, `Temporal[IO]` albo `Concurrent[IO]` znajduje bean `Async[IO]`, bo te klasy są jego nadtypami. Moduły dokładają własne instancje, np. logger w `F`.
- **Uruchamianie.** Pierwszym efektem aplikacji jest `cats.effect.IO`, uruchamiany przez `IORuntime` zbudowany przez Coilix (ADR 0031 staje się zasadą, nie opcją). Inny efekt z instancją `Async`, np. ZIO przez `zio-interop-cats`, wymaga runnera, który przyjdzie osobnym modułem. Moduł `23_cats_effect` rozpuszcza się w rdzeniu: interpreter planu, `CoilixApp` i adapter runnerów trafiają do `concurrency` i `boot`, a `testkit-cats-effect` do `testkit`. `Coilix.run` uruchamia aplikację przez ten sam runtime.
- **Nie piszemy własnego efektu.** Fibery, anulowanie, bezpieczeństwo zasobów i scheduler to lata pracy, a ekosystem (http4s, fs2, doobie, skunk) mówi cats-effect. Własny typ wymagałby konwersji na każdej granicy.
- **Inne światy przez klasy typów.** `Future` przez `Async[F].fromFuture`, więc most do Pekko (ADR 0036, COILIX-00029) jest prosty. Kod blokujący przez `Sync[F].blocking`. Biblioteki Javy z callbackami na własnych wątkach przez `Async[F].async` albo `Dispatcher[F]`.
- **Implementacja runtime'u na prymitywach cats-effect, polimorficznie w `F`.** Stan współdzielony to `Ref[F, _]`, czekanie to `Deferred` i `Queue`, prace w tle to fibery pod `Supervisor`, zasoby to `Resource[F, _]`. W modułach runtime'u nie używa się `synchronized`, `wait`/`notify`, `Atomic*` ani `Thread.sleep`. Wątki platformowe zostają tylko tam, gdzie muszą działać niezależnie od puli, np. wątek diagnostyki i strażnik błędów fatalnych.
- **Strumienie.** Czy źródła procesorów mają być `fs2.Stream[F, _]`, rozstrzyga osobny ADR przy migracji `processing`. Do tego czasu kontrakty używają samego `F`.

## Konsekwencje

- Częściowo zastąpione: ADR 0019 (zasada „rdzeń nie zna efektów” zostaje tylko dla kontenera, a niezwiązane `F[_]` jest wiązane z efektem aplikacji), 0020 i 0022 (`@Scheduled`, `@Async`), 0025 (odbiorcy zdarzeń), 0028 i 0031 (pule i runtime), 0032–0034 (warianty `IO` stają się jedynymi i polimorficznymi), 0037 i 0041 (styl procesorów), 0040 (wskaźniki zdrowia). Każdy krok migracji aktualizuje te ADR-y i przewodniki.
- Coilix przestaje wspierać styl bezpośredni w kontraktach runtime'u. Kod synchroniczny działa w `Sync[F].delay`, a blokujący w `Sync[F].blocking`.
- Aplikacja zawsze ma cats-effect na classpath.
- Kontrakty i wnętrze runtime'u są trudniejsze do napisania i do czytania niż wersja z samym `IO`: każdy typ niesie `F`, a wnętrze potrzebuje klas typów zamiast metod `IO`. W zamian kod aplikacji nie jest przypięty do `IO`, a testy mogą podstawić inny efekt.
- Migracja zaczyna się od kontenera (wiązanie `F` z efektem aplikacji, klasy typów jako beany), potem idzie od modułów najmniej związanych ze startem: `processing` (niewydany, w tym otwarty PR z krokiem 5), `events`, `concurrency` z `@Scheduled` i `@Async`, `diagnostics`, na końcu połączenie modułu `cats-effect` z rdzeniem i testkit. Plan jest w COILIX-00031. Nowe funkcje runtime'u powstają od razu w nowym modelu.
