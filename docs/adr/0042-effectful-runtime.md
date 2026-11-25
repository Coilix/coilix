# 0042: Runtime Coilix na efektach, kontrakty w `IO`

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00031

## Kontekst

ADR 0019 ustalił, że rdzeń nie zna `IO`: kontener i kontrakty runtime'u są w stylu bezpośrednim, a moduł `cats-effect` dokłada warianty z `IO` (beany i zasoby w ADR 0032, cykl życia i runnery w ADR 0033, `CoilixIOApp` w ADR 0034). Na tej podstawie powstały kontrakty, które runtime wywołuje w kodzie aplikacji i które zwracają `Unit`: `process` procesorów (ADR 0037, 0041), odbiorcy zdarzeń (ADR 0025), metody `@Scheduled` i `@Async` (ADR 0020, 0022), `HealthIndicator.state` (ADR 0040). Ich implementacje opierają się na `synchronized`, `wait`/`notify` i `Atomic*`.

Opiekun ocenił 2026-11-25, że to wygląda jak Java, a nie Scala. W aplikacji asynchronicznej efekt idzie od punktu wejścia do samego dołu: endpoint, serwisy i repozytoria zwracają efekt. Kontrakt zwracający `Unit` zmusza taką aplikację do `unsafeRun` w środku procesora albo odbiorcy, a utrzymywanie każdego kontraktu w dwóch wersjach (bezpośredniej i `IO`) podwaja API na zawsze. Każda aplikacja i tak pracuje na jakimś efekcie, choćby `Future`. Bulldozer, który ma przejść na Coilix, działa na cats-effect.

Żadna grupa wydań rdzenia nie została jeszcze wydana, więc zmiany niezgodne nic teraz nie kosztują.

## Decyzja

- **Dwie warstwy.** Kontener (`common`, `annotations`, `config`, `context` i składanie kontekstu w `boot`) zostaje synchroniczny: składanie beanów przy starcie nie potrzebuje efektu, a beany z `IO` i `Resource` już tworzy interpreter planu (ADR 0032). Warstwa runtime, czyli wszystko, co działa po starcie i woła kod aplikacji (`concurrency`, `events`, `diagnostics`, `processing` i przyszłe `web` i `data`), dostaje kontrakty w efekcie.
- **Efektem jest `cats.effect.IO`.** Kontrakty runtime'u zwracają `IO`, np. `process(element, context): IO[Unit]`, `InputSource.poll(...): IO[List[Input[T]]]`, odbiorca zdarzeń `IO[Unit]`, `@Scheduled` i `@Async` zwracające `IO`, `HealthIndicator.state: IO[HealthState]`. Każdy kontrakt istnieje w jednej wersji.
- **Nie piszemy własnego efektu.** Fibery, anulowanie, bezpieczeństwo zasobów i scheduler to lata pracy, a ekosystem (http4s, fs2, doobie, skunk) mówi cats-effect. Własny typ wymagałby konwersji na każdej granicy.
- **Konkretne `IO`, nie `F[_]`.** Kontener rozróżnia beany po argumentach typów, a beany z niezwiązanym `F[_]` odrzuca (ADR 0019). Do tego kod aplikacji pisany przez `F[_]: Async` jest trudniejszy, a bulldozer używa `IO`. `F[_]` można dodać później, gdy pojawi się potrzeba.
- **Inne efekty przez adaptery.** `Future` przez `IO.fromFuture`, ZIO przez `zio-interop-cats`, kod blokujący przez `IO.blocking`. Pekko łączy most z ADR 0036 i COILIX-00029.
- **cats-effect w rdzeniu runtime'u.** `concurrency` zależy od cats-effect, a `IORuntime` zbudowany przez Coilix jest jedynym runtime'em aplikacji (ADR 0031 staje się zasadą, nie opcją). Moduł `23_cats_effect` rozpuszcza się w rdzeniu: interpreter planu, `CoilixIOApp` i adapter runnerów trafiają do `concurrency` i `boot`, a `testkit-cats-effect` do `testkit`. `Coilix.run` uruchamia aplikację przez ten sam runtime.
- **Implementacja runtime'u na prymitywach cats-effect.** Stan współdzielony to `Ref`, czekanie to `Deferred` i `Queue`, prace w tle to fibery pod `Supervisor`, zasoby to `Resource`. W modułach runtime'u nie używa się `synchronized`, `wait`/`notify`, `Atomic*` ani `Thread.sleep`. Wątki platformowe zostają tylko tam, gdzie muszą działać niezależnie od puli, np. wątek diagnostyki i strażnik błędów fatalnych.
- **Strumienie.** Czy źródła procesorów mają być `fs2.Stream`, rozstrzyga osobny ADR przy migracji `processing`. Do tego czasu kontrakty używają samego `IO`.

## Konsekwencje

- Częściowo zastąpione: ADR 0019 (zasada „rdzeń nie zna `IO`” zostaje tylko dla kontenera), 0020 i 0022 (`@Scheduled`, `@Async`), 0025 (odbiorcy zdarzeń), 0028 i 0031 (pule i runtime), 0033 i 0034 (warianty `IO` stają się jedynymi), 0037 i 0041 (styl procesorów), 0040 (wskaźniki zdrowia). Każdy krok migracji aktualizuje te ADR-y i przewodniki.
- Coilix przestaje wspierać styl bezpośredni w kontraktach runtime'u. Kod synchroniczny działa w `IO.delay`, a blokujący w `IO.blocking`.
- Aplikacja zawsze ma cats-effect na classpath, także gdy sama go nie używa.
- Migracja idzie od modułów najmniej związanych ze startem: `processing` (niewydany, w tym otwarty PR z krokiem 5), `events`, `concurrency` z `@Scheduled` i `@Async`, `diagnostics`, na końcu połączenie modułu `cats-effect` z rdzeniem i testkit. Plan jest w COILIX-00031. Nowe funkcje runtime'u powstają od razu w nowym modelu.
