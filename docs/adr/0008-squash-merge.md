# 0008: Scalanie przez squash, jeden PR to jedna zmiana

- Status: przyjęta
- Data: 2026-11-02
- Task: COILIX-00011

## Kontekst

Proces z COILIX-00011 zakładał „Rebase and merge”, czyli przeniesienie każdego commita z gałęzi na `master` bez zmian. Wymagało to dopracowania każdego commita przed PR (`git commit --fixup`, `git rebase --autosquash`), a kroki pośrednie i poprawki po przeglądzie i tak trafiały do historii. PR #6 został scalony przez squash i jeden commit na PR okazał się czytelniejszy.

## Decyzja

- Jedyna włączona metoda scalania to „Squash and merge”, wymuszana regułą `master and series` (`.github/rulesets/master-and-series.json`).
- Jeden PR dotyczy jednej funkcji albo jednej poprawki. Wszystkie commity na gałęzi wnoszą albo poprawiają tę jedną rzecz.
- Tytuł commita na `master` to tytuł PR (`COILIX-NNNNN: Opis (#N)`), a treść to wiadomości commitów z gałęzi razem z ich `Signed-off-by`.

## Konsekwencje

- Historia `master` ma jeden commit na PR, więc `git revert` i `git cherry-pick -x` na linię `series/X.Y` obejmują całą zmianę naraz.
- Na gałęzi można commitować kroki pośrednie bez przepisywania historii przed PR.
- Tytuł PR musi opisywać całą zmianę, bo zostaje w historii.
- Duża funkcja wymaga podziału na kilka samodzielnych PR, bo inaczej trafi na `master` jako jeden duży commit.
- Każdy commit na gałęzi nadal potrzebuje `Signed-off-by`, bo aplikacja DCO sprawdza commity PR, a nie commit po squashu.
