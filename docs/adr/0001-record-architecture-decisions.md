# 0001: Zapisywanie decyzji jako ADR

- Status: przyjęta
- Data: 2026-10-30
- Task: COILIX-00000

## Kontekst

Uzasadnienia kluczowych wyborów (refleksja, adnotacje w Javie, plugin kompilatora, wersja Scali) są rozproszone po taskach, `docs/architecture` i `docs/legal`. Taski opisują, co zrobić, a po zamknięciu nikt do nich nie wraca. Nowy kontrybutor nie ma jednego miejsca, w którym dowie się, dlaczego projekt wygląda tak, a nie inaczej.

## Decyzja

Decyzje, które wpływają na publiczne API, strukturę modułów, zależności albo proces wydań, zapisujemy jako ADR w `docs/adr/`. Task, który wprowadza taką decyzję, dodaje ADR w tym samym PR.

## Konsekwencje

- Jedno miejsce z historią decyzji, niezależne od tasków.
- Odwrócenie decyzji wymaga nowego ADR, więc zmiana kierunku jest widoczna.
- Trochę więcej pracy przy taskach architektonicznych.
