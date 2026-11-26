# 0048: Procesory strumieniowe z opróżnianiem

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00032 (krok 4, część 2), COILIX-00031 (krok 3)

## Kontekst

ADR 0043 wymaga, żeby zatrzymanie procesora opartego na strumieniu opróżniało przepływ zamiast anulować fiber: źródło przestaje dawać elementy, reszta przepływu przetwarza i zatwierdza to, co już przeczytano, a anulowanie przychodzi dopiero po limicie czasu. ADR 0044 dał jeden styl, `IOProcessor`, na `FiberEngine`. Zatrzymanie dawało pracy połowę limitu na samodzielne zakończenie, a potem ją anulowało. Flow bulldozera (Kafka, paczki po 50 albo 5 s, zapis, zatwierdzenie) zapisany jako `IOProcessor` musiałby sam pilnować sygnału zatrzymania w każdym miejscu przepływu.

## Decyzja

- **Styl `StreamProcessor[A]`.** Procesor opisuje `source(shard): Stream[IO, A]` i `flow(shard): Pipe[IO, A, Any]`. Wynik przepływu jest pomijany, więc przepływ może zwracać zatwierdzone offsety, `Unit` albo cokolwiek innego.
- **Sygnał zatrzymania za źródłem.** Coilix składa pracę jako `source.interruptWhen(shard.stopped.attempt).through(flow).compile.drain`. Zatrzymanie kończy samo źródło przy następnym elemencie. Elementy już przeczytane przechodzą przez przepływ, który kończy się jak po końcu źródła (np. `groupWithin` wysyła niepełną paczkę). Przepływ nie musi znać sygnału zatrzymania.
- **Ten sam silnik.** `FiberEngine` uruchamia oba style na fiberach pod `Supervisor`, a `FiberWorker` dostaje pracę jako `IO[Unit]`. Restarty, `on-completion`, statystyki i polityka nadzoru są wspólne z `IOProcessor`.
- **`drain-timeout`.** Nowy klucz procesora (`coilix.processors.<nazwa>.drain-timeout`, także w `defaults`) mówi, jak długo praca może działać po sygnale zatrzymania, zanim zostanie anulowana. Bez niego zostaje dotychczasowa połowa limitu zatrzymania. Wartość przechodzi do `Shard.drainTimeout`, więc procesor też ją widzi. Limit opróżniania nie przekracza limitu zatrzymania, a po anulowaniu worker czeka na finalizery resztę limitu, co najmniej sekundę. Klucz dotyczy obu stylów.

## Konsekwencje

- Zatrzymanie procesora strumieniowego nie gubi przeczytanych elementów, jeśli przepływ skończy się w `drain-timeout`. Pokazują to testy silnika, demo (`VehicleCheckIn`) i przykład z przewodnika (`OrderLoading`).
- Źródło, które czeka na element (np. `Queue.take`), jest przerywane od razu, bo `interruptWhen` anuluje oczekiwanie źródła, a nie przepływu.
- Moduł Kafki (krok 5 COILIX-00032) daje źródło z przydziałem partycji do slotów i zatwierdzanie w przepływie. `stopConsuming` dochodzi tam jako finalizer źródła.
