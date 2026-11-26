---
name: pr-conflicts
description: Ustala kolejność otwartych PR-ów Coilix, wykrywa i rozwiązuje konflikty z master oraz między zależnymi PR-ami po scaleniu poprzednika (squash), potem waliduje dotknięte moduły. Użyj przy „konflikty”, „rebase”, „PR nie da się scalić”, „co dalej z PR-ami”, po scaleniu PR-a, od którego zależy inny, i przed wypchnięciem gałęzi zbudowanej na innej gałęzi.
---

# Konflikty i kolejność PR-ów

`master` przyjmuje tylko „Squash and merge” (ADR 0008). Po scaleniu PR-a jego commity nie trafiają na `master` w tej samej postaci, więc gałąź zbudowana na jego gałęzi zawsze ma konflikt albo zdublowane zmiany. Zwykły `git rebase origin/master` nie wystarczy: trzeba odciąć stare commity poprzednika (`--onto`).

Komendy uruchamiaj w Bash z katalogu repozytorium. Wyniki `gh` i `git log` ograniczaj (`--limit`, `-n`, `head`).

## 1. Stan

```sh
git fetch --prune origin
git status --short --branch
gh pr list --state open --json number,title,headRefName,baseRefName,mergeable,mergeStateStatus \
  --template '{{range .}}#{{.number}} {{.headRefName}} -> {{.baseRefName}} {{.mergeable}} {{.mergeStateStatus}}{{"\n"}}{{end}}'
```

- `baseRefName` inny niż `master` oznacza PR zależny. Zapisz łańcuch, np. `#109 -> #108 -> master`.
- PR z bazą `master` może być też zależny w praktyce: sprawdź, czy jego gałąź zawiera commity innej otwartej gałęzi:
  `git log --oneline origin/master..origin/<gałąź> -n 30` i porównaj z `git log --oneline origin/master..origin/<inna> -n 30`.
- `mergeable: CONFLICTING` albo `mergeStateStatus: DIRTY` to konflikt. `BEHIND` to tylko nieaktualna gałąź (ochrona `master` wymaga aktualności), też wymaga rebase.

Kolejność scalania: od najniższego modułu (numer prefiksu) i od PR-a, od którego zależą inne. Raportuj użytkownikowi łańcuch i kolejność, zanim zaczniesz rebase kilku gałęzi.

## 2. Rebase gałęzi, której poprzednik został scalony

Poprzednik `P` (gałąź `feature/P`) scalony przez squash, gałąź `feature/N` zbudowana na nim:

```sh
git switch feature/N
old_base=$(git merge-base feature/N origin/feature/P 2>/dev/null || git merge-base feature/N "<ostatni commit P sprzed squasha>")
git rebase --onto origin/master "$old_base"
```

Gdy zdalna gałąź `P` została już usunięta, ostatni commit `P` znajdziesz w `gh pr view <nr P> --json commits --jq '.commits[-1].oid'`, a lokalnie w `git reflog`/starej gałęzi. Po rebase zmień bazę PR-a, jeśli wskazywała na `P`: `gh pr edit <nr N> --base master`.

## 3. Rebase gałęzi bez zależności

```sh
git switch <gałąź>
git rebase origin/master
```

## 4. Rozwiązywanie konfliktów

- `git diff --name-only --diff-filter=U` daje listę plików. Rozwiązuj plik po pliku, czytając obie strony.
- Wersja z `master` wygrywa w tym, co już scalono (nazwy, sygnatury, przeniesione pliki). Gałąź wnosi tylko swoją funkcję. Nie przywracaj kodu, który `master` usunął lub zmienił (np. tagless final, Pekko, `Future` w kontraktach: kierunek wyznacza ADR 0043).
- Typowe miejsca konfliktów i reguła:
  - `changelog/<grupa>/unreleased.md`: zachowaj wpisy obu stron.
  - `tasks/README.md`: zachowaj statusy z `master` i zmień tylko wiersz swojego taska.
  - tabela modułów w `CLAUDE.md`, `docs/architecture/overview.md`, `docs/adr/README.md`: zachowaj wiersze obu stron. Numer nowego ADR-a, który zajął już `master`, zmień na następny wolny (plik, nagłówek i odnośniki).
  - `build.sbt`, `project/Dependencies.scala`: wersje bibliotek z nowszej strony, moduły i zależności z obu.
- `git add <plik>` i `git rebase --continue`. Nigdy `git rebase --skip` bez sprawdzenia, że commit jest naprawdę pusty.

## 5. Walidacja po rozwiązaniu

Moduły dotknięte przez gałąź:

```sh
git diff --name-only origin/master...HEAD | cut -d/ -f1 | sort -u
```

Uruchom testy tych modułów i modułów od nich zależnych (wyższy numer), z nazwami projektów sbt z tabeli w `CLAUDE.md`:

```sh
tools/sbt-lite.sh checkFormat <projekt>/testFull <zależny>/testFull doc
```

Gdy konflikt dotyczył `build.sbt`, `project/` albo modułu rdzenia `01`–`16`, uruchom pełne `tools/sbt-lite.sh testFull doc demo/run`.

Skrypt wypisuje tylko podsumowanie. Przy błędzie szczegóły czytaj z `target/sbt-lite/last.log` (`grep -n -A20`), nie uruchamiaj sbt ponownie bez zmian. Raportuj komendę i wynik (przeszło / co padło).

## 6. Wypchnięcie

```sh
git push --force-with-lease origin <gałąź>
```

Tylko na gałęzi tematycznej, nigdy na `master`. Potem sprawdź `gh pr view <nr> --json mergeable,mergeStateStatus` i przejdź do następnego PR-a w łańcuchu (krok 2).
