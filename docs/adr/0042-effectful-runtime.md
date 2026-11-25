# 0042: Runtime Coilix na `IO`, komponenty aplikacji w tagless final

- Status: proponowana; punkty o tagless final, klasach typów jako beanach i strumieniach zastąpione przez 0043
- Data: 2026-11-25
- Task: COILIX-00031

## Kontekst

ADR 0019 ustalił, że rdzeń nie zna `IO`: kontener i kontrakty runtime'u są w stylu bezpośrednim, a moduł `cats-effect` dokłada warianty z `IO` (beany i zasoby w ADR 0032, cykl życia i runnery w ADR 0033, `CoilixIOApp` w ADR 0034). Na tej podstawie powstały kontrakty, które runtime wywołuje w kodzie aplikacji i które zwracają `Unit`: `process` procesorów (ADR 0037, 0041), odbiorcy zdarzeń (ADR 0025), metody `@Scheduled` i `@Async` (ADR 0020, 0022), `HealthIndicator.state` (ADR 0040). Ich implementacje opierają się na `synchronized`, `wait`/`notify` i `Atomic*`.

Opiekun ocenił 2026-11-25, że to wygląda jak Java, a nie Scala. W aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu: endpoint, serwisy i repozytoria zwracają efekt. Kontrakt zwracający `Unit` zmusza taką aplikację do `unsafeRun` w środku procesora albo odbiorcy, a utrzymywanie każdego kontraktu w dwóch wersjach (bezpośredniej i `IO`) podwaja API na zawsze. Bulldozer, który ma przejść na Coilix, działa na cats-effect.

Rozważony był pełny tagless final także we frameworku (kontrakty `StatelessProcessor[F, T]`, wnętrze polimorficzne w `F`). Kontrakty Coilix to jednak brzegi aplikacji: procesor, odbiorca zdarzeń, harmonogram, przyszły endpoint. W architekturze tagless final efekt wybiera się właśnie na brzegach, więc kontrakty w `IO` nie przeszkadzają komponentom aplikacji pisanym w `F[_]`, a framework nie płaci za `F` w każdym typie i klasy typów zamiast metod `IO`. Opiekun wybrał wariant „aplikacja na `IO`, komponenty w tagless final”.

Żadna grupa wydań rdzenia nie została jeszcze wydana, więc zmiany niezgodne nic teraz nie kosztują.

## Decyzja

