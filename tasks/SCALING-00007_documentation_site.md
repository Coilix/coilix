# SCALING-00007: Dokumentacja API i strona na GitHub Pages z wersjami

## Cel

Udostępnić dokumentację frameworka jako stronę na GitHub Pages: przewodniki dla programisty aplikacji i dokumentację API wygenerowaną ze Scaladoc. Każde wydanie ma własną, niezmienną wersję dokumentacji, a strona pozwala przełączać się między wersjami.

## Stan obecny

- `CLAUDE.md` wymaga Scaladoc na publicznym API (sekcja „Dokumentacja”), ale istniejący kod nie ma komentarzy, bo wcześniej obowiązywał zakaz Scaladoc.
- `sbt doc` działa: każdy moduł generuje osobną dokumentację w `target/out/jvm/<scala>/<moduł>/api`. Opcje pluginów kompilatora są odfiltrowane z opcji scaladoc, a javadoc dla `02_annotations` dostaje `-Xdoclint:all,-missing -Werror` zamiast opcji javac.
- Nie ma wspólnej strony dla wszystkich modułów, przewodników, remote'a na GitHubie ani CI.
- Wersja jest wpisana na sztywno w `build.sbt` (`0.1.0-SNAPSHOT`).
- `docs/` zawiera tylko historyczną specyfikację `AGENT_INTELLIJ_SLF4S_PLUGIN_SPEC.md`. SCALING-00006 planuje tam `docs/legal/`.

## Zakres

1. **Scaladoc dla istniejącego kodu** według zasad z `CLAUDE.md`, moduł po module: `01_common`, `04_config`, `05_context`, `09_boot`, `03_slf4s_plugin`, `10_demo`, oraz Javadoc dla adnotacji w `02_annotations`. Po tym kroku javadoc dostaje pełne `-Xdoclint:all` (z kontrolą brakujących komentarzy).
2. **Moduł `12_docs`** (projekt sbt `docs`), który zależy od modułów `01`–`10` i generuje jedną stronę Scaladoc dla nich wszystkich. Moduł nie ma kodu, tylko przewodniki w Markdownie.
3. **Przewodniki** w `12_docs/site/_docs/`: wprowadzenie, pierwsza aplikacja, komponenty i wstrzykiwanie zależności, konfiguracja (`@Value`, `@ConfigurationProperties`, nadpisywanie komponentów kluczem `class`), `@Slf4s` i plugin IntelliJ. Przykłady w blokach ` ```scala sc:compile `, żeby kompilator je sprawdzał.
4. **Wersjonowanie dokumentacji**: przełącznik wersji w Scaladoc i układ katalogów na gałęzi `gh-pages`. Wersja projektu z tagu gita pochodzi z SCALING-00009.
5. **CI w GitHub Actions**: budowanie i publikacja dokumentacji wydania oraz bieżącej wersji z `master`.

## Proponowany projekt

### Moduł `12_docs`

Numer 12 jest wyższy niż numery wszystkich modułów, od których zależy, więc spełnia zasadę z `CLAUDE.md`. Nie zależy od `11_intellij_plugin` (Scala 2.13, wewnętrzny plugin IDE).

Do sprawdzenia w pierwszej kolejności: czy sbt-unidoc działa z sbt 2 i Scalą 3. Jeśli nie, `12_docs` sam składa wejście dla scaladoc: pliki TASTy wszystkich zależnych modułów i ich classpath. Javadoc z `02_annotations` publikujemy jako osobny katalog `javadoc/`, do którego Scaladoc linkuje przez `-external-mappings`.

Opcje scaladoc dla `12_docs`:

- `-project Scaling`, `-project-version <wersja>`,
- `-siteroot 12_docs/site`,
- `-source-links:github://<owner>/scaling/<tag lub master>` dla linków „source” przy każdym elemencie,
- `-social-links:github::https://github.com/<owner>/scaling`,
- `-versions-dictionary-url https://<owner>.github.io/scaling/versions.json` dla przełącznika wersji.

### Wersja projektu

Wersję z tagu gita wprowadza SCALING-00009 ([versioning.md](../docs/architecture/versioning.md)). Ten task tylko jej używa.

### Układ gałęzi `gh-pages`

```
gh-pages/
  index.html          przekierowanie do latest/
  versions.json       {"versions": {"snapshot": ".../snapshot/", "0.2.0": ".../0.2.0/", "0.1.0": ".../0.1.0/"}}
  latest/             kopia najnowszego wydania
  snapshot/           dokumentacja z master, nadpisywana przy każdym pushu
  0.1.0/
  0.2.0/
    index.html        strona główna Scaladoc z przewodnikami
    api/...
    javadoc/...
```

Katalogi wydań się nie zmieniają. Stara dokumentacja zostaje taka, jaka była w dniu wydania, a `versions.json` jest jedynym plikiem, który zmienia się przy każdym wydaniu. Strony starszych wersji czytają go przy otwarciu, więc pokazują w przełączniku także nowsze wydania.

### CI

`.github/workflows/docs.yml`:

- push do `master` → `sbt docs/doc` → publikacja do `snapshot/`,
- tag `v*` → `sbt docs/doc` → publikacja do `<wersja>/` i `latest/`, dopisanie wersji do `versions.json`,
- publikacja przez `peaceiris/actions-gh-pages` z `destination_dir` i `keep_files: true`, żeby nie kasować innych wersji,
- krok sprawdzający, czy log `sbt docs/doc` nie zawiera `Couldn't resolve a member` (scaladoc nie przerywa builda przy zepsutych linkach).

## Kryteria akceptacji

1. Każdy publiczny element w `src/main` modułów `01`–`10` ma Scaladoc albo Javadoc zgodny z `CLAUDE.md`.
2. `sbt docs/doc` generuje jedną stronę ze wszystkimi modułami, przewodnikami i bez ostrzeżeń o nierozwiązanych linkach.
3. Przykłady w przewodnikach kompilują się podczas `sbt docs/doc`.
4. Katalog wersji na `gh-pages` odpowiada wersji z tagu gita (SCALING-00009).
5. Push na `master` aktualizuje `snapshot/`, a tag `v*` tworzy nowy katalog wersji, aktualizuje `latest/` i `versions.json`.
6. Przełącznik wersji na stronie pokazuje wszystkie wydania i `snapshot`.
7. `CLAUDE.md` opisuje moduł `12_docs` (tabela modułów, komendy).

## Poza zakresem

- Własna domena, wyszukiwarka pełnotekstowa poza wbudowaną w Scaladoc i tłumaczenie dokumentacji na polski.
- Publikacja artefaktów na Maven Central (osobny task po SCALING-00006).

## Ryzyka i otwarte pytania

- Repozytorium nie ma jeszcze remote'a na GitHubie. GitHub Pages dla repozytorium prywatnego wymaga płatnego planu, więc publikacja strony praktycznie czeka na decyzję o upublicznieniu z SCALING-00006.
- Zgodność sbt-unidoc i sbt-dynver z sbt 2 jest niepotwierdzona.
- `docs/` w katalogu głównym zostaje na dokumenty wewnętrzne (`docs/legal/`, specyfikacje). Strona publiczna powstaje tylko z `12_docs/site`, żeby nic wewnętrznego nie trafiło na GitHub Pages.
- Przy wydaniu poprawkowym starszej linii (np. `0.1.1` po `0.2.0`) `latest/` nie może cofnąć się do starszej wersji. Workflow musi porównywać wersje, a nie brać ostatniego tagu.
