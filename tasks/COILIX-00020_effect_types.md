# COILIX-00020: Wsparcie efektów `F[_]` (cats-effect, ZIO)

## Cel

Aplikacja napisana w stylu efektowym (cats-effect, ZIO, tagless final z `F[_]`) może używać Coilix do składania komponentów, bez rezygnowania z kontroli nad efektami i zasobami.

Ten task jest najpierw analizą. Wynikiem ma być ADR z decyzją, co i w jakiej formie Coilix wspiera, a dopiero potem implementacja w osobnych taskach.

## Stan obecny

- Kontener jest synchroniczny. Bean powstaje przez wywołanie konstruktora albo metody `@Bean`, a wynik trafia do kontekstu od razu.
- Tworzenie beana nie może być efektem (`IO[A]`, `ZIO[R, E, A]`), a zasób z cyklem życia (`Resource[F, A]`, `ZLayer`, `Scope`) nie ma odpowiednika. Zamykanie kontekstu i `@PreDestroy` dopiero powstaną w COILIX-00004.
- `ApplicationRunner.run` zwraca `Unit`, więc logika startowa w `F` musi być uruchomiona ręcznie (`unsafeRunSync`).
- Zależności są rozpoznawane przez refleksję w runtime ([ADR 0002](../docs/adr/0002-runtime-reflection-and-java-annotations.md)). Nie jest sprawdzone, co JVM widzi z typów wyższego rzędu, np. czy `UserRepository[IO]` da się odróżnić od `UserRepository[Task]` po wymazaniu typów.

## Zakres analizy

1. Jak wygląda typowa aplikacja cats-effect i ZIO, która składa komponenty ręcznie, i co Coilix może w niej uprościć.
2. Beany tworzone efektem: metoda `@Bean` zwracająca `F[A]` albo `Resource[F, A]`. Zasoby otwierane w kolejności zależności i zamykane w odwrotnej.
3. Start i zamykanie aplikacji w `F`: odpowiednik `IOApp` / `ZIOAppDefault`, runner zwracający `F[Unit]`, zamknięcie kontekstu jako finalizer.
4. Jeden typ efektu na aplikację (np. tylko `IO`) czy dowolne `F[_]` z instancjami typu `Sync[F]` jako beanami.
5. Rozpoznawanie zależności typu `Repository[F]` w runtime: co zostaje w sygnaturach generycznych Scali 3 i czy trzeba `TypeTag` / `Tag` (izumi-reflect) albo makr.
6. Czy część weryfikacji może przejść na czas kompilacji (makra Scali 3), co pasuje do oczekiwań użytkowników FP.
7. Przegląd istniejących rozwiązań (distage, macwire, ZLayer) tylko po to, żeby znaleźć miejsce dla Coilix. Bez kopiowania kodu, dokumentacji ani API.

## Proponowany kierunek (do potwierdzenia w ADR)

- Rdzeń (`common`, `config`, `context`, `boot`) zostaje bez zależności od bibliotek efektów, zgodnie z [modularity.md](../docs/architecture/modularity.md).
- Integracje jako opcjonalne moduły funkcji, np. `coilix-cats-effect` i `coilix-zio`, podłączane przez `CoilixModule` (COILIX-00010).
- Rdzeń dostaje tylko ogólne punkty rozszerzeń, których potrzebują integracje: asynchroniczne tworzenie beana, rejestracja finalizatorów, własny sposób uruchomienia runnerów.

## Kryteria akceptacji

1. ADR z decyzją: zakres wsparcia, moduły, zmiany w rdzeniu i ich wpływ na publiczne API.
2. Prototyp na gałęzi: aplikacja cats-effect z beanem z `Resource[IO, A]`, która startuje i zamyka zasób w dobrej kolejności.
3. Taski implementacyjne z kryteriami akceptacji dla wybranego zakresu.

## Poza zakresem

- Implementacja modułów integracyjnych (osobne taski po ADR).
- Biblioteki efektów inne niż cats-effect i ZIO.

## Ryzyka i otwarte pytania

- Wymazywanie typów może wymusić dodatkowe informacje o typach (`Tag`) w API, co zmienia sposób deklarowania zależności.
- Punkty rozszerzeń dla efektów zmieniają kontrakt `BeanInstantiator` i `ApplicationRunner`. Najlepiej wprowadzić je przed `1.0`, żeby nie łamać API.
- Część społeczności FP woli składanie zależności w czasie kompilacji. Trzeba jasno opisać, kiedy Coilix ma sens, a kiedy nie.
- Zależności od cats-effect i ZIO mają licencję Apache-2.0, więc są dozwolone, ale tylko w modułach integracyjnych.