- **Dwie warstwy.** Kontener (`common`, `annotations`, `config`, `context` i składanie kontekstu w `boot`) zostaje synchroniczny: składanie beanów przy starcie nie potrzebuje efektu, a beany z `IO` i `Resource` tworzy interpreter planu (ADR 0032). Warstwa runtime, czyli wszystko, co działa po starcie i woła kod aplikacji (`concurrency`, `events`, `diagnostics`, `processing` i przyszłe `web` i `data`), dostaje kontrakty w efekcie.
- **Kontrakty frameworka w `cats.effect.IO`.** Na przykład `process(element, context): IO[Unit]`, `InputSource.poll(...): IO[List[Input[T]]]`, metoda `@EventListener` i `@Scheduled` zwracająca `IO[Unit]`, `HealthIndicator.state: IO[HealthState]`. Każdy kontrakt istnieje w jednej wersji, a wnętrze runtime'u też jest napisane na `IO`.
- **Komponenty aplikacji mogą być w tagless final.** Kontener wiąże niezwiązany parametr typu `F[_]` klasy komponentu i metody `@Bean` z `IO`, więc `final class OrderService[F[_]: Async](repo: OrderRepository[F])` jest beanem `OrderService[IO]` bez metody `@Bean`, a zależność `OrderRepository[F]` oznacza `OrderRepository[IO]`. Zmienia to zasadę z ADR 0019, która odrzucała takie zależności. Komponent, który implementuje kontrakt frameworka (procesor, odbiorca), jest brzegiem i używa `IO`, np. procesor wstrzykuje `OrderService[IO]`.
- **Klasy typów jako beany.** Kontener rejestruje `Async[IO]` jako bean. Context bound to parametr konstruktora (`using Async[F]`), więc po związaniu `F` z `IO` zależność `Sync[IO]`, `Temporal[IO]` albo `Concurrent[IO]` znajduje bean `Async[IO]`, bo te klasy są jego nadtypami. Moduły mogą dokładać własne instancje, np. logger w `IO`.
- **Jeden runtime.** `IORuntime` zbudowany przez Coilix jest jedynym runtime'em aplikacji (ADR 0031 staje się zasadą, nie opcją). Moduł `23_cats_effect` rozpuszcza się w rdzeniu: interpreter planu, `CoilixIOApp` i adapter runnerów trafiają do `concurrency` i `boot`, a `testkit-cats-effect` do `testkit`. `Coilix.run` uruchamia aplikację przez ten sam runtime.
- **Nie piszemy własnego efektu.** Fibery, anulowanie, bezpieczeństwo zasobów i scheduler to lata pracy, a ekosystem (http4s, fs2, doobie, skunk) mówi cats-effect. Własny typ wymagałby konwersji na każdej granicy.
- **Inne światy przez `IO`.** `Future` przez `IO.fromFuture`, więc most do Pekko (ADR 0036, COILIX-00029) jest prosty. Kod blokujący przez `IO.blocking`. Biblioteki Javy z callbackami na własnych wątkach przez `IO.async` albo `Dispatcher[IO]`. Uruchomienie frameworka na innym efekcie (np. ZIO) nie jest celem; ZIO łączy się przez `zio-interop-cats`.
- **Implementacja runtime'u na prymitywach cats-effect.** Stan współdzielony to `Ref`, czekanie to `Deferred` i `Queue`, prace w tle to fibery pod `Supervisor`, zasoby to `Resource`. W modułach runtime'u nie używa się `synchronized`, `wait`/`notify`, `Atomic*` ani `Thread.sleep`. Wątki platformowe zostają tylko tam, gdzie muszą działać niezależnie od puli, np. wątek diagnostyki i strażnik błędów fatalnych.
- **Strumienie.** Czy źródła procesorów mają być `fs2.Stream[IO, _]`, rozstrzyga osobny ADR przy migracji `processing`. Do tego czasu kontrakty używają samego `IO`.

## Konsekwencje

- Częściowo zastąpione: ADR 0019 (zasada „rdzeń nie zna `IO`” zostaje tylko dla kontenera, a niezwiązane `F[_]` jest wiązane z `IO`), 0020 i 0022 (`@Scheduled`, `@Async`), 0025 (odbiorcy zdarzeń), 0028 i 0031 (pule i runtime), 0032–0034 (warianty `IO` stają się jedynymi), 0037 i 0041 (styl procesorów), 0040 (wskaźniki zdrowia). Każdy krok migracji aktualizuje te ADR-y i przewodniki.
- Coilix przestaje wspierać styl bezpośredni w kontraktach runtime'u. Kod synchroniczny działa w `IO.delay`, a blokujący w `IO.blocking`.
- Aplikacja zawsze ma cats-effect na classpath.
- Aplikacja może pisać serwisy i repozytoria w tagless final i testować je z innym efektem, a framework zostaje prosty. Jeśli kiedyś trzeba będzie uruchamiać framework na innym efekcie, kontrakty dostaną `F` w osobnej decyzji.
- Migracja zaczyna się od kontenera (wiązanie `F[_]` z `IO`, `Async[IO]` jako bean), potem idzie od modułów najmniej związanych ze startem: `processing` (niewydany, w tym otwarty PR z krokiem 5), `events`, `concurrency` z `@Scheduled` i `@Async`, `diagnostics`, na końcu połączenie modułu `cats-effect` z rdzeniem i testkit. Plan jest w COILIX-00031. Nowe funkcje runtime'u powstają od razu w nowym modelu.
