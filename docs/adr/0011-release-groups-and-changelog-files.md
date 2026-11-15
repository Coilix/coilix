# 0011: Grupy wydań, changelog w plikach i publikacja przy tagu

- Status: przyjęta
- Data: 2026-11-03
- Task: COILIX-00009, COILIX-00007
- Zastępuje: zasadę „jedna wersja dla całego frameworka” z `docs/architecture/versioning.md` oraz punkty 4, 5 i 7 z [ADR 0010](0010-docusaurus-documentation-site.md)

## Kontekst

Dotąd wszystkie publikowane moduły miały jedną wersję i jeden tag `vX.Y.Z`, changelog był jednym plikiem `CHANGELOG.md`, a strona dokumentacji miała być publikowana przy każdym pushu na `master` razem z wersją `next`.

Opiekun chce, żeby:

- moduły funkcji (`data`, `web`, `security` i przyszłe) wychodziły niezależnie od siebie i od rdzenia, bo poprawka w `web` nie powinna wymuszać wydania całego frameworka,
- changelog był pisany ręcznie, jako plik na `master` dla każdego modułu i każdej wersji, i był widoczny na stronie,
- tag na `master` uruchamiał publikację artefaktów i dokumentacji tylko tego, czego dotyczy,
- tag bez opisu wersji się nie publikował,
- strona pokazywała tylko wydane wersje i była sprawdzana w całości przed publikacją.

Moduły rdzenia zależą od siebie łańcuchem (`boot` → `context` → `config` → `common`, plus `annotations` i `slf4s-plugin`). Niezależne wersje każdego z nich wymagałyby macierzy zgodności i BOM-u. Moduły funkcji zależą tylko od rdzenia i nie zależą od siebie (`docs/architecture/modularity.md`), więc dla nich niezależne wersje są proste.

## Decyzja

### Grupy wydań

| Grupa | Moduły | Tag | Gdzie trafia |
|---|---|---|---|
| `core` | `common`, `annotations`, `slf4s-plugin`, `config`, `context`, `boot` | `core-vX.Y.Z` | Maven Central |
| `<moduł funkcji>`, np. `web` | jeden moduł funkcji | `web-vX.Y.Z` | Maven Central |
| `intellij` | `intellij-plugin` | `intellij-vX.Y.Z` | JetBrains Marketplace |

1. Każda grupa ma własną wersję według SemVer i `early-semver`. Moduły w grupie mają zawsze tę samą wersję.
2. Przynależność modułu do grupy jest zapisana tylko w `build.sbt`. Workflow, skrypty i strona czytają ją stamtąd.
3. Wersja modułu pochodzi z ostatniego tagu jego grupy (sbt-dynver z prefiksem `<grupa>-v`). MiMa porównuje moduł z poprzednim wydaniem tej samej linii jego grupy.
4. Moduł funkcji w opublikowanym POM-ie zależy od **wydanej** wersji rdzenia. Wydanie modułu funkcji wymaga, żeby kod rdzenia nie zmienił się od ostatniego tagu `core-v*`. Jeśli się zmienił, najpierw wychodzi rdzeń. Dzięki temu artefakt nigdy nie zależy od snapshotu.
5. Zgodność modułu funkcji z rdzeniem wynika z jego zależności w POM-ie i jest zapisana w pliku changelogu (`requires-core`). Strona pokazuje z tego tabelę zgodności.

### Changelog w plikach

```
changelog/
  core/
    unreleased.md
    0.1.0.md
  web/
    unreleased.md
    0.1.0.md
  intellij/
    unreleased.md
```

