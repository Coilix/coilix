# COILIX-00032: IO, Resource i fs2 zamiast Pekko i tagless final

> Decyzje opiekuna z 2026-11-25: flowy bulldozera da się napisać na fs2, więc Coilix rezygnuje z Pekko. Zatrzymanie procesora ma opróżniać przepływ, a nie anulować fiber. HTTP też na fs2. Coilix jest frameworkiem na frameworku, a typy `IO`, `Resource` i `fs2.Stream` są jawne, bez tagless final. Kierunek zapisuje ADR 0043.

## Cel

- API Coilix i komponenty aplikacji używają konkretnych `IO`, `Resource[IO, _]` i `fs2.Stream[IO, _]`.
- W Coilix nie ma Pekko ani kodu do wiązania `F[_]` z `IO`.
- Procesor oparty na strumieniu zatrzymuje się bezpiecznie: źródło przestaje dawać elementy, reszta przepływu przetwarza i zatwierdza to, co już przeczytano, a anulowanie przychodzi dopiero po limicie czasu.
- Flowy bulldozera (Kafka z ręcznym przydziałem partycji, paczki, zatwierdzanie, tick) przenoszą się na procesory Coilix bez obejść.

## Zakres

| Obszar | Dziś | Po zmianie |
|---|---|---|
| `context`, `boot`, `concurrency` | `EffectTypeBinding`, `CoilixModule.effectType`, bean `ioAsync: Async[IO]` | brak wiązania; niezwiązany `F[_]` w zależności to błąd startu (ADR 0019) |
| `25_pekko` | `ActorSystem` i `Materializer` jako beany | moduł usunięty |
| rdzeń | cats-effect | cats-effect i `fs2-core` |
| `processing` | silnik na wątkach wirtualnych, kontrakty `Unit` | `IO`, źródła jako `fs2.Stream[IO, _]`, shard jako fiber z opróżnianiem i `drain-timeout` (krok 3 COILIX-00031) |
| Kafka | brak | moduł funkcji na fs2-kafka i moduł integracyjny z procesorami |
| `21_web` | pusty | http4s z serwerem Ember |

## Kroki (osobne PR-y)

1. ADR 0043 i ten task.
2. Usunięcie wiązania `F[_]`: `EffectTypeBinding`, `CoilixModule.effectType`, `ConflictingEffectTypesException`, bean `ioAsync`, kontrola w planerze, testy komponentów w tagless final. Demo (`LabourRates`, `JobEstimates`) i przykłady na `IO`. Sekcja przewodnika cats-effect o tagless final znika. Zasady w `CLAUDE.md` (model efektów, przenoszenie kodu z bulldozera) i ADR 0042 dostają odwołanie do 0043.
3. Usunięcie `25_pekko`, grupy wydań `pekko`, przewodnika `pekko` w obu językach, przykładów i changeloga. `ModuleCensus` i `ServiceDesk` w demo liczą flotę strumieniem fs2. `fs2-core` dochodzi do rdzenia z licencją w `docs/legal/dependency-licenses.md`. COILIX-00029 dostaje status „zastąpiony”.
4. `processing` na `IO` z `fs2.Stream` (krok 3 COILIX-00031): procesor strumieniowy opisany jako źródło i `Pipe`, sygnał zatrzymania wstawiany przez Coilix za źródłem, opróżnianie z `drain-timeout`, restart z backoffem, statystyki w `Ref`. Testy pokazują, że elementy już pobrane ze źródła przechodzą przez przepływ przed zatrzymaniem, a zawieszony przepływ jest anulowany po limicie.
5. Moduł Kafki (fs2-kafka) i moduł integracyjny z procesorami: przydział partycji do shardów, zatwierdzanie paczkami, `stopConsuming` przy zatrzymaniu, odzyskanie offsetów przed startem (`recover` z bulldozera). Testy na kontenerze albo kliencie w pamięci.
6. `21_web` na http4s: trasy jako beany, serwer Ember w cyklu życia kontekstu, zamykanie z opróżnianiem otwartych żądań.

Każdy krok aktualizuje przewodniki w obu językach, demo, changelog i ADR-y, które zastępuje. Nowe klasy są `case class` według zasady z `CLAUDE.md`.

## Kryteria akceptacji

1. W kodzie Coilix nie ma zależności od Pekko ani parametrów `F[_]` w API.
2. Zatrzymanie procesora strumieniowego nie gubi elementów pobranych ze źródła, jeśli przepływ kończy się przed `drain-timeout`. Test to sprawdza.
3. Flow bulldozera `EntityChangeEventLoadingFlow` (Kafka, paczki po 50 albo 5 s, zapis, zatwierdzenie) da się zapisać jako procesor Coilix w przykładzie z przewodnika.
4. Demo i przykłady nie wołają `unsafeRun*`.

## Ryzyka i otwarte pytania

1. SFTP nie ma dojrzałej biblioteki fs2. Konektor owija bibliotekę Javy (np. sshj) w `Resource` i `Stream`, w osobnym tasku konektora.
2. Subskrypcje GraphQL bulldozera stoją na aktorach i `PekkoSubscriptionStream`. Ich odpowiednik na fs2 powstaje razem z modułem GraphQL.
3. `drain-timeout` dobiera się do przepływu. Domyślna wartość i nadpisanie per procesor ustala krok 4.
