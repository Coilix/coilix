# COILIX-00007: Dokumentacja API i strona dokumentacji z wersjami i dwoma językami

## Cel

Publiczna strona `https://docs.coilix.dev` z przewodnikami dla programisty aplikacji po angielsku i po polsku, przełącznikiem wersji, changelogiem i dokumentacją API ze Scaladoc. Każda linia wydań ma własną wersję dokumentacji, a zasady pisania i publikacji są spisane i sprawdzane przez CI.

Narzędzie i adres wybiera [ADR 0010](../docs/adr/0010-docusaurus-documentation-site.md). Zasady są w [docs/process/documentation.md](../docs/process/documentation.md).

## Stan obecny

- Scaladoc mają `01_common` i `04_config`, a Javadoc `02_annotations` (z `-Xdoclint:all -Werror`).
- Bez Scaladoc: `05_context`, `09_boot`, `03_slf4s_plugin`, `10_demo`.
- `sbt doc` generuje osobną dokumentację dla każdego modułu. Nie ma wspólnej strony, przewodników ani publikacji.
- Wersja projektu pochodzi z tagu gita (COILIX-00009), repozytorium jest publiczne, domena `coilix.dev` należy do projektu.

## Zakres

Kolejne PR-y, każdy zostawia `master` w stanie gotowym do wydania:

1. **Projekt i zasady:** ADR 0010, `docs/process/documentation.md`, ten task.
2. **Scaladoc `05_context`.**
3. **Scaladoc `09_boot`.**
4. **Scaladoc `03_slf4s_plugin` i `10_demo`.**
5. **Szkielet `12_docs`:**
   - projekt sbt `docs` (bez publikacji) kompilujący `examples/next/` i budujący jedną stronę Scaladoc dla modułów `01`–`10` z Javadoc obok,
   - Docusaurus w `12_docs/website/` z językami EN i PL, przełącznikiem wersji, stroną changelogu i stałymi działami menu,
   - skrypty sprawdzające z sekcji „Sprawdzenia w CI” zasad i krok w CI, który buduje stronę w PR bez publikacji.
6. **Przewodniki:** Getting started, Guides i Reference w obu językach, z przykładami w `examples/next/`. Jeden dział na PR.
7. **Publikacja:**
   - `.github/workflows/docs.yml` (push na `master`: przewodniki i `/api/next/`, tag: `/api/X.Y/`),
   - gałąź `gh-pages`, GitHub Pages z domeną `docs.coilix.dev`, rekord CNAME w Cloudflare (akcja ręczna opiekuna),
   - skrypt `npm run version` i opis w `docs/process/development.md` w sekcji „Wydanie”.

## Proponowany projekt

Szczegóły (struktura katalogów, menu, tłumaczenia, wersje, changelog, publikacja) są w [docs/process/documentation.md](../docs/process/documentation.md). Ten task nie powtarza ich, żeby było jedno źródło.

Do sprawdzenia przy kroku 5:

- jak w sbt 2 zbudować jedną stronę Scaladoc dla kilku modułów (sbt-unidoc albo własne zadanie podające TASTy wszystkich modułów i ich classpath),
- czy `raw-loader` działa z aliasem `@examples` w Docusaurusie 3 bez własnej konfiguracji webpacka, czy potrzebna jest mała wtyczka,
- narzędzie do sprawdzania licencji zależności npm.

## Kryteria akceptacji

1. Każdy publiczny element w `src/main` modułów `01`–`10` ma Scaladoc albo Javadoc zgodny z `CLAUDE.md`.
2. `docs.coilix.dev` pokazuje przewodniki po angielsku (`/`) i po polsku (`/pl/`), przełącznik wersji z `next` i wydanymi liniami, stronę `/changelog` i dokumentację API dla każdej wersji.
3. Wszystkie sprawdzenia z sekcji „Sprawdzenia w CI” zasad działają w CI i przerywają build.
4. Push na `master` aktualizuje wersję `next`, a wydanie `X.Y.0` tworzy wersję `X.Y` skryptem `npm run version`.
5. `CLAUDE.md` opisuje moduł `12_docs` (tabela modułów, komendy).

## Poza zakresem

- Strona główna projektu pod `coilix.dev`.
- Wyszukiwarka pełnotekstowa (osobny task, gdy będzie rozwiązanie z obsługą polskiego albo zgłoszenie do Algolia DocSearch).
- Tłumaczenie changelogu i dokumentacji API.

## Ryzyka i otwarte pytania

- Zgodność sbt-unidoc z sbt 2 jest niepotwierdzona.
- Utrzymanie dwóch języków podwaja pracę przy tekstach. CI pilnuje, żeby żadna strona nie została bez tłumaczenia.
- Przy wydaniu poprawkowym starszej linii (np. `0.1.1` po `0.2.0`) workflow nadpisuje tylko `/api/0.1/` i nie zmienia wersji domyślnej strony.
