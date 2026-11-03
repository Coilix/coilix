# 0010: Strona dokumentacji w Docusaurusie pod `docs.coilix.dev`

- Status: przyjęta, punkty 4, 5 i 7 zastąpione przez [0011](0011-release-groups-and-changelog-files.md)
- Data: 2026-11-03
- Task: COILIX-00007

## Kontekst

Coilix potrzebuje publicznej dokumentacji z przewodnikami i dokumentacją API. Wymagania opiekuna:

- przełącznik wersji, żeby użytkownik starszego wydania czytał opis swojej wersji,
- changelog na stronie,
- dwa języki: angielski i polski,
- jasne zasady publikacji, żeby strona nie rozrastała się chaotycznie.

Pierwotny plan z COILIX-00007 zakładał stronę z samego Scaladoc (`-siteroot`). Scaladoc ma przełącznik wersji, ale nie obsługuje tłumaczeń. Wersję polską trzeba by budować jako osobną, ręcznie linkowaną stronę.

Rozważane narzędzia:

| Narzędzie | Wersje | Języki | Uwagi |
|---|---|---|---|
| Scaladoc | tak | nie | bez dodatkowych narzędzi, ale nie spełnia wymagania o językach |
| MkDocs Material + mike | tak | przez plugin | Python, Material jest w trybie utrzymania |
| Laika (sbt-typelevel-site) | tak | nie | natywne dla Scali, bez tłumaczeń |
| Docusaurus 3 | wbudowane | wbudowane | Node.js, szeroko używany do dokumentacji bibliotek |

## Decyzja

1. **Przewodniki** powstają w Docusaurusie 3, w katalogu `12_docs/website/`. Dokumentacja API jest nadal generowana przez Scaladoc i Javadoc i linkowana ze strony.
2. **Adres:** `https://docs.coilix.dev`. Domena `coilix.dev` zostaje na przyszłą stronę główną projektu.
3. **Języki:** angielski jest domyślny i jest źródłem treści (`/`), polski to tłumaczenie (`/pl/`). Dokumentacja API i changelog są tylko po angielsku.
4. **Wersje dokumentacji** odpowiadają liniom `MINOR`: `next` (z `master`), `0.2`, `0.1`. Wydania poprawkowe aktualizują dokumentację swojej linii, a nie tworzą nowej wersji.
5. **Changelog** ma jedno źródło: `CHANGELOG.md` w katalogu głównym. Strona kopiuje go przy budowaniu, nikt nie edytuje kopii.
6. **Przykłady kodu** w przewodnikach to prawdziwe pliki w `12_docs/examples/next/`: kod Scali kompiluje sbt z `-Werror`, a pliki konfiguracji wczytuje test. Strona wkleja je przy budowaniu, więc przykład, który się nie kompiluje, przerywa CI.
7. **Publikacja** idzie przez GitHub Actions na gałąź `gh-pages`. Przewodniki są budowane w całości przy każdej publikacji, a dokumentacja API każdej linii (`/api/<linia>/`) jest dopisywana przy wydaniu i nie jest kasowana przy publikacji przewodników.
8. **Zasady** pisania, tłumaczenia, wersjonowania i publikacji są w [docs/process/documentation.md](../process/documentation.md). CI sprawdza te z nich, które da się sprawdzić automatycznie.

## Konsekwencje

- Do budowania strony potrzebny jest Node.js (wersja w `12_docs/website/.nvmrc`). Do pracy nad kodem Coilix Node nie jest potrzebny.
- Zależności npm są tylko narzędziem budowania strony i nie trafiają do artefaktów Coilix, ale ich licencje i tak są sprawdzane, a Dependabot je aktualizuje.
- Każda zmiana przewodnika wymaga zmiany w obu językach w tym samym PR. To podwaja pracę przy tekstach, ale strona nigdy nie pokazuje mieszanki języków.
- Wersjonowanie przez Docusaurusa kopiuje przewodniki do `versioned_docs/` przy każdej nowej linii `MINOR`, więc repozytorium rośnie o jedną kopię na linię.
- Wyszukiwarka nie jest częścią pierwszej wersji strony. Wtyczki lokalne nie mają potwierdzonej obsługi polskiego, a Algolia DocSearch wymaga zgłoszenia projektu.
- Katalog `docs/` w katalogu głównym zostaje na dokumenty wewnętrzne (proces, ADR, sprawy prawne) i nigdy nie trafia na stronę.
