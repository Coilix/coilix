# Proces developmentu

Jak prowadzić zmiany od taska do wydania. Zasady kodu są w [CLAUDE.md](../../CLAUDE.md), standard wersji w [versioning.md](../architecture/versioning.md), a skrót dla kontrybutorów z zewnątrz w [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Model gałęzi

Trunk-based development: jedna gałąź główna `master` i krótko żyjące gałęzie tematyczne. Nie ma gałęzi `develop`, a wydanie to tag na `master`.

| Gałąź | Skąd | Dokąd | Żyje | Do czego |
|---|---|---|---|---|
| `master` | | | zawsze | Zawsze kompiluje się i przechodzi testy. Każdy commit da się wydać. |
| `feature/COILIX-NNNNN-opis` | `master` | `master` przez PR | dni | Realizacja taska albo jego części. |
| `fix/COILIX-NNNNN-opis` | `master` albo `series/X.Y` | ta sama gałąź przez PR | godziny | Poprawka błędu. |
| `release/vX.Y.Z` | `master` albo `series/X.Y` | ta sama gałąź przez PR | do scalenia | Tylko zmiana nagłówka w `CHANGELOG.md` przed tagiem. |
| `series/X.Y` | tag `vX.Y.0` | nigdzie | do końca wsparcia linii | Poprawki `X.Y.Z+1` po tym, jak `master` przeszedł na następną wersję. |

Zasady:

- `opis` to kilka słów w kebab-case po angielsku: `feature/COILIX-00004-bean-lifecycle`.
- Jedna gałąź to jeden task. Duży task można podzielić na kilka kolejnych gałęzi, ale każda zostawia `master` w stanie gotowym do wydania.
- Gałąź żyje krótko. Jeśli praca trwa dłużej niż tydzień, rebase na `master` co najmniej raz w tygodniu, żeby konflikty nie rosły.
- Funkcja, która nie jest gotowa, nie trafia na `master` w połowie. Albo czeka na gałęzi, albo jest dzielona na samodzielne kroki (najpierw trait i test, potem implementacja, potem podpięcie w `boot`).
- Gałęzi `series/X.Y` nie tworzy się na zapas, tylko wtedy, gdy trzeba wydać poprawkę starszej linii.

## Cykl życia taska

### 1. Przygotowanie

1. Task istnieje w `tasks/` według szablonu z [tasks/README.md](../../tasks/README.md). Nowy pomysł najpierw dostaje plik taska, potem kod.
2. Status w `tasks/README.md` zmienia się na „w toku”.
3. Gałąź od aktualnego `master`:

   ```sh
   git switch master
   git pull --rebase
   git switch -c feature/COILIX-NNNNN-opis
   ```

4. Do pracy nad dwiema rzeczami naraz zamiast przełączania gałęzi służy osobny katalog: `git worktree add ../coilix-NNNNN feature/COILIX-NNNNN-opis`.

### 2. Praca

- Commity według sekcji „Commity” w `CLAUDE.md`: angielski, prefiks z ID taska, jedna logiczna zmiana na commit.
- Commity, PR-y, komentarze, issues, `CHANGELOG.md` i notki wydań nie zawierają informacji, że zmianę przygotowano z pomocą AI lub agenta: bez stopek `Co-Authored-By` z narzędziem AI, bez linii „Generated with …” i bez wzmianek o modelu.
- Każdy commit ma `Signed-off-by` (DCO): `git commit -s`. Żeby nie zapominać, wystarczy alias `git config alias.cs "commit -s"`.
- Nowe pliki od razu przez `git add`, przeniesienia przez `git mv`.
- W tym samym commicie co kod: testy munit, Scaladoc publicznego API, wpis w `CHANGELOG.md` (sekcja `Unreleased`), jeśli zmiana jest widoczna dla użytkownika.

### 3. Sprawdzenie lokalne przed PR

```sh
sbt -Dsbt.server.autostart=false testFull doc demo/run
```

- `testFull` przechodzi (kompilacja z `-Werror`, wartremover, scalafmt, zakaz `println`).
- W logu `doc` nie ma `Couldn't resolve a member for the given link query`.
- `demo/run` startuje i pokazuje nową funkcję, jeśli task jej dotyczy.
- Po zmianie pluginu IntelliJ: `sbt intellij-plugin/installIntellijPlugin` i ręczna weryfikacja w IDE.

### 4. Pull request

1. Rebase na `master`, żeby historia była liniowa: `git fetch && git rebase origin/master`.
2. `git push -u origin feature/COILIX-NNNNN-opis` i PR do `master` (albo do `series/X.Y` dla poprawki starszej linii).
3. Tytuł PR jak pierwszy commit: `COILIX-NNNNN: Opis`. Opis PR: co się zmienia, jak to sprawdzić, czy zmiana jest **BREAKING**.
4. CI musi być zielone: formatowanie (scalafmt dla kodu i plików buildu, google-java-format dla Javy), testy na JDK 17 i 25, `doc`, audyt licencji, pokrycie testami (`testCoverage`), a po pierwszym wydaniu MiMa i TASTy-MiMa.
5. Przegląd według listy z sekcji [Przegląd](#przegląd).
6. Scalenie przez **Rebase and merge**. Commity zachowują się w historii jeden do jednego, bez commita scalającego. Squash tylko wtedy, gdy commity na gałęzi są poprawkami poprzednich („fix typo”, „review”), a i tak lepiej poprawić je przed PR przez `git commit --fixup` i `git rebase --autosquash`.
7. Po scaleniu gałąź jest usuwana (GitHub robi to automatycznie przy ustawieniu „Automatically delete head branches”).

### 5. Zamknięcie taska

Status w `tasks/README.md` zmienia się na „zrobiony” w ostatnim PR taska. Task jest zrobiony, gdy spełnia kryteria akceptacji z pliku taska i definicję ukończenia niżej.

## Definicja ukończenia

- Kryteria akceptacji z pliku taska spełnione.
- `sbt testFull`, `sbt doc` (bez zepsutych linków) i `sbt demo/run` przechodzą.
- Każda nowa klasa ma testy, każdy publiczny element w `src/main` ma Scaladoc.
- Wpis w `CHANGELOG.md` z ID taska, z **BREAKING** i instrukcją migracji przy zmianie niezgodnej.
- `CLAUDE.md`, dokumenty w `docs/` (w tym tabela punktów rozszerzeń w `docs/architecture/overview.md`) i status w `tasks/README.md` zaktualizowane.
- Decyzja architektoniczna ma ADR w `docs/adr/`.
- Nowa zależność dopisana do [dependency-licenses.md](../legal/dependency-licenses.md) z licencją z listy dozwolonych.

## Przegląd

Projekt ma na razie jednego opiekuna, więc przegląd to samodzielne przejście listy w PR przed scaleniem. Gdy pojawią się kolejni opiekunowie, PR wymaga zatwierdzenia przez innego opiekuna.

- Czy zmiana zmienia publiczne API albo klucz configu? Jeśli tak, czy to jest odnotowane jako **BREAKING** i czy numer następnej wersji to uwzględnia?
- Czy usuwany element był najpierw oznaczony `@deprecated`?
- Czy żaden tekst, komunikat ani test nie jest skopiowany ani sparafrazowany ze Springa?
- Czy rdzeń (`common`, `annotations`, `config`, `context`, `boot`) nie zależy od modułów funkcji?
- Czy komunikaty błędów mówią użytkownikowi, co zrobić?

## Wydanie

Wydania wychodzą z `master` (linia bieżąca) albo z `series/X.Y` (starsza linia). Numer wynika z [versioning.md](../architecture/versioning.md): w fazie `0.x` zmiana niezgodna podnosi `MINOR`, reszta `PATCH`.

1. Upewnij się, że CI na gałęzi wydania jest zielone.
2. Na gałęzi `release/vX.Y.Z` (żyje tylko do scalenia tego jednego PR) zmień w `CHANGELOG.md` nagłówek `Unreleased` na `[X.Y.Z] - RRRR-MM-DD` i dodaj nad nim nową, pustą sekcję `Unreleased`. Commit: `COILIX-00000: Release X.Y.Z`. Scal przez PR.
3. Tag podpisany na commicie z `master`:

   ```sh
   git switch master && git pull --rebase
   git tag -s vX.Y.Z -m "Coilix X.Y.Z"
   git push origin vX.Y.Z
   ```

4. Tag uruchamia workflow `release.yml`: sprawdzenia, publikacja na Maven Central i GitHub Release z sekcją z changelogu. Plugin IntelliJ i dokumentacja dojdą w osobnych taskach.
5. Sprawdź artefakty na Maven Central (pojawiają się po kilkunastu minutach) i stronę GitHub Release.

Wydania przedpremierowe (`vX.Y.Z-M1`, `vX.Y.Z-RC1`) idą tą samą drogą. `-M` to kamień milowy bez gwarancji stabilności API, `-RC` to kandydat, w którym zmieniają się już tylko poprawki.

## Utrzymanie starszej linii

Przykład: wydane jest `0.3.0`, `master` ma już zmiany niezgodne do `0.4.0`, a w `0.3.0` jest błąd.

1. Poprawka trafia najpierw na `master` przez zwykły PR (gałąź `fix/COILIX-NNNNN-opis`), o ile błąd tam występuje.
2. Gałąź linii, jeśli jeszcze nie istnieje: `git switch -c series/0.3 v0.3.0 && git push -u origin series/0.3`.
3. Przeniesienie poprawki: gałąź `fix/COILIX-NNNNN-opis-0.3` od `series/0.3`, `git cherry-pick -x <commit>`, PR do `series/0.3`.
4. Na `series/0.3`: sekcja w `CHANGELOG.md`, tag `v0.3.1` na tej gałęzi, push tagu.
5. Wpis o wydaniu `0.3.1` trafia też do `CHANGELOG.md` na `master`, żeby historia wydań była w jednym miejscu.

Wspierana jest tylko najnowsza linia `MINOR`. Starsze linie dostają poprawki bezpieczeństwa, jeśli opiekun tak zdecyduje, a decyzja trafia do `SECURITY.md`.

## Ochrona gałęzi na GitHubie

Repozytorium jest publiczne od 2026-11-02 i ma reguły (rulesets) zapisane w [`.github/rulesets/`](../../.github/rulesets). Po zmianie reguły w ustawieniach GitHuba aktualizuje się też plik JSON, a nowe repozytorium konfiguruje się przez `gh api -X POST repos/<org>/<repo>/rulesets --input <plik>`.

- `master` i `series/**`: zmiany tylko przez PR, wymagane zielone checki `Build on JDK 17`, `Build on JDK 25` i `Test coverage` na gałęzi aktualnej względem bazy, rozwiązane wątki przeglądu, liniowa historia, zakaz force-push i usuwania. Zatwierdzenie przez drugą osobę nie jest wymagane, dopóki projekt ma jednego opiekuna,
- tagi `v*`: tworzy je tylko administrator repozytorium, nikt nie może ich usunąć ani przesunąć,
- metody scalania: tylko „Rebase and merge” (i ewentualnie „Squash”), wyłączony „Merge commit”,
- „Automatically delete head branches” włączone,
- aplikacja DCO sprawdzająca `Signed-off-by` w każdym commicie PR (do zainstalowania, wtedy jej check dochodzi do wymaganych).