1. `CHANGELOG.md` zostaje zastąpiony katalogiem `changelog/`. Każda grupa ma swój katalog, a każde wydanie swój plik `<wersja>.md`.
2. PR ze zmianą widoczną dla użytkownika dopisuje wpis do `changelog/<grupa>/unreleased.md` tej grupy, której moduły zmienia. Wpisy pisze człowiek, nie generuje się ich z commitów.
3. Plik wydania ma nagłówek (front matter) i treść w formacie [Keep a Changelog](https://keepachangelog.com/en/1.1.0/):

   ```markdown
   ---
   version: 0.2.0
   date: 2026-12-01
   requires-core: "0.2"
   ---

   ### Added

   - `@PostConstruct` and `@PreDestroy` lifecycle callbacks (COILIX-00004).

   ### Changed

   - **BREAKING** ...
   ```

   Dozwolone sekcje: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`. Pole `requires-core` mają tylko grupy inne niż `core` i `intellij`. Zmiana niezgodna ma oznaczenie **BREAKING** i opis migracji.
4. Przed wydaniem PR przygotowujący (gałąź `release/<grupa>-vX.Y.Z`) zmienia nazwę `unreleased.md` na `X.Y.Z.md`, uzupełnia nagłówek i zakłada pusty `unreleased.md`. Robi to skrypt, a ręczny workflow „Prepare release” (grupa i wersja jako parametry) uruchamia ten skrypt i otwiera PR. Przy wersji `X.Y.0` ten sam PR tworzy kopię przewodników grupy dla linii `X.Y`.
5. CI w każdym PR sprawdza format wszystkich plików w `changelog/`.

### Tag i publikacja

Opiekun robi podpisany tag `<grupa>-vX.Y.Z` na commicie z `master` (albo z `series/<grupa>-X.Y` przy poprawce starszej linii). Workflow wydania:

1. Odczytuje grupę i wersję z nazwy tagu.
2. Sprawdza, że tag jest na `master` albo na gałęzi linii tej grupy, że istnieje `changelog/<grupa>/X.Y.Z.md` z poprawnym nagłówkiem, niepustą treścią i tą samą wersją, że wersja jest wyższa od poprzedniego tagu grupy i że dla modułu funkcji rdzeń nie zmienił się od ostatniego wydania. Do tego testy, licencje i MiMa modułów grupy. Każdy błąd zatrzymuje wydanie przed publikacją.
3. Publikuje tylko moduły tej grupy (Maven Central albo Marketplace).
4. Tworzy GitHub Release z treścią pliku changelogu.
5. Uruchamia publikację strony.

### Strona dokumentacji

1. Strona jest publikowana tylko przy tagu i ręcznie (workflow „Publish docs”), nie przy pushu na `master`. Nie pokazuje wersji `next`, tylko wydane linie.
2. Każda grupa z przewodnikami ma własną sekcję z własnym przełącznikiem wersji (osobne instancje wtyczki docs Docusaurusa): `/core/...`, `/web/...`. Plugin IntelliJ jest opisany w przewodnikach `core`.
3. Stare wersje przewodników są kopiami na `master` (`versioned_docs` każdej instancji), jak w ADR 0010. Stronę buduje się zawsze z aktualnego `master`, więc poprawka opisu starszej linii trafia na `master`.
4. Dokumentacja API każdej linii każdej grupy jest pod `/api/<grupa>/<linia>/`, budowana z commitu tagu i dopisywana do `gh-pages` bez kasowania starszych.
5. Strona `/changelog` pokazuje pliki z `changelog/` pogrupowane według grup i wersji, bez `unreleased.md`.
6. Przed publikacją cała zbudowana strona, razem z API, przechodzi przez link checker (lychee) i test obecności kluczowych stron. Błąd zatrzymuje publikację.
7. W PR-ach CI dalej buduje i sprawdza stronę z bieżącego stanu, w tym z niewydanych przewodników, ale nic nie publikuje.

### Zmiany przy wdrożeniu

- Punkt 4 changelogu: PR wydania przygotowuje lokalny skrypt `npm run prepare-release` zamiast ręcznego workflow „Prepare release”, bo PR otwarty domyślnym tokenem GitHub Actions nie uruchamia CI, a osobny token w sekretach byłby kolejnym kluczem do pilnowania. Kopia przewodników dla linii `X.Y` dojdzie do skryptu razem z publikacją strony.

## Konsekwencje

- Zmienia się `docs/architecture/versioning.md` (grupy zamiast jednej wersji), `docs/process/development.md` (wydanie, linie utrzymaniowe), `docs/process/documentation.md` (publikacja, sekcje, changelog), `CLAUDE.md` (wersjonowanie, changelog) i ruleset tagów (`*-v*` zamiast `v*`). Zmiany wchodzą razem z wdrożeniem, a do tego czasu obowiązuje obecny opis.
- Użytkownik dodaje moduł funkcji w jego własnej wersji i musi sprawdzić, z jakim rdzeniem działa. Tabela zgodności na stronie i zależność w POM-ie to ułatwiają, a Coursier wybierze nowszy rdzeń, jeśli dwa moduły funkcji wymagają różnych wersji tej samej linii.
- Moduły rdzenia wychodzą razem, nawet jeśli zmienił się tylko jeden z nich. To świadomy koszt: rdzeń nie ma macierzy zgodności.
- Wydanie modułu funkcji po zmianie w rdzeniu wymaga dwóch wydań w kolejności: najpierw `core`, potem moduł.
- sbt-dynver i sbt-ci-release trzeba skonfigurować per grupa (prefiks tagu, wersja projektu, publikacja wybranych modułów). Jeśli sbt-dynver nie pozwoli na prefiks per projekt, wersja będzie liczona przez funkcję w `project/` na podstawie `git describe --match '<grupa>-v*'`.
- Poprawka literówki w przewodniku nie wymaga wydania, ale wymaga ręcznego uruchomienia „Publish docs”.
- Changelog wymaga dyscypliny przy każdym PR. CI sprawdza format plików, a obecność wpisu sprawdza przegląd PR.
